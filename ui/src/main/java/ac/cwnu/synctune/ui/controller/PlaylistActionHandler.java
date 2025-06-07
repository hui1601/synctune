package ac.cwnu.synctune.ui.controller;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.ui.view.PlaylistView;

/**
 * PlaylistView에서 발생하는 사용자 액션을 처리하고
 * PlaylistEvent를 발행하는 컨트롤러 클래스
 */
public class PlaylistActionHandler {
    private final PlaylistView view;
    private final EventPublisher publisher;

    /**
     * 생성자
     * @param view PlaylistView 인스턴스
     * @param publisher 이벤트 발행을 위한 EventPublisher
     */
    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
    }

    private void attachEventHandlers() {
        view.getCreateButton().setOnAction(e -> 
            publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(view.getPlaylistNameInput()))
        );

        view.getDeleteButton().setOnAction(e -> 
            publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(view.getSelectedPlaylist()))
        );

        view.getAddButton().setOnAction(e -> 
            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(
                view.getSelectedPlaylist(), view.getSelectedMusic()))
        );

        view.getRemoveButton().setOnAction(e -> 
            publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(
                view.getSelectedPlaylist(), view.getSelectedMusic()))
        );

        // 재생목록 순서 변경 시 이벤트 발행
        view.getPlaylistItems().addListener((obs, oldList, newList) -> 
            publisher.publish(new PlaylistEvent.PlaylistOrderChangedEvent(
                view.getSelectedPlaylist(), view.getCurrentOrder()))
        );
    }
}
