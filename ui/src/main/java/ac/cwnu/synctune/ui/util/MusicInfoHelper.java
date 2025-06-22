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
 * 음악 파일의 메타데이터를 추출하고 MusicInfo 객체를 생성하는 헬퍼 클래스
 */
public class MusicInfoHelper {
    private static final Logger log = LoggerFactory.getLogger(MusicInfoHelper.class);
    
    /**
     * 파일에서 MusicInfo 객체 생성 (실제 재생 시간 계산 포함)
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
        
        // LRC 파일 찾기
        String lrcPath = findLrcFile(file);
        
        log.debug("MusicInfo 생성: {} - {} ({}ms)", artist, title, duration);
        
        return new MusicInfo(title, artist, album, file.getAbsolutePath(), duration, lrcPath);
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
     * LRC 파일 찾기
     */
    private static String findLrcFile(File musicFile) {
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            
            // 1. 같은 디렉토리에서 찾기
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
            // 2. lyrics 폴더에서 찾기
            File lyricsDir = new File(musicFile.getParent(), "lyrics");
            if (lyricsDir.exists()) {
                File lrcInLyricsDir = new File(lyricsDir, baseName + ".lrc");
                if (lrcInLyricsDir.exists()) {
                    return lrcInLyricsDir.getAbsolutePath();
                }
            }
            
            // 3. 루트 lyrics 폴더에서 찾기
            File rootLyricsDir = new File("lyrics");
            if (rootLyricsDir.exists()) {
                File lrcInRootLyrics = new File(rootLyricsDir, baseName + ".lrc");
                if (lrcInRootLyrics.exists()) {
                    return lrcInRootLyrics.getAbsolutePath();
                }
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 중 오류: {}", musicFile.getName(), e);
        }
        
        return null;
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