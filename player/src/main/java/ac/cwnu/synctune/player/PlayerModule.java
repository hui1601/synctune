package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * PlayerModule은 SyncTune의 플레이어 기능을 구현합니다.
 * 이벤트 처리와 컴포넌트 조율을 담당하는 메인 모듈입니다.
 */
@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);
    
    // 핵심 컴포넌트들
    private PlaybackState playbackState;
    private AudioEngine audioEngine;
    private ProgressTracker progressTracker;
    private PlaybackMonitor playbackMonitor;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // 컴포넌트 초기화
        initializeComponents();
        
        log.info("[{}] 모든 컴포넌트 초기화 완료.", getModuleName());
    }
    
    /**
     * 플레이어 컴포넌트들을 초기화합니다
     */
    private void initializeComponents() {
        // 재생 상태 관리자 생성
        playbackState = new PlaybackState();
        
        // 오디오 엔진 생성
        audioEngine = new AudioEngine(eventPublisher, playbackState);
        
        // 진행 상황 추적기 생성
        progressTracker = new ProgressTracker(eventPublisher, playbackState, audioEngine);
        
        // 재생 완료 모니터 생성
        playbackMonitor = new PlaybackMonitor(eventPublisher, playbackState, audioEngine);
        
        log.debug("[{}] 컴포넌트 초기화: PlaybackState, AudioEngine, ProgressTracker, PlaybackMonitor", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        
        // 컴포넌트들을 역순으로 정리
        disposeComponents();
        
        log.info("[{}] 모듈 종료 완료.", getModuleName());
    }
    
    /**
     * 모든 컴포넌트를 안전하게 해제합니다
     */
    private void disposeComponents() {
        try {
            // 모니터링 및 추적 중지
            if (playbackMonitor != null) {
                playbackMonitor.dispose();
            }
            
            if (progressTracker != null) {
                progressTracker.dispose();
            }
            
            // 오디오 엔진 정리
            if (audioEngine != null) {
                audioEngine.dispose();
            }
            
            // 상태 초기화
            if (playbackState != null) {
                playbackState.reset();
            }
            
            log.debug("[{}] 모든 컴포넌트가 안전하게 해제되었습니다.", getModuleName());
            
        } catch (Exception e) {
            log.error("[{}] 컴포넌트 해제 중 오류 발생", getModuleName(), e);
        }
    }

    // ========== 미디어 제어 이벤트 리스너들 ==========
    
    @EventListener
    public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) {
        log.debug("[{}] 재생 요청 수신: {}", getModuleName(), event);
        
        try {
            MusicInfo musicToPlay = event.getMusicToPlay();
            
            if (musicToPlay != null) {
                // 새로운 곡 재생
                handlePlayNewMusic(musicToPlay);
            } else if (playbackState.getCurrentMusic() != null) {
                // 현재 곡이 있으면 재생/재개
                handlePlayCurrentMusic();
            } else {
                log.warn("[{}] 재생할 곡이 지정되지 않았습니다.", getModuleName());
                publish(new ErrorEvent("재생할 곡이 없습니다.", null, false));
            }
            
        } catch (Exception e) {
            log.error("[{}] 재생 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("재생 요청 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    @EventListener
    public void onPauseRequest(MediaControlEvent.RequestPauseEvent event) {
        log.debug("[{}] 일시정지 요청 수신: {}", getModuleName(), event);
        
        try {
            if (audioEngine.pause()) {
                progressTracker.stopTracking();
                playbackMonitor.stopMonitoring();
            }
        } catch (Exception e) {
            log.error("[{}] 일시정지 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("일시정지 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    @EventListener
    public void onStopRequest(MediaControlEvent.RequestStopEvent event) {
        log.debug("[{}] 정지 요청 수신: {}", getModuleName(), event);
        
        try {
            if (audioEngine.stop()) {
                progressTracker.stopTracking();
                playbackMonitor.stopMonitoring();
            }
        } catch (Exception e) {
            log.error("[{}] 정지 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("정지 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    @EventListener
    public void onSeekRequest(MediaControlEvent.RequestSeekEvent event) {
        log.debug("[{}] 탐색 요청 수신: {}ms", getModuleName(), event.getPositionMillis());
        
        try {
            audioEngine.seekTo(event.getPositionMillis());
            // 탐색 후 즉시 진행 상황 업데이트
            progressTracker.forceUpdate();
        } catch (Exception e) {
            log.error("[{}] 탐색 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("탐색 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    // ========== UI 이벤트 리스너들 ==========
    
    @EventListener
    public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
        log.debug("[{}] 메인 창 닫힘 이벤트 수신: {}", getModuleName(), event);
        
        // 창이 닫혀도 재생은 계속되도록 처리
        if (playbackState.isPlaying()) {
            log.info("[{}] 메인 창이 닫혔지만 재생은 계속됩니다.", getModuleName());
        }
        
        // 필요시 상태 저장 등의 로직 추가 가능
    }

    @EventListener
    public void onMainWindowRestored(PlayerUIEvent.MainWindowRestoredEvent event) {
        log.debug("[{}] 메인 창 복원 이벤트 수신: {}", getModuleName(), event);
        
        // 창 복원 시 현재 상태를 UI에 알리기 위해 강제 업데이트
        if (playbackState.getCurrentMusic() != null) {
            progressTracker.forceUpdate();
        }
    }

    // ========== 재생 처리 헬퍼 메서드들 ==========
    
    /**
     * 새로운 음악을 재생합니다
     */
    private void handlePlayNewMusic(MusicInfo music) {
        // 기존 재생 중단
        if (playbackState.isPlaying()) {
            progressTracker.stopTracking();
            playbackMonitor.stopMonitoring();
        }
        
        // 새 음악 로드 및 재생
        if (audioEngine.loadAndPlay(music)) {
            // 재생 성공 시 추적 및 모니터링 시작
            progressTracker.startTracking();
            playbackMonitor.startMonitoring();
        }
    }
    
    /**
     * 현재 음악을 재생/재개합니다
     */
    private void handlePlayCurrentMusic() {
        boolean success = false;
        
        if (playbackState.isPaused()) {
            // 일시정지 상태면 재개
            success = audioEngine.resume();
        } else {
            // 정지 상태면 처음부터 재생
            success = audioEngine.start();
        }
        
        if (success) {
            // 재생 성공 시 추적 및 모니터링 시작
            progressTracker.startTracking();
            playbackMonitor.startMonitoring();
        }
    }

    // ========== 상태 조회 메서드들 (외부 접근용) ==========
    
    /**
     * 현재 재생 중인지 확인합니다
     */
    public boolean isCurrentlyPlaying() {
        return playbackState != null && playbackState.isPlaying();
    }
    
    /**
     * 현재 일시정지 중인지 확인합니다
     */
    public boolean isCurrentlyPaused() {
        return playbackState != null && playbackState.isPaused();
    }
    
    /**
     * 현재 재생 중인 음악 정보를 반환합니다
     */
    public MusicInfo getCurrentMusic() {
        return playbackState != null ? playbackState.getCurrentMusic() : null;
    }
    
    /**
     * 현재 재생 위치를 밀리초로 반환합니다
     */
    public long getCurrentPositionMillis() {
        return audioEngine != null ? audioEngine.getCurrentPositionMillis() : 0;
    }
    
    /**
     * 총 재생 시간을 밀리초로 반환합니다
     */
    public long getTotalDurationMillis() {
        return audioEngine != null ? audioEngine.getTotalDurationMillis() : 0;
    }
    
    /**
     * 현재 재생 상태를 문자열로 반환합니다 (디버깅용)
     */
    public String getPlaybackStateDescription() {
        if (playbackState == null) {
            return "NOT_INITIALIZED";
        }
        return playbackState.getStateDescription();
    }
    
    // ========== 컴포넌트 접근자 (테스트 또는 고급 사용자용) ==========
    
    /**
     * PlaybackState 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected PlaybackState getPlaybackState() {
        return playbackState;
    }
    
    /**
     * AudioEngine 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected AudioEngine getAudioEngine() {
        return audioEngine;
    }
    
    /**
     * ProgressTracker 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected ProgressTracker getProgressTracker() {
        return progressTracker;
    }
    
    /**
     * PlaybackMonitor 인스턴스를 반환합니다 (주로 테스트용)
     */
    protected PlaybackMonitor getPlaybackMonitor() {
        return playbackMonitor;
    }
}