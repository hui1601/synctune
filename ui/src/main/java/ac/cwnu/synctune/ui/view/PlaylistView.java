package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.StyledButton;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistView extends VBox {
    private static final Logger log = LoggerFactory.getLogger(PlaylistView.class);
    
    // UI 컴포넌트들
    private TextField playlistNameInput;
    private StyledButton createButton;
    private StyledButton deleteButton;
    private StyledButton addButton;
    private StyledButton removeButton;
    private StyledButton clearButton;
    
    private ListView<String> playlistListView;
    private ListView<MusicInfoItem> musicListView;
    private Label statusLabel;
    private Label playlistCountLabel;
    private Label musicCountLabel;
    
    // 데이터
    private final ObservableList<String> playlists;
    private final ObservableList<MusicInfoItem> currentPlaylistItems;
    
    // 현재 선택된 플레이리스트
    private String selectedPlaylistName;
    
    // 기본 플레이리스트 목록
    private static final List<String> DEFAULT_PLAYLISTS = List.of("즐겨찾기", "최근 재생");
    
    private EventPublisher eventPublisher;
    
    // 음악 정보를 표시하기 위한 래퍼 클래스
    public static class MusicInfoItem {
        private final MusicInfo musicInfo;
        private final String displayText;
        
        public MusicInfoItem(MusicInfo musicInfo) {
            this.musicInfo = musicInfo;
            this.displayText = formatDisplayText(musicInfo);
        }
        
        private String formatDisplayText(MusicInfo music) {
            if (music == null) return "알 수 없는 곡";
            
            StringBuilder sb = new StringBuilder();
            sb.append(music.getTitle());
            
            if (music.getArtist() != null && !music.getArtist().isEmpty() && 
                !music.getArtist().equals("Unknown Artist")) {
                sb.append(" - ").append(music.getArtist());
            }
            
            // 재생 시간 추가
            if (music.getDurationMillis() > 0) {
                sb.append(" (").append(UIUtils.formatTime(music.getDurationMillis())).append(")");
            }
            
            return sb.toString();
        }
        
        public MusicInfo getMusicInfo() { return musicInfo; }
        public String getDisplayText() { return displayText; }
        
        @Override
        public String toString() { return displayText; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MusicInfoItem that = (MusicInfoItem) obj;
            return musicInfo != null ? musicInfo.equals(that.musicInfo) : that.musicInfo == null;
        }
        
        @Override
        public int hashCode() {
            return musicInfo != null ? musicInfo.hashCode() : 0;
        }
    }

    public PlaylistView(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        playlists = FXCollections.observableArrayList();
        currentPlaylistItems = FXCollections.observableArrayList();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupContextMenus();
        loadDefaultPlaylists();
    }

    private void initializeComponents() {
        // 텍스트 필드
        playlistNameInput = new TextField();
        playlistNameInput.setPromptText("새 플레이리스트 이름 입력...");
        playlistNameInput.setPrefWidth(200);

        // 버튼들
        createButton = new StyledButton("생성", StyledButton.ButtonStyle.PRIMARY);
        deleteButton = new StyledButton("삭제", StyledButton.ButtonStyle.DANGER);
        addButton = new StyledButton("곡 추가", StyledButton.ButtonStyle.SUCCESS);
        removeButton = new StyledButton("곡 제거", StyledButton.ButtonStyle.WARNING);
        clearButton = new StyledButton("전체 삭제", StyledButton.ButtonStyle.DANGER);

        setupButtonTooltips();

        // 리스트 뷰들
        playlistListView = new ListView<>(playlists);
        playlistListView.setPrefHeight(150);
        playlistListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        musicListView = new ListView<>(currentPlaylistItems);
        musicListView.setPrefHeight(300);
        musicListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // 커스텀 셀 팩토리 설정
        setupCustomCellFactories();
        
        // 상태 표시
        statusLabel = new Label("플레이리스트를 선택하세요");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        playlistCountLabel = new Label("플레이리스트: 0개");
        playlistCountLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");
        
        musicCountLabel = new Label("곡: 0개");
        musicCountLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

        // 초기 버튼 상태
        updateButtonStates();
    }

    private void setupButtonTooltips() {
        createButton.setTooltip(new Tooltip("새 플레이리스트 생성"));
        deleteButton.setTooltip(new Tooltip("선택한 플레이리스트 삭제"));
        addButton.setTooltip(new Tooltip("음악 파일 추가"));
        removeButton.setTooltip(new Tooltip("선택한 곡 제거"));
        clearButton.setTooltip(new Tooltip("플레이리스트 비우기"));
    }

    private void setupCustomCellFactories() {
        // 플레이리스트 리스트뷰 셀 팩토리
        playlistListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // 기본 플레이리스트는 다른 스타일
                        if (isDefaultPlaylist(item)) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");
                            setText("⭐ " + item);
                        } else {
                            setStyle("-fx-font-weight: normal; -fx-text-fill: #2c3e50;");
                            setText("🎵 " + item);
                        }
                    }
                }
            };
            return cell;
        });
        
        // 음악 리스트뷰 셀 팩토리
        musicListView.setCellFactory(lv -> {
            ListCell<MusicInfoItem> cell = new ListCell<MusicInfoItem>() {
                @Override
                protected void updateItem(MusicInfoItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                        setStyle("");
                    } else {
                        setText(item.getDisplayText());
                        
                        // 툴팁에 자세한 정보 표시
                        MusicInfo music = item.getMusicInfo();
                        Tooltip tooltip = new Tooltip();
                        tooltip.setText(String.format(
                            "제목: %s\n아티스트: %s\n앨범: %s\n파일: %s",
                            music.getTitle(),
                            music.getArtist(),
                            music.getAlbum(),
                            new File(music.getFilePath()).getName()
                        ));
                        setTooltip(tooltip);
                        
                        // 파일 존재 여부에 따른 스타일
                        File musicFile = new File(music.getFilePath());
                        if (!musicFile.exists()) {
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                            setText("❌ " + item.getDisplayText() + " (파일 없음)");
                        } else {
                            setStyle("-fx-text-fill: #2c3e50;");
                        }
                    }
                }
            };
            
            // 더블클릭으로 재생
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    playSelectedMusic(cell.getItem().getMusicInfo());
                }
            });
            
            return cell;
        });
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(15));
        setPrefWidth(350);
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;");

        // 제목과 통계
        Label titleLabel = new Label("플레이리스트");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 통계 정보
        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.getChildren().addAll(playlistCountLabel, musicCountLabel);

        // 플레이리스트 관리 영역
        VBox playlistSection = createPlaylistSection();
        
        // 구분선
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #dee2e6;");

        // 재생 목록 관리 영역
        VBox musicSection = createMusicSection();

        // 상태 표시
        VBox statusSection = new VBox(5);
        statusSection.getChildren().add(statusLabel);

        getChildren().addAll(titleLabel, statsBox, playlistSection, separator, musicSection, statusSection);
    }

    private VBox createPlaylistSection() {
        VBox section = new VBox(10);
        
        Label sectionTitle = new Label("플레이리스트 관리");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // 생성 영역
        HBox createBox = new HBox(5);
        createBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(playlistNameInput, Priority.ALWAYS);
        createBox.getChildren().addAll(playlistNameInput, createButton);
        
        // 관리 버튼
        HBox manageBox = new HBox(5);
        manageBox.setAlignment(Pos.CENTER_LEFT);
        manageBox.getChildren().add(deleteButton);
        
        section.getChildren().addAll(sectionTitle, createBox, playlistListView, manageBox);
        return section;
    }

    private VBox createMusicSection() {
        VBox section = new VBox(10);
        
        Label sectionTitle = new Label("재생 목록");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // 곡 관리 버튼들
        HBox musicControls1 = new HBox(5);
        musicControls1.setAlignment(Pos.CENTER_LEFT);
        musicControls1.getChildren().addAll(addButton, removeButton);
        
        HBox musicControls2 = new HBox(5);
        musicControls2.setAlignment(Pos.CENTER_LEFT);
        musicControls2.getChildren().add(clearButton);

        section.getChildren().addAll(sectionTitle, musicControls1, musicControls2, musicListView);
        return section;
    }

    private void setupEventHandlers() {
        // 플레이리스트 선택 변경
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPlaylistName = newVal;
            loadPlaylistSongs(newVal);
            updateButtonStates();
            updateCounts();
        });

        // 음악 리스트 선택 변경
        musicListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates();
        });

        // Enter 키로 플레이리스트 생성
        playlistNameInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                createButton.fire();
            }
        });
    }

    private void setupContextMenus() {
        // 음악 리스트 컨텍스트 메뉴
        ContextMenu musicContextMenu = new ContextMenu();
        MenuItem playItem = new MenuItem("재생");
        MenuItem removeItem = new MenuItem("목록에서 제거");
        
        playItem.setOnAction(e -> {
            MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedMusic(selected.getMusicInfo());
            }
        });
        
        removeItem.setOnAction(e -> removeButton.fire());
        
        musicContextMenu.getItems().addAll(playItem, removeItem);
        musicListView.setContextMenu(musicContextMenu);
    }

    private void loadDefaultPlaylists() {
        Platform.runLater(() -> {
            playlists.addAll(DEFAULT_PLAYLISTS);
            updateCounts();
            if (!playlists.isEmpty()) {
                playlistListView.getSelectionModel().selectFirst();
            }
        });
    }

    private void loadPlaylistSongs(String playlistName) {
        if (playlistName == null) {
            currentPlaylistItems.clear();
            updateStatusLabel("플레이리스트를 선택하세요", false);
            return;
        }
        
        // 현재는 샘플 데이터 생성
        currentPlaylistItems.clear();
        
        if (playlistName.equals("즐겨찾기")) {
            addSampleMusic("My Favorite Song", "Favorite Artist", "Best Album");
            addSampleMusic("Amazing Grace", "Classical Artist", "Hymns Collection");
        } else if (playlistName.equals("최근 재생")) {
            addSampleMusic("Recently Played 1", "Recent Artist", "New Album");
            addSampleMusic("Recently Played 2", "Another Artist", "Latest Hits");
        }
        
        updateStatusLabel(String.format("%s (%d곡)", playlistName, currentPlaylistItems.size()), false);
    }

    private void addSampleMusic(String title, String artist, String album) {
        MusicInfo sampleMusic = new MusicInfo(title, artist, album, 
                                            "sample/" + title.replaceAll(" ", "_") + ".mp3", 
                                            (long)(Math.random() * 300000) + 120000, 
                                            null);
        MusicInfoItem item = new MusicInfoItem(sampleMusic);
        currentPlaylistItems.add(item);
    }

    private void updateButtonStates() {
        boolean hasSelectedPlaylist = selectedPlaylistName != null;
        boolean hasSelectedMusic = !musicListView.getSelectionModel().getSelectedItems().isEmpty();
        boolean isDefaultPlaylist = hasSelectedPlaylist && isDefaultPlaylist(selectedPlaylistName);
        boolean hasMusic = !currentPlaylistItems.isEmpty();
        
        // 플레이리스트 관련 버튼
        deleteButton.setDisable(!hasSelectedPlaylist || isDefaultPlaylist);
        
        // 곡 관련 버튼
        addButton.setDisable(!hasSelectedPlaylist);
        removeButton.setDisable(!hasSelectedMusic);
        clearButton.setDisable(!hasSelectedPlaylist || !hasMusic);
    }
    
    private void updateCounts() {
        Platform.runLater(() -> {
            playlistCountLabel.setText("플레이리스트: " + playlists.size() + "개");
            musicCountLabel.setText("곡: " + currentPlaylistItems.size() + "개");
        });
    }

    public boolean isDefaultPlaylist(String name) {
        return DEFAULT_PLAYLISTS.contains(name);
    }

    public void updateStatusLabel(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? 
                "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : 
                "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        });
    }

    private void playSelectedMusic(MusicInfo music) {
        if (music != null && eventPublisher != null) {
            log.debug("플레이리스트에서 곡 재생 요청: {}", music.getTitle());
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(music));
            updateStatusLabel("재생 요청: " + music.getTitle(), false);
        }
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

    // 외부에서 플레이리스트 업데이트를 위한 메서드들
    public void addPlaylist(String name) {
        if (name != null && !name.trim().isEmpty() && !playlists.contains(name)) {
            Platform.runLater(() -> {
                playlists.add(name);
                playlistListView.getSelectionModel().select(name);
                updateStatusLabel("새 플레이리스트가 생성되었습니다: " + name, false);
                updateCounts();
            });
        }
    }

    public void removePlaylist(String name) {
        if (name != null && playlists.contains(name)) {
            Platform.runLater(() -> {
                playlists.remove(name);
                if (name.equals(selectedPlaylistName)) {
                    selectedPlaylistName = null;
                    currentPlaylistItems.clear();
                }
                updateButtonStates();
                updateStatusLabel("플레이리스트가 삭제되었습니다: " + name, false);
                updateCounts();
            });
        }
    }

    public void addMusicToCurrentPlaylist(MusicInfo music) {
        if (music != null && selectedPlaylistName != null) {
            Platform.runLater(() -> {
                MusicInfoItem item = new MusicInfoItem(music);
                if (!currentPlaylistItems.contains(item)) {
                    currentPlaylistItems.add(item);
                    updateStatusLabel(String.format("%s에 추가됨: %s", selectedPlaylistName, music.getTitle()), false);
                    updateButtonStates();
                    updateCounts();
                }
            });
        }
    }

    public void removeMusicFromCurrentPlaylist(MusicInfo music) {
        if (music != null) {
            Platform.runLater(() -> {
                MusicInfoItem toRemove = null;
                for (MusicInfoItem item : currentPlaylistItems) {
                    if (item.getMusicInfo().equals(music)) {
                        toRemove = item;
                        break;
                    }
                }
                if (toRemove != null) {
                    currentPlaylistItems.remove(toRemove);
                    updateStatusLabel("곡이 제거되었습니다: " + music.getTitle(), false);
                    updateButtonStates();
                    updateCounts();
                }
            });
        }
    }

    public void updatePlaylistItems(List<MusicInfo> musicList) {
        Platform.runLater(() -> {
            currentPlaylistItems.clear();
            musicList.forEach(music -> currentPlaylistItems.add(new MusicInfoItem(music)));
            
            updateStatusLabel(String.format("%s (%d곡)", 
                selectedPlaylistName != null ? selectedPlaylistName : "플레이리스트", 
                currentPlaylistItems.size()), false);
            updateButtonStates();
            updateCounts();
        });
    }
    
    public void clearCurrentPlaylistItems() {
        Platform.runLater(() -> {
            currentPlaylistItems.clear();
            updateCounts();
        });
    }

    // Getter 메서드들
    public StyledButton getCreateButton() { return createButton; }
    public StyledButton getDeleteButton() { return deleteButton; }
    public StyledButton getAddButton() { return addButton; }
    public StyledButton getRemoveButton() { return removeButton; }
    public StyledButton getClearButton() { return clearButton; }
    
    public ObservableList<String> getPlaylists() { return playlists; }
    
    public String getPlaylistNameInput() { 
        return playlistNameInput.getText().trim(); 
    }
    
    public String getSelectedPlaylist() { 
        return playlistListView.getSelectionModel().getSelectedItem(); 
    }
    
    public MusicInfo getSelectedMusic() {
        MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getMusicInfo() : null;
    }
    
    public List<MusicInfo> getSelectedMusicList() {
        return musicListView.getSelectionModel().getSelectedItems().stream()
            .map(MusicInfoItem::getMusicInfo)
            .collect(Collectors.toList());
    }
    
    public List<MusicInfo> getAllMusicInCurrentPlaylist() {
        return currentPlaylistItems.stream()
            .map(MusicInfoItem::getMusicInfo)
            .collect(Collectors.toList());
    }
    
    public void clearPlaylistNameInput() {
        Platform.runLater(() -> playlistNameInput.clear());
    }
    
    public void selectPlaylist(String name) {
        Platform.runLater(() -> {
            if (playlists.contains(name)) {
                playlistListView.getSelectionModel().select(name);
            }
        });
    }
}