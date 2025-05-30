package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * 미디어 재생 제어와 관련된 이벤트들을 정의합니다.
 */
public class MediaControlEvent {

    private MediaControlEvent() {} // 인스턴스화 방지

    // --- 요청 이벤트 ---
    public static class RequestPlayEvent extends BaseEvent {
        private final MusicInfo musicToPlay; // 특정 곡 재생 요청 시 사용

        public RequestPlayEvent() {
            this.musicToPlay = null;
        }
        public RequestPlayEvent(MusicInfo musicToPlay) {
            this.musicToPlay = musicToPlay;
        }
        public MusicInfo getMusicToPlay() {
            return musicToPlay;
        }
        @Override public String toString() { return super.toString() + (musicToPlay != null ? " {music=" + musicToPlay.getTitle() + "}" : ""); }
    }

    public static class RequestPauseEvent extends BaseEvent {}
    public static class RequestStopEvent extends BaseEvent {} // 추가: 정지 요청
    public static class RequestNextMusicEvent extends BaseEvent {}
    public static class RequestPreviousMusicEvent extends BaseEvent {} // 추가: 이전 곡 요청
    public static class RequestSeekEvent extends BaseEvent { // 추가: 탐색 요청
        private final long positionMillis; // 밀리초 단위
        public RequestSeekEvent(long positionMillis) { this.positionMillis = positionMillis; }
        public long getPositionMillis() { return positionMillis; }
        @Override public String toString() { return super.toString() + " {position=" + positionMillis + "ms}"; }
    }

    // --- 상태 변경 이벤트 ---
    public static class PlayEvent extends BaseEvent {
        private final MusicInfo currentMusic;
        public PlayEvent(MusicInfo currentMusic) { this.currentMusic = currentMusic; }
        public MusicInfo getCurrentMusic() { return currentMusic; }
        @Override public String toString() { return super.toString() + " {music=" + currentMusic.getTitle() + "}"; }
    }

    public static class PauseEvent extends BaseEvent {}
    public static class StopEvent extends BaseEvent {} // 추가: 정지 완료
    public static class MusicChangedEvent extends BaseEvent {
        private final MusicInfo newMusic;
        public MusicChangedEvent(MusicInfo newMusic) { this.newMusic = newMusic; }
        public MusicInfo getNewMusic() { return newMusic; }
        @Override public String toString() { return super.toString() + " {newMusic=" + newMusic.getTitle() + "}"; }
    }

    public static class ProgressUpdateEvent extends BaseEvent { // 추가: 재생 시간 업데이트
        private final long currentTimeMillis;
        private final long totalTimeMillis;
        public ProgressUpdateEvent(long currentTimeMillis, long totalTimeMillis) {
            this.currentTimeMillis = currentTimeMillis;
            this.totalTimeMillis = totalTimeMillis;
        }
        public long getCurrentTimeMillis() { return currentTimeMillis; }
        public long getTotalTimeMillis() { return totalTimeMillis; }
        @Override public String toString() { return super.toString() + " {current=" + currentTimeMillis + "ms, total=" + totalTimeMillis + "ms}"; }
    }
}