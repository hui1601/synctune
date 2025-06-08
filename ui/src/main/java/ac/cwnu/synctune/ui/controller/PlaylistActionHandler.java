package ac.cwnu.synctune.ui.controller;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.ui.view.PlaylistView;
import ac.cwnu.synctune.sdk.playlist.Playlist;
import ac.cwnu.synctune.sdk.playlist.PlaylistManager;

/**
 * PlaylistView에서 발생하는 사용자 액션을 처리하고
 * PlaylistEvent를 발행하는 컨트롤러 클래스
 */
public class PlaylistActionHandler {
    private final PlaylistView view;
    private final EventPublisher publisher;
    private final PlaylistManager playlistManager;

    /**
     * 생성자
     * @param view PlaylistView 인스턴스
     * @param publisher 이벤트 발행을 위한 EventPublisher
     */
    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher, PlaylistManager playlistManager) {
        this.view = view;
        this.publisher = publisher;
        this.playlistManager = playlistManager;
        attachEventHandlers();
    }

    private void attachEventHandlers() {
        view.getCreateButton().setOnAction(e -> {
            String name = view.getPlaylistNameInput();
            Playlist playlist = playlistManager.createPlaylist(name);
            publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(playlist));
        });

        view.getDeleteButton().setOnAction(e -> {
            String name = view.getSelectedPlaylist();
            Playlist playlist = playlistManager.getPlaylistByName(name);
            publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(playlist));
        });

        view.getAddButton().setOnAction(e -> {
            String name = view.getSelectedPlaylist();
            Playlist playlist = playlistManager.getPlaylistByName(name);
            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(
                playlist, view.getSelectedMusic()));
        });

        view.getRemoveButton().setOnAction(e -> {
            String name = view.getSelectedPlaylist();
            Playlist playlist = playlistManager.getPlaylistByName(name);
            publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(
                playlist, view.getSelectedMusic()));
        });

        // 재생목록 순서 변경 시 이벤트 발행
        view.getPlaylistItems().addListener((obs, oldList, newList) -> {
            String name = view.getSelectedPlaylist();
            Playlist playlist = playlistManager.getPlaylistByName(name);
            publisher.publish(new PlaylistEvent.PlaylistOrderChangedEvent(
                playlist, view.getCurrentOrder()));
        });
    }
}
