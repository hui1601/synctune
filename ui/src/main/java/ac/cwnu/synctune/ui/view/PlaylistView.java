package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.StyledButton;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class PlaylistView extends VBox {
    // UI 컴포넌트들
    private TextField playlistNameInput;
    private StyledButton createButton;
    private StyledButton deleteButton;
    private StyledButton addButton;
    private StyledButton removeButton;
    private StyledButton importButton;
    private StyledButton exportButton;
    private StyledButton clearButton;
    
    private ListView<String> playlistListView;
    private ListView<MusicInfoItem> musicListView;
    private Label statusLabel;
    private ProgressBar operationProgress;
    
    // 데이터
    private final ObservableList<String> playlists;
    private final ObservableList<MusicInfoItem> currentPlaylistItems;
    
    // 현재 선택된 플레이리스트
    private String selectedPlaylistName;
    
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

    public PlaylistView() {
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
        removeButton = new StyledButton("제거", StyledButton.ButtonStyle.WARNING);
        importButton = new StyledButton("가져오기", StyledButton.ButtonStyle.CONTROL);
        exportButton = new StyledButton("내보내기", StyledButton.ButtonStyle.CONTROL);
        clearButton = new StyledButton("전체 삭제", StyledButton.ButtonStyle.DANGER);

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
        
        operationProgress = new ProgressBar();
        operationProgress.setVisible(false);
        operationProgress.setPrefWidth(Double.MAX_VALUE);

        // 초기 버튼 상태
        updateButtonStates();
    }

    private void setupCustomCellFactories() {
        // 플레이리스트 리스트뷰 셀 팩토리
        playlistListView.setCellFactory(lv -> {
            ListCell<String> cell = new TextFieldListCell<>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        // 기본 플레이리스트는 다른 스타일
                        if (isDefaultPlaylist(item)) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");
                        } else {
                            setStyle("-fx-font-weight: normal; -fx-text-fill: #2c3e50;");
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
                    }
                }
            };
            return cell;
        });
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(15));
        setPrefWidth(350);
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;");

        // 제목
        Label titleLabel = new Label("플레이리스트");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 플레이리스트 관리 영역
        VBox playlistSection = createPlaylistSection();
        
        // 구분선
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #dee2e6;");

        // 재생 목록 관리 영역
        VBox musicSection = createMusicSection();

        // 상태 및 진행 표시
        VBox statusSection = new VBox(5);
        statusSection.getChildren().addAll(statusLabel, operationProgress);

        getChildren().addAll(titleLabel, playlistSection, separator, musicSection, statusSection);
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
        
        // 관리 버튼들
        HBox manageBox = new HBox(5);
        manageBox.setAlignment(Pos.CENTER_LEFT);
        manageBox.getChildren().addAll(deleteButton, importButton, exportButton);

        section.getChildren().addAll(sectionTitle, createBox, playlistListView, manageBox);
        return section;
    }

    private VBox createMusicSection() {
        VBox section = new VBox(10);
        
        Label sectionTitle = new Label("재생 목록");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // 곡 관리 버튼들
        HBox musicControls = new HBox(5);
        musicControls.setAlignment(Pos.CENTER_LEFT);
        musicControls.getChildren().addAll(addButton, removeButton, clearButton);

        section.getChildren().addAll(sectionTitle, musicControls, musicListView);
        return section;
    }

    private void setupEventHandlers() {
        // 플레이리스트 선택 변경
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPlaylistName = newVal;
            loadPlaylistSongs(newVal);
            updateButtonStates();
        });

        // 음악 리스트 선택 변경
        musicListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates();
        });

        // 더블클릭으로 곡 재생
        musicListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playSelectedMusic(selected.getMusicInfo());
                }
            }
        });

        // 키보드 단축키
        setupKeyboardShortcuts();
        
        // Enter 키로 플레이리스트 생성
        playlistNameInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                createButton.fire();
            }
        });
    }

    private void setupKeyboardShortcuts() {
        setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case N: // Ctrl+N: 새 플레이리스트
                        playlistNameInput.requestFocus();
                        break;
                    case DELETE: // Ctrl+Del: 플레이리스트 삭제
                        if (getSelectedPlaylist() != null) {
                            deleteButton.fire();
                        }
                        break;
                    case A: // Ctrl+A: 곡 추가
                        addButton.fire();
                        break;
                    default:
                        break;
                }
                event.consume();
                            } else if (event.getCode() == KeyCode.DELETE) {
                // Del: 선택된 곡 제거
                if (!musicListView.getSelectionModel().getSelectedItems().isEmpty()) {
                    removeButton.fire();
                }
                event.consume();
            }
        });
    }

    private void setupContextMenus() {
        // 플레이리스트 리스트 컨텍스트 메뉴
        ContextMenu playlistContextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("이름 변경");
        MenuItem duplicateItem = new MenuItem("복제");
        MenuItem deleteItem = new MenuItem("삭제");
        
        renameItem.setOnAction(e -> renameSelectedPlaylist());
        duplicateItem.setOnAction(e -> duplicateSelectedPlaylist());
        deleteItem.setOnAction(e -> deleteButton.fire());
        
        playlistContextMenu.getItems().addAll(renameItem, duplicateItem, new SeparatorMenuItem(), deleteItem);
        playlistListView.setContextMenu(playlistContextMenu);
        
        // 음악 리스트 컨텍스트 메뉴
        ContextMenu musicContextMenu = new ContextMenu();
        MenuItem playItem = new MenuItem("재생");
        MenuItem removeItem = new MenuItem("목록에서 제거");
        MenuItem showFileItem = new MenuItem("파일 위치 열기");
        MenuItem propertiesItem = new MenuItem("속성");
        
        playItem.setOnAction(e -> {
            MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedMusic(selected.getMusicInfo());
            }
        });
        
        removeItem.setOnAction(e -> removeButton.fire());
        showFileItem.setOnAction(e -> showSelectedMusicFile());
        propertiesItem.setOnAction(e -> showMusicProperties());
        
        musicContextMenu.getItems().addAll(playItem, new SeparatorMenuItem(), 
                                          removeItem, new SeparatorMenuItem(),
                                          showFileItem, propertiesItem);
        musicListView.setContextMenu(musicContextMenu);
    }

    private void loadDefaultPlaylists() {
        Platform.runLater(() -> {
            playlists.addAll("즐겨찾기", "최근 재생", "내 플레이리스트");
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
        
        // TODO: 실제 플레이리스트 데이터 로드
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
                                            180000L, null);
        currentPlaylistItems.add(new MusicInfoItem(sampleMusic));
    }

    private void updateButtonStates() {
        boolean hasSelectedPlaylist = selectedPlaylistName != null;
        boolean hasSelectedMusic = !musicListView.getSelectionModel().getSelectedItems().isEmpty();
        boolean isDefaultPlaylist = hasSelectedPlaylist && isDefaultPlaylist(selectedPlaylistName);
        
        // 플레이리스트 관련 버튼
        deleteButton.setDisable(!hasSelectedPlaylist || isDefaultPlaylist);
        exportButton.setDisable(!hasSelectedPlaylist || currentPlaylistItems.isEmpty());
        
        // 곡 관련 버튼
        addButton.setDisable(!hasSelectedPlaylist);
        removeButton.setDisable(!hasSelectedMusic);
        clearButton.setDisable(!hasSelectedPlaylist || currentPlaylistItems.isEmpty());
    }

    private boolean isDefaultPlaylist(String name) {
        return "즐겨찾기".equals(name) || "최근 재생".equals(name);
    }

    private void updateStatusLabel(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? 
                "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : 
                "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        });
    }

    private void showProgress(boolean show) {
        Platform.runLater(() -> operationProgress.setVisible(show));
    }

    private void playSelectedMusic(MusicInfo music) {
        // TODO: 실제 재생 이벤트 발행
        updateStatusLabel("재생: " + music.getTitle(), false);
    }

    private void renameSelectedPlaylist() {
        String selected = getSelectedPlaylist();
        if (selected == null || isDefaultPlaylist(selected)) return;
        
        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("플레이리스트 이름 변경");
        dialog.setHeaderText("새로운 플레이리스트 이름을 입력하세요:");
        dialog.setContentText("이름:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !playlists.contains(newName)) {
                int index = playlists.indexOf(selected);
                playlists.set(index, newName);
                playlistListView.getSelectionModel().select(newName);
                updateStatusLabel("플레이리스트 이름이 변경되었습니다: " + newName, false);
            }
        });
    }

    private void duplicateSelectedPlaylist() {
        String selected = getSelectedPlaylist();
        if (selected == null) return;
        
        String newName = selected + " (복사본)";
        int counter = 1;
        while (playlists.contains(newName)) {
            newName = selected + " (복사본 " + counter + ")";
            counter++;
        }
        
        playlists.add(newName);
        playlistListView.getSelectionModel().select(newName);
        
        // 현재 플레이리스트의 곡들도 복사
        // TODO: 실제 구현에서는 플레이리스트 데이터 복사
        
        updateStatusLabel("플레이리스트가 복제되었습니다: " + newName, false);
    }

    private void showSelectedMusicFile() {
        MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                File musicFile = new File(selected.getMusicInfo().getFilePath());
                if (musicFile.exists()) {
                    // 파일 탐색기에서 파일 위치 열기
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("windows")) {
                        new ProcessBuilder("explorer.exe", "/select,", musicFile.getAbsolutePath()).start();
                    } else if (os.contains("mac")) {
                        new ProcessBuilder("open", "-R", musicFile.getAbsolutePath()).start();
                    } else {
                        // Linux - 파일 매니저 열기
                        new ProcessBuilder("xdg-open", musicFile.getParent()).start();
                    }
                } else {
                    updateStatusLabel("파일을 찾을 수 없습니다: " + musicFile.getName(), true);
                }
            } catch (Exception e) {
                updateStatusLabel("파일 위치를 열 수 없습니다", true);
            }
        }
    }

    private void showMusicProperties() {
        MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            MusicInfo music = selected.getMusicInfo();
            
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("음악 속성");
            dialog.setHeaderText(music.getTitle());
            
            String content = String.format(
                "아티스트: %s\n" +
                "앨범: %s\n" +
                "재생 시간: %s\n" +
                "파일 경로: %s\n" +
                "가사 파일: %s",
                music.getArtist(),
                music.getAlbum(),
                UIUtils.formatTime(music.getDurationMillis()),
                music.getFilePath(),
                music.getLrcPath() != null ? music.getLrcPath() : "없음"
            );
            
            dialog.setContentText(content);
            dialog.showAndWait();
        }
    }

    // 외부에서 플레이리스트 업데이트를 위한 메서드들
    public void addPlaylist(String name) {
        if (name != null && !name.trim().isEmpty() && !playlists.contains(name)) {
            Platform.runLater(() -> {
                playlists.add(name);
                playlistListView.getSelectionModel().select(name);
                updateStatusLabel("새 플레이리스트가 생성되었습니다: " + name, false);
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
        });
    }

    // Getter 메서드들
    public Button getCreateButton() { return createButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getImportButton() { return importButton; }
    public Button getExportButton() { return exportButton; }
    public Button getClearButton() { return clearButton; }
    
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
            .toList();
    }
    
    public ObservableList<MusicInfoItem> getPlaylistItems() { 
        return currentPlaylistItems; 
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