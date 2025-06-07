package ac.cwnu.synctune.player.metadata;

import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 앨범 커버 이미지 로드/캐싱을 담당하는 서비스
 * 음악 파일과 같은 디렉토리에서 커버 이미지를 찾거나, 내장된 커버를 추출합니다.
 */
public class CoverArtService {
    private static final Logger log = LogManager.getLogger(CoverArtService.class);
    
    // 커버 이미지 캐시 (파일 경로 -> 이미지)
    private final Map<String, CachedCoverArt> coverCache = new ConcurrentHashMap<>();
    
    // 지원되는 이미지 파일 형식
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp"
    );
    
    // 일반적인 커버 이미지 파일명들
    private static final List<String> COVER_FILE_NAMES = List.of(
        "cover", "folder", "album", "front", "artwork", "thumb"
    );
    
    // 기본 커버 이미지 크기
    private static final int DEFAULT_COVER_SIZE = 300;
    private static final int THUMBNAIL_SIZE = 64;
    
    /**
     * 캐시된 커버 아트 정보
     */
    public static class CachedCoverArt {
        private final BufferedImage originalImage;
        private final BufferedImage thumbnailImage;
        private final String sourcePath;
        private final CoverSource source;
        private final long cacheTime;
        
        public CachedCoverArt(BufferedImage originalImage, BufferedImage thumbnailImage, 
                             String sourcePath, CoverSource source) {
            this.originalImage = originalImage;
            this.thumbnailImage = thumbnailImage;
            this.sourcePath = sourcePath;
            this.source = source;
            this.cacheTime = System.currentTimeMillis();
        }
        
        public BufferedImage getOriginalImage() { return originalImage; }
        public BufferedImage getThumbnailImage() { return thumbnailImage; }
        public String getSourcePath() { return sourcePath; }
        public CoverSource getSource() { return source; }
        public long getCacheTime() { return cacheTime; }
        
        public int getWidth() { return originalImage != null ? originalImage.getWidth() : 0; }
        public int getHeight() { return originalImage != null ? originalImage.getHeight() : 0; }
    }
    
    /**
     * 커버 이미지 소스 타입
     */
    public enum CoverSource {
        EMBEDDED,     // 음악 파일에 내장된 이미지
        FOLDER,       // 폴더 내 이미지 파일
        ONLINE,       // 온라인에서 다운로드 (향후 구현)
        DEFAULT       // 기본 이미지
    }
    
    /**
     * 음악 파일의 커버 아트를 비동기로 로드합니다
     */
    public CompletableFuture<CachedCoverArt> loadCoverArtAsync(File musicFile) {
        return CompletableFuture.supplyAsync(() -> loadCoverArt(musicFile));
    }
    
    /**
     * 음악 파일의 커버 아트를 로드합니다
     */
    public CachedCoverArt loadCoverArt(File musicFile) {
        if (musicFile == null || !musicFile.exists() || !musicFile.isFile()) {
            log.warn("유효하지 않은 파일: {}", musicFile);
            return createDefaultCoverArt();
        }
        
        String cacheKey = generateCacheKey(musicFile);
        
        // 캐시 확인
        CachedCoverArt cached = coverCache.get(cacheKey);
        if (cached != null && isCoverArtValid(cached, musicFile)) {
            log.debug("캐시된 커버 아트 반환: {}", musicFile.getName());
            return cached;
        }
        
        try {
            log.debug("커버 아트 로딩 시작: {}", musicFile.getName());
            
            CachedCoverArt coverArt = findCoverArt(musicFile);
            
            // 캐시에 저장
            coverCache.put(cacheKey, coverArt);
            
            log.debug("커버 아트 로딩 완료: {} (소스: {})", musicFile.getName(), coverArt.getSource());
            return coverArt;
            
        } catch (Exception e) {
            log.error("커버 아트 로딩 중 오류: {}", musicFile.getName(), e);
            return createDefaultCoverArt();
        }
    }
    
    /**
     * 여러 음악 파일의 커버 아트를 동시에 로드합니다
     */
    public CompletableFuture<Map<String, CachedCoverArt>> loadCoverArtBatch(List<File> musicFiles) {
        List<CompletableFuture<Map.Entry<String, CachedCoverArt>>> futures = musicFiles.stream()
            .map(file -> loadCoverArtAsync(file)
                .thenApply(coverArt -> Map.entry(file.getAbsolutePath(), coverArt)))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, CachedCoverArt> results = new HashMap<>();
                futures.forEach(future -> {
                    try {
                        Map.Entry<String, CachedCoverArt> entry = future.get();
                        results.put(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        log.error("배치 커버 아트 로딩 중 오류", e);
                    }
                });
                return results;
            });
    }
    
    /**
     * 앨범별로 커버 아트를 그룹화하여 로드합니다
     */
    public Map<String, CachedCoverArt> loadCoverArtByAlbum(List<File> musicFiles) {
        Map<String, List<File>> albumGroups = groupFilesByDirectory(musicFiles);
        Map<String, CachedCoverArt> albumCovers = new HashMap<>();
        
        albumGroups.forEach((album, files) -> {
            if (!files.isEmpty()) {
                // 앨범의 첫 번째 파일로 커버 아트 로드
                CachedCoverArt coverArt = loadCoverArt(files.get(0));
                albumCovers.put(album, coverArt);
            }
        });
        
        return albumCovers;
    }
    
    /**
     * 이미지를 특정 크기로 리사이즈합니다
     */
    public BufferedImage resizeImage(BufferedImage originalImage, int targetSize) {
        if (originalImage == null) {
            return null;
        }
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 이미 목표 크기면 원본 반환
        if (originalWidth == targetSize && originalHeight == targetSize) {
            return originalImage;
        }
        
        // 정사각형으로 만들기 위해 작은 쪽에 맞춤
        int size = Math.min(originalWidth, originalHeight);
        
        // 중앙에서 정사각형으로 크롭
        int x = (originalWidth - size) / 2;
        int y = (originalHeight - size) / 2;
        BufferedImage croppedImage = originalImage.getSubimage(x, y, size, size);
        
        // 목표 크기로 리사이즈
        BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // 고품질 리사이징 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(croppedImage, 0, 0, targetSize, targetSize, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    /**
     * 커버 아트 캐시를 모두 지웁니다
     */
    public void clearCache() {
        coverCache.clear();
        log.debug("커버 아트 캐시 클리어됨");
    }
    
    /**
     * 특정 파일의 캐시된 커버 아트를 제거합니다
     */
    public void removeCachedCoverArt(String filePath) {
        String cacheKey = generateCacheKey(new File(filePath));
        coverCache.remove(cacheKey);
        log.debug("캐시된 커버 아트 제거: {}", filePath);
    }
    
    /**
     * 캐시 통계를 반환합니다
     */
    public CoverCacheStatistics getCacheStatistics() {
        return new CoverCacheStatistics(coverCache.size(), coverCache.keySet());
    }
    
    // ========== Private 메서드들 ==========
    
    private CachedCoverArt findCoverArt(File musicFile) {
        // 1. 폴더 내 이미지 파일 찾기
        CachedCoverArt folderCover = findFolderCoverArt(musicFile);
        if (folderCover != null) {
            return folderCover;
        }
        
        // 2. 음악 파일에 내장된 이미지 추출 (TODO: ID3 라이브러리 필요)
        // CachedCoverArt embeddedCover = extractEmbeddedCoverArt(musicFile);
        // if (embeddedCover != null) {
        //     return embeddedCover;
        // }
        
        // 3. 기본 커버 아트 반환
        return createDefaultCoverArt();
    }
    
    private CachedCoverArt findFolderCoverArt(File musicFile) {
        try {
            File parentDir = musicFile.getParentFile();
            if (parentDir == null || !parentDir.isDirectory()) {
                return null;
            }
            
            // 디렉토리 내 이미지 파일들 찾기
            File[] imageFiles = parentDir.listFiles((dir, name) -> {
                String lowercaseName = name.toLowerCase();
                return SUPPORTED_IMAGE_EXTENSIONS.stream().anyMatch(lowercaseName::endsWith);
            });
            
            if (imageFiles == null || imageFiles.length == 0) {
                return null;
            }
            
            // 우선순위에 따라 커버 이미지 선택
            File bestCoverFile = selectBestCoverFile(imageFiles);
            
            if (bestCoverFile != null) {
                BufferedImage originalImage = ImageIO.read(bestCoverFile);
                if (originalImage != null) {
                    BufferedImage thumbnail = resizeImage(originalImage, THUMBNAIL_SIZE);
                    return new CachedCoverArt(originalImage, thumbnail, 
                                            bestCoverFile.getAbsolutePath(), CoverSource.FOLDER);
                }
            }
            
        } catch (Exception e) {
            log.debug("폴더 커버 아트 찾기 실패: {}", musicFile.getName(), e);
        }
        
        return null;
    }
    
    private File selectBestCoverFile(File[] imageFiles) {
        // 우선순위: cover > folder > album > front > artwork > thumb > 기타
        
        Map<String, Integer> priorityMap = new HashMap<>();
        priorityMap.put("cover", 1);
        priorityMap.put("folder", 2);
        priorityMap.put("album", 3);
        priorityMap.put("front", 4);
        priorityMap.put("artwork", 5);
        priorityMap.put("thumb", 6);
        
        File bestFile = null;
        int bestPriority = Integer.MAX_VALUE;
        
        for (File imageFile : imageFiles) {
            String fileName = getFileNameWithoutExtension(imageFile.getName()).toLowerCase();
            
            // 정확히 일치하는 이름 찾기
            int priority = priorityMap.getOrDefault(fileName, 100);
            
            // 부분 일치도 확인
            if (priority == 100) {
                for (String coverName : COVER_FILE_NAMES) {
                    if (fileName.contains(coverName)) {
                        priority = priorityMap.get(coverName) + 10; // 부분 일치는 우선순위 낮춤
                        break;
                    }
                }
            }
            
            if (priority < bestPriority) {
                bestPriority = priority;
                bestFile = imageFile;
            }
        }
        
        // 우선순위가 같으면 파일 크기가 큰 것 선택
        if (bestFile == null && imageFiles.length > 0) {
            bestFile = Arrays.stream(imageFiles)
                .max(Comparator.comparingLong(File::length))
                .orElse(imageFiles[0]);
        }
        
        return bestFile;
    }
    
    private CachedCoverArt createDefaultCoverArt() {
        try {
            // 기본 커버 아트 생성 (단색 이미지)
            BufferedImage defaultImage = createDefaultImage(DEFAULT_COVER_SIZE);
            BufferedImage defaultThumbnail = createDefaultImage(THUMBNAIL_SIZE);
            
            return new CachedCoverArt(defaultImage, defaultThumbnail, null, CoverSource.DEFAULT);
            
        } catch (Exception e) {
            log.error("기본 커버 아트 생성 중 오류", e);
            return null;
        }
    }
    
    private BufferedImage createDefaultImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // 그라데이션 배경
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(64, 64, 64),
            size, size, new Color(32, 32, 32)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, size, size);
        
        // 음표 아이콘 그리기 (간단한 형태)
        g2d.setColor(new Color(128, 128, 128));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int iconSize = size / 3;
        int x = (size - iconSize) / 2;
        int y = (size - iconSize) / 2;
        
        // 음표 모양 (단순화된 형태)
        g2d.fillOval(x, y + iconSize * 2 / 3, iconSize / 3, iconSize / 4);
        g2d.fillRect(x + iconSize / 4, y, iconSize / 20, iconSize * 2 / 3);
        
        g2d.dispose();
        return image;
    }
    
    private boolean isCoverArtValid(CachedCoverArt coverArt, File musicFile) {
        try {
            // 폴더 커버의 경우 소스 파일 수정 시간 확인
            if (coverArt.getSource() == CoverSource.FOLDER && coverArt.getSourcePath() != null) {
                File sourceFile = new File(coverArt.getSourcePath());
                if (sourceFile.exists()) {
                    return sourceFile.lastModified() <= coverArt.getCacheTime();
                }
            }
            
            // 기본 커버나 내장 커버는 항상 유효
            return true;
            
        } catch (Exception e) {
            log.debug("커버 아트 유효성 검사 실패: {}", musicFile.getName());
            return false;
        }
    }
    
    private String generateCacheKey(File musicFile) {
        // 음악 파일이 있는 디렉토리를 기준으로 캐시 키 생성
        // 같은 폴더의 음악들은 같은 커버를 공유할 가능성이 높음
        File parentDir = musicFile.getParentFile();
        return parentDir != null ? parentDir.getAbsolutePath() : musicFile.getAbsolutePath();
    }
    
    private Map<String, List<File>> groupFilesByDirectory(List<File> musicFiles) {
        Map<String, List<File>> groups = new HashMap<>();
        
        for (File file : musicFiles) {
            String dirPath = file.getParent();
            if (dirPath != null) {
                groups.computeIfAbsent(dirPath, k -> new ArrayList<>()).add(file);
            }
        }
        
        return groups;
    }
    
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    // TODO: 향후 ID3 라이브러리 추가 시 구현
    /*
    private CachedCoverArt extractEmbeddedCoverArt(File musicFile) {
        try {
            // ID3 태그에서 앨범 아트 추출
            // MP3: ID3v2 APIC 프레임
            // FLAC: METADATA_BLOCK_PICTURE
            // M4A: MP4 covr atom
            
            return null; // 현재 미구현
            
        } catch (Exception e) {
            log.debug("내장 커버 아트 추출 실패: {}", musicFile.getName(), e);
            return null;
        }
    }
    */
    
    // ========== 고급 기능들 (향후 구현) ==========
    
    /**
     * 온라인에서 앨범 커버를 다운로드합니다 (TODO: 향후 구현)
     */
    public CompletableFuture<CachedCoverArt> downloadCoverArtAsync(String artist, String album) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Last.fm, MusicBrainz, Spotify API 등을 활용한 커버 다운로드
            log.debug("온라인 커버 다운로드 (미구현): {} - {}", artist, album);
            return createDefaultCoverArt();
        });
    }
    
    /**
     * 커버 아트를 파일로 저장합니다
     */
    public boolean saveCoverArt(CachedCoverArt coverArt, File outputFile, String format) {
        if (coverArt == null || coverArt.getOriginalImage() == null) {
            return false;
        }
        
        try {
            return ImageIO.write(coverArt.getOriginalImage(), format, outputFile);
        } catch (IOException e) {
            log.error("커버 아트 저장 실패: {}", outputFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 여러 크기의 커버 아트를 생성합니다
     */
    public Map<String, BufferedImage> generateMultipleSizes(CachedCoverArt coverArt, int... sizes) {
        Map<String, BufferedImage> results = new HashMap<>();
        
        if (coverArt == null || coverArt.getOriginalImage() == null) {
            return results;
        }
        
        for (int size : sizes) {
            BufferedImage resized = resizeImage(coverArt.getOriginalImage(), size);
            results.put(size + "x" + size, resized);
        }
        
        return results;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * 커버 아트 캐시 통계
     */
    public static class CoverCacheStatistics {
        private final int cacheSize;
        private final Set<String> cachedKeys;
        
        public CoverCacheStatistics(int cacheSize, Set<String> cachedKeys) {
            this.cacheSize = cacheSize;
            this.cachedKeys = new HashSet<>(cachedKeys);
        }
        
        public int getCacheSize() { return cacheSize; }
        public Set<String> getCachedKeys() { return new HashSet<>(cachedKeys); }
        
        @Override
        public String toString() {
            return String.format("CoverCacheStatistics{size=%d}", cacheSize);
        }
    }
}