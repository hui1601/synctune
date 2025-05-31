package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * 미디어 재생 제어 "요청"과 관련된 이벤트들을 정의합니다.
 */
public class MediaControlEvent {

    private MediaControlEvent() {
    }

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

        @Override
        public String toString() {
            return super.toString() + (musicToPlay != null ? " {music=" + musicToPlay.getTitle() + "}" : "");
        }
    }

    public static class RequestPauseEvent extends BaseEvent {
    }

    public static class RequestStopEvent extends BaseEvent {
    }

    public static class RequestNextMusicEvent extends BaseEvent {
    }

    public static class RequestPreviousMusicEvent extends BaseEvent {
    }

    public static class RequestSeekEvent extends BaseEvent {
        private final long positionMillis; // 밀리초 단위

        public RequestSeekEvent(long positionMillis) {
            this.positionMillis = positionMillis;
        }

        public long getPositionMillis() {
            return positionMillis;
        }

        @Override
        public String toString() {
            return super.toString() + " {position=" + positionMillis + "ms}";
        }
    }
}