package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.ui.component.StyledButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlaylistView extends VBox {
    private final TextField playlistNameInput;
    private final StyledButton createButton;
    private final StyledButton deleteButton;
    private final StyledButton addButton;
    private final StyledButton removeButton;
    private final ListView<String> playlistListView;
    private final ListView<String> musicListView;
    private final ObservableList<String> playlistItems;

    public PlaylistView() {
        playlistItems = FXCollections.observableArrayList();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // 텍스트 필드
        playlistNameInput = new TextField();
        playlistNameInput.setPromptText("새 플레이리스트 이름");

        // 버튼들
        createButton = new StyledButton("생성", StyledButton.ButtonStyle.PRIMARY);
        deleteButton = new StyledButton("삭제", StyledButton.ButtonStyle.DANGER);
        addButton = new StyledButton("곡 추가", StyledButton.ButtonStyle.SUCCESS);
        removeButton = new StyledButton("곡 제거", StyledButton.ButtonStyle.WARNING);

        // 리스트 뷰
        playlistListView = new ListView<>();
        playlistListView.setPrefHeight(200);
        playlistListView.setItems(FXCollections.observableArrayList("기본 플레이리스트", "즐겨찾기"));

        musicListView = new ListView<>();
        musicListView.setPrefHeight(300);
        musicListView.setItems(playlistItems);
    }

    private void layoutComponents() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(300);

        // 플레이리스트 관리 영역
        HBox playlistControls = new HBox(5);
        playlistControls.getChildren().addAll(playlistNameInput, createButton, deleteButton);

        // 곡 관리 영역
        HBox musicControls = new HBox(5);
        musicControls.getChildren().addAll(addButton, removeButton);

        getChildren().addAll(
            new Label("플레이리스트"),
            playlistControls,
            playlistListView,
            new Separator(),
            new Label("재생 목록"),
            musicControls,
            musicListView
        );
    }

    private void setupEventHandlers() {
        // 플레이리스트 선택 시 곡 목록 업데이트
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadPlaylistSongs(newVal);
            }
        });

        // 더블클릭으로 곡 재생
        musicListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedSong = musicListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    // TODO: 곡 재생 이벤트 발행
                }
            }
        });
    }

    private void loadPlaylistSongs(String playlistName) {
        // TODO: 실제 플레이리스트 데이터 로드
        playlistItems.clear();
        playlistItems.addAll("샘플 곡 1", "샘플 곡 2", "샘플 곡 3");
    }

    // Getter 메서드들
    public Button getCreateButton() { return createButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public String getPlaylistNameInput() { return playlistNameInput.getText().trim(); }
    public String getSelectedPlaylist() { return playlistListView.getSelectionModel().getSelectedItem(); }
    public String getSelectedMusic() { return musicListView.getSelectionModel().getSelectedItem(); }
    public ObservableList<String> getPlaylistItems() { return playlistItems; }
}