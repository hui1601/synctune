package ac.cwnu.synctune.player.playback;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 실제 오디오 파일 재생/정지/탐색을 담당하는 엔진
 * javax.sound.sampled를 사용한 실제 구현
 */
public class AudioEngine {
    private static final Logger log = LogManager.getLogger(AudioEngine.class);
    
    private final EventPublisher eventPublisher;
    private final PlaybackStateManager stateManager;
    
    // 오디오 관련 객체들
    private AudioInputStream audioInputStream;
    private Clip audioClip;
    private FloatControl volumeControl;
    
    // 재생 완료 감지
    private final AtomicBoolean shouldMonitor = new AtomicBoolean(false);
    private Thread monitorThread;

    public AudioEngine(EventPublisher eventPublisher, PlaybackStateManager stateManager) {
        this.eventPublisher = eventPublisher;
        this.stateManager = stateManager;
    }
    
    /**
     * 음악 파일을 비동기로 로드합니다
     */
    public CompletableFuture<Boolean> loadMusicAsync(MusicInfo music) {
        return CompletableFuture.supplyAsync(() -> loadMusic(music));
    }
    
    /**
     * 음악 파일을 로드합니다
     */
    public boolean loadMusic(MusicInfo music) {
        if (music == null) {
            log.warn("로드할 음악 정보가 null입니다.");
            return false;
        }
        
        try {
            stateManager.setState(PlaybackStateManager.PlaybackState.LOADING);
            
            // 기존 리소스 정리
            releaseResources();
            
            File musicFile = new File(music.getFilePath());
            if (!musicFile.exists()) {
                log.warn("음악 파일을 찾을 수 없습니다: {} (시뮬레이션 모드로 전환)", music.getFilePath());
                // 파일이 없으면 시뮬레이션 모드로 진행
                stateManager.setCurrentMusic(music);
                stateManager.setTotalDuration(music.getDurationMillis());
                stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
                return true;
            }
            
            log.info("실제 오디오 파일 로딩 시작: {}", music.getTitle());
            
            // 오디오 스트림 생성
            audioInputStream = AudioSystem.getAudioInputStream(musicFile);
            AudioFormat format = audioInputStream.getFormat();
            
            // PCM 형식으로 변환 (필요한 경우)
            AudioFormat targetFormat = getTargetFormat(format);
            if (!format.equals(targetFormat)) {
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                format = targetFormat;
            }
            
            // Clip 생성 및 오디오 로드
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            // 볼륨 컨트롤 설정
            setupVolumeControl();
            
            // 상태 업데이트
            stateManager.setCurrentMusic(music);
            long durationMs = (long) (audioClip.getFrameLength() * 1000.0 / format.getFrameRate());
            stateManager.setTotalDuration(durationMs);
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            
            log.info("실제 오디오 파일 로딩 완료: {} ({}ms)", music.getTitle(), durationMs);
            return true;
            
        } catch (UnsupportedAudioFileException e) {
            log.error("지원되지 않는 오디오 파일 형식: {} (시뮬레이션 모드로 전환)", music.getFilePath(), e);
            // 지원되지 않는 형식이면 시뮬레이션 모드로 진행
            stateManager.setCurrentMusic(music);
            stateManager.setTotalDuration(music.getDurationMillis());
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            return true;
        } catch (IOException e) {
            log.error("파일 읽기 오류: {}", music.getFilePath(), e);
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            return false;
        } catch (LineUnavailableException e) {
            log.error("오디오 라인을 사용할 수 없습니다.", e);
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            return false;
        } catch (Exception e) {
            log.error("음악 로딩 중 예상치 못한 오류 발생", e);
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            return false;
        }
    }
    
