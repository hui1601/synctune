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
    
    // UI 컴포넌트들 - 플레이리스트 관리 버튼들 제거
    private StyledButton addButton;
    private StyledButton removeButton;
    private StyledButton clearButton;
    
    private ListView<MusicInfoItem> musicListView;
    private Label statusLabel;
    private Label musicCountLabel;
    
    // 데이터 - 단일 플레이리스트만 사용
    private final ObservableList<MusicInfoItem> playlistItems;
    
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
        playlistItems = FXCollections.observableArrayList();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupContextMenus();
        initializePlaylist();
    }

    private void initializeComponents() {
        // 버튼들 - 플레이리스트 생성/삭제 버튼 제거
        addButton = new StyledButton("곡 추가", StyledButton.ButtonStyle.SUCCESS);
        removeButton = new StyledButton("곡 제거", StyledButton.ButtonStyle.WARNING);
        clearButton = new StyledButton("전체 삭제", StyledButton.ButtonStyle.DANGER);

        setupButtonTooltips();

        // 리스트 뷰 - 단일 음악 리스트만 사용
        musicListView = new ListView<>(playlistItems);
        musicListView.setPrefHeight(400); // 높이 증가
        musicListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // 커스텀 셀 팩토리 설정
        setupCustomCellFactories();
        
        // 상태 표시
        statusLabel = new Label("곡을 추가하여 재생목록을 만드세요");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        musicCountLabel = new Label("곡: 0개");
        musicCountLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

        // 초기 버튼 상태
        updateButtonStates();
    }

    private void setupButtonTooltips() {
        addButton.setTooltip(new Tooltip("음악 파일 추가"));
        removeButton.setTooltip(new Tooltip("선택한 곡 제거"));
        clearButton.setTooltip(new Tooltip("재생목록 비우기"));
    }

    private void setupCustomCellFactories() {
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
        Label titleLabel = new Label("재생목록");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 통계 정보
        HBox statsBox = new HBox();
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.getChildren().add(musicCountLabel);

        // 재생 목록 관리 영역
        VBox musicSection = createMusicSection();

        // 상태 표시
        VBox statusSection = new VBox(5);
        statusSection.getChildren().add(statusLabel);

        getChildren().addAll(titleLabel, statsBox, musicSection, statusSection);
    }

    private VBox createMusicSection() {
        VBox section = new VBox(10);
        
        // 곡 관리 버튼들
        HBox musicControls1 = new HBox(5);
        musicControls1.setAlignment(Pos.CENTER_LEFT);
        musicControls1.getChildren().addAll(addButton, removeButton);
        
        HBox musicControls2 = new HBox(5);
        musicControls2.setAlignment(Pos.CENTER_LEFT);
        musicControls2.getChildren().add(clearButton);

        section.getChildren().addAll(musicControls1, musicControls2, musicListView);
        return section;
    }

    private void setupEventHandlers() {
        // 음악 리스트 선택 변경
        musicListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates();
        });

        // Enter 키로 선택된 곡 재생
        musicListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playSelectedMusic(selected.getMusicInfo());
                }
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

    private void initializePlaylist() {
        Platform.runLater(() -> {
            updateCounts();
            updateStatusLabel("곡을 추가하여 재생목록을 만드세요", false);
        });
    }

    private void updateButtonStates() {
        boolean hasSelectedMusic = !musicListView.getSelectionModel().getSelectedItems().isEmpty();
        boolean hasMusic = !playlistItems.isEmpty();
        
        // 곡 관련 버튼
        removeButton.setDisable(!hasSelectedMusic);
        clearButton.setDisable(!hasMusic);
    }
    
    private void updateCounts() {
        Platform.runLater(() -> {
            musicCountLabel.setText("곡: " + playlistItems.size() + "개");
        });
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
            log.debug("재생목록에서 곡 재생 요청: {}", music.getTitle());
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
    public void addMusicToCurrentPlaylist(MusicInfo music) {
        if (music != null) {
            Platform.runLater(() -> {
                MusicInfoItem item = new MusicInfoItem(music);
                if (!playlistItems.contains(item)) {
                    playlistItems.add(item);
                    updateStatusLabel("추가됨: " + music.getTitle(), false);
                    updateButtonStates();
                    updateCounts();
                } else {
                    updateStatusLabel("이미 재생목록에 있는 곡입니다: " + music.getTitle(), true);
                }
            });
        }
    }

    public void removeMusicFromCurrentPlaylist(MusicInfo music) {
        if (music != null) {
            Platform.runLater(() -> {
                MusicInfoItem toRemove = null;
                for (MusicInfoItem item : playlistItems) {
                    if (item.getMusicInfo().equals(music)) {
                        toRemove = item;
                        break;
                    }
                }
                if (toRemove != null) {
                    playlistItems.remove(toRemove);
                    updateStatusLabel("곡이 제거되었습니다: " + music.getTitle(), false);
                    updateButtonStates();
                    updateCounts();
                    
                    // 플레이리스트가 비었을 때 안내 메시지
                    if (playlistItems.isEmpty()) {
                        updateStatusLabel("재생목록이 비어있습니다. '곡 추가' 버튼으로 음악을 추가하세요.", false);
                    }
                }
            });
        }
    }

    public void updatePlaylistItems(List<MusicInfo> musicList) {
        Platform.runLater(() -> {
            playlistItems.clear();
            musicList.forEach(music -> playlistItems.add(new MusicInfoItem(music)));
            
            updateStatusLabel(String.format("재생목록 (%d곡)", playlistItems.size()), false);
            updateButtonStates();
            updateCounts();
        });
    }
    
    public void clearCurrentPlaylistItems() {
        Platform.runLater(() -> {
            playlistItems.clear();
            updateCounts();
            updateButtonStates();
            updateStatusLabel("재생목록이 비워졌습니다. '곡 추가' 버튼으로 음악을 추가하세요.", false);
        });
    }

    private int currentPlayingIndex = -1;

public MusicInfo getNextMusic() {
    if (playlistItems.isEmpty()) return null;
    
    int nextIndex = (currentPlayingIndex + 1) % playlistItems.size();
    return playlistItems.get(nextIndex).getMusicInfo();
}

    public MusicInfo getPreviousMusic() {
        if (playlistItems.isEmpty()) return null;
    
        int prevIndex = currentPlayingIndex - 1;
        if (prevIndex < 0) prevIndex = playlistItems.size() - 1;
        return playlistItems.get(prevIndex).getMusicInfo();
    }

    public void setCurrentPlayingMusic(MusicInfo music) {
        for (int i = 0; i < playlistItems.size(); i++) {
            if (playlistItems.get(i).getMusicInfo().equals(music)) {
                currentPlayingIndex = i;
                break;
            }
        }
    }

    // Getter 메서드들 - 플레이리스트 관련 제거
    public StyledButton getAddButton() { return addButton; }
    public StyledButton getRemoveButton() { return removeButton; }
    public StyledButton getClearButton() { return clearButton; }
    
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
        return playlistItems.stream()
            .map(MusicInfoItem::getMusicInfo)
            .collect(Collectors.toList());
    }

    // 기존 메서드들을 호환성을 위해 유지 (단일 플레이리스트 기준으로 동작)
    public ObservableList<String> getPlaylists() { 
        return FXCollections.observableArrayList("재생목록"); 
    }
    
    public String getPlaylistNameInput() { 
        return ""; // 플레이리스트 이름 입력 기능 제거
    }
    
    public String getSelectedPlaylist() { 
        return "재생목록"; // 항상 단일 플레이리스트 반환
    }
    
    public boolean isDefaultPlaylist(String name) {
        return true; // 단일 플레이리스트는 항상 기본으로 취급
    }
    
    public void addPlaylist(String name) {
        // 단일 플레이리스트에서는 사용하지 않음
    }
    
    public void removePlaylist(String name) {
        // 단일 플레이리스트에서는 사용하지 않음
    }
    
    public void clearPlaylistNameInput() {
        // 단일 플레이리스트에서는 사용하지 않음
    }
    
    public void selectPlaylist(String name) {
        // 단일 플레이리스트에서는 사용하지 않음
    }

    // 추가 getter들
    public StyledButton getCreateButton() { return null; } // 더 이상 사용하지 않음
    public StyledButton getDeleteButton() { return null; } // 더 이상 사용하지 않음
}