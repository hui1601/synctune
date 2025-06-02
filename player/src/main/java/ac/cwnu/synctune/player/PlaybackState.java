package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PlayerModule의 재생 상태를 관리하는 클래스
 */
public class PlaybackState {
    
    // 재생 상태 플래그들
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    
    // 재생 위치 정보
    private final AtomicLong pausePosition = new AtomicLong(0);
    
    // 현재 재생 중인 음악 정보
    private volatile MusicInfo currentMusic;
    
    /**
     * 재생 상태를 시작으로 설정
     */
    public void setPlaying(boolean playing) {
        isPlaying.set(playing);
        if (playing) {
            isPaused.set(false);
        }
    }
    
    /**
     * 일시정지 상태를 설정
     */
    public void setPaused(boolean paused) {
        isPaused.set(paused);
        if (paused) {
            isPlaying.set(false);
        }
    }
    
    /**
     * 모든 재생 상태를 초기화 (정지 상태)
     */
    public void reset() {
        isPlaying.set(false);
        isPaused.set(false);
        pausePosition.set(0);
        currentMusic = null;
    }
    
    /**
     * 일시정지 위치를 설정
     */
    public void setPausePosition(long framePosition) {
        pausePosition.set(framePosition);
    }
    
    /**
     * 현재 재생 중인 음악을 설정
     */
    public void setCurrentMusic(MusicInfo music) {
        this.currentMusic = music;
    }
    
    // ========== Getter 메서드들 ==========
    
    public boolean isPlaying() {
        return isPlaying.get();
    }
    
    public boolean isPaused() {
        return isPaused.get();
    }
    
    public long getPausePosition() {
        return pausePosition.get();
    }
    
    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }
    
    /**
     * 현재 아무것도 재생되지 않고 있는지 확인
     */
    public boolean isIdle() {
        return !isPlaying.get() && !isPaused.get();
    }
    
    /**
     * 현재 상태를 문자열로 반환 (디버깅용)
     */
    public String getStateDescription() {
        if (isPlaying.get()) {
            return "PLAYING";
        } else if (isPaused.get()) {
            return "PAUSED";
        } else {
            return "STOPPED";
        }
    }
    
    @Override
    public String toString() {
        return String.format("PlaybackState{playing=%s, paused=%s, position=%d, music=%s}", 
            isPlaying.get(), isPaused.get(), pausePosition.get(), 
            currentMusic != null ? currentMusic.getTitle() : "none");
    }
}