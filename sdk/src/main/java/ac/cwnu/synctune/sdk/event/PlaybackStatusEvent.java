package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * 미디어 재생 상태 변경과 관련된 이벤트들을 정의합니다.
 */
public class PlaybackStatusEvent {

    private PlaybackStatusEvent() {
    }

    /**
     * 재생이 시작되었음을 알리는 이벤트입니다.
     */
    public static class PlaybackStartedEvent extends BaseEvent {
        private final MusicInfo currentMusic;

        public PlaybackStartedEvent(MusicInfo currentMusic) {
            this.currentMusic = currentMusic;
        }

        public MusicInfo getCurrentMusic() {
            return currentMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {music=" + (currentMusic != null ? currentMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 재생이 일시정지되었음을 알리는 이벤트입니다.
     */
    public static class PlaybackPausedEvent extends BaseEvent {
    }

    /**
     * 재생이 정지되었음을 알리는 이벤트입니다.
     */
    public static class PlaybackStoppedEvent extends BaseEvent {
    }

    /**
     * 현재 재생 중인 곡이 변경되었음을 알리는 이벤트입니다.
     */
    public static class MusicChangedEvent extends BaseEvent {
        private final MusicInfo newMusic;

        public MusicChangedEvent(MusicInfo newMusic) {
            this.newMusic = newMusic;
        }

        public MusicInfo getNewMusic() {
            return newMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {newMusic=" + (newMusic != null ? newMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 재생 진행 시간이 업데이트되었음을 알리는 이벤트입니다.
     */
    public static class PlaybackProgressUpdateEvent extends BaseEvent {
        private final long currentTimeMillis;
        private final long totalTimeMillis;

        public PlaybackProgressUpdateEvent(long currentTimeMillis, long totalTimeMillis) {
            this.currentTimeMillis = currentTimeMillis;
            this.totalTimeMillis = totalTimeMillis;
        }

        public long getCurrentTimeMillis() {
            return currentTimeMillis;
        }

        public long getTotalTimeMillis() {
            return totalTimeMillis;
        }

        @Override
        public String toString() {
            return super.toString() + " {current=" + currentTimeMillis + "ms, total=" + totalTimeMillis + "ms}";
        }
    }
}