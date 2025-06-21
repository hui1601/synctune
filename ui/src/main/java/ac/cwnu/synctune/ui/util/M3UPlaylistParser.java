package ac.cwnu.synctune.ui.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * M3U 플레이리스트 파일 파싱 및 생성 유틸리티
 */
public class M3UPlaylistParser {
    private static final Logger log = LoggerFactory.getLogger(M3UPlaylistParser.class);
    
    // #EXTINF:120,Artist - Title 형식의 패턴
    private static final Pattern EXTINF_PATTERN = Pattern.compile(
        "#EXTINF:(-?\\d+),(.+?)(?:\\s-\\s(.+))?$"
    );
    
    /**
     * M3U 파일 파싱 결과
     */
    public static class M3UPlaylist {
        private final String name;
        private final List<MusicInfo> songs;
        private final String originalPath;
        
        public M3UPlaylist(String name, List<MusicInfo> songs, String originalPath) {
            this.name = name;
            this.songs = new ArrayList<>(songs);
            this.originalPath = originalPath;
        }
        
        public String getName() { return name; }
        public List<MusicInfo> getSongs() { return new ArrayList<>(songs); }
        public String getOriginalPath() { return originalPath; }
        public int size() { return songs.size(); }
    }
    
    /**
     * 파싱 중 발생한 문제점들을 담는 클래스
     */
    public static class ParseResult {
        private final M3UPlaylist playlist;
        private final List<String> warnings;
        private final List<String> errors;
        private final int totalLines;
        private final int parsedSongs;
        
        public ParseResult(M3UPlaylist playlist, List<String> warnings, List<String> errors, 
                          int totalLines, int parsedSongs) {
            this.playlist = playlist;
            this.warnings = new ArrayList<>(warnings);
            this.errors = new ArrayList<>(errors);
            this.totalLines = totalLines;
            this.parsedSongs = parsedSongs;
        }
        
        public M3UPlaylist getPlaylist() { return playlist; }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public int getTotalLines() { return totalLines; }
        public int getParsedSongs() { return parsedSongs; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean isSuccess() { return playlist != null; }
    }
    
