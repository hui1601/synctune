package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.player.playback.AudioEngine;
import ac.cwnu.synctune.player.playback.PlaybackStateManager;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PlayerModule은 SyncTune의 플레이어 기능을 구현합니다.
 * playback 패키지의 AudioEngine과 PlaybackStateManager를 사용합니다.
 */
@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);
    
    // 핵심 컴포넌트들
    private PlaybackStateManager stateManager;
    private AudioEngine audioEngine;
    
    // 진행 상황 업데이트를 위한 스케줄러
    private ScheduledExecutorService progressUpdateScheduler;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // 컴포넌트 초기화
        initializeComponents();
        
        // 진행 상황 업데이트 스케줄러 시작
        startProgressUpdates();
        
        log.info("[{}] 모듈 초기화 완료.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        
        // 컴포넌트 정리
        disposeComponents();
        
        log.info("[{}] 모듈 종료 완료.", getModuleName());
    }
    
    /**
     * 플레이어 컴포넌트들을 초기화합니다
     */
    private void initializeComponents() {
        // 재생 상태 관리자 생성
        stateManager = new PlaybackStateManager();
        
        // 오디오 엔진 생성
        audioEngine = new AudioEngine(eventPublisher, stateManager);
        
        // 진행 상황 업데이트 스케줄러 생성
        progressUpdateScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerModule-ProgressUpdate");
            t.setDaemon(true);
            return t;
        });
        
        log.debug("[{}] 컴포넌트 초기화 완료: PlaybackStateManager, AudioEngine", getModuleName());
    }
    
    /**
     * 모든 컴포넌트를 안전하게 해제합니다
     */
    private void disposeComponents() {
        try {
            // 진행 상황 업데이트 중지
            if (progressUpdateScheduler != null && !progressUpdateScheduler.isShutdown()) {
                progressUpdateScheduler.shutdown();
                if (!progressUpdateScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    progressUpdateScheduler.shutdownNow();
                }
            }
            
            // 오디오 엔진 정리
            if (audioEngine != null) {
                audioEngine.dispose();
            }
            
            // 상태 관리자 초기화
            if (stateManager != null) {
                stateManager.reset();
            }
            
            log.debug("[{}] 모든 컴포넌트가 안전하게 해제되었습니다.", getModuleName());
            
        } catch (Exception e) {
            log.error("[{}] 컴포넌트 해제 중 오류 발생", getModuleName(), e);
        }
    }
    
    /**
     * 진행 상황 업데이트를 시작합니다
     */
    private void startProgressUpdates() {
        if (progressUpdateScheduler == null || progressUpdateScheduler.isShutdown()) {
            return;
        }
        
        progressUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                if (stateManager.isPlaying()) {
                    audioEngine.updateCurrentPosition();
                    
                    // TODO: 진행 상황 업데이트 이벤트 발행 (이벤트 클래스 확인 후 구현)
                    // long currentMs = stateManager.getCurrentPosition();
                    // long totalMs = stateManager.getTotalDuration();
                    // publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(currentMs, totalMs));
                }
            } catch (Exception e) {
                log.error("[{}] 진행 상황 업데이트 중 오류", getModuleName(), e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS); // 0.5초마다 업데이트
    }

    // ========== 이벤트 리스너들 (TODO: 실제 이벤트 클래스 확인 후 구현) ==========
    
    // TODO: 실제 이벤트 클래스들이 확인되면 아래 주석을 해제하고 구현
    /*
    @EventListener
    public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) {
        log.debug("[{}] 재생 요청 수신: {}", getModuleName(), event);
        
        try {
            MusicInfo musicToPlay = event.getMusicToPlay();
            if (musicToPlay != null) {
                // 새로운 곡 재생
                if (audioEngine.loadMusic(musicToPlay)) {
                    audioEngine.play();
                }
            } else if (stateManager.getCurrentMusic() != null) {
                // 현재 곡 재생/재개
                if (stateManager.isPaused()) {
                    audioEngine.play(); // 재개
                } else {
                    // 처음부터 재생
                    audioEngine.seekTo(0);
                    audioEngine.play();
                }
            } else {
                log.warn("[{}] 재생할 곡이 없습니다.", getModuleName());
            }
        } catch (Exception e) {
            log.error("[{}] 재생 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onPauseRequest(MediaControlEvent.RequestPauseEvent event) {
        log.debug("[{}] 일시정지 요청 수신: {}", getModuleName(), event);
        
        try {
            audioEngine.pause();
        } catch (Exception e) {
            log.error("[{}] 일시정지 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onStopRequest(MediaControlEvent.RequestStopEvent event) {
        log.debug("[{}] 정지 요청 수신: {}", getModuleName(), event);
        
        try {
            audioEngine.stop();
        } catch (Exception e) {
            log.error("[{}] 정지 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onSeekRequest(MediaControlEvent.RequestSeekEvent event) {
        log.debug("[{}] 탐색 요청 수신: {}ms", getModuleName(), event.getPositionMillis());
        
        try {
            audioEngine.seekTo(event.getPositionMillis());
        } catch (Exception e) {
            log.error("[{}] 탐색 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
        log.debug("[{}] 메인 창 닫힘 이벤트 수신: {}", getModuleName(), event);
        
        // 창이 닫혀도 재생은 계속되도록 처리
        if (stateManager.isPlaying()) {
            log.info("[{}] 메인 창이 닫혔지만 재생은 계속됩니다.", getModuleName());
        }
    }

    @EventListener
    public void onMainWindowRestored(PlayerUIEvent.MainWindowRestoredEvent event) {
        log.debug("[{}] 메인 창 복원 이벤트 수신: {}", getModuleName(), event);
        
        // 창 복원 시 현재 상태를 UI에 알리기 위해 강제 업데이트
        if (stateManager.getCurrentMusic() != null) {
            // TODO: 강제 진행 상황 업데이트 이벤트 발행
        }
    }
    */

    // ========== 공개 API 메서드들 ==========
    
    /**
     * 음악을 로드하고 재생합니다
     */
    public boolean playMusic(MusicInfo music) {
        if (music == null) {
            log.warn("[{}] 재생할 음악 정보가 null입니다.", getModuleName());
            return false;
        }
        
        try {
            log.info("[{}] 음악 재생 요청: {}", getModuleName(), music.getTitle());
            
            if (audioEngine.loadMusic(music)) {
                return audioEngine.play();
            }
            return false;
            
        } catch (Exception e) {
            log.error("[{}] 음악 재생 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 재생을 시작/재개합니다
     */
    public boolean play() {
        try {
            return audioEngine.play();
        } catch (Exception e) {
            log.error("[{}] 재생 시작 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 재생을 일시정지합니다
     */
    public boolean pause() {
        try {
            return audioEngine.pause();
        } catch (Exception e) {
            log.error("[{}] 일시정지 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 재생을 정지합니다
     */
    public boolean stopPlayback() {
        try {
            return audioEngine.stop();
        } catch (Exception e) {
            log.error("[{}] 정지 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 특정 위치로 탐색합니다 (밀리초)
     */
    public boolean seekTo(long positionMs) {
        try {
            return audioEngine.seekTo(positionMs);
        } catch (Exception e) {
            log.error("[{}] 탐색 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 볼륨을 설정합니다 (0.0 ~ 1.0)
     */
    public boolean setVolume(float volume) {
        try {
            return audioEngine.setVolume(volume);
        } catch (Exception e) {
            log.error("[{}] 볼륨 설정 중 오류", getModuleName(), e);
            return false;
        }
    }
    
    /**
     * 음소거 상태를 설정합니다
     */
    public boolean setMuted(boolean muted) {
        try {
            return audioEngine.setMuted(muted);
        } catch (Exception e) {
            log.error("[{}] 음소거 설정 중 오류", getModuleName(), e);
            return false;
        }
    }

    // ========== 상태 조회 메서드들 ==========
    
    /**
     * 현재 재생 상태를 반환합니다
     */
    public PlaybackStateManager.PlaybackState getPlaybackState() {
        return stateManager != null ? stateManager.getState() : PlaybackStateManager.PlaybackState.STOPPED;
    }
    
    /**
     * 현재 재생 중인지 확인합니다
     */
    public boolean isCurrentlyPlaying() {
        return stateManager != null && stateManager.isPlaying();
    }
    
    /**
     * 현재 일시정지 중인지 확인합니다
     */
    public boolean isCurrentlyPaused() {
        return stateManager != null && stateManager.isPaused();
    }
    
    /**
     * 현재 재생 중인 음악 정보를 반환합니다
     */
    public MusicInfo getCurrentMusic() {
        return stateManager != null ? stateManager.getCurrentMusic() : null;
    }
    
    /**
     * 현재 재생 위치를 밀리초로 반환합니다
     */
    public long getCurrentPositionMillis() {
        return stateManager != null ? stateManager.getCurrentPosition() : 0;
    }
    
    /**
     * 총 재생 시간을 밀리초로 반환합니다
     */
    public long getTotalDurationMillis() {
        return stateManager != null ? stateManager.getTotalDuration() : 0;
    }
    
    /**
     * 현재 볼륨을 반환합니다 (0.0 ~ 1.0)
     */
    public float getCurrentVolume() {
        return stateManager != null ? stateManager.getVolume() : 1.0f;
    }
    
    /**
     * 음소거 상태인지 확인합니다
     */
    public boolean isMuted() {
        return stateManager != null && stateManager.isMuted();
    }
    
    /**
     * 재생 진행률을 반환합니다 (0.0 ~ 1.0)
     */
    public double getProgress() {
        return stateManager != null ? stateManager.getProgress() : 0.0;
    }
    
    /**
     * 현재 상태를 포맷팅된 문자열로 반환합니다 (디버깅용)
     */
    public String getFormattedStatus() {
        return stateManager != null ? stateManager.getFormattedStatus() : "PlayerModule not initialized";
    }
    
    // ========== 컴포넌트 접근자 (테스트용) ==========
    
    /**
     * PlaybackStateManager 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected PlaybackStateManager getStateManager() {
        return stateManager;
    }
    
    /**
     * AudioEngine 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected AudioEngine getAudioEngine() {
        return audioEngine;
    }
}