package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 재생 진행 상황을 추적하고 주기적으로 업데이트 이벤트를 발행하는 클래스
 */
public class ProgressTracker {
    private static final Logger log = LogManager.getLogger(ProgressTracker.class);
    
    // 진행 상황 업데이트 간격 (밀리초)
    private static final long UPDATE_INTERVAL_MS = 500;
    
    private final EventPublisher eventPublisher;
    private final PlaybackState playbackState;
    private final AudioEngine audioEngine;
    
    // 스케줄러 관련
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isTracking = new AtomicBoolean(false);
    
    public ProgressTracker(EventPublisher eventPublisher, PlaybackState playbackState, AudioEngine audioEngine) {
        this.eventPublisher = eventPublisher;
        this.playbackState = playbackState;
        this.audioEngine = audioEngine;
    }
    
    /**
     * 진행 상황 추적을 시작합니다
     */
    public void startTracking() {
        if (isTracking.get()) {
            log.debug("이미 진행 상황 추적이 시작되어 있습니다.");
            return;
        }
        
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PlayerModule-ProgressTracker");
                t.setDaemon(true);
                return t;
            });
        }
        
        isTracking.set(true);
        
        scheduler.scheduleAtFixedRate(this::updateProgress, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        log.debug("진행 상황 추적을 시작했습니다.");
    }
    
    /**
     * 진행 상황 추적을 중지합니다
     */
    public void stopTracking() {
        if (!isTracking.get()) {
            return;
        }
        
        isTracking.set(false);
        log.debug("진행 상황 추적을 중지했습니다.");
    }
    
    /**
     * 진행 상황을 업데이트하고 이벤트를 발행합니다
     */
    private void updateProgress() {
        try {
            // 재생 중이고 오디오 클립이 실행 중일 때만 업데이트
            if (!playbackState.isPlaying() || !audioEngine.isAudioClipRunning()) {
                return;
            }
            
            long currentTimeMillis = audioEngine.getCurrentPositionMillis();
            long totalTimeMillis = audioEngine.getTotalDurationMillis();
            
            // 유효한 시간 값인지 확인
            if (currentTimeMillis >= 0 && totalTimeMillis > 0) {
                eventPublisher.publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(
                    currentTimeMillis, totalTimeMillis));
            }
            
        } catch (Exception e) {
            log.error("진행 상황 업데이트 중 오류", e);
            // 오류가 발생해도 추적은 계속함
        }
    }
    
    /**
     * 즉시 현재 진행 상황을 업데이트합니다
     */
    public void forceUpdate() {
        if (playbackState.getCurrentMusic() != null) {
            updateProgress();
        }
    }
    
    /**
     * 리소스를 해제하고 스케줄러를 종료합니다
     */
    public void dispose() {
        stopTracking();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    log.warn("진행 상황 추적 스케줄러를 강제로 종료했습니다.");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("진행 상황 추적 스케줄러 종료 중 인터럽트 발생", e);
            }
        }
        
        log.debug("ProgressTracker 리소스를 해제했습니다.");
    }
    
    /**
     * 현재 추적 중인지 확인합니다
     */
    public boolean isTracking() {
        return isTracking.get();
    }
    
    /**
     * 업데이트 간격을 반환합니다 (밀리초)
     */
    public long getUpdateInterval() {
        return UPDATE_INTERVAL_MS;
    }
}