package ac.cwnu.synctune.player.scanner;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 발견된 파일 정보를 FileScanEvent로 발행하는 클래스
 * (또는 MediaInfoEvent 사용 - SDK에서 제공하는 이벤트에 따라)
 */
public class FileDiscoveryReporter {
    private static final Logger log = LogManager.getLogger(FileDiscoveryReporter.class);
    
    private final EventPublisher eventPublisher;
    
    // 보고 통계
    private final AtomicLong totalReportedFiles = new AtomicLong(0);
    private final AtomicLong totalReportedScans = new AtomicLong(0);
    
    public FileDiscoveryReporter(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 스캔 시작을 보고합니다
     */
    public void reportScanStarted(String directoryPath) {
        try {
            log.info("스캔 시작 보고: {}", directoryPath);
            totalReportedScans.incrementAndGet();
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.ScanStartedEvent(directoryPath));
            
            logEvent("ScanStarted", directoryPath, null);
            
        } catch (Exception e) {
            log.error("스캔 시작 보고 중 오류", e);
        }
    }
    
    /**
     * 파일 발견을 보고합니다
     */
    public void reportFileFound(File foundFile) {
        try {
            if (foundFile == null) {
                log.warn("발견된 파일이 null입니다.");
                return;
            }
            
            totalReportedFiles.incrementAndGet();
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.FileFoundEvent(foundFile));
            // 또는
            // publish(new MediaInfoEvent.MediaFileDiscoveredEvent(foundFile));
            
            logEvent("FileFound", foundFile.getAbsolutePath(), getFileInfo(foundFile));
            
        } catch (Exception e) {
            log.error("파일 발견 보고 중 오류: {}", foundFile, e);
        }
    }
    
    /**
     * 스캔 완료를 보고합니다
     */
    public void reportScanCompleted(String directoryPath, int totalFilesFound) {
        try {
            log.info("스캔 완료 보고: {} ({}개 파일)", directoryPath, totalFilesFound);
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.ScanCompletedEvent(directoryPath, totalFilesFound));
            
            logEvent("ScanCompleted", directoryPath, "총 " + totalFilesFound + "개 파일 발견");
            
        } catch (Exception e) {
            log.error("스캔 완료 보고 중 오류", e);
        }
    }
    
    /**
     * 스캔 취소를 보고합니다
     */
    public void reportScanCancelled(String directoryPath, int partialFilesFound) {
        try {
            log.info("스캔 취소 보고: {} ({}개 파일 부분 발견)", directoryPath, partialFilesFound);
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.ScanCancelledEvent(directoryPath, partialFilesFound));
            
            logEvent("ScanCancelled", directoryPath, "부분적으로 " + partialFilesFound + "개 파일 발견");
            
        } catch (Exception e) {
            log.error("스캔 취소 보고 중 오류", e);
        }
    }
    
    /**
     * 스캔 오류를 보고합니다
     */
    public void reportScanError(String directoryPath, String errorMessage) {
        try {
            log.error("스캔 오류 보고: {} - {}", directoryPath, errorMessage);
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.ScanErrorEvent(directoryPath, errorMessage));
            // 또는
            // publish(new ErrorEvent("파일 스캔 오류: " + errorMessage, null, false));
            
            logEvent("ScanError", directoryPath, errorMessage);
            
        } catch (Exception e) {
            log.error("스캔 오류 보고 중 오류", e);
        }
    }
    
    /**
     * 스캔 진행 상황을 보고합니다
     */
    public void reportScanProgress(String directoryPath, int currentCount, String currentFile) {
        try {
            // 너무 자주 로그가 출력되지 않도록 조건부 로깅
            if (currentCount % 100 == 0 || currentCount < 10) {
                log.debug("스캔 진행: {} ({}개 파일 발견, 현재: {})", 
                    directoryPath, currentCount, getShortFileName(currentFile));
            }
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.ScanProgressEvent(directoryPath, currentCount, currentFile));
            
        } catch (Exception e) {
            log.error("스캔 진행 보고 중 오류", e);
        }
    }
    
    /**
     * 배치 파일 발견을 보고합니다 (성능 최적화용)
     */
    public void reportBatchFilesFound(String directoryPath, java.util.List<File> files) {
        try {
            if (files == null || files.isEmpty()) {
                return;
            }
            
            log.debug("배치 파일 발견 보고: {} ({}개 파일)", directoryPath, files.size());
            totalReportedFiles.addAndGet(files.size());
            
            // TODO: 실제 이벤트 클래스 확인 후 구현
            // publish(new FileScanEvent.BatchFilesFoundEvent(directoryPath, new ArrayList<>(files)));
            
            logEvent("BatchFilesFound", directoryPath, files.size() + "개 파일 배치 발견");
            
        } catch (Exception e) {
            log.error("배치 파일 발견 보고 중 오류", e);
        }
    }
    
    /**
     * 보고 통계를 반환합니다
     */
    public ReportStatistics getStatistics() {
        return new ReportStatistics(
            totalReportedFiles.get(),
            totalReportedScans.get()
        );
    }
    
    /**
     * 통계를 초기화합니다
     */
    public void resetStatistics() {
        totalReportedFiles.set(0);
        totalReportedScans.set(0);
        log.debug("보고 통계 초기화됨");
    }
    
    // ========== Private 헬퍼 메서드들 ==========
    
    private void logEvent(String eventType, String target, String details) {
        if (details != null) {
            log.debug("[이벤트] {}: {} - {}", eventType, getShortPath(target), details);
        } else {
            log.debug("[이벤트] {}: {}", eventType, getShortPath(target));
        }
    }
    
    private String getFileInfo(File file) {
        try {
            long sizeKB = file.length() / 1024;
            String extension = getFileExtension(file.getName());
            return String.format("%s, %d KB", extension.toUpperCase(), sizeKB);
        } catch (Exception e) {
            return "정보 없음";
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }
    
    private String getShortPath(String fullPath) {
        if (fullPath == null || fullPath.length() <= 50) {
            return fullPath;
        }
        return "..." + fullPath.substring(fullPath.length() - 47);
    }
    
    private String getShortFileName(String fullPath) {
        if (fullPath == null) {
            return "";
        }
        File file = new File(fullPath);
        return file.getName();
    }
    
    // TODO: 실제 이벤트 발행 메서드 (이벤트 클래스 확인 후 구현)
    /*
    private void publish(BaseEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("이벤트 발행 중 오류", e);
        }
    }
    */
    
    // ========== Inner Classes ==========
    
    /**
     * 보고 통계 정보
     */
    public static class ReportStatistics {
        private final long totalReportedFiles;
        private final long totalReportedScans;
        private final long timestamp;
        
        public ReportStatistics(long totalReportedFiles, long totalReportedScans) {
            this.totalReportedFiles = totalReportedFiles;
            this.totalReportedScans = totalReportedScans;
            this.timestamp = System.currentTimeMillis();
        }
        
        public long getTotalReportedFiles() {
            return totalReportedFiles;
        }
        
        public long getTotalReportedScans() {
            return totalReportedScans;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("ReportStatistics{files=%d, scans=%d, timestamp=%d}", 
                totalReportedFiles, totalReportedScans, timestamp);
        }
    }
}