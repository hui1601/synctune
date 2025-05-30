package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import java.util.List;

/**
 * 플레이리스트 관련 이벤트를 정의합니다.
 */
public class PlaylistEvent {

    private PlaylistEvent() {} // 인스턴스화 방지

    public static class PlaylistCreatedEvent extends BaseEvent {
        private final Playlist playlist;
        public PlaylistCreatedEvent(Playlist playlist) { this.playlist = playlist; }
        public Playlist getPlaylist() { return playlist; }
        @Override public String toString() { return super.toString() + " {playlistName=" + playlist.getName() + "}"; }
    }

    public static class PlaylistDeletedEvent extends BaseEvent {
        private final String playlistName; // 또는 ID
        public PlaylistDeletedEvent(String playlistName) { this.playlistName = playlistName; }
        public String getPlaylistName() { return playlistName; }
        @Override public String toString() { return super.toString() + " {playlistName=" + playlistName + "}"; }
    }

    public static class MusicAddedToPlaylistEvent extends BaseEvent {
        private final String playlistName;
        private final MusicInfo musicInfo;
        public MusicAddedToPlaylistEvent(String playlistName, MusicInfo musicInfo) {
            this.playlistName = playlistName;
            this.musicInfo = musicInfo;
        }
        public String getPlaylistName() { return playlistName; }
        public MusicInfo getMusicInfo() { return musicInfo; }
        @Override public String toString() { return super.toString() + " {playlistName=" + playlistName + ", music=" + musicInfo.getTitle() + "}"; }
    }

    public static class MusicRemovedFromPlaylistEvent extends BaseEvent {
        private final String playlistName;
        private final MusicInfo musicInfo;
        public MusicRemovedFromPlaylistEvent(String playlistName, MusicInfo musicInfo) {
            this.playlistName = playlistName;
            this.musicInfo = musicInfo;
        }
        public String getPlaylistName() { return playlistName; }
        public MusicInfo getMusicInfo() { return musicInfo; }
        @Override public String toString() { return super.toString() + " {playlistName=" + playlistName + ", music=" + musicInfo.getTitle() + "}"; }
    }

    public static class PlaylistOrderChangedEvent extends BaseEvent {
        private final Playlist playlist;
        public PlaylistOrderChangedEvent(Playlist playlist) { this.playlist = playlist; }
        public Playlist getPlaylist() { return playlist; }
        @Override public String toString() { return super.toString() + " {playlistName=" + playlist.getName() + "}"; }
    }

    public static class AllPlaylistsLoadedEvent extends BaseEvent { // 추가: 모든 플레이리스트 로드 완료
        private final List<Playlist> playlists;
        public AllPlaylistsLoadedEvent(List<Playlist> playlists) { this.playlists = List.copyOf(playlists); }
        public List<Playlist> getPlaylists() { return playlists; }
        @Override public String toString() { return super.toString() + " {count=" + playlists.size() + "}"; }
    }
}