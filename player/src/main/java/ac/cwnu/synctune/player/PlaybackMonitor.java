package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 재생 완료를 감지하고 관련 이벤트를 발행하는 클래스
 */
public class PlaybackMonitor {
    private static final Logger log = LogManager.getLogger(PlaybackMonitor.class);
    
    // 모니터링 체크 간격 (밀리초)
    private static final long CHECK_INTERVAL_MS = 100;
    
    private final EventPublisher eventPublisher;
    private final PlaybackState playbackState;
    private final AudioEngine audioEngine;
    
    // 모니터링 관련
    private Thread monitorThread;
    private final AtomicBoolean shouldMonitor = new AtomicBoolean(false);
    
    public PlaybackMonitor(EventPublisher eventPublisher, PlaybackState playbackState, AudioEngine audioEngine) {
        this.eventPublisher = eventPublisher;
        this.playbackState = playbackState;
        this.audioEngine = audioEngine;
    }
    
    /**
     * 재생 완료 모니터링을 시작합니다
     */
    public void startMonitoring() {
        if (shouldMonitor.get()) {
            log.debug("이미 재생 완료 모니터링이 시작되어 있습니다.");
            return;
        }
        
        shouldMonitor.set(true);
        
        monitorThread = new Thread(this::monitorPlayback, "PlayerModule-PlaybackMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        
        log.debug("재생 완료 모니터링을 시작했습니다.");
    }
    
    /**
     * 재생 완료 모니터링을 중지합니다
     */
    public void stopMonitoring() {
        if (!shouldMonitor.get()) {
            return;
        }
        
        shouldMonitor.set(false);
        
        if (monitorThread != null && monitorThread.isAlive()) {
            monitorThread.interrupt();
            try {
                monitorThread.join(1000); // 1초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        log.debug("재생 완료 모니터링을 중지했습니다.");
    }
    
    /**
     * 재생 상태를 모니터링하는 메인 루프
     */
    private void monitorPlayback() {
        while (shouldMonitor.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // 재생 중인데 오디오 클립이 멈춘 경우 = 재생 완료
                if (playbackState.isPlaying() && !audioEngine.isAudioClipRunning()) {
                    handlePlaybackCompleted();
                    break; // 재생 완료 후 모니터링 중지
                }
                
                Thread.sleep(CHECK_INTERVAL_MS);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("재생 완료 모니터링 스레드가 인터럽트되었습니다.");
                break;
            } catch (Exception e) {
                log.error("재생 완료 모니터링 중 오류 발생", e);
                // 오류가 발생해도 모니터링은 계속함
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.debug("재생 완료 모니터링 루프가 종료되었습니다.");
    }
    
    /**
     * 재생 완료를 처리합니다
     */
    private void handlePlaybackCompleted() {
        MusicInfo completedMusic = playbackState.getCurrentMusic();
        
        // 상태를 정지로 변경
        playbackState.reset();
        
        log.info("재생 완료: {}", 
            completedMusic != null ? completedMusic.getTitle() : "Unknown");
        
        // 재생 완료 이벤트 발행
        eventPublisher.publish(new PlaybackStatusEvent.PlaybackCompletedEvent(completedMusic));
        
        // 모니터링 중지
        shouldMonitor.set(false);
    }
    
    /**
     * 강제로 재생 완료를 트리거합니다 (테스트 또는 특수 상황용)
     */
    public void forceCompletion() {
        if (playbackState.getCurrentMusic() != null) {
            handlePlaybackCompleted();
        }
    }
    
    /**
     * 현재 모니터링 중인지 확인합니다
     */
    public boolean isMonitoring() {
        return shouldMonitor.get();
    }
    
    /**
     * 리소스를 해제합니다
     */
    public void dispose() {
        stopMonitoring();
        log.debug("PlaybackMonitor 리소스를 해제했습니다.");
    }
}