package ac.cwnu.synctune.player.playback;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 현재 재생 상태를 관리하는 클래스
 * 재생중, 일시정지, 정지 등의 상태와 현재 곡 정보를 추적합니다.
 */
public class PlaybackStateManager {
    private static final Logger log = LogManager.getLogger(PlaybackStateManager.class);
    
    /**
     * 재생 상태 열거형
     */
    public enum PlaybackState {
        STOPPED,    // 정지됨
        PLAYING,    // 재생중
        PAUSED,     // 일시정지됨
        LOADING     // 로딩중
    }
    
    // 현재 재생 상태
    private final AtomicReference<PlaybackState> currentState = new AtomicReference<>(PlaybackState.STOPPED);
    
    // 현재 재생 중인 음악 정보
    private final AtomicReference<MusicInfo> currentMusic = new AtomicReference<>();
    
    // 재생 위치 정보 (밀리초)
    private final AtomicLong currentPositionMs = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    
    // 일시정지 시 저장되는 위치
    private final AtomicLong pausePositionMs = new AtomicLong(0);
    
    // 볼륨 (0.0 ~ 1.0)
    private final AtomicReference<Float> volume = new AtomicReference<>(1.0f);
    
    // 음소거 상태
    private final AtomicBoolean isMuted = new AtomicBoolean(false);
    
    /**
     * 재생 상태를 변경합니다
     */
    public void setState(PlaybackState newState) {
        PlaybackState oldState = currentState.getAndSet(newState);
        if (oldState != newState) {
            log.debug("재생 상태 변경: {} -> {}", oldState, newState);
            
            // 상태 변경에 따른 추가 처리
            switch (newState) {
                case STOPPED:
                    currentPositionMs.set(0);
                    pausePositionMs.set(0);
                    break;
                case PAUSED:
                    pausePositionMs.set(currentPositionMs.get());
                    break;
                case PLAYING:
                    // 재생 시작 시 특별한 처리 없음
                    break;
                case LOADING:
                    // 로딩 시 특별한 처리 없음
                    break;
            }
        }
    }
    
    /**
     * 현재 재생 상태를 반환합니다
     */
    public PlaybackState getState() {
        return currentState.get();
    }
    
    /**
     * 특정 상태인지 확인합니다
     */
    public boolean isState(PlaybackState state) {
        return currentState.get() == state;
    }
    
    /**
     * 재생 중인지 확인합니다
     */
    public boolean isPlaying() {
        return currentState.get() == PlaybackState.PLAYING;
    }
    
    /**
     * 일시정지 중인지 확인합니다
     */
    public boolean isPaused() {
        return currentState.get() == PlaybackState.PAUSED;
    }
    
    /**
     * 정지 상태인지 확인합니다
     */
    public boolean isStopped() {
        return currentState.get() == PlaybackState.STOPPED;
    }
    
    /**
     * 로딩 중인지 확인합니다
     */
    public boolean isLoading() {
        return currentState.get() == PlaybackState.LOADING;
    }
    
    /**
     * 현재 재생 중인 음악을 설정합니다
     */
    public void setCurrentMusic(MusicInfo music) {
        MusicInfo oldMusic = currentMusic.getAndSet(music);
        if (music != null) {
            totalDurationMs.set(music.getDurationMillis());
            log.debug("현재 음악 설정: {}", music.getTitle());
        } else {
            totalDurationMs.set(0);
            log.debug("현재 음악 해제");
        }
        
        // 음악이 변경되면 재생 위치 초기화
        if (oldMusic != music) {
            currentPositionMs.set(0);
            pausePositionMs.set(0);
        }
    }
    
    /**
     * 현재 재생 중인 음악을 반환합니다
     */
    public MusicInfo getCurrentMusic() {
        return currentMusic.get();
    }
    
    /**
     * 현재 재생 위치를 설정합니다 (밀리초)
     */
    public void setCurrentPosition(long positionMs) {
        long validPosition = Math.max(0, Math.min(positionMs, totalDurationMs.get()));
        currentPositionMs.set(validPosition);
    }
    
