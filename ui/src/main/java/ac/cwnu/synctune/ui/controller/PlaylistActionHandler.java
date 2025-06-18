package ac.cwnu.synctune.ui.controller;

import java.util.Optional;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.view.PlaylistView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;

    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        log.debug("PlaylistActionHandler 초기화 완료");
    }

    private void attachEventHandlers() {
        // 플레이리스트 생성
        view.getCreateButton().setOnAction(e -> {
            String name = view.getPlaylistNameInput();
            if (name != null && !name.isEmpty()) {
                log.debug("새 플레이리스트 생성 요청: {}", name);
                Playlist playlist = new Playlist(name);
                publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(playlist));
                
                // 입력 필드 초기화 - 수정된 부분
                Platform.runLater(() -> view.clearPlaylistNameInput());
            } else {
                showAlert("오류", "플레이리스트 이름을 입력해주세요.", Alert.AlertType.WARNING);
            }
        });

        // 플레이리스트 삭제
        view.getDeleteButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            if (selectedPlaylist != null) {
                // 삭제 확인 다이얼로그
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("플레이리스트 삭제");
                confirmAlert.setHeaderText("플레이리스트를 삭제하시겠습니까?");
                confirmAlert.setContentText("플레이리스트 '" + selectedPlaylist + "'를 삭제하시겠습니까?");
                
                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    log.debug("플레이리스트 삭제 요청: {}", selectedPlaylist);
                    publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(selectedPlaylist));
                }
            } else {
                showAlert("오류", "삭제할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            }
        });

        // 곡 추가 (현재는 샘플 구현)
        view.getAddButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            if (selectedPlaylist != null) {
                // TODO: 파일 선택 다이얼로그 구현
                log.debug("곡 추가 요청 - 플레이리스트: {}", selectedPlaylist);
                showAlert("정보", "곡 추가 기능은 아직 구현 중입니다.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            }
        });

        // 곡 제거
        view.getRemoveButton().setOnAction(e -> {
            String selectedPlaylist = view.getSelectedPlaylist();
            String selectedMusic = view.getSelectedMusic();
            
            if (selectedPlaylist != null && selectedMusic != null) {
                log.debug("곡 제거 요청 - 플레이리스트: {}, 곡: {}", selectedPlaylist, selectedMusic);
                
                // TODO: 실제 MusicInfo 객체 생성 필요
                MusicInfo dummyMusic = new MusicInfo(selectedMusic, "Unknown", "Unknown", "", 0);
                publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(selectedPlaylist, dummyMusic));
                
                // UI에서 즉시 제거
                Platform.runLater(() -> view.getPlaylistItems().remove(selectedMusic));
            } else {
                showAlert("오류", "제거할 곡을 선택해주세요.", Alert.AlertType.WARNING);
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void addMusicToCurrentPlaylist(MusicInfo music) {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist != null && music != null) {
            Platform.runLater(() -> {
                view.getPlaylistItems().add(music.getTitle() + " - " + music.getArtist());
            });
            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(selectedPlaylist, music));
        }
    }

    public void updatePlaylistItems(java.util.List<MusicInfo> musicList) {
        Platform.runLater(() -> {
            view.getPlaylistItems().clear();
            musicList.forEach(music -> 
                view.getPlaylistItems().add(music.getTitle() + " - " + music.getArtist())
            );
        });
    }
}