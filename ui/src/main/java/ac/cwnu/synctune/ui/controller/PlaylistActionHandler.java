package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.PlaylistView;

/**
 * 단순화된 PlaylistActionHandler
 * 실제 UI 동작은 PlaylistView에서 직접 처리하고,
 * 여기서는 이벤트 발행만 담당
 */
public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;

    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        setupEventMonitoring();
        log.debug("PlaylistActionHandler 초기화 완료");
    }

    private void setupEventMonitoring() {
        // PlaylistView의 변경사항을 모니터링하여 이벤트 발행
        // 실제 동작은 PlaylistView에서 처리되므로, 필요시에만 이벤트 발행
        
        log.debug("PlaylistView와 연동된 이벤트 모니터링 설정 완료");
    }

    /**
     * 외부에서 플레이리스트 생성 이벤트를 발행하고 싶을 때 사용
     */
    public void publishPlaylistCreatedEvent(String playlistName) {
        var playlist = new ac.cwnu.synctune.sdk.model.Playlist(playlistName);
        publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(playlist));
        log.debug("PlaylistCreatedEvent 발행: {}", playlistName);
    }

    /**
     * 외부에서 플레이리스트 삭제 이벤트를 발행하고 싶을 때 사용
     */
    public void publishPlaylistDeletedEvent(String playlistName) {
        publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(playlistName));
        log.debug("PlaylistDeletedEvent 발행: {}", playlistName);
    }

    /**
     * 플레이리스트 데이터 동기화 (필요시 사용)
     */
    public void syncPlaylistData() {
        var playlists = view.getPlaylists();
        log.debug("현재 플레이리스트 개수: {}", playlists.size());
        
        // 필요시 다른 모듈과 데이터 동기화 로직 추가
    }
}