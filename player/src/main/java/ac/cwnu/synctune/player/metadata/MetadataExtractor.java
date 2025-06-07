package ac.cwnu.synctune.player.metadata;

import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 음악 파일에서 ID3 태그 등 메타데이터 정보를 추출하는 클래스
 * 현재는 기본적인 파일 정보만 추출하며, 향후 ID3 라이브러리 추가 시 확장 예정
 */
public class MetadataExtractor {
    private static final Logger log = LogManager.getLogger(MetadataExtractor.class);
    
    // 추출된 메타데이터 캐시 (파일 경로 -> 메타데이터)
    private final Map<String, ExtractedMetadata> metadataCache = new ConcurrentHashMap<>();
    
    // 지원되는 오디오 파일 확장자들
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg", ".wma"
    );
    
    /**
     * 추출된 메타데이터를 담는 클래스
     */
    public static class ExtractedMetadata {
        private final String title;
        private final String artist;
        private final String album;
        private final String genre;
        private final Integer year;
        private final Integer trackNumber;
        private final Long durationMillis;
        private final Long fileSizeBytes;
        private final String format;
        private final Integer bitrate;
        private final Integer sampleRate;
        private final long extractionTime;
        
        public ExtractedMetadata(String title, String artist, String album, String genre,
                               Integer year, Integer trackNumber, Long durationMillis,
                               Long fileSizeBytes, String format, Integer bitrate, Integer sampleRate) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.genre = genre;
            this.year = year;
            this.trackNumber = trackNumber;
            this.durationMillis = durationMillis;
            this.fileSizeBytes = fileSizeBytes;
            this.format = format;
            this.bitrate = bitrate;
            this.sampleRate = sampleRate;
            this.extractionTime = System.currentTimeMillis();
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public String getGenre() { return genre; }
        public Integer getYear() { return year; }
        public Integer getTrackNumber() { return trackNumber; }
        public Long getDurationMillis() { return durationMillis; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public String getFormat() { return format; }
        public Integer getBitrate() { return bitrate; }
        public Integer getSampleRate() { return sampleRate; }
        public long getExtractionTime() { return extractionTime; }
        
        @Override
        public String toString() {
            return String.format("Metadata{title='%s', artist='%s', album='%s', duration=%dms}",
                title, artist, album, durationMillis);
        }
    }
    
    /**
     * 음악 파일에서 메타데이터를 비동기로 추출합니다
     */
    public CompletableFuture<ExtractedMetadata> extractMetadataAsync(File musicFile) {
        return CompletableFuture.supplyAsync(() -> extractMetadata(musicFile));
    }
    
    /**
     * 음악 파일에서 메타데이터를 추출합니다
     */
    public ExtractedMetadata extractMetadata(File musicFile) {
        if (musicFile == null || !musicFile.exists() || !musicFile.isFile()) {
            log.warn("유효하지 않은 파일: {}", musicFile);
            return createEmptyMetadata();
        }
        
        String absolutePath = musicFile.getAbsolutePath();
        
        // 캐시 확인
        ExtractedMetadata cached = metadataCache.get(absolutePath);
        if (cached != null && isMetadataValid(cached, musicFile)) {
            log.debug("캐시된 메타데이터 반환: {}", musicFile.getName());
            return cached;
        }
        
        try {
            log.debug("메타데이터 추출 시작: {}", musicFile.getName());
            
            ExtractedMetadata metadata = performExtraction(musicFile);
            
            // 캐시에 저장
            metadataCache.put(absolutePath, metadata);
            
            log.debug("메타데이터 추출 완료: {}", metadata);
            return metadata;
            
        } catch (Exception e) {
            log.error("메타데이터 추출 중 오류: {}", musicFile.getName(), e);
            return createErrorMetadata(musicFile);
        }
    }
    
    /**
     * 여러 파일의 메타데이터를 동시에 추출합니다
     */
    public CompletableFuture<Map<String, ExtractedMetadata>> extractMetadataBatch(List<File> musicFiles) {
        List<CompletableFuture<Map.Entry<String, ExtractedMetadata>>> futures = musicFiles.stream()
            .map(file -> extractMetadataAsync(file)
                .thenApply(metadata -> Map.entry(file.getAbsolutePath(), metadata)))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, ExtractedMetadata> results = new HashMap<>();
                futures.forEach(future -> {
                    try {
                        Map.Entry<String, ExtractedMetadata> entry = future.get();
                        results.put(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        log.error("배치 메타데이터 추출 중 오류", e);
                    }
                });
                return results;
            });
    }
    
    /**
     * MusicInfo 객체를 생성합니다
     */
    public MusicInfo createMusicInfo(File musicFile, ExtractedMetadata metadata) {
        String title = metadata.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = getFileNameWithoutExtension(musicFile.getName());
        }
        
        String artist = metadata.getArtist();
        if (artist == null || artist.trim().isEmpty()) {
            artist = "Unknown Artist";
        }
        
        String album = metadata.getAlbum();
        if (album == null || album.trim().isEmpty()) {
            album = "Unknown Album";
        }
        
        long duration = metadata.getDurationMillis() != null ? metadata.getDurationMillis() : 0L;
        
        // TODO: LRC 파일 경로 찾기 (향후 구현)
        String lrcPath = findLrcFile(musicFile);
        
        return new MusicInfo(title, artist, album, musicFile.getAbsolutePath(), duration, lrcPath);
    }
    
    /**
     * 파일이 지원되는 오디오 파일인지 확인합니다
     */
    public static boolean isSupportedAudioFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
    
    /**
     * 캐시된 메타데이터를 모두 지웁니다
     */
    public void clearCache() {
        metadataCache.clear();
        log.debug("메타데이터 캐시 클리어됨");
    }
    
    /**
     * 특정 파일의 캐시된 메타데이터를 제거합니다
     */
    public void removeCachedMetadata(String filePath) {
        metadataCache.remove(filePath);
        log.debug("캐시된 메타데이터 제거: {}", filePath);
    }
    
    /**
     * 캐시 상태를 반환합니다
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(metadataCache.size(), metadataCache.keySet());
    }
    
    // ========== Private 메서드들 ==========
    
    private ExtractedMetadata performExtraction(File musicFile) {
        try {
            // 기본 파일 정보
            long fileSize = musicFile.length();
            String format = getFileExtension(musicFile.getName()).toUpperCase();
            
            // 파일명에서 기본 정보 추출
            String fileName = getFileNameWithoutExtension(musicFile.getName());
            String title = extractTitleFromFileName(fileName);
            String artist = extractArtistFromFileName(fileName);
            
            // 오디오 파일 포맷 정보 (Java 기본 지원 범위 내에서)
            Long duration = null;
            Integer sampleRate = null;
            
            try {
                AudioFileFormat audioFormat = AudioSystem.getAudioFileFormat(musicFile);
                
                // 재생 시간 계산 (프레임 수 기반)
                if (audioFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED &&
                    audioFormat.getFormat().getFrameRate() != AudioSystem.NOT_SPECIFIED) {
                    duration = (long) (audioFormat.getFrameLength() / audioFormat.getFormat().getFrameRate() * 1000);
                }
                
                // 샘플 레이트
                if (audioFormat.getFormat().getSampleRate() != AudioSystem.NOT_SPECIFIED) {
                    sampleRate = (int) audioFormat.getFormat().getSampleRate();
                }
                
            } catch (Exception e) {
                log.debug("오디오 포맷 정보 추출 실패: {} ({})", musicFile.getName(), e.getMessage());
            }
            
            // TODO: ID3 태그 추출은 향후 전용 라이브러리 추가 시 구현
            // 현재는 파일명 기반으로만 정보 추출
            
            return new ExtractedMetadata(
                title,           // 제목
                artist,          // 아티스트
                null,            // 앨범 (파일명에서 추출 어려움)
                null,            // 장르
                null,            // 연도
                null,            // 트랙 번호
                duration,        // 재생 시간
                fileSize,        // 파일 크기
                format,          // 파일 형식
                null,            // 비트레이트 (ID3 태그 필요)
                sampleRate       // 샘플 레이트
            );
            
        } catch (Exception e) {
            log.error("메타데이터 추출 중 예외 발생: {}", musicFile.getName(), e);
            return createErrorMetadata(musicFile);
        }
    }
    
    private boolean isMetadataValid(ExtractedMetadata metadata, File musicFile) {
        // 파일 수정 시간 기반 캐시 유효성 검사
        try {
            long fileModified = musicFile.lastModified();
            long extractionTime = metadata.getExtractionTime();
            
            // 파일이 메타데이터 추출 이후에 수정되었으면 무효
            return fileModified <= extractionTime;
            
        } catch (Exception e) {
            log.debug("캐시 유효성 검사 실패: {}", musicFile.getName());
            return false;
        }
    }
    
    private ExtractedMetadata createEmptyMetadata() {
        return new ExtractedMetadata(
            null, null, null, null, null, null, 0L, 0L, "UNKNOWN", null, null
        );
    }
    
    private ExtractedMetadata createErrorMetadata(File musicFile) {
        String fileName = getFileNameWithoutExtension(musicFile.getName());
        return new ExtractedMetadata(
            fileName,                                    // 파일명을 제목으로
            "Unknown Artist",                           // 기본 아티스트
            "Unknown Album",                            // 기본 앨범
            null, null, null,                          // 장르, 연도, 트랙번호
            0L,                                        // 재생시간 불명
            musicFile.length(),                        // 파일 크기
            getFileExtension(musicFile.getName()).toUpperCase(), // 파일 형식
            null, null                                 // 비트레이트, 샘플레이트
        );
    }
    
    private String extractTitleFromFileName(String fileName) {
        // 파일명에서 제목 추출 로직
        // 예: "01 - Artist - Title.mp3" -> "Title"
        // 예: "Artist - Title.mp3" -> "Title"
        
        String[] parts = fileName.split(" - ");
        if (parts.length >= 2) {
            return parts[parts.length - 1].trim(); // 마지막 부분을 제목으로
        }
        
        // 트랙 번호 제거 (예: "01. Title" -> "Title")
        String withoutTrackNumber = fileName.replaceFirst("^\\d+\\.?\\s*", "");
        
        return withoutTrackNumber.isEmpty() ? fileName : withoutTrackNumber;
    }
    
    private String extractArtistFromFileName(String fileName) {
        // 파일명에서 아티스트 추출 로직
        // 예: "Artist - Title.mp3" -> "Artist"
        
        String[] parts = fileName.split(" - ");
        if (parts.length >= 2) {
            String firstPart = parts[0].trim();
            // 트랙 번호 제거
            return firstPart.replaceFirst("^\\d+\\.?\\s*", "");
        }
        
        return null; // 아티스트 정보 없음
    }
    
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }
    
    private String findLrcFile(File musicFile) {
        // 같은 디렉토리에서 동일한 이름의 .lrc 파일 찾기
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 실패: {}", musicFile.getName());
        }
        
        return null;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * 캐시 통계 정보
     */
    public static class CacheStatistics {
        private final int cacheSize;
        private final Set<String> cachedFiles;
        
        public CacheStatistics(int cacheSize, Set<String> cachedFiles) {
            this.cacheSize = cacheSize;
            this.cachedFiles = new HashSet<>(cachedFiles);
        }
        
        public int getCacheSize() { return cacheSize; }
        public Set<String> getCachedFiles() { return new HashSet<>(cachedFiles); }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{size=%d}", cacheSize);
        }
    }
}