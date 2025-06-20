package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.view.PlaylistView;

/**
 * PlaylistView와 연동하여 플레이리스트 관련 이벤트를 처리하는 핸들러
 * 실제 UI 동작은 PlaylistView에서 직접 처리하고,
 * 여기서는 이벤트 발행과 모니터링을 담당
 */
public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;

    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        setupEventHandlers();
        log.debug("PlaylistActionHandler 초기화 완료");
    }

    private void setupEventHandlers() {
        // PlaylistView의 버튼들에 이벤트 핸들러 연결
        
        // 플레이리스트 생성 버튼
        view.getCreateButton().setOnAction(e -> {
            String playlistName = view.getPlaylistNameInput();
            if (playlistName != null && !playlistName.trim().isEmpty()) {
                createPlaylist(playlistName.trim());
                view.clearPlaylistNameInput();
            } else {
                log.warn("플레이리스트 이름이 비어있습니다.");
            }
        });
        
        // 플레이리스트 삭제 버튼
        view.getDeleteButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            if (selectedPlaylist != null) {
                deletePlaylist(selectedPlaylist);
            } else {
                log.warn("삭제할 플레이리스트가 선택되지 않았습니다.");
            }
        });
        
        // 곡 추가 버튼 (TODO: 실제 곡 선택 UI와 연동)
        view.getAddButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            if (selectedPlaylist != null) {
                log.debug("곡 추가 요청: 플레이리스트 '{}'", selectedPlaylist);
                // TODO: 곡 선택 다이얼로그 표시 후 곡 추가
                showAddMusicDialog(selectedPlaylist);
            } else {
                log.warn("곡을 추가할 플레이리스트가 선택되지 않았습니다.");
            }
        });
        
        // 곡 제거 버튼
        view.getRemoveButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            String selectedMusic = view.getSelectedMusic();
            if (selectedPlaylist != null && selectedMusic != null) {
                log.debug("곡 제거 요청: '{}' <- '{}'", selectedMusic, selectedPlaylist);
                removeMusicFromPlaylist(selectedPlaylist, selectedMusic);
            } else {
                log.warn("제거할 곡이나 플레이리스트가 선택되지 않았습니다.");
            }
        });
        
        log.debug("PlaylistView 이벤트 핸들러 설정 완료");
    }

    /**
     * 새 플레이리스트 생성
     */
    private void createPlaylist(String playlistName) {
        try {
            // UI에 즉시 반영
            view.addPlaylist(playlistName);
            
            // 이벤트 발행
            Playlist newPlaylist = new Playlist(playlistName);
            publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(newPlaylist));
            
            log.info("플레이리스트 생성됨: {}", playlistName);
        } catch (Exception e) {
            log.error("플레이리스트 생성 중 오류: {}", playlistName, e);
        }
    }

    /**
     * 플레이리스트 삭제
     */
    private void deletePlaylist(String playlistName) {
        try {
            // UI에서 즉시 제거
            view.removePlaylist(playlistName);
            
            // 이벤트 발행
            publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(playlistName));
            
            log.info("플레이리스트 삭제됨: {}", playlistName);
        } catch (Exception e) {
            log.error("플레이리스트 삭제 중 오류: {}", playlistName, e);
        }
    }

    /**
     * 곡 추가 다이얼로그 표시 (임시 구현)
     */
    private void showAddMusicDialog(String playlistName) {
        // TODO: 실제 음악 라이브러리에서 곡을 선택할 수 있는 다이얼로그 구현
        // 현재는 시뮬레이션용 더미 데이터 추가
        
        log.debug("곡 추가 다이얼로그 표시 (미구현): {}", playlistName);
        
        // 시뮬레이션: 더미 곡 정보 생성
        ac.cwnu.synctune.sdk.model.MusicInfo dummyMusic = new ac.cwnu.synctune.sdk.model.MusicInfo(
            "샘플 곡 " + System.currentTimeMillis() % 1000,
            "샘플 아티스트",
            "샘플 앨범",
            "/path/to/sample.mp3",
            180000L, // 3분
            null
        );
        
        // UI에 즉시 반영
        view.addMusicToPlaylist(playlistName, dummyMusic);
        
        // 이벤트 발행
        publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, dummyMusic));
        
        log.debug("샘플 곡이 플레이리스트에 추가됨: {} -> {}", dummyMusic.getTitle(), playlistName);
    }

    /**
     * 플레이리스트에서 곡 제거
     */
    private void removeMusicFromPlaylist(String playlistName, String musicDisplayText) {
        try {
            // 현재는 표시 텍스트만 가지고 있으므로, 실제 MusicInfo 객체를 찾아야 함
            // 이는 PlaylistView 내부에서 처리하거나, 별도의 매핑 테이블이 필요
            
            log.debug("곡 제거 시뮬레이션: '{}' <- '{}'", musicDisplayText, playlistName);
            
            // TODO: 실제 MusicInfo 객체를 찾아서 제거
            // 현재는 UI에서만 제거 (PlaylistView가 내부적으로 처리)
            
        } catch (Exception e) {
            log.error("곡 제거 중 오류: {} <- {}", musicDisplayText, playlistName, e);
        }
    }

    /**
     * 외부에서 플레이리스트 생성 이벤트를 발행하고 싶을 때 사용
     */
    public void publishPlaylistCreatedEvent(String playlistName) {
        Playlist playlist = new Playlist(playlistName);
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
        try {
            var playlists = view.getPlaylists();
            log.debug("현재 플레이리스트 개수: {}", playlists.size());
            
            // 필요시 다른 모듈과 데이터 동기화 로직 추가
            // 예: 전체 플레이리스트 목록을 이벤트로 발행
            if (!playlists.isEmpty()) {
                publisher.publish(new PlaylistEvent.AllPlaylistsLoadedEvent(playlists));
                log.debug("전체 플레이리스트 동기화 이벤트 발행: {}개", playlists.size());
            }
        } catch (Exception e) {
            log.error("플레이리스트 데이터 동기화 중 오류", e);
        }
    }

    /**
     * 플레이리스트 새로고침
     */
    public void refreshPlaylists() {
        view.refreshPlaylists();
        syncPlaylistData();
        log.debug("플레이리스트 새로고침 완료");
    }

    /**
     * 플레이리스트 통계 표시
     */
    public void showStatistics() {
        view.showPlaylistStatistics();
        log.debug("플레이리스트 통계 다이얼로그 표시");
    }

    /**
     * 음악 검색 필터 적용
     */
    public void filterMusic(String searchText) {
        view.filterMusicLibrary(searchText);
        log.debug("음악 라이브러리 필터 적용: '{}'", searchText);
    }

    /**
     * 현재 선택된 플레이리스트 반환
     */
    public String getCurrentPlaylist() {
        return view.getSelectedPlaylist();
    }

    /**
     * 현재 선택된 곡 반환
     */
    public String getCurrentMusic() {
        return view.getSelectedMusic();
    }

    /**
     * 리소스 정리
     */
    public void dispose() {
        log.debug("PlaylistActionHandler 리소스 정리");
        // 필요시 정리 작업 수행
    }
}