    /**
     * 현재 재생 위치를 반환합니다 (밀리초)
     */
    public long getCurrentPosition() {
        return currentPositionMs.get();
    }
    
    /**
     * 일시정지 위치를 반환합니다 (밀리초)
     */
    public long getPausePosition() {
        return pausePositionMs.get();
    }
    
    /**
     * 총 재생 시간을 설정합니다 (밀리초)
     */
    public void setTotalDuration(long durationMs) {
        totalDurationMs.set(Math.max(0, durationMs));
    }
    
    /**
     * 총 재생 시간을 반환합니다 (밀리초)
     */
    public long getTotalDuration() {
        return totalDurationMs.get();
    }
    
    /**
     * 재생 진행률을 반환합니다 (0.0 ~ 1.0)
     */
    public double getProgress() {
        long total = getTotalDuration();
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) getCurrentPosition() / total);
    }
    
    /**
     * 남은 재생 시간을 반환합니다 (밀리초)
     */
    public long getRemainingTime() {
        return Math.max(0, getTotalDuration() - getCurrentPosition());
    }
    
    /**
     * 볼륨을 설정합니다 (0.0 ~ 1.0)
     */
    public void setVolume(float newVolume) {
        float validVolume = Math.max(0.0f, Math.min(1.0f, newVolume));
        float oldVolume = volume.getAndSet(validVolume);
        if (oldVolume != validVolume) {
            log.debug("볼륨 변경: {} -> {}", oldVolume, validVolume);
        }
    }
    
    /**
     * 현재 볼륨을 반환합니다 (0.0 ~ 1.0)
     */
    public float getVolume() {
        return volume.get();
    }
    
    /**
     * 음소거 상태를 설정합니다
     */
    public void setMuted(boolean muted) {
        boolean oldMuted = isMuted.getAndSet(muted);
        if (oldMuted != muted) {
            log.debug("음소거 상태 변경: {} -> {}", oldMuted, muted);
        }
    }
    
    /**
     * 음소거 상태인지 확인합니다
     */
    public boolean isMuted() {
        return isMuted.get();
    }
    
    /**
     * 음소거 상태를 토글합니다
     */
    public void toggleMute() {
        setMuted(!isMuted());
    }
    
    /**
     * 실제 재생 볼륨을 반환합니다 (음소거 상태 고려)
     */
    public float getEffectiveVolume() {
        return isMuted() ? 0.0f : getVolume();
    }
    
    /**
     * 모든 상태를 초기화합니다
     */
    public void reset() {
        log.debug("재생 상태 초기화");
        setState(PlaybackState.STOPPED);
        setCurrentMusic(null);
        currentPositionMs.set(0);
        pausePositionMs.set(0);
        totalDurationMs.set(0);
    }
    
    /**
     * 현재 상태 정보를 문자열로 반환합니다 (디버깅용)
     */
    @Override
    public String toString() {
        MusicInfo music = getCurrentMusic();
        return String.format(
            "PlaybackState{state=%s, music=%s, position=%d/%d ms, volume=%.2f, muted=%s}",
            getState(),
            music != null ? music.getTitle() : "none",
            getCurrentPosition(),
            getTotalDuration(),
            getVolume(),
            isMuted()
        );
    }
    
    /**
     * 상태 정보를 포맷팅된 문자열로 반환합니다
     */
    public String getFormattedStatus() {
        MusicInfo music = getCurrentMusic();
        if (music == null) {
            return "재생 중인 음악 없음";
        }
        
        long currentSec = getCurrentPosition() / 1000;
        long totalSec = getTotalDuration() / 1000;
        
        return String.format("%s - %s [%d:%02d / %d:%02d] (%.1f%%)",
            getState(),
            music.getTitle(),
            currentSec / 60, currentSec % 60,
            totalSec / 60, totalSec % 60,
            getProgress() * 100
        );
    }
}