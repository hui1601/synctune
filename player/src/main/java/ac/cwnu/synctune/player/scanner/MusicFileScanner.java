package ac.cwnu.synctune.player.scanner;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 지정된 디렉토리에서 음악 파일을 탐색하는 스캐너
 * 재귀적으로 하위 디렉토리까지 탐색하며, 지원되는 오디오 파일 형식을 찾습니다.
 */
public class MusicFileScanner {
    private static final Logger log = LogManager.getLogger(MusicFileScanner.class);
    
    // 지원되는 오디오 파일 확장자들
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".mp3", ".wav", ".flac", ".m4a", ".aac", ".ogg", ".wma"
    );
    
    // 제외할 디렉토리들 (시스템 폴더, 숨김 폴더 등)
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
        ".git", ".svn", ".hg", "node_modules", "__pycache__", 
        "System Volume Information", "$RECYCLE.BIN", ".Trash"
    );
    
    private final EventPublisher eventPublisher;
    private final FileDiscoveryReporter reporter;
    
    // 스캔 상태 관리
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    
    // 스캔 통계
    private final AtomicInteger totalFilesFound = new AtomicInteger(0);
    private final AtomicInteger directoriesScanned = new AtomicInteger(0);
    
    // 발견된 파일들을 저장하는 리스트 (스레드 안전)
    private final List<File> discoveredFiles = new CopyOnWriteArrayList<>();
    
    public MusicFileScanner(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.reporter = new FileDiscoveryReporter(eventPublisher);
    }
    
    /**
     * 디렉토리를 비동기로 스캔합니다
     */
    public CompletableFuture<ScanResult> scanDirectoryAsync(String directoryPath) {
        return CompletableFuture.supplyAsync(() -> scanDirectory(directoryPath));
    }
    
    /**
     * 디렉토리를 동기적으로 스캔합니다
     */
    public ScanResult scanDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            log.warn("스캔할 디렉토리 경로가 비어있습니다.");
            return new ScanResult(false, "디렉토리 경로가 비어있습니다.", Collections.emptyList());
        }
        
        Path scanPath = Paths.get(directoryPath);
        if (!Files.exists(scanPath)) {
            log.warn("스캔할 디렉토리가 존재하지 않습니다: {}", directoryPath);
            return new ScanResult(false, "디렉토리가 존재하지 않습니다: " + directoryPath, Collections.emptyList());
        }
        
        if (!Files.isDirectory(scanPath)) {
            log.warn("지정된 경로가 디렉토리가 아닙니다: {}", directoryPath);
            return new ScanResult(false, "디렉토리가 아닙니다: " + directoryPath, Collections.emptyList());
        }
        
        // 스캔 시작
        return performScan(scanPath);
    }
    
    /**
     * 여러 디렉토리를 동시에 스캔합니다
     */
    public CompletableFuture<List<ScanResult>> scanMultipleDirectoriesAsync(List<String> directoryPaths) {
        List<CompletableFuture<ScanResult>> futures = directoryPaths.stream()
            .map(this::scanDirectoryAsync)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
    
    /**
     * 현재 진행 중인 스캔을 중지합니다
     */
    public void stopScanning() {
        if (isScanning.get()) {
            log.info("스캔 중지 요청됨");
            shouldStop.set(true);
        }
    }
    
    /**
     * 현재 스캔 중인지 확인합니다
     */
    public boolean isCurrentlyScanning() {
        return isScanning.get();
    }
    
    /**
     * 마지막 스캔 통계를 반환합니다
     */
    public ScanStatistics getLastScanStatistics() {
        return new ScanStatistics(
            totalFilesFound.get(),
            directoriesScanned.get(),
            discoveredFiles.size()
        );
    }
    
    /**
     * 파일이 지원되는 오디오 파일인지 확인합니다
     */
    public static boolean isSupportedAudioFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream()
            .anyMatch(fileName::endsWith);
    }
    
    /**
     * 지원되는 파일 확장자 목록을 반환합니다
     */
    public static Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_EXTENSIONS);
    }
    
    // ========== Private 메서드들 ==========
    
    private ScanResult performScan(Path rootPath) {
        if (!isScanning.compareAndSet(false, true)) {
            log.warn("이미 스캔이 진행 중입니다.");
            return new ScanResult(false, "이미 스캔이 진행 중입니다.", Collections.emptyList());
        }
        
        try {
            // 스캔 초기화
            resetScanState();
            String rootPathStr = rootPath.toString();
            
            log.info("디렉토리 스캔 시작: {}", rootPathStr);
            reporter.reportScanStarted(rootPathStr);
            
            long startTime = System.currentTimeMillis();
            
            // 파일 시스템 워킹
            Files.walkFileTree(rootPath, new MusicFileVisitor());
            
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            // 스캔 완료 처리
            List<File> foundFiles = new ArrayList<>(discoveredFiles);
            int totalFound = foundFiles.size();
            
            if (shouldStop.get()) {
                log.info("스캔이 사용자에 의해 중지됨: {} ({}개 파일 발견, {}ms 소요)", 
                    rootPathStr, totalFound, durationMs);
                reporter.reportScanCancelled(rootPathStr, totalFound);
                return new ScanResult(false, "스캔이 중지되었습니다.", foundFiles);
            } else {
                log.info("디렉토리 스캔 완료: {} ({}개 파일 발견, {}ms 소요)", 
                    rootPathStr, totalFound, durationMs);
                reporter.reportScanCompleted(rootPathStr, totalFound);
                return new ScanResult(true, "스캔 완료", foundFiles);
            }
            
        } catch (Exception e) {
            log.error("디렉토리 스캔 중 오류 발생: {}", rootPath, e);
            reporter.reportScanError(rootPath.toString(), e.getMessage());
            return new ScanResult(false, "스캔 중 오류: " + e.getMessage(), new ArrayList<>(discoveredFiles));
        } finally {
            isScanning.set(false);
            shouldStop.set(false);
        }
    }
    
    private void resetScanState() {
        totalFilesFound.set(0);
        directoriesScanned.set(0);
        discoveredFiles.clear();
        shouldStop.set(false);
    }
    
    private boolean shouldExcludeDirectory(String dirName) {
        return EXCLUDED_DIRECTORIES.contains(dirName) || dirName.startsWith(".");
    }
    
    // ========== Inner Classes ==========
    
    /**
     * 파일 시스템을 순회하며 음악 파일을 찾는 Visitor
     */
    private class MusicFileVisitor extends SimpleFileVisitor<Path> {
        
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (shouldStop.get()) {
                return FileVisitResult.TERMINATE;
            }
            
            String dirName = dir.getFileName().toString();
            if (shouldExcludeDirectory(dirName)) {
                log.debug("디렉토리 제외: {}", dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
            
            directoriesScanned.incrementAndGet();
            log.debug("디렉토리 스캔 중: {}", dir);
            
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (shouldStop.get()) {
                return FileVisitResult.TERMINATE;
            }
            
            File javaFile = file.toFile();
            if (isSupportedAudioFile(javaFile)) {
                discoveredFiles.add(javaFile);
                totalFilesFound.incrementAndGet();
                
                log.debug("음악 파일 발견: {}", file);
                reporter.reportFileFound(javaFile);
            }
            
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            log.warn("파일 접근 실패: {} ({})", file, exc.getMessage());
            return FileVisitResult.CONTINUE;
        }
    }
    
    /**
     * 스캔 결과를 나타내는 클래스
     */
    public static class ScanResult {
        private final boolean success;
        private final String message;
        private final List<File> foundFiles;
        private final long timestamp;
        
        public ScanResult(boolean success, String message, List<File> foundFiles) {
            this.success = success;
            this.message = message;
            this.foundFiles = new ArrayList<>(foundFiles);
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<File> getFoundFiles() { return new ArrayList<>(foundFiles); }
        public int getFileCount() { return foundFiles.size(); }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("ScanResult{success=%s, fileCount=%d, message='%s'}", 
                success, foundFiles.size(), message);
        }
    }
    
    /**
     * 스캔 통계 정보
     */
    public static class ScanStatistics {
        private final int totalFilesFound;
        private final int directoriesScanned;
        private final int validAudioFiles;
        
        public ScanStatistics(int totalFilesFound, int directoriesScanned, int validAudioFiles) {
            this.totalFilesFound = totalFilesFound;
            this.directoriesScanned = directoriesScanned;
            this.validAudioFiles = validAudioFiles;
        }
        
        public int getTotalFilesFound() { return totalFilesFound; }
        public int getDirectoriesScanned() { return directoriesScanned; }
        public int getValidAudioFiles() { return validAudioFiles; }
        
        @Override
        public String toString() {
            return String.format("ScanStatistics{totalFiles=%d, directories=%d, audioFiles=%d}", 
                totalFilesFound, directoriesScanned, validAudioFiles);
        }
    }
}