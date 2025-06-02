package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 실제 오디오 재생을 담당하는 엔진 클래스
 */
public class AudioEngine {
    private static final Logger log = LogManager.getLogger(AudioEngine.class);
    
    private final EventPublisher eventPublisher;
    private final PlaybackState playbackState;
    
    // 오디오 관련 객체들
    private AudioInputStream audioInputStream;
    private Clip audioClip;
    
    public AudioEngine(EventPublisher eventPublisher, PlaybackState playbackState) {
        this.eventPublisher = eventPublisher;
        this.playbackState = playbackState;
    }
    
    /**
     * 음악 파일을 로드하고 재생을 시작
     */
    public boolean loadAndPlay(MusicInfo music) {
        try {
            // 기존 재생 중인 음악이 있으면 정지
            if (playbackState.isPlaying()) {
                stop();
            }
            
            File musicFile = new File(music.getFilePath());
            if (!musicFile.exists()) {
                throw new IOException("음악 파일을 찾을 수 없습니다: " + music.getFilePath());
            }
            
            log.info("음악 로딩 중: {}", music.getTitle());
            
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
            
            // 상태 업데이트
            playbackState.setCurrentMusic(music);
            playbackState.setPausePosition(0);
            
            // 재생 시작
            return start();
            
        } catch (UnsupportedAudioFileException e) {
            log.error("지원되지 않는 오디오 파일 형식: {}", music.getFilePath(), e);
            eventPublisher.publish(new ErrorEvent("지원되지 않는 오디오 파일 형식입니다.", e, false));
            return false;
        } catch (IOException e) {
            log.error("파일 읽기 오류: {}", music.getFilePath(), e);
            eventPublisher.publish(new ErrorEvent("음악 파일을 읽을 수 없습니다.", e, false));
            return false;
        } catch (LineUnavailableException e) {
            log.error("오디오 라인을 사용할 수 없습니다.", e);
            eventPublisher.publish(new ErrorEvent("오디오 시스템을 사용할 수 없습니다.", e, false));
            return false;
        } catch (Exception e) {
            log.error("음악 로딩 중 예상치 못한 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("음악 로딩 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 현재 로드된 음악의 재생을 시작
     */
    public boolean start() {
        if (audioClip == null) {
            log.warn("재생할 오디오 클립이 없습니다.");
            return false;
        }
        
        try {
            audioClip.setFramePosition((int) playbackState.getPausePosition());
            audioClip.start();
            
            playbackState.setPlaying(true);
            
            MusicInfo currentMusic = playbackState.getCurrentMusic();
            log.info("재생 시작: {}", currentMusic != null ? currentMusic.getTitle() : "Unknown");
            eventPublisher.publish(new PlaybackStatusEvent.PlaybackStartedEvent(currentMusic));
            
            return true;
            
        } catch (Exception e) {
            log.error("재생 시작 중 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("재생 시작 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 재생을 일시정지
     */
    public boolean pause() {
        if (audioClip == null || !playbackState.isPlaying()) {
            log.debug("일시정지할 수 없습니다. 재생 중이 아닙니다.");
            return false;
        }
        
        try {
            playbackState.setPausePosition(audioClip.getFramePosition());
            audioClip.stop();
            
            playbackState.setPaused(true);
            
            log.info("일시정지됨");
            eventPublisher.publish(new PlaybackStatusEvent.PlaybackPausedEvent());
            
            return true;
            
        } catch (Exception e) {
            log.error("일시정지 중 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("일시정지 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 일시정지된 재생을 재개
     */
    public boolean resume() {
        if (audioClip == null || !playbackState.isPaused()) {
            log.debug("재개할 수 없습니다. 일시정지 상태가 아닙니다.");
            return false;
        }
        
        try {
            audioClip.setFramePosition((int) playbackState.getPausePosition());
            audioClip.start();
            
            playbackState.setPlaying(true);
            
            log.info("재생 재개됨");
            eventPublisher.publish(new PlaybackStatusEvent.PlaybackResumedEvent());
            
            return true;
            
        } catch (Exception e) {
            log.error("재생 재개 중 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("재생 재개 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 재생을 완전히 정지하고 리소스를 해제
     */
    public boolean stop() {
        if (audioClip == null) {
            return true; // 이미 정지된 상태
        }
        
        try {
            audioClip.stop();
            audioClip.close();
            
            if (audioInputStream != null) {
                audioInputStream.close();
                audioInputStream = null;
            }
            
            audioClip = null;
            playbackState.reset();
            
            log.info("재생 정지됨");
            eventPublisher.publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
            
            return true;
            
        } catch (Exception e) {
            log.error("재생 정지 중 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("재생 정지 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 특정 위치로 탐색
     */
    public boolean seekTo(long positionMillis) {
        if (audioClip == null) {
            log.warn("탐색할 수 없습니다. 로드된 음악이 없습니다.");
            return false;
        }
        
        try {
            // 밀리초를 프레임 위치로 변환
            AudioFormat format = audioClip.getFormat();
            long framePosition = (long) (positionMillis * format.getFrameRate() / 1000.0);
            
            // 유효한 범위 확인
            long totalFrames = audioClip.getFrameLength();
            framePosition = Math.max(0, Math.min(framePosition, totalFrames - 1));
            
            audioClip.setFramePosition((int) framePosition);
            playbackState.setPausePosition(framePosition);
            
            log.debug("탐색 완료: {}ms (프레임: {})", positionMillis, framePosition);
            
            // 현재 재생 시간 업데이트 이벤트 발행
            long currentTimeMillis = (long) (framePosition * 1000.0 / format.getFrameRate());
            long totalTimeMillis = (long) (totalFrames * 1000.0 / format.getFrameRate());
            eventPublisher.publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(currentTimeMillis, totalTimeMillis));
            
            return true;
            
        } catch (Exception e) {
            log.error("탐색 중 오류 발생", e);
            eventPublisher.publish(new ErrorEvent("탐색 중 오류가 발생했습니다.", e, false));
            return false;
        }
    }
    
    /**
     * 현재 재생 위치를 밀리초로 반환
     */
    public long getCurrentPositionMillis() {
        if (audioClip == null) {
            return 0;
        }
        
        try {
            AudioFormat format = audioClip.getFormat();
            long currentFrame = playbackState.isPlaying() ? audioClip.getFramePosition() : playbackState.getPausePosition();
            return (long) (currentFrame * 1000.0 / format.getFrameRate());
        } catch (Exception e) {
            log.error("현재 위치 조회 중 오류", e);
            return 0;
        }
    }
    
    /**
     * 총 재생 시간을 밀리초로 반환
     */
    public long getTotalDurationMillis() {
        if (audioClip == null) {
            MusicInfo currentMusic = playbackState.getCurrentMusic();
            return currentMusic != null ? currentMusic.getDurationMillis() : 0;
        }
        
        try {
            AudioFormat format = audioClip.getFormat();
            long totalFrames = audioClip.getFrameLength();
            return (long) (totalFrames * 1000.0 / format.getFrameRate());
        } catch (Exception e) {
            log.error("총 길이 조회 중 오류", e);
            MusicInfo currentMusic = playbackState.getCurrentMusic();
            return currentMusic != null ? currentMusic.getDurationMillis() : 0;
        }
    }
    
    /**
     * 오디오 클립이 현재 실행 중인지 확인합
     */
    public boolean isAudioClipRunning() {
        return audioClip != null && audioClip.isRunning();
    }
    
    /**
     * 리소스를 안전하게 해제
     */
    public void dispose() {
        stop();
    }
}