    /**
     * 재생을 시작합니다
     */
    public boolean play() {
        if (audioClip == null) {
            log.debug("오디오 클립이 없습니다. 시뮬레이션 모드입니다.");
            return false;
        }
        
        try {
            // 일시정지 상태였다면 해당 위치부터 재생
            if (stateManager.isPaused()) {
                long pausePosition = stateManager.getPausePosition();
                seekToPosition(pausePosition);
            }
            
            audioClip.start();
            stateManager.setState(PlaybackStateManager.PlaybackState.PLAYING);
            
            // 재생 완료 모니터링 시작
            startPlaybackMonitoring();
            
            log.info("실제 오디오 재생 시작: {}", stateManager.getCurrentMusic().getTitle());
            return true;
            
        } catch (Exception e) {
            log.error("재생 시작 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 재생을 일시정지합니다
     */
    public boolean pause() {
        if (audioClip == null || !stateManager.isPlaying()) {
            log.debug("일시정지할 수 없습니다. 재생 중이 아닙니다.");
            return false;
        }
        
        try {
            audioClip.stop();
            stateManager.setState(PlaybackStateManager.PlaybackState.PAUSED);
            stopPlaybackMonitoring();
            
            log.info("실제 오디오 일시정지됨");
            return true;
            
        } catch (Exception e) {
            log.error("일시정지 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 재생을 정지합니다
     */
    public boolean stop() {
        try {
            if (audioClip != null) {
                audioClip.stop();
            }
            
            stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
            stopPlaybackMonitoring();
            
            log.info("실제 오디오 재생 정지됨");
            return true;
            
        } catch (Exception e) {
            log.error("재생 정지 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 특정 위치로 탐색합니다 (밀리초)
     */
    public boolean seekTo(long positionMs) {
        if (audioClip == null) {
            log.warn("탐색할 수 없습니다. 로드된 음악이 없습니다.");
            return false;
        }
        
        try {
            seekToPosition(positionMs);
            stateManager.setCurrentPosition(positionMs);
            
            log.debug("탐색 완료: {}ms", positionMs);
            return true;
            
        } catch (Exception e) {
            log.error("탐색 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 볼륨을 설정합니다 (0.0 ~ 1.0)
     */
    public boolean setVolume(float volume) {
        stateManager.setVolume(volume);
        return applyVolumeSettings();
    }
    
    /**
     * 음소거 상태를 설정합니다
     */
    public boolean setMuted(boolean muted) {
        stateManager.setMuted(muted);
        return applyVolumeSettings();
    }
    
    /**
     * 현재 재생 위치를 업데이트합니다
     */
    public void updateCurrentPosition() {
        if (audioClip != null && stateManager.isPlaying()) {
            try {
                AudioFormat format = audioClip.getFormat();
                long currentFrame = audioClip.getFramePosition();
                long currentTimeMs = (long) (currentFrame * 1000.0 / format.getFrameRate());
                stateManager.setCurrentPosition(currentTimeMs);
            } catch (Exception e) {
                log.error("재생 위치 업데이트 중 오류", e);
            }
        }
    }
    
    /**
     * 오디오 클립이 실행 중인지 확인합니다
     */
    public boolean isAudioClipRunning() {
        return audioClip != null && audioClip.isRunning();
    }
    
    /**
     * 리소스를 해제하고 정리합니다
     */
    public void dispose() {
        stop();
        releaseResources();
    }
    
    // ========== Private 헬퍼 메서드들 ==========
    
    private AudioFormat getTargetFormat(AudioFormat sourceFormat) {
        // PCM 형식이 아니면 변환
        if (sourceFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false
            );
        }
        return sourceFormat;
    }
    
    private void setupVolumeControl() {
        try {
            if (audioClip != null && audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                applyVolumeSettings();
            } else {
                log.warn("볼륨 컨트롤이 지원되지 않습니다.");
            }
        } catch (Exception e) {
            log.error("볼륨 컨트롤 설정 중 오류", e);
        }
    }
    
    private boolean applyVolumeSettings() {
        if (volumeControl == null) {
            return false;
        }
        
        try {
            float effectiveVolume = stateManager.getEffectiveVolume();
            
            // 볼륨을 데시벨로 변환 (0.0 ~ 1.0 -> dB)
            float gainDB;
            if (effectiveVolume <= 0.0f) {
                gainDB = volumeControl.getMinimum();
            } else {
                // 로그 스케일 변환
                float range = volumeControl.getMaximum() - volumeControl.getMinimum();
                gainDB = volumeControl.getMinimum() + (range * effectiveVolume);
            }
            
            volumeControl.setValue(gainDB);
            return true;
            
        } catch (Exception e) {
            log.error("볼륨 적용 중 오류", e);
            return false;
        }
    }
    
    private void seekToPosition(long positionMs) {
        if (audioClip == null) return;
        
        AudioFormat format = audioClip.getFormat();
        long framePosition = (long) (positionMs * format.getFrameRate() / 1000.0);
        
        // 유효한 범위 확인
        long totalFrames = audioClip.getFrameLength();
        framePosition = Math.max(0, Math.min(framePosition, totalFrames - 1));
        
        audioClip.setFramePosition((int) framePosition);
    }
    
    private void startPlaybackMonitoring() {
        shouldMonitor.set(true);
        
        monitorThread = new Thread(() -> {
            while (shouldMonitor.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // 재생 위치 업데이트
                    updateCurrentPosition();
                    
                    // 재생 완료 감지
                    if (stateManager.isPlaying() && !isAudioClipRunning()) {
                        handlePlaybackCompleted();
                        break;
                    }
                    
                    Thread.sleep(100); // 100ms마다 체크
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("재생 모니터링 중 오류", e);
                }
            }
        }, "AudioEngine-PlaybackMonitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    private void stopPlaybackMonitoring() {
        shouldMonitor.set(false);
        if (monitorThread != null && monitorThread.isAlive()) {
            monitorThread.interrupt();
        }
    }
    
    private void handlePlaybackCompleted() {
        log.info("실제 오디오 재생 완료: {}", stateManager.getCurrentMusic().getTitle());
        stateManager.setState(PlaybackStateManager.PlaybackState.STOPPED);
        shouldMonitor.set(false);
        
        // 재생 완료 이벤트 발행
        eventPublisher.publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
    }
    
    private void releaseResources() {
        try {
            if (audioClip != null) {
                audioClip.close();
                audioClip = null;
            }
            
            if (audioInputStream != null) {
                audioInputStream.close();
                audioInputStream = null;
            }
            
            volumeControl = null;
            
        } catch (Exception e) {
            log.error("리소스 해제 중 오류", e);
        }
    }
}