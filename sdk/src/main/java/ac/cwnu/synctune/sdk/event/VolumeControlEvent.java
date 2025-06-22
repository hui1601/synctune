package ac.cwnu.synctune.sdk.event;

/**
 * 볼륨 제어와 관련된 이벤트들을 정의합니다.
 */
public class VolumeControlEvent {

    private VolumeControlEvent() {
    }

    /**
     * 볼륨 변경 요청 이벤트
     */
    public static class RequestVolumeChangeEvent extends BaseEvent {
        private final float volume; // 0.0 ~ 1.0

        public RequestVolumeChangeEvent(float volume) {
            this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        }

        public float getVolume() {
            return volume;
        }

        @Override
        public String toString() {
            return super.toString() + " {volume=" + volume + "}";
        }
    }

    /**
     * 음소거 상태 변경 요청 이벤트
     */
    public static class RequestMuteEvent extends BaseEvent {
        private final boolean muted;

        public RequestMuteEvent(boolean muted) {
            this.muted = muted;
        }

        public boolean isMuted() {
            return muted;
        }

        @Override
        public String toString() {
            return super.toString() + " {muted=" + muted + "}";
        }
    }

    /**
     * 볼륨 상태 변경 알림 이벤트
     */
    public static class VolumeChangedEvent extends BaseEvent {
        private final float volume;
        private final boolean muted;

        public VolumeChangedEvent(float volume, boolean muted) {
            this.volume = volume;
            this.muted = muted;
        }

        public float getVolume() {
            return volume;
        }

        public boolean isMuted() {
            return muted;
        }

        @Override
        public String toString() {
            return super.toString() + " {volume=" + volume + ", muted=" + muted + "}";
        }
    }
}