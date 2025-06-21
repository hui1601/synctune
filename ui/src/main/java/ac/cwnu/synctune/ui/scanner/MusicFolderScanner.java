package ac.cwnu.synctune.ui.scanner;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 음악 폴더를 스캔하여 음악 파일을 찾는 스캐너
 */
public class MusicFolderScanner {
    private static final Logger log = LoggerFactory.getLogger(MusicFolderScanner.class);
    
    // 지원되는 음악 파일 확장자
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg", ".wma"
    );
    
    // 제외할 디렉토리들
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
        ".git", ".svn", ".hg", "node_modules", "__pycache__", 
        "System Volume Information", "$RECYCLE.BIN", ".Trash",
        "Thumbs.db", ".DS_Store"
    );
    
    /**
     * 스캔 결과를 담는 클래스
     */
    public static class ScanResult {
        private final List<MusicInfo> musicFiles;
        private final int totalFilesScanned;
        private final int directoriesScanned;
        private final long scanTimeMs;
        private final boolean cancelled;
        private final String errorMessage;
        
        public ScanResult(List<MusicInfo> musicFiles, int totalFilesScanned, 
                         int directoriesScanned, long scanTimeMs, 
                         boolean cancelled, String errorMessage) {
            this.musicFiles = new ArrayList<>(musicFiles);
            this.totalFilesScanned = totalFilesScanned;
            this.directoriesScanned = directoriesScanned;
            this.scanTimeMs = scanTimeMs;
            this.cancelled = cancelled;
            this.errorMessage = errorMessage;
        }
        
        public List<MusicInfo> getMusicFiles() { return new ArrayList<>(musicFiles); }
        public int getTotalFilesScanned() { return totalFilesScanned; }
        public int getDirectoriesScanned() { return directoriesScanned; }
        public long getScanTimeMs() { return scanTimeMs; }
        public boolean isCancelled() { return cancelled; }
        public boolean isSuccess() { return errorMessage == null && !cancelled; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return String.format("ScanResult{musicFiles=%d, scanned=%d files in %d dirs, time=%dms, cancelled=%s}", 
                musicFiles.size(), totalFilesScanned, directoriesScanned, scanTimeMs, cancelled);
        }
    }
    
    /**
     * 스캔 진행 상황을 통지하는 콜백 인터페이스
     */
    public interface ScanProgressCallback {
        void onProgress(int scannedFiles, int foundMusic, String currentFile);
        void onDirectoryEntered(String directoryPath);
        void onComplete(ScanResult result);
        void onError(String error);
    }
    
    /**
     * 스캔 설정
     */
    public static class ScanOptions {
        private boolean recursive = true;
        private boolean includeHiddenFiles = false;
        private boolean followSymlinks = false;
        private int maxDepth = Integer.MAX_VALUE;
        private long maxFileSize = Long.MAX_VALUE; // 바이트
        private Set<String> additionalExtensions = new HashSet<>();
        private Set<String> excludeDirectories = new HashSet<>(EXCLUDED_DIRECTORIES);
        
        public boolean isRecursive() { return recursive; }
        public ScanOptions setRecursive(boolean recursive) { this.recursive = recursive; return this; }
        
        public boolean isIncludeHiddenFiles() { return includeHiddenFiles; }
        public ScanOptions setIncludeHiddenFiles(boolean includeHiddenFiles) { this.includeHiddenFiles = includeHiddenFiles; return this; }
        
        public boolean isFollowSymlinks() { return followSymlinks; }
        public ScanOptions setFollowSymlinks(boolean followSymlinks) { this.followSymlinks = followSymlinks; return this; }
        
        public int getMaxDepth() { return maxDepth; }
        public ScanOptions setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; return this; }
        
        public long getMaxFileSize() { return maxFileSize; }
        public ScanOptions setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; return this; }
        
        public Set<String> getAdditionalExtensions() { return new HashSet<>(additionalExtensions); }
        public ScanOptions setAdditionalExtensions(Set<String> extensions) { 
            this.additionalExtensions = new HashSet<>(extensions); 
            return this; 
        }
        
        public Set<String> getExcludeDirectories() { return new HashSet<>(excludeDirectories); }
        public ScanOptions setExcludeDirectories(Set<String> directories) { 
            this.excludeDirectories = new HashSet<>(directories); 
            return this; 
        }
    }
    
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    /**
     * 폴더를 동기적으로 스캔합니다
     */
    public ScanResult scanFolder(File folder, ScanOptions options, ScanProgressCallback callback) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            String error = "유효하지 않은 폴더: " + (folder != null ? folder.getAbsolutePath() : "null");
            if (callback != null) callback.onError(error);
            return new ScanResult(Collections.emptyList(), 0, 0, 0, false, error);
        }
        
        cancelled.set(false);
        long startTime = System.currentTimeMillis();
        
        List<MusicInfo> musicFiles = new ArrayList<>();
        AtomicInteger totalFiles = new AtomicInteger(0);
        AtomicInteger directories = new AtomicInteger(0);
        AtomicLong processedFiles = new AtomicLong(0);
        
        try {
            log.info("음악 폴더 스캔 시작: {}", folder.getAbsolutePath());
            
            Set<FileVisitOption> visitOptions = options.isFollowSymlinks() ? 
                EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class);
            
            Files.walkFileTree(folder.toPath(), visitOptions, options.getMaxDepth(), 
                new MusicFileVisitor(musicFiles, totalFiles, directories, processedFiles, options, callback));
                
        } catch (IOException e) {
            log.error("폴더 스캔 중 오류 발생: {}", folder.getAbsolutePath(), e);
            String error = "스캔 중 오류: " + e.getMessage();
            if (callback != null) callback.onError(error);
            return new ScanResult(musicFiles, totalFiles.get(), directories.get(), 
                                System.currentTimeMillis() - startTime, cancelled.get(), error);
        }
        
        long endTime = System.currentTimeMillis();
        ScanResult result = new ScanResult(musicFiles, totalFiles.get(), directories.get(), 
                                         endTime - startTime, cancelled.get(), null);
        
        log.info("음악 폴더 스캔 완료: {} ({}개 파일 발견, {}ms 소요)", 
                folder.getAbsolutePath(), musicFiles.size(), result.getScanTimeMs());
        
        if (callback != null) {
            callback.onComplete(result);
        }
        
        return result;
    }
    
    /**
     * 폴더를 비동기적으로 스캔하는 Task를 생성합니다
     */
    public Task<ScanResult> createScanTask(File folder, ScanOptions options, ScanProgressCallback callback) {
        return new Task<ScanResult>() {
            @Override
            protected ScanResult call() throws Exception {
                return scanFolder(folder, options, new ScanProgressCallback() {
                    @Override
                    public void onProgress(int scannedFiles, int foundMusic, String currentFile) {
                        Platform.runLater(() -> {
                            updateMessage(String.format("스캔 중... %d개 파일 (%d개 음악) - %s", 
                                                       scannedFiles, foundMusic, 
                                                       truncateFileName(currentFile, 30)));
                            updateProgress(scannedFiles, scannedFiles + 100); // 대략적인 진행률
                        });
                        if (callback != null) callback.onProgress(scannedFiles, foundMusic, currentFile);
                    }
                    
                    @Override
                    public void onDirectoryEntered(String directoryPath) {
                        Platform.runLater(() -> {
                            updateMessage("디렉토리 스캔 중: " + truncateFileName(directoryPath, 40));
                        });
                        if (callback != null) callback.onDirectoryEntered(directoryPath);
                    }
                    
                    @Override
                    public void onComplete(ScanResult result) {
                        Platform.runLater(() -> {
                            updateMessage(String.format("스캔 완료: %d개 음악 파일 발견", result.getMusicFiles().size()));
                            updateProgress(100, 100);
                        });
                        if (callback != null) callback.onComplete(result);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            updateMessage("스캔 오류: " + error);
                        });
                        if (callback != null) callback.onError(error);
                    }
                });
            }
            
            @Override
            protected void cancelled() {
                MusicFolderScanner.this.cancelled.set(true);
                updateMessage("스캔 취소됨");
            }
        };
    }
    
    /**
     * 현재 스캔을 취소합니다
     */
    public void cancelScan() {
        cancelled.set(true);
        log.info("음악 폴더 스캔 취소 요청");
    }
    
    /**
     * 파일이 지원되는 음악 파일인지 확인
     */
    public static boolean isSupportedMusicFile(File file, ScanOptions options) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        
        // 기본 지원 확장자 확인
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        
        // 추가 확장자 확인
        for (String ext : options.getAdditionalExtensions()) {
            if (fileName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * MusicInfo 객체 생성
     */
    private static MusicInfo createMusicInfoFromFile(File file) {
        try {
            String fileName = file.getName();
            String baseName = getFileNameWithoutExtension(fileName);
            
            // 파일명 파싱
            String title = baseName;
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            
            // 패턴 매칭으로 아티스트와 제목 분리
            if (baseName.contains(" - ")) {
                String[] parts = baseName.split(" - ", 2);
                if (parts.length == 2) {
                    artist = parts[0].trim();
                    title = parts[1].trim();
                }
            } else if (baseName.matches("^\\d+\\.\\s*.*")) {
                // 트랙 번호 제거
                String withoutTrack = baseName.replaceFirst("^\\d+\\.\\s*", "");
                if (withoutTrack.contains(" - ")) {
                    String[] parts = withoutTrack.split(" - ", 2);
                    if (parts.length == 2) {
                        artist = parts[0].trim();
                        title = parts[1].trim();
                    }
                } else {
                    title = withoutTrack;
                }
            }
            
            // 앨범 정보는 폴더명에서 추출
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                String parentName = parentDir.getName();
                if (!parentName.equals("Music") && !parentName.matches("\\d{4}") && 
                    !EXCLUDED_DIRECTORIES.contains(parentName.toLowerCase())) {
                    album = parentName;
                }
            }
            
            // 재생 시간 추정
            long duration = estimateDuration(file);
            
            // LRC 파일 찾기
            String lrcPath = findLrcFile(file);
            
            return new MusicInfo(title, artist, album, file.getAbsolutePath(), duration, lrcPath);
            
        } catch (Exception e) {
            log.error("MusicInfo 생성 실패: {}", file.getAbsolutePath(), e);
            return new MusicInfo(file.getName(), "Unknown Artist", "Unknown Album", 
                               file.getAbsolutePath(), 180000L, null);
        }
    }
    
    /**
     * 재생 시간 추정
     */
    private static long estimateDuration(File file) {
        String fileName = file.getName().toLowerCase();
        long fileSize = file.length();
        
        if (fileName.endsWith(".mp3")) {
            // MP3: 평균 128kbps 기준
            return (fileSize * 8) / (128 * 1000 / 1000);
        } else if (fileName.endsWith(".wav")) {
            // WAV: 44.1kHz, 16bit, 스테레오
            return (fileSize * 1000) / (44100 * 2 * 2);
        } else if (fileName.endsWith(".flac")) {
            // FLAC: WAV의 약 60% 크기
            return (fileSize * 1000) / (100 * 1024);
        } else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
            // AAC: 약 MP3와 비슷
            return (fileSize * 8) / (128 * 1000 / 1000);
        } else {
            // 기본값: 3분
            return 180000L;
        }
    }
    
    /**
     * LRC 파일 찾기
     */
    private static String findLrcFile(File musicFile) {
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            
            // 같은 디렉토리
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
            // lyrics 하위 폴더
            File lyricsDir = new File(musicFile.getParent(), "lyrics");
            if (lyricsDir.exists()) {
                File lrcInLyrics = new File(lyricsDir, baseName + ".lrc");
                if (lrcInLyrics.exists()) {
                    return lrcInLyrics.getAbsolutePath();
                }
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 실패: {}", musicFile.getName());
        }
        
        return null;
    }
    
    private static String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    private static String truncateFileName(String fileName, int maxLength) {
        if (fileName == null || fileName.length() <= maxLength) {
            return fileName;
        }
        return "..." + fileName.substring(fileName.length() - maxLength + 3);
    }
    
    /**
     * 파일 시스템 방문자 클래스
     */
    private class MusicFileVisitor extends SimpleFileVisitor<Path> {
        private final List<MusicInfo> musicFiles;
        private final AtomicInteger totalFiles;
        private final AtomicInteger directories;
        private final AtomicLong processedFiles;
        private final ScanOptions options;
        private final ScanProgressCallback callback;
        
        public MusicFileVisitor(List<MusicInfo> musicFiles, AtomicInteger totalFiles, 
                               AtomicInteger directories, AtomicLong processedFiles,
                               ScanOptions options, ScanProgressCallback callback) {
            this.musicFiles = musicFiles;
            this.totalFiles = totalFiles;
            this.directories = directories;
            this.processedFiles = processedFiles;
            this.options = options;
            this.callback = callback;
        }
        
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (cancelled.get()) {
                return FileVisitResult.TERMINATE;
            }
            
            String dirName = dir.getFileName().toString();
            
            // 숨김 파일/폴더 제외
            if (!options.isIncludeHiddenFiles() && dirName.startsWith(".")) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            
            // 제외 디렉토리 확인
            if (options.getExcludeDirectories().contains(dirName.toLowerCase())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            
            directories.incrementAndGet();
            
            if (callback != null) {
                callback.onDirectoryEntered(dir.toString());
            }
            
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (cancelled.get()) {
                return FileVisitResult.TERMINATE;
            }
            
            File javaFile = file.toFile();
            totalFiles.incrementAndGet();
            
            // 파일 크기 확인
            if (attrs.size() > options.getMaxFileSize()) {
                return FileVisitResult.CONTINUE;
            }
            
            // 숨김 파일 제외
            if (!options.isIncludeHiddenFiles() && javaFile.getName().startsWith(".")) {
                return FileVisitResult.CONTINUE;
            }
            
            // 음악 파일인지 확인
            if (isSupportedMusicFile(javaFile, options)) {
                try {
                    MusicInfo musicInfo = createMusicInfoFromFile(javaFile);
                    musicFiles.add(musicInfo);
                    
                    log.debug("음악 파일 발견: {}", javaFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("음악 파일 처리 실패: {}", javaFile.getAbsolutePath(), e);
                }
            }
            
            // 진행 상황 업데이트 (100개 파일마다)
            long processed = processedFiles.incrementAndGet();
            if (processed % 100 == 0 && callback != null) {
                callback.onProgress((int)processed, musicFiles.size(), javaFile.getName());
            }
            
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            log.warn("파일 접근 실패: {} - {}", file, exc.getMessage());
            return FileVisitResult.CONTINUE;
        }
    }
    
    /**
     * 기본 스캔 옵션 생성
     */
    public static ScanOptions createDefaultOptions() {
        return new ScanOptions()
            .setRecursive(true)
            .setIncludeHiddenFiles(false)
            .setFollowSymlinks(false)
            .setMaxDepth(20)
            .setMaxFileSize(500 * 1024 * 1024); // 500MB
    }
    
    /**
     * 빠른 스캔 옵션 (얕은 깊이)
     */
    public static ScanOptions createQuickScanOptions() {
        return new ScanOptions()
            .setRecursive(true)
            .setMaxDepth(3)
            .setIncludeHiddenFiles(false)
            .setMaxFileSize(100 * 1024 * 1024); // 100MB
    }
}