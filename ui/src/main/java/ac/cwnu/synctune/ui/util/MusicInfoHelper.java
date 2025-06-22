package ac.cwnu.synctune.ui.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * 음악 파일의 메타데이터를 추출하고 MusicInfo 객체를 생성하는 헬퍼 클래스 (개선된 버전)
 */
public class MusicInfoHelper {
    private static final Logger log = LoggerFactory.getLogger(MusicInfoHelper.class);
    
    /**
     * 파일에서 MusicInfo 객체 생성 (개선된 LRC 파일 찾기 포함)
     */
    public static MusicInfo createFromFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("유효하지 않은 파일: " + file);
        }
        
        // 기본 정보 추출
        String fileName = file.getName();
        String baseName = getFileNameWithoutExtension(fileName);
        
        String title = baseName;
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        
        // 파일명에서 아티스트-제목 파싱
        String[] parsed = parseArtistAndTitle(baseName);
        artist = parsed[0];
        title = parsed[1];
        
        // 실제 재생 시간 계산
        long duration = calculateActualDuration(file);
        
        // LRC 파일 찾기 (개선된 버전)
        String lrcPath = findBestMatchingLrcFile(file);
        
        log.debug("MusicInfo 생성: {} - {} ({}ms) [LRC: {}]", 
            artist, title, duration, lrcPath != null ? "있음" : "없음");
        
        return new MusicInfo(title, artist, album, file.getAbsolutePath(), duration, lrcPath);
    }
    
    /**
     * 개선된 LRC 파일 찾기 메서드
     */
    private static String findBestMatchingLrcFile(File musicFile) {
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            String parentDir = musicFile.getParent();
            
            // 1. 정확한 이름 매칭 우선
            String[] exactMatchPaths = {
                parentDir + File.separator + baseName + ".lrc",
                parentDir + File.separator + baseName + ".LRC",
                parentDir + File.separator + baseName + ".Lrc",
                parentDir + File.separator + "lyrics" + File.separator + baseName + ".lrc",
                "lyrics" + File.separator + baseName + ".lrc"
            };
            
            for (String path : exactMatchPaths) {
                File lrcFile = new File(path);
                if (lrcFile.exists() && lrcFile.isFile()) {
                    log.debug("정확한 LRC 매칭: {} -> {}", musicFile.getName(), lrcFile.getName());
                    return lrcFile.getAbsolutePath();
                }
            }
            
            // 2. 디렉토리 내 모든 LRC 파일 중에서 가장 유사한 것 찾기
            File parentDirectory = new File(parentDir);
            File[] lrcFiles = parentDirectory.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".lrc"));
            
            if (lrcFiles != null && lrcFiles.length > 0) {
                File bestMatch = findBestSimilarLrcFile(musicFile, lrcFiles);
                if (bestMatch != null) {
                    log.debug("유사한 LRC 매칭: {} -> {}", musicFile.getName(), bestMatch.getName());
                    return bestMatch.getAbsolutePath();
                }
            }
            
            // 3. lyrics 서브폴더에서도 검색
            File lyricsSubDir = new File(parentDir, "lyrics");
            if (lyricsSubDir.exists() && lyricsSubDir.isDirectory()) {
                File[] lyricsSubFiles = lyricsSubDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".lrc"));
                
                if (lyricsSubFiles != null && lyricsSubFiles.length > 0) {
                    File bestMatch = findBestSimilarLrcFile(musicFile, lyricsSubFiles);
                    if (bestMatch != null) {
                        log.debug("lyrics 폴더에서 유사한 LRC 매칭: {} -> {}", 
                            musicFile.getName(), bestMatch.getName());
                        return bestMatch.getAbsolutePath();
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 중 오류: {}", musicFile.getName(), e);
        }
        
        return null;
    }
    
    /**
     * 가장 유사한 LRC 파일 찾기
     */
    private static File findBestSimilarLrcFile(File musicFile, File[] lrcFiles) {
        String musicBaseName = getFileNameWithoutExtension(musicFile.getName()).toLowerCase();
        
        // 특수 문자 제거하고 정규화
        String normalizedMusicName = normalizeFileName(musicBaseName);
        
        File bestMatch = null;
        double bestSimilarity = 0.0;
        
        for (File lrcFile : lrcFiles) {
            String lrcBaseName = getFileNameWithoutExtension(lrcFile.getName()).toLowerCase();
            String normalizedLrcName = normalizeFileName(lrcBaseName);
            
            // 1. 완전 일치 확인
            if (normalizedMusicName.equals(normalizedLrcName)) {
                return lrcFile;
            }
            
            // 2. 유사도 계산
            double similarity = calculateAdvancedSimilarity(normalizedMusicName, normalizedLrcName);
            
            // 3. 70% 이상 유사하면 후보로 고려
            if (similarity > 0.7 && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = lrcFile;
            }
        }
        
        // 최소 75% 이상 유사해야 매칭으로 인정
        return bestSimilarity > 0.75 ? bestMatch : null;
    }
    
    /**
     * 파일명 정규화 (특수문자 제거, 공백 처리 등)
     */
    private static String normalizeFileName(String fileName) {
        return fileName
            .replaceAll("[\\[\\](){}]", "")  // 괄호 제거
            .replaceAll("[_\\-]", " ")       // 언더스코어, 하이픈을 공백으로
            .replaceAll("\\s+", " ")         // 연속 공백을 하나로
            .replaceAll("^\\d+\\.?\\s*", "") // 앞의 숫자 제거 (트랙 번호)
            .trim();
    }
    
    /**
     * 개선된 유사도 계산 (토큰 기반 + Levenshtein)
     */
    private static double calculateAdvancedSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        // 1. 토큰 기반 유사도 (단어별 매칭)
        String[] tokens1 = s1.split("\\s+");
        String[] tokens2 = s2.split("\\s+");
        
        double tokenSimilarity = calculateTokenSimilarity(tokens1, tokens2);
        
        // 2. Levenshtein 기반 유사도
        double editSimilarity = calculateEditSimilarity(s1, s2);
        
        // 3. 포함 관계 확인
        double containmentSimilarity = calculateContainmentSimilarity(s1, s2);
        
        // 가중 평균으로 최종 유사도 계산
        return (tokenSimilarity * 0.5) + (editSimilarity * 0.3) + (containmentSimilarity * 0.2);
    }
    
    private static double calculateTokenSimilarity(String[] tokens1, String[] tokens2) {
        if (tokens1.length == 0 && tokens2.length == 0) return 1.0;
        if (tokens1.length == 0 || tokens2.length == 0) return 0.0;
        
        int matchCount = 0;
        int totalTokens = Math.max(tokens1.length, tokens2.length);
        
        for (String token1 : tokens1) {
            for (String token2 : tokens2) {
                if (token1.length() > 2 && token2.length() > 2 && 
                    (token1.contains(token2) || token2.contains(token1))) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return (double) matchCount / totalTokens;
    }
    
    private static double calculateEditSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }
    
    private static double calculateContainmentSimilarity(String s1, String s2) {
        if (s1.length() < 3 || s2.length() < 3) return 0.0;
        
        String longer = s1.length() > s2.length() ? s1 : s2;
        String shorter = s1.length() > s2.length() ? s2 : s1;
        
        return longer.contains(shorter) ? 0.8 : 0.0;
    }
    
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 실제 오디오 파일의 재생 시간을 정확히 계산
     */
    public static long calculateActualDuration(File audioFile) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioFile);
            
            // 파일 포맷에서 재생 시간 정보 추출 시도
            if (fileFormat.properties().containsKey("duration")) {
                Long durationMicros = (Long) fileFormat.properties().get("duration");
                return durationMicros / 1000; // 마이크로초를 밀리초로 변환
            }
            
            // AudioInputStream을 사용한 계산
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
                AudioFormat format = audioStream.getFormat();
                long frameLength = audioStream.getFrameLength();
                
                if (frameLength != AudioSystem.NOT_SPECIFIED && format.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
                    double durationSeconds = frameLength / format.getFrameRate();
                    long durationMs = (long) (durationSeconds * 1000);
                    
                    log.debug("오디오 스트림에서 재생시간 계산: {}초 ({}ms)", durationSeconds, durationMs);
                    return durationMs;
                }
            }
            
            // 파일 크기 기반 추정 (최후의 수단)
            return estimateDurationFromFileSize(audioFile);
            
        } catch (UnsupportedAudioFileException e) {
            log.warn("지원되지 않는 오디오 파일 형식: {} - 파일 크기 기반 추정 사용", audioFile.getName());
            return estimateDurationFromFileSize(audioFile);
        } catch (IOException e) {
            log.error("오디오 파일 읽기 오류: {} - 기본값 사용", audioFile.getName(), e);
            return estimateDurationFromFileSize(audioFile);
        } catch (Exception e) {
            log.error("재생시간 계산 중 예상치 못한 오류: {} - 기본값 사용", audioFile.getName(), e);
            return estimateDurationFromFileSize(audioFile);
        }
    }
    
    /**
     * 파일 크기 기반 재생 시간 추정
     */
    private static long estimateDurationFromFileSize(File file) {
        long fileSize = file.length();
        String fileName = file.getName().toLowerCase();
        
        long estimatedMs;
        
        if (fileName.endsWith(".mp3")) {
            // MP3: 평균 128kbps 가정
            estimatedMs = (fileSize * 8) / (128 * 1000) * 1000;
        } else if (fileName.endsWith(".wav")) {
            // WAV: 16bit 44.1kHz 스테레오 = 1411.2 kbps
            estimatedMs = (fileSize * 8) / (1411 * 1000) * 1000;
        } else if (fileName.endsWith(".flac")) {
            // FLAC: 대략 800kbps 가정 (가변)
            estimatedMs = (fileSize * 8) / (800 * 1000) * 1000;
        } else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
            // AAC: 평균 128kbps 가정
            estimatedMs = (fileSize * 8) / (128 * 1000) * 1000;
        } else if (fileName.endsWith(".ogg")) {
            // OGG: 평균 160kbps 가정
            estimatedMs = (fileSize * 8) / (160 * 1000) * 1000;
        } else {
            // 알 수 없는 형식: 3분 기본값
            estimatedMs = 180000L;
        }
        
        // 최소 30초, 최대 2시간으로 제한
        estimatedMs = Math.max(30000L, Math.min(estimatedMs, 7200000L));
        
        log.debug("파일 크기 기반 재생시간 추정: {} -> {}ms", formatFileSize(fileSize), estimatedMs);
        return estimatedMs;
    }
    
    /**
     * 파일명에서 아티스트와 제목 파싱
     */
    private static String[] parseArtistAndTitle(String baseName) {
        String artist = "Unknown Artist";
        String title = baseName;
        
        // 트랙 번호 제거 (예: "01. " 또는 "01 - ")
        String cleanName = baseName.replaceFirst("^\\d+[.\\-]?\\s*", "");
        
        if (cleanName.contains(" - ")) {
            String[] parts = cleanName.split(" - ", 2);
            if (parts.length == 2) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        } else if (cleanName.contains("_")) {
            // 언더스코어로 구분된 경우
            String[] parts = cleanName.split("_", 2);
            if (parts.length == 2) {
                artist = parts[0].trim().replace("_", " ");
                title = parts[1].trim().replace("_", " ");
            }
        } else {
            title = cleanName;
        }
        
        return new String[]{artist, title};
    }
    
    /**
     * 파일 확장자 제거
     */
    private static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * 파일 크기를 읽기 쉬운 형식으로 포맷
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * 파일이 지원되는 오디오 파일인지 확인
     */
    public static boolean isSupportedAudioFile(File file) {
        if (file == null || !file.isFile()) return false;
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
               fileName.endsWith(".flac") || fileName.endsWith(".m4a") ||
               fileName.endsWith(".aac") || fileName.endsWith(".ogg") ||
               fileName.endsWith(".wma");
    }
    
    /**
     * 오디오 파일 형식 정보 가져오기
     */
    public static String getAudioFormatInfo(File audioFile) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioFile);
            AudioFormat format = fileFormat.getFormat();
            
            return String.format("%s, %.1f kHz, %d-bit, %s",
                fileFormat.getType().toString(),
                format.getSampleRate() / 1000.0,
                format.getSampleSizeInBits(),
                format.getChannels() == 1 ? "Mono" : "Stereo"
            );
        } catch (Exception e) {
            return "Unknown format";
        }
    }
}