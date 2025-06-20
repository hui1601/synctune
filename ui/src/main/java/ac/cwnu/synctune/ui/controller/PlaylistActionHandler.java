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
        System.out.println("PlaylistActionHandler 생성자 호출");
        
        if (view == null) {
            throw new IllegalArgumentException("PlaylistView는 null일 수 없습니다.");
        }
        if (publisher == null) {
            throw new IllegalArgumentException("EventPublisher는 null일 수 없습니다.");
        }
        
        this.view = view;
        this.publisher = publisher;
        
        System.out.println("PlaylistActionHandler 이벤트 핸들러 설정 중...");
        setupEventHandlers();
        
        log.debug("PlaylistActionHandler 초기화 완료");
        System.out.println("PlaylistActionHandler 초기화 완료");
    }

    private void setupEventHandlers() {
        try {
            // PlaylistView의 버튼들에 이벤트 핸들러 연결
            
            // 플레이리스트 생성 버튼
            if (view.getCreateButton() != null) {
                view.getCreateButton().setOnAction(e -> {
                    System.out.println("플레이리스트 생성 버튼 클릭됨");
                    String playlistName = view.getPlaylistNameInput();
                    if (playlistName != null && !playlistName.trim().isEmpty()) {
                        createPlaylist(playlistName.trim());
                        view.clearPlaylistNameInput();
                    } else {
                        log.warn("플레이리스트 이름이 비어있습니다.");
                        System.out.println("플레이리스트 이름이 비어있음");
                    }
                });
                System.out.println("생성 버튼 이벤트 핸들러 연결 완료");
            } else {
                log.error("생성 버튼이 null입니다!");
            }
            
            // 플레이리스트 삭제 버튼
            if (view.getDeleteButton() != null) {
                view.getDeleteButton().setOnAction(e -> {
                    System.out.println("플레이리스트 삭제 버튼 클릭됨");
                    String selectedPlaylist = view.getSelectedPlaylist();
                    if (selectedPlaylist != null) {
                        deletePlaylist(selectedPlaylist);
                    } else {
                        log.warn("삭제할 플레이리스트가 선택되지 않았습니다.");
                        System.out.println("삭제할 플레이리스트가 선택되지 않음");
                    }
                });
                System.out.println("삭제 버튼 이벤트 핸들러 연결 완료");
            } else {
                log.error("삭제 버튼이 null입니다!");
            }
            
            // 곡 추가 버튼
            if (view.getAddButton() != null) {
                view.getAddButton().setOnAction(e -> {
                    System.out.println("곡 추가 버튼 클릭됨");
                    String selectedPlaylist = view.getSelectedPlaylist();
                    if (selectedPlaylist != null) {
                        log.debug("곡 추가 요청: 플레이리스트 '{}'", selectedPlaylist);
                        showAddMusicDialog(selectedPlaylist);
                    } else {
                        log.warn("곡을 추가할 플레이리스트가 선택되지 않았습니다.");
                        System.out.println("곡을 추가할 플레이리스트가 선택되지 않음");
                    }
                });
                System.out.println("곡 추가 버튼 이벤트 핸들러 연결 완료");
            } else {
                log.error("곡 추가 버튼이 null입니다!");
            }
            
            // 곡 제거 버튼
            if (view.getRemoveButton() != null) {
                view.getRemoveButton().setOnAction(e -> {
                    System.out.println("곡 제거 버튼 클릭됨");
                    String selectedPlaylist = view.getSelectedPlaylist();
                    String selectedMusic = view.getSelectedMusic();
                    if (selectedPlaylist != null && selectedMusic != null) {
                        log.debug("곡 제거 요청: '{}' <- '{}'", selectedMusic, selectedPlaylist);
                        removeMusicFromPlaylist(selectedPlaylist, selectedMusic);
                    } else {
                        log.warn("제거할 곡이나 플레이리스트가 선택되지 않았습니다.");
                        System.out.println("제거할 곡이나 플레이리스트가 선택되지 않음");
                    }
                });
                System.out.println("곡 제거 버튼 이벤트 핸들러 연결 완료");
            } else {
                log.error("곡 제거 버튼이 null입니다!");
            }
            
            log.debug("PlaylistView 이벤트 핸들러 설정 완료");
            System.out.println("모든 이벤트 핸들러 설정 완료");
            
        } catch (Exception e) {
            log.error("이벤트 핸들러 설정 중 오류", e);
            System.err.println("PlaylistActionHandler 이벤트 핸들러 설정 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 새 플레이리스트 생성
     */
    private void createPlaylist(String playlistName) {
        try {
            System.out.println("플레이리스트 생성 중: " + playlistName);
            
            // UI에 즉시 반영
            view.addPlaylist(playlistName);
            
            // 이벤트 발행
            Playlist newPlaylist = new Playlist(playlistName);
            publishEvent(new PlaylistEvent.PlaylistCreatedEvent(newPlaylist));
            
            log.info("플레이리스트 생성됨: {}", playlistName);
            System.out.println("플레이리스트 생성 완료: " + playlistName);
            
        } catch (Exception e) {
            log.error("플레이리스트 생성 중 오류: {}", playlistName, e);
            System.err.println("플레이리스트 생성 오류: " + e.getMessage());
        }
    }

    /**
     * 플레이리스트 삭제
     */
    private void deletePlaylist(String playlistName) {
        try {
            System.out.println("플레이리스트 삭제 중: " + playlistName);
            
            // UI에서 즉시 제거
            view.removePlaylist(playlistName);
            
            // 이벤트 발행
            publishEvent(new PlaylistEvent.PlaylistDeletedEvent(playlistName));
            
            log.info("플레이리스트 삭제됨: {}", playlistName);
            System.out.println("플레이리스트 삭제 완료: " + playlistName);
            
        } catch (Exception e) {
            log.error("플레이리스트 삭제 중 오류: {}", playlistName, e);
            System.err.println("플레이리스트 삭제 오류: " + e.getMessage());
        }
    }

    /**
     * 곡 추가 다이얼로그 표시 (임시 구현)
     */
    private void showAddMusicDialog(String playlistName) {
        try {
            log.debug("곡 추가 다이얼로그 표시 (미구현): {}", playlistName);
            System.out.println("곡 추가 다이얼로그 표시: " + playlistName);
            
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
            publishEvent(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, dummyMusic));
            
            log.debug("샘플 곡이 플레이리스트에 추가됨: {} -> {}", dummyMusic.getTitle(), playlistName);
            System.out.println("샘플 곡 추가 완료: " + dummyMusic.getTitle());
            
        } catch (Exception e) {
            log.error("곡 추가 중 오류: {}", playlistName, e);
            System.err.println("곡 추가 오류: " + e.getMessage());
        }
    }

    /**
     * 플레이리스트에서 곡 제거
     */
    private void removeMusicFromPlaylist(String playlistName, String musicDisplayText) {
        try {
            System.out.println("곡 제거 중: " + musicDisplayText + " from " + playlistName);
            
            // 현재는 표시 텍스트만 가지고 있으므로, 실제 MusicInfo 객체를 찾아야 함
            // 이는 PlaylistView 내부에서 처리하거나, 별도의 매핑 테이블이 필요
            
            log.debug("곡 제거 시뮬레이션: '{}' <- '{}'", musicDisplayText, playlistName);
            System.out.println("곡 제거 처리 완료");
            
            // TODO: 실제 MusicInfo 객체를 찾아서 제거
            // 현재는 UI에서만 제거 (PlaylistView가 내부적으로 처리)
            
        } catch (Exception e) {
            log.error("곡 제거 중 오류: {} <- {}", musicDisplayText, playlistName, e);
            System.err.println("곡 제거 오류: " + e.getMessage());
        }
    }

    /**
     * 안전한 이벤트 발행
     */
    private void publishEvent(ac.cwnu.synctune.sdk.event.BaseEvent event) {
        try {
            if (publisher != null) {
                publisher.publish(event);
                System.out.println("이벤트 발행 완료: " + event.getClass().getSimpleName());
            } else {
                log.error("EventPublisher가 null입니다. 이벤트를 발행할 수 없습니다.");
                System.err.println("EventPublisher가 null - 이벤트 발행 실패");
            }
        } catch (Exception e) {
            log.error("이벤트 발행 중 오류", e);
            System.err.println("이벤트 발행 오류: " + e.getMessage());
        }
    }

    /**
     * 외부에서 플레이리스트 생성 이벤트를 발행하고 싶을 때 사용
     */
    public void publishPlaylistCreatedEvent(String playlistName) {
        try {
            Playlist playlist = new Playlist(playlistName);
            publishEvent(new PlaylistEvent.PlaylistCreatedEvent(playlist));
            log.debug("PlaylistCreatedEvent 발행: {}", playlistName);
        } catch (Exception e) {
            log.error("PlaylistCreatedEvent 발행 중 오류", e);
        }
    }

    /**
     * 외부에서 플레이리스트 삭제 이벤트를 발행하고 싶을 때 사용
     */
    public void publishPlaylistDeletedEvent(String playlistName) {
        try {
            publishEvent(new PlaylistEvent.PlaylistDeletedEvent(playlistName));
            log.debug("PlaylistDeletedEvent 발행: {}", playlistName);
        } catch (Exception e) {
            log.error("PlaylistDeletedEvent 발행 중 오류", e);
        }
    }

    /**
     * 플레이리스트 데이터 동기화 (필요시 사용)
     */
    public void syncPlaylistData() {
        try {
            var playlists = view.getPlaylists();
            log.debug("현재 플레이리스트 개수: {}", playlists.size());
            System.out.println("플레이리스트 동기화: " + playlists.size() + "개");
            
            // 필요시 다른 모듈과 데이터 동기화 로직 추가
            // 예: 전체 플레이리스트 목록을 이벤트로 발행
            if (!playlists.isEmpty()) {
                publishEvent(new PlaylistEvent.AllPlaylistsLoadedEvent(playlists));
                log.debug("전체 플레이리스트 동기화 이벤트 발행: {}개", playlists.size());
            }
        } catch (Exception e) {
            log.error("플레이리스트 데이터 동기화 중 오류", e);
            System.err.println("플레이리스트 동기화 오류: " + e.getMessage());
        }
    }

    /**
     * 플레이리스트 새로고침
     */
    public void refreshPlaylists() {
        try {
            view.refreshPlaylists();
            syncPlaylistData();
            log.debug("플레이리스트 새로고침 완료");
            System.out.println("플레이리스트 새로고침 완료");
        } catch (Exception e) {
            log.error("플레이리스트 새로고침 중 오류", e);
            System.err.println("플레이리스트 새로고침 오류: " + e.getMessage());
        }
    }

    /**
     * 플레이리스트 통계 표시
     */
    public void showStatistics() {
        try {
            view.showPlaylistStatistics();
            log.debug("플레이리스트 통계 다이얼로그 표시");
        } catch (Exception e) {
            log.error("플레이리스트 통계 표시 중 오류", e);
            System.err.println("플레이리스트 통계 표시 오류: " + e.getMessage());
        }
    }

    /**
     * 음악 검색 필터 적용
     */
    public void filterMusic(String searchText) {
        try {
            view.filterMusicLibrary(searchText);
            log.debug("음악 라이브러리 필터 적용: '{}'", searchText);
        } catch (Exception e) {
            log.error("음악 필터 적용 중 오류", e);
            System.err.println("음악 필터 적용 오류: " + e.getMessage());
        }
    }

    /**
     * 현재 선택된 플레이리스트 반환
     */
    public String getCurrentPlaylist() {
        try {
            return view.getSelectedPlaylist();
        } catch (Exception e) {
            log.error("현재 플레이리스트 조회 중 오류", e);
            return null;
        }
    }

    /**
     * 현재 선택된 곡 반환
     */
    public String getCurrentMusic() {
        try {
            return view.getSelectedMusic();
        } catch (Exception e) {
            log.error("현재 음악 조회 중 오류", e);
            return null;
        }
    }

    /**
     * 리소스 정리
     */
    public void dispose() {
        try {
            log.debug("PlaylistActionHandler 리소스 정리");
            System.out.println("PlaylistActionHandler 리소스 정리");
            // 필요시 정리 작업 수행
        } catch (Exception e) {
            log.error("PlaylistActionHandler 정리 중 오류", e);
            System.err.println("PlaylistActionHandler 정리 오류: " + e.getMessage());
        }
    }
}