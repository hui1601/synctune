package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;

/**
 * 플레이리스트 조회와 관련된 이벤트들을 정의합니다.
 */
public class PlaylistQueryEvent {

    private PlaylistQueryEvent() {
    } // 인스턴스화 방지

    /**
     * 다음 곡 요청 이벤트
     */
    public static class RequestNextMusicInPlaylistEvent extends BaseEvent {
        private final MusicInfo currentMusic; // 현재 곡 (null이면 첫 번째 곡)

        public RequestNextMusicInPlaylistEvent(MusicInfo currentMusic) {
            this.currentMusic = currentMusic;
        }

        public MusicInfo getCurrentMusic() {
            return currentMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {currentMusic=" + 
                (currentMusic != null ? currentMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 이전 곡 요청 이벤트
     */
    public static class RequestPreviousMusicInPlaylistEvent extends BaseEvent {
        private final MusicInfo currentMusic; // 현재 곡

        public RequestPreviousMusicInPlaylistEvent(MusicInfo currentMusic) {
            this.currentMusic = currentMusic;
        }

        public MusicInfo getCurrentMusic() {
            return currentMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {currentMusic=" + 
                (currentMusic != null ? currentMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 플레이리스트에서 다음 곡 응답 이벤트
     */
    public static class NextMusicFoundEvent extends BaseEvent {
        private final MusicInfo nextMusic;

        public NextMusicFoundEvent(MusicInfo nextMusic) {
            this.nextMusic = nextMusic;
        }

        public MusicInfo getNextMusic() {
            return nextMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {nextMusic=" + 
                (nextMusic != null ? nextMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 플레이리스트에서 이전 곡 응답 이벤트
     */
    public static class PreviousMusicFoundEvent extends BaseEvent {
        private final MusicInfo previousMusic;

        public PreviousMusicFoundEvent(MusicInfo previousMusic) {
            this.previousMusic = previousMusic;
        }

        public MusicInfo getPreviousMusic() {
            return previousMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {previousMusic=" + 
                (previousMusic != null ? previousMusic.getTitle() : "null") + "}";
        }
    }

    /**
     * 현재 재생 중인 곡이 플레이리스트에서 제거되었음을 알리는 이벤트
     */
    public static class CurrentMusicRemovedFromPlaylistEvent extends BaseEvent {
        private final MusicInfo removedMusic;

        public CurrentMusicRemovedFromPlaylistEvent(MusicInfo removedMusic) {
            this.removedMusic = removedMusic;
        }

        public MusicInfo getRemovedMusic() {
            return removedMusic;
        }

        @Override
        public String toString() {
            return super.toString() + " {removedMusic=" + 
                (removedMusic != null ? removedMusic.getTitle() : "null") + "}";
        }
    }
}