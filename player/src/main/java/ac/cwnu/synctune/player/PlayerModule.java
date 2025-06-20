package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
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
        
        // 테스트용 샘플 음악 생성 및 재생 시작
        createAndPlaySampleMusic();
        
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
                    
                    // 진행 상황 업데이트 이벤트 발행
                    long currentMs = stateManager.getCurrentPosition();
                    long totalMs = stateManager.getTotalDuration();
                    publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(currentMs, totalMs));
                }
            } catch (Exception e) {
                log.error("[{}] 진행 상황 업데이트 중 오류", getModuleName(), e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS); // 0.5초마다 업데이트
    }

    /**
     * 테스트용 샘플 음악을 생성하고 재생합니다
     */
    private void createAndPlaySampleMusic() {
        // 샘플 음악 정보 생성
        MusicInfo sampleMusic = new MusicInfo(
            "샘플 테스트 음악",
            "SyncTune System",
            "Test Album",
            "sample/test.mp3", // 실제로는 존재하지 않지만 테스트용
            180000L, // 3분
            "sample/sample.lrc"
        );
        
        // 상태 매니저에 현재 음악 설정
        stateManager.setCurrentMusic(sampleMusic);
        stateManager.setTotalDuration(sampleMusic.getDurationMillis());
        
        // 재생 시작 이벤트 발행
        log.info("[{}] 샘플 음악 재생 시작: {}", getModuleName(), sampleMusic.getTitle());
        publish(new PlaybackStatusEvent.PlaybackStartedEvent(sampleMusic));
        
        // 재생 상태를 PLAYING으로 변경
        stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
        
        // 시뮬레이션된 재생 시간 업데이트 시작
        startSimulatedPlayback();
    }
    
    /**
     * 실제 오디오 파일 없이 재생을 시뮬레이션합니다
     */
    private void startSimulatedPlayback() {
        // 시뮬레이션된 재생 진행을 위한 별도 스케줄러
        ScheduledExecutorService simulationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerModule-PlaybackSimulation");
            t.setDaemon(true);
            return t;
        });
        
        simulationScheduler.scheduleAtFixedRate(() -> {
            try {
                if (stateManager.isPlaying()) {
                    long currentPos = stateManager.getCurrentPosition();
                    long totalDuration = stateManager.getTotalDuration();
                    
                    // 500ms씩 시간 증가 (실제 재생 시뮬레이션)
                    long newPos = currentPos + 500;
                    
                    if (newPos >= totalDuration) {
                        // 재생 완료
                        stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
                        stateManager.setCurrentPosition(0);
                        publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
                        log.info("[{}] 시뮬레이션된 재생 완료", getModuleName());
                        simulationScheduler.shutdown();
                    } else {
                        stateManager.setCurrentPosition(newPos);
                    }
                }
            } catch (Exception e) {
                log.error("[{}] 재생 시뮬레이션 중 오류", getModuleName(), e);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    // ========== 이벤트 리스너들 ==========
    
    @EventListener
    public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) {
        log.debug("[{}] 재생 요청 수신: {}", getModuleName(), event);
        
        try {
            MusicInfo musicToPlay = event.getMusicToPlay();
            if (musicToPlay != null) {
                // 새로운 곡 재생
                stateManager.setCurrentMusic(musicToPlay);
                stateManager.setTotalDuration(musicToPlay.getDurationMillis());
                stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
                publish(new PlaybackStatusEvent.PlaybackStartedEvent(musicToPlay));
                log.info("[{}] 새 곡 재생 시작: {}", getModuleName(), musicToPlay.getTitle());
            } else if (stateManager.getCurrentMusic() != null) {
                // 현재 곡 재생/재개
                if (stateManager.isPaused()) {
                    stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
                    log.info("[{}] 재생 재개", getModuleName());
                } else {
                    // 처음부터 재생
                    stateManager.setCurrentPosition(0);
                    stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
                    publish(new PlaybackStatusEvent.PlaybackStartedEvent(stateManager.getCurrentMusic()));
                    log.info("[{}] 재생 시작", getModuleName());
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
            if (stateManager.isPlaying()) {
                stateManager.setState(PlaybackStateManager.PlaybackState.PAUSED);
                publish(new PlaybackStatusEvent.PlaybackPausedEvent());
                log.info("[{}] 일시정지됨", getModuleName());
            }
        } catch (Exception e) {
            log.error("[{}] 일시정지 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onStopRequest(MediaControlEvent.RequestStopEvent event) {
        log.debug("[{}] 정지 요청 수신: {}", getModuleName(), event);
        
        try {
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            stateManager.setCurrentPosition(0);
            publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
            log.info("[{}] 정지됨", getModuleName());
        } catch (Exception e) {
            log.error("[{}] 정지 요청 처리 중 오류", getModuleName(), e);
        }
    }

    @EventListener
    public void onSeekRequest(MediaControlEvent.RequestSeekEvent event) {
        log.debug("[{}] 탐색 요청 수신: {}ms", getModuleName(), event.getPositionMillis());
        
        try {
            long totalDuration = stateManager.getTotalDuration();
            long seekPosition = Math.max(0, Math.min(event.getPositionMillis(), totalDuration));
            stateManager.setCurrentPosition(seekPosition);
            log.info("[{}] 탐색 완료: {}ms", getModuleName(), seekPosition);
        } catch (Exception e) {
            log.error("[{}] 탐색 요청 처리 중 오류", getModuleName(), e);
        }
    }

    // ========== 공개 API 메서드들 ==========
    
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
     * 현재 상태를 포맷팅된 문자열로 반환합니다 (디버깅용)
     */
    public String getFormattedStatus() {
        return stateManager != null ? stateManager.getFormattedStatus() : "PlayerModule not initialized";
    }
}