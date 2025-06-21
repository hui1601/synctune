package ac.cwnu.synctune.player;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import ac.cwnu.synctune.player.playback.AudioEngine;
import ac.cwnu.synctune.player.playback.PlaybackStateManager;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;

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
    
    // 시뮬레이션 재생을 위한 스케줄러
    private ScheduledExecutorService simulationScheduler;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // 컴포넌트 초기화
        initializeComponents();
        
        // 진행 상황 업데이트 스케줄러 시작
        startProgressUpdates();
        
        // 자동 재생 제거 - 사용자가 직접 재생 버튼을 눌러야 재생됨
        log.info("[{}] 모듈 초기화 완료. 재생할 음악을 선택하세요.", getModuleName());
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
            
            // 시뮬레이션 스케줄러 정리
            stopSimulatedPlayback();
            
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
     * 샘플 음악을 준비합니다 (재생하지는 않음)
     */
    private MusicInfo prepareSampleMusic() {
        // 먼저 실제 음악 파일을 찾아보기
        MusicInfo realMusic = findRealMusicFile();
        
        if (realMusic != null) {
            log.info("[{}] 실제 음악 파일 발견: {}", getModuleName(), realMusic.getTitle());
            return realMusic;
        } else {
            // 실제 파일이 없으면 샘플 음악 정보만 생성
            log.info("[{}] 실제 음악 파일이 없어 샘플 음악 정보를 준비합니다.", getModuleName());
            return createSampleMusicInfo();
        }
    }
    
    /**
     * 실제 음악 파일을 찾습니다
     */
    private MusicInfo findRealMusicFile() {
        // 1. music 폴더에서 음악 파일 찾기
        File musicDir = new File("music");
        if (!musicDir.exists()) {
            log.debug("music 폴더가 존재하지 않습니다.");
            return null;
        }
        
        File[] musicFiles = musicDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || 
                   lowerName.endsWith(".flac") || lowerName.endsWith(".m4a") ||
                   lowerName.endsWith(".aac") || lowerName.endsWith(".ogg");
        });
        
        if (musicFiles == null || musicFiles.length == 0) {
            log.debug("music 폴더에 음악 파일이 없습니다.");
            return null;
        }
        
        // 첫 번째 음악 파일 사용
        File musicFile = musicFiles[0];
        String baseName = getFileNameWithoutExtension(musicFile.getName());
        
        // 해당하는 LRC 파일 찾기
        String lrcPath = findLrcFile(musicFile);
        
        // 음악 파일 정보 생성
        MusicInfo musicInfo = new MusicInfo(
            baseName,  // 파일명을 제목으로 사용
            "Unknown Artist",
            "Unknown Album", 
            musicFile.getAbsolutePath(),
            180000L, // 기본 3분 (실제로는 메타데이터에서 추출해야 함)
            lrcPath
        );
        
        log.info("[{}] 음악 파일 정보 생성: {} (LRC: {})", 
            getModuleName(), musicInfo.getTitle(), lrcPath != null ? "있음" : "없음");
        
        return musicInfo;
    }
    
    /**
     * 음악 파일에 해당하는 LRC 파일을 찾습니다
     */
    private String findLrcFile(File musicFile) {
        String baseName = getFileNameWithoutExtension(musicFile.getName());
        
        // 1. 음악 파일과 같은 폴더에서 찾기
        File lrcInSameDir = new File(musicFile.getParent(), baseName + ".lrc");
        if (lrcInSameDir.exists()) {
            log.debug("같은 폴더에서 LRC 발견: {}", lrcInSameDir.getAbsolutePath());
            return lrcInSameDir.getAbsolutePath();
        }
        
        // 2. lyrics 폴더에서 찾기
        File lrcInLyricsDir = new File("lyrics", baseName + ".lrc");
        if (lrcInLyricsDir.exists()) {
            log.debug("lyrics 폴더에서 LRC 발견: {}", lrcInLyricsDir.getAbsolutePath());
            return lrcInLyricsDir.getAbsolutePath();
        }
        
        log.debug("LRC 파일을 찾을 수 없음: {}", baseName);
        return null;
    }
    
    /**
     * 음악을 재생합니다 (실제 오디오 파일 또는 시뮬레이션)
     */
    private void playMusic(MusicInfo music) {
        // 기존 시뮬레이션 스케줄러 정리
        stopSimulatedPlayback();
        
        // 상태 매니저에 현재 음악 설정
        stateManager.setCurrentMusic(music);
        stateManager.setTotalDuration(music.getDurationMillis());
        
        // 재생 시작 이벤트 발행
        publish(new PlaybackStatusEvent.PlaybackStartedEvent(music));
        
        // 실제 오디오 파일 재생 시도
        boolean realAudioLoaded = audioEngine.loadMusic(music);
        
        if (realAudioLoaded && audioEngine.play()) {
            // 실제 오디오 재생 성공
            log.info("[{}] 실제 오디오 재생 시작: {}", getModuleName(), music.getTitle());
            stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
        } else {
            // 실제 오디오 재생 실패 시 시뮬레이션 모드
            log.info("[{}] 실제 오디오 재생 실패, 시뮬레이션 모드로 전환: {}", getModuleName(), music.getTitle());
            stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
            startSimulatedPlayback();
        }
    }
    
    /**
     * 샘플 음악 정보만 생성 (재생하지 않음)
     */
    private MusicInfo createSampleMusicInfo() {
        return new MusicInfo(
            "샘플 테스트 음악",
            "SyncTune System",
            "Test Album",
            "sample/test.mp3", // 실제로는 존재하지 않지만 테스트용
            180000L, // 3분
            "sample/sample.lrc"
        );
    }
    
    /**
     * 파일명에서 확장자를 제거합니다
     */
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * 시뮬레이션 재생을 정지합니다
     */
    private void stopSimulatedPlayback() {
        if (simulationScheduler != null && !simulationScheduler.isShutdown()) {
            simulationScheduler.shutdown();
            try {
                if (!simulationScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    simulationScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                simulationScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 실제 오디오 파일 없이 재생을 시뮬레이션합니다
     */
    private void startSimulatedPlayback() {
        // 기존 시뮬레이션 스케줄러 정리
        stopSimulatedPlayback();
        
        // 새로운 시뮬레이션 스케줄러 생성
        simulationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
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
                playMusic(musicToPlay);
            } else if (stateManager.getCurrentMusic() != null) {
                // 현재 곡 재생/재개
                if (stateManager.isPaused()) {
                    // 일시정지에서 재개 - 시뮬레이션 재개
                    stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
                    startSimulatedPlayback();
                    log.info("[{}] 재생 재개", getModuleName());
                } else {
                    // 처음부터 재생
                    stateManager.setCurrentPosition(0);
                    playMusic(stateManager.getCurrentMusic());
                    log.info("[{}] 재생 시작", getModuleName());
                }
            } else {
                // 재생할 곡이 없으면 샘플 음악 준비
                log.info("[{}] 재생할 곡이 없어 샘플 음악을 준비합니다.", getModuleName());
                MusicInfo sampleMusic = prepareSampleMusic();
                playMusic(sampleMusic);
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
                // 실제 오디오 일시정지 (AudioEngine.pause() 메서드가 있다면)
                // audioEngine.pause();
                
                // 시뮬레이션 정지
                stopSimulatedPlayback();
                
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
            // 실제 오디오 정지 (AudioEngine.stop() 메서드가 있다면)
            // audioEngine.stop();
            
            // 시뮬레이션 정지
            stopSimulatedPlayback();
            
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
            
            // 시뮬레이션에서 탐색 (AudioEngine.seek() 메서드가 없으므로)
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