package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PlayerModule은 SyncTune의 플레이어 기능을 구현합니다.
 * SDK 설계에 따라 올바른 이벤트를 발행합니다.
 */
@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);
    
    // 미디어 재생 관련 필드
    private AudioInputStream audioInputStream;
    private Clip audioClip;
    private MusicInfo currentMusic;
    
    // 재생 상태 관리
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong pausePosition = new AtomicLong(0);
    
    // 진행 상황 업데이트를 위한 스케줄러
    private ScheduledExecutorService progressUpdateScheduler;
    
    // 재생 완료 감지를 위한 스레드
    private Thread playbackMonitorThread;
    private final AtomicBoolean shouldMonitor = new AtomicBoolean(false);

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // 진행 상황 업데이트 스케줄러 초기화
        progressUpdateScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerModule-ProgressUpdateThread");
            t.setDaemon(true);
            return t;
        });
        
        log.info("[{}] 모듈 초기화 완료.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        
        // 재생 중이면 정지
        if (isPlaying.get()) {
            stopPlayback();
        }
        
        // 스케줄러 종료
        if (progressUpdateScheduler != null && !progressUpdateScheduler.isShutdown()) {
            progressUpdateScheduler.shutdown();
            try {
                if (!progressUpdateScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    progressUpdateScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                progressUpdateScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 모니터 스레드 정리
        shouldMonitor.set(false);
        if (playbackMonitorThread != null && playbackMonitorThread.isAlive()) {
            playbackMonitorThread.interrupt();
        }
        
        log.info("[{}] 모듈 종료 완료.", getModuleName());
    }

    // ========== 미디어 제어 이벤트 리스너들 ==========
    
    @EventListener
    public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) {
        log.debug("[{}] 재생 요청 수신: {}", getModuleName(), event);
        
        try {
            MusicInfo musicToPlay = event.getMusicToPlay();
            if (musicToPlay != null) {
                // 새로운 곡 재생
                loadAndPlayMusic(musicToPlay);
            } else if (currentMusic != null) {
                // 현재 곡이 있으면 재생/재개
                if (isPaused.get()) {
                    resumePlayback();
                } else {
                    loadAndPlayMusic(currentMusic);
                }
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
            pausePlayback();
        } catch (Exception e) {
            log.error("[{}] 일시정지 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("일시정지 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    @EventListener
    public void onStopRequest(MediaControlEvent.RequestStopEvent event) {
        log.debug("[{}] 정지 요청 수신: {}", getModuleName(), event);
        try {
            stopPlayback();
        } catch (Exception e) {
            log.error("[{}] 정지 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("정지 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    @EventListener
    public void onSeekRequest(MediaControlEvent.RequestSeekEvent event) {
        log.debug("[{}] 탐색 요청 수신: {}ms", getModuleName(), event.getPositionMillis());
        try {
            seekTo(event.getPositionMillis());
        } catch (Exception e) {
            log.error("[{}] 탐색 요청 처리 중 오류", getModuleName(), e);
            publish(new ErrorEvent("탐색 처리 중 오류가 발생했습니다.", e, false));
        }
    }

    // ========== UI 이벤트 리스너들 ==========
    
    @EventListener
    public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
        log.debug("[{}] 메인 창 닫힘 이벤트 수신: {}", getModuleName(), event);
        if (isPlaying.get()) {
            log.info("[{}] 메인 창이 닫혔지만 재생은 계속됩니다.", getModuleName());
        }
    }

    @EventListener
    public void onMainWindowRestored(PlayerUIEvent.MainWindowRestoredEvent event) {
        log.debug("[{}] 메인 창 복원 이벤트 수신: {}", getModuleName(), event);
        // 필요시 UI 요소 새로고침
    }

    // ========== 실제 재생 로직 구현 ==========
    
    private void loadAndPlayMusic(MusicInfo music) {
        try {
            // 기존 재생 중인 음악이 있으면 정지
            if (isPlaying.get()) {
                stopPlayback();
            }
            
            File musicFile = new File(music.getFilePath());
            if (!musicFile.exists()) {
                throw new IOException("음악 파일을 찾을 수 없습니다: " + music.getFilePath());
            }
            
            log.info("[{}] 음악 로딩 중: {}", getModuleName(), music.getTitle());
            
            // 오디오 스트림 생성
            audioInputStream = AudioSystem.getAudioInputStream(musicFile);
            AudioFormat format = audioInputStream.getFormat();
            
            // PCM 형식으로 변환 (필요한 경우)
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false
                );
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            }
            
            // Clip 생성 및 오디오 로드
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            currentMusic = music;
            pausePosition.set(0);
            
            // 재생 시작
            startPlayback();
            
        } catch (UnsupportedAudioFileException e) {
            log.error("[{}] 지원되지 않는 오디오 파일 형식: {}", getModuleName(), music.getFilePath(), e);
            publish(new ErrorEvent("지원되지 않는 오디오 파일 형식입니다.", e, false));
        } catch (IOException e) {
            log.error("[{}] 파일 읽기 오류: {}", getModuleName(), music.getFilePath(), e);
            publish(new ErrorEvent("음악 파일을 읽을 수 없습니다.", e, false));
        } catch (LineUnavailableException e) {
            log.error("[{}] 오디오 라인을 사용할 수 없습니다.", getModuleName(), e);
            publish(new ErrorEvent("오디오 시스템을 사용할 수 없습니다.", e, false));
        } catch (Exception e) {
            log.error("[{}] 음악 로딩 중 예상치 못한 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("음악 로딩 중 오류가 발생했습니다.", e, false));
        }
    }
    
    private void startPlayback() {
        if (audioClip == null) {
            log.warn("[{}] 재생할 오디오 클립이 없습니다.", getModuleName());
            return;
        }
        
        try {
            audioClip.setFramePosition((int) pausePosition.get());
            audioClip.start();
            
            isPlaying.set(true);
            isPaused.set(false);
            
            log.info("[{}] 재생 시작: {}", getModuleName(), currentMusic.getTitle());
            // SDK 설계: PlaybackStartedEvent 발행
            publish(new PlaybackStatusEvent.PlaybackStartedEvent(currentMusic));
            
            // 진행 상황 업데이트 시작
            startProgressUpdates();
            
            // 재생 완료 모니터링 시작
            startPlaybackMonitoring();
            
        } catch (Exception e) {
            log.error("[{}] 재생 시작 중 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("재생 시작 중 오류가 발생했습니다.", e, false));
        }
    }
    
    private void pausePlayback() {
        if (audioClip == null || !isPlaying.get()) {
            log.debug("[{}] 일시정지할 수 없습니다. 재생 중이 아닙니다.", getModuleName());
            return;
        }
        
        try {
            pausePosition.set(audioClip.getFramePosition());
            audioClip.stop();
            
            isPlaying.set(false);
            isPaused.set(true);
            
            log.info("[{}] 일시정지됨", getModuleName());
            // SDK 설계: PlaybackPausedEvent 발행
            publish(new PlaybackStatusEvent.PlaybackPausedEvent());
            
            // 진행 상황 업데이트 중지
            stopProgressUpdates();
            
        } catch (Exception e) {
            log.error("[{}] 일시정지 중 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("일시정지 중 오류가 발생했습니다.", e, false));
        }
    }
    
    private void resumePlayback() {
        if (audioClip == null || !isPaused.get()) {
            log.debug("[{}] 재개할 수 없습니다. 일시정지 상태가 아닙니다.", getModuleName());
            return;
        }
        
        try {
            audioClip.setFramePosition((int) pausePosition.get());
            audioClip.start();
            
            isPlaying.set(true);
            isPaused.set(false);
            
            log.info("[{}] 재생 재개됨", getModuleName());
            // SDK 설계: 일시정지 재개 시 PlaybackStartedEvent 발행
            publish(new PlaybackStatusEvent.PlaybackStartedEvent(currentMusic));
            
            // 진행 상황 업데이트 재시작
            startProgressUpdates();
            
        } catch (Exception e) {
            log.error("[{}] 재생 재개 중 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("재생 재개 중 오류가 발생했습니다.", e, false));
        }
    }
    
    private void stopPlayback() {
        if (audioClip == null) {
            return;
        }
        
        try {
            audioClip.stop();
            audioClip.close();
            
            if (audioInputStream != null) {
                audioInputStream.close();
                audioInputStream = null;
            }
            
            audioClip = null;
            isPlaying.set(false);
            isPaused.set(false);
            pausePosition.set(0);
            
            log.info("[{}] 재생 정지됨", getModuleName());
            // SDK 설계: PlaybackStoppedEvent 발행
            publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
            
            // 진행 상황 업데이트 중지
            stopProgressUpdates();
            
            // 모니터링 중지
            shouldMonitor.set(false);
            
        } catch (Exception e) {
            log.error("[{}] 재생 정지 중 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("재생 정지 중 오류가 발생했습니다.", e, false));
        }
    }
    
    private void seekTo(long positionMillis) {
        if (audioClip == null) {
            log.warn("[{}] 탐색할 수 없습니다. 로드된 음악이 없습니다.", getModuleName());
            return;
        }
        
        try {
            // 밀리초를 프레임 위치로 변환
            AudioFormat format = audioClip.getFormat();
            long framePosition = (long) (positionMillis * format.getFrameRate() / 1000.0);
            
            // 유효한 범위 확인
            long totalFrames = audioClip.getFrameLength();
            framePosition = Math.max(0, Math.min(framePosition, totalFrames - 1));
            
            audioClip.setFramePosition((int) framePosition);
            pausePosition.set(framePosition);
            
            log.debug("[{}] 탐색 완료: {}ms (프레임: {})", getModuleName(), positionMillis, framePosition);
            
            // 현재 재생 시간 업데이트 이벤트 발행
            long currentTimeMillis = (long) (framePosition * 1000.0 / format.getFrameRate());
            long totalTimeMillis = (long) (totalFrames * 1000.0 / format.getFrameRate());
            publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(currentTimeMillis, totalTimeMillis));
            
        } catch (Exception e) {
            log.error("[{}] 탐색 중 오류 발생", getModuleName(), e);
            publish(new ErrorEvent("탐색 중 오류가 발생했습니다.", e, false));
        }
    }
    
    // ========== 진행 상황 업데이트 관련 ==========
    
    private void startProgressUpdates() {
        if (progressUpdateScheduler == null || progressUpdateScheduler.isShutdown()) {
            return;
        }
        
        progressUpdateScheduler.scheduleAtFixedRate(() -> {
            if (isPlaying.get() && audioClip != null && audioClip.isRunning()) {
                try {
                    AudioFormat format = audioClip.getFormat();
                    long currentFrame = audioClip.getFramePosition();
                    long totalFrames = audioClip.getFrameLength();
                    
                    long currentTimeMillis = (long) (currentFrame * 1000.0 / format.getFrameRate());
                    long totalTimeMillis = (long) (totalFrames * 1000.0 / format.getFrameRate());
                    
                    publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(currentTimeMillis, totalTimeMillis));
                    
                } catch (Exception e) {
                    log.error("[{}] 진행 상황 업데이트 중 오류", getModuleName(), e);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS); // 0.5초마다 업데이트
    }
    
    private void stopProgressUpdates() {
        // 현재 실행 중인 태스크들은 자연스럽게 종료되도록 둠
    }
    
    // ========== 재생 완료 모니터링 ==========
    
    private void startPlaybackMonitoring() {
        shouldMonitor.set(true);
        
        playbackMonitorThread = new Thread(() -> {
            while (shouldMonitor.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (audioClip != null && isPlaying.get() && !audioClip.isRunning()) {
                        // 재생이 자연스럽게 끝남
                        log.info("[{}] 재생 완료: {}", getModuleName(), currentMusic.getTitle());
                        
                        MusicInfo completedMusic = currentMusic;
                        
                        // 상태 초기화
                        isPlaying.set(false);
                        isPaused.set(false);
                        pausePosition.set(0);
                        
                        // SDK 설계: 재생 완료 시 PlaybackStoppedEvent 발행
                        // (다음 곡 재생은 별도 로직에서 MusicChangedEvent -> PlaybackStartedEvent 순으로 발행)
                        publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
                        
                        shouldMonitor.set(false);
                        break;
                    }
                    
                    Thread.sleep(100); // 0.1초마다 체크
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[{}] 재생 모니터링 중 오류", getModuleName(), e);
                }
            }
        }, "PlayerModule-PlaybackMonitorThread");
        
        playbackMonitorThread.setDaemon(true);
        playbackMonitorThread.start();
    }
    
    // ========== 상태 조회 메서드들 ==========
    
    public boolean isCurrentlyPlaying() {
        return isPlaying.get();
    }
    
    public boolean isCurrentlyPaused() {
        return isPaused.get();
    }
    
    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }
    
    public long getCurrentPositionMillis() {
        if (audioClip == null) return 0;
        
        try {
            AudioFormat format = audioClip.getFormat();
            long currentFrame = isPlaying.get() ? audioClip.getFramePosition() : pausePosition.get();
            return (long) (currentFrame * 1000.0 / format.getFrameRate());
        } catch (Exception e) {
            log.error("[{}] 현재 위치 조회 중 오류", getModuleName(), e);
            return 0;
        }
    }
    
    public long getTotalDurationMillis() {
        if (audioClip == null || currentMusic == null) return 0;
        
        try {
            AudioFormat format = audioClip.getFormat();
            long totalFrames = audioClip.getFrameLength();
            return (long) (totalFrames * 1000.0 / format.getFrameRate());
        } catch (Exception e) {
            log.error("[{}] 총 길이 조회 중 오류", getModuleName(), e);
            return currentMusic.getDurationMillis();
        }
    }
}