    /**
     * M3U 파일을 파싱합니다
     */
    public static ParseResult parseM3UFile(File m3uFile) {
        if (m3uFile == null || !m3uFile.exists()) {
            List<String> errors = List.of("M3U 파일이 존재하지 않습니다: " + 
                                        (m3uFile != null ? m3uFile.getAbsolutePath() : "null"));
            return new ParseResult(null, List.of(), errors, 0, 0);
        }
        
        List<MusicInfo> songs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        String playlistName = getFileNameWithoutExtension(m3uFile.getName());
        String basePath = m3uFile.getParent();
        
        int totalLines = 0;
        int parsedSongs = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(m3uFile), "UTF-8"))) {
            
            String line;
            String currentExtinf = null;
            boolean isExtendedM3U = false;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                line = line.trim();
                
                if (line.isEmpty()) {
                    continue;
                }
                
                if (line.startsWith("#EXTM3U")) {
                    isExtendedM3U = true;
                    log.debug("확장 M3U 형식 감지");
                    continue;
                }
                
                if (line.startsWith("#PLAYLIST:")) {
                    String extractedName = line.substring("#PLAYLIST:".length()).trim();
                    if (!extractedName.isEmpty()) {
                        playlistName = extractedName;
                    }
                    continue;
                }
                
                if (line.startsWith("#EXTINF:")) {
                    currentExtinf = line;
                    continue;
                }
                
                if (line.startsWith("#")) {
                    // 기타 주석은 무시
                    continue;
                }
                
                // 음악 파일 경로 처리
                MusicInfo musicInfo = parseMusicPath(line, currentExtinf, basePath, warnings);
                if (musicInfo != null) {
                    songs.add(musicInfo);
                    parsedSongs++;
                } else {
                    errors.add("파싱 실패: " + line);
                }
                
                currentExtinf = null; // 사용된 EXTINF 정보 초기화
            }
            
            if (!isExtendedM3U && songs.size() > 0) {
                warnings.add("단순 M3U 형식입니다. 메타데이터 정보가 제한적일 수 있습니다.");
            }
            
        } catch (IOException e) {
            log.error("M3U 파일 읽기 실패: {}", m3uFile.getAbsolutePath(), e);
            errors.add("파일 읽기 오류: " + e.getMessage());
        }
        
        M3UPlaylist playlist = songs.isEmpty() ? null : 
            new M3UPlaylist(playlistName, songs, m3uFile.getAbsolutePath());
        
        return new ParseResult(playlist, warnings, errors, totalLines, parsedSongs);
    }
    
    /**
     * 음악 파일 경로와 메타데이터를 파싱하여 MusicInfo 생성
     */
    private static MusicInfo parseMusicPath(String path, String extinf, String basePath, 
                                          List<String> warnings) {
        try {
            // 절대 경로인지 확인
            File musicFile = new File(path);
            if (!musicFile.isAbsolute()) {
                // 상대 경로인 경우 M3U 파일 위치 기준으로 처리
                musicFile = new File(basePath, path);
            }
            
            String actualPath = musicFile.getAbsolutePath();
            String title = getFileNameWithoutExtension(musicFile.getName());
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            long duration = 0;
            
            // EXTINF 정보 파싱
            if (extinf != null) {
                Matcher matcher = EXTINF_PATTERN.matcher(extinf);
                if (matcher.matches()) {
                    try {
                        duration = Long.parseLong(matcher.group(1)) * 1000; // 초를 밀리초로
                        if (duration < 0) duration = 0; // 음수 duration 처리
                    } catch (NumberFormatException e) {
                        warnings.add("잘못된 재생시간 형식: " + extinf);
                    }
                    
                    String info = matcher.group(2);
                    if (info != null && !info.isEmpty()) {
                        // "Artist - Title" 형식 파싱
                        if (info.contains(" - ")) {
                            String[] parts = info.split(" - ", 2);
                            artist = parts[0].trim();
                            title = parts[1].trim();
                        } else {
                            title = info.trim();
                        }
                    }
                }
            }
            
            // 파일이 존재하지 않으면 경고
            if (!musicFile.exists()) {
                warnings.add("파일을 찾을 수 없음: " + actualPath);
            }
            
            // LRC 파일 찾기
            String lrcPath = findLrcFile(musicFile);
            
            return new MusicInfo(title, artist, album, actualPath, duration, lrcPath);
            
        } catch (Exception e) {
            log.error("음악 파일 경로 파싱 실패: {}", path, e);
            return null;
        }
    }
    
    /**
     * 플레이리스트를 M3U 파일로 저장
     */
    public static boolean saveAsM3U(File outputFile, String playlistName, List<MusicInfo> songs, 
                                   boolean useRelativePaths) {
        if (outputFile == null || songs == null) {
            return false;
        }
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
            
            // M3U 헤더
            writer.write("#EXTM3U\n");
            if (playlistName != null && !playlistName.isEmpty()) {
                writer.write("#PLAYLIST:" + playlistName + "\n");
            }
            writer.write("\n");
            
            String basePath = outputFile.getParent();
            
            for (MusicInfo music : songs) {
                // EXTINF 라인
                long durationSeconds = music.getDurationMillis() / 1000;
                String artistTitle = music.getArtist().equals("Unknown Artist") ? 
                    music.getTitle() : music.getArtist() + " - " + music.getTitle();
                
                writer.write(String.format("#EXTINF:%d,%s\n", durationSeconds, artistTitle));
                
                // 파일 경로
                String filePath = music.getFilePath();
                if (useRelativePaths) {
                    filePath = getRelativePath(basePath, filePath);
                }
                writer.write(filePath + "\n");
            }
            
            log.info("M3U 플레이리스트 저장 완료: {} ({}곡)", outputFile.getAbsolutePath(), songs.size());
            return true;
            
        } catch (IOException e) {
            log.error("M3U 파일 저장 실패: {}", outputFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 상대 경로 계산
     */
    private static String getRelativePath(String basePath, String targetPath) {
        try {
            Path base = Paths.get(basePath).normalize();
            Path target = Paths.get(targetPath).normalize();
            return base.relativize(target).toString();
        } catch (Exception e) {
            // 상대 경로 계산 실패 시 절대 경로 반환
            return targetPath;
        }
    }
    
    /**
     * LRC 파일 찾기
     */
    private static String findLrcFile(File musicFile) {
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            
            // 같은 디렉토리에서 찾기
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
            // lyrics 폴더에서 찾기
            File lyricsDir = new File(musicFile.getParent(), "lyrics");
            if (lyricsDir.exists()) {
                File lrcInLyricsDir = new File(lyricsDir, baseName + ".lrc");
                if (lrcInLyricsDir.exists()) {
                    return lrcInLyricsDir.getAbsolutePath();
                }
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 실패: {}", musicFile.getName());
        }
        
        return null;
    }
    
    /**
     * 파일명에서 확장자 제거
     */
    private static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * M3U 파일 유효성 검사
     */
    public static boolean isValidM3UFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".m3u") && !fileName.endsWith(".m3u8")) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            // 첫 줄이 #EXTM3U이거나 유효한 파일 경로면 M3U 파일로 간주
            return firstLine != null && 
                   (firstLine.trim().startsWith("#EXTM3U") || 
                    !firstLine.trim().startsWith("#"));
        } catch (IOException e) {
            return false;
        }
    }
} 