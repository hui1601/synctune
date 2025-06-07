package ac.cwnu.synctune.ui.view;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

/**
 * 플레이리스트 CRUD와 곡 목록 표시를 담당하는 뷰 컴포넌트
 */
public class PlaylistView extends VBox {
    private final TextField playlistNameInput;
    private final Button createButton;
    private final Button deleteButton;
    private final Button addButton;
    private final Button removeButton;
    private final ListView<String> playlistListView;
    private final ListView<String> musicListView;

    public PlaylistView() {
        this.setSpacing(10);
        this.setPadding(new Insets(10));

        // 플레이리스트 생성/삭제 컨트롤
        HBox controls = new HBox(5);
        playlistNameInput = new TextField();
        playlistNameInput.setPromptText("새 플레이리스트 이름");
        createButton = new Button("생성");
        deleteButton = new Button("삭제");
        controls.getChildren().addAll(playlistNameInput, createButton, deleteButton);

        // 리스트 뷰 설정
        playlistListView = new ListView<>();
        playlistListView.setPrefHeight(150);

        // 곡 추가/제거 컨트롤
        HBox musicControls = new HBox(5);
        addButton = new Button("곡 추가");
        removeButton = new Button("곡 제거");
        musicControls.getChildren().addAll(addButton, removeButton);

        // 곡 목록 뷰
        musicListView = new ListView<>();
        musicListView.setPrefHeight(200);

        // 최종 레이아웃 배치
        this.getChildren().addAll(new Label("Playlists"), controls, playlistListView,
                                  new Label("Tracks in Playlist"), musicControls, musicListView);
    }

    public Button getCreateButton() {
        return createButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public String getPlaylistNameInput() {
        return playlistNameInput.getText().trim();
    }

    public String getSelectedPlaylist() {
        return playlistListView.getSelectionModel().getSelectedItem();
    }

    public String getSelectedMusic() {
        return musicListView.getSelectionModel().getSelectedItem();
    }

    public ObservableList<String> getPlaylistItems() {
        return musicListView.getItems();
    }

    public ObservableList<String> getPlaylistNames() {
        return playlistListView.getItems();
    }

    /**
     * 현재 뷰에 표시된 곡 순서를 리스트로 반환
     */
    public java.util.List<String> getCurrentOrder() {
        return FXCollections.observableArrayList(musicListView.getItems());
    }
}