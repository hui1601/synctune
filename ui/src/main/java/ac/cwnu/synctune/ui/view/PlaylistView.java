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
    
    // UI 컴포넌트들 - 폴더 추가 버튼 포함
    private StyledButton addButton;
    private StyledButton addFolderButton;  // 새로 추가
    private StyledButton removeButton;
    private StyledButton clearButton;
    
    private ListView<MusicInfoItem> musicListView;
    private Label statusLabel;
    private Label musicCountLabel;
    
    // 데이터 - 단일 플레이리스트만 사용
    private final ObservableList<MusicInfoItem> playlistItems;
    
    private EventPublisher eventPublisher;
    
    // 현재 재생 중인 곡 추적
    private int currentPlayingIndex = -1;
    private MusicInfo currentPlayingMusic = null;
    
    // 음악 정보를 표시하기 위한 래퍼 클래스
    public static class MusicInfoItem {
        private final MusicInfo musicInfo;
        private final String displayText;
        private boolean isCurrentlyPlaying = false;
        
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
        public boolean isCurrentlyPlaying() { return isCurrentlyPlaying; }
        public void setCurrentlyPlaying(boolean playing) { this.isCurrentlyPlaying = playing; }
        
        @Override
        public String toString() { 
            return isCurrentlyPlaying ? "♪ " + displayText : displayText; 
        }
        
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
        // 버튼들 - 폴더 추가 버튼 포함
        addButton = new StyledButton("파일 추가", StyledButton.ButtonStyle.SUCCESS);
        addFolderButton = new StyledButton("폴더 추가", StyledButton.ButtonStyle.SUCCESS);  // 새로 추가
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
        statusLabel = new Label("파일 추가나 폴더 추가 버튼으로 음악을 추가하세요");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        musicCountLabel = new Label("곡: 0개");
        musicCountLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

        // 초기 버튼 상태
        updateButtonStates();
    }

    private void setupButtonTooltips() {
        addButton.setTooltip(new Tooltip("음악 파일 선택해서 추가"));
        addFolderButton.setTooltip(new Tooltip("폴더에서 음악 파일들을 모두 추가"));  // 새로 추가
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
                        setText(item.toString());
                        
                        // 툴팁에 자세한 정보 표시
                        MusicInfo music = item.getMusicInfo();
                        Tooltip tooltip = new Tooltip();
                        String lrcStatus = music.getLrcPath() != null ? "가사 파일 있음" : "가사 파일 없음";
                        tooltip.setText(String.format(
                            "제목: %s\n아티스트: %s\n앨범: %s\n파일: %s\n%s",
                            music.getTitle(),
                            music.getArtist(),
                            music.getAlbum(),
                            new File(music.getFilePath()).getName(),
                            lrcStatus
                        ));
                        setTooltip(tooltip);
                        
                        // 현재 재생 중인 곡 하이라이트
                        if (item.isCurrentlyPlaying()) {
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffeaa7;");
                        } else {
                            // 파일 존재 여부에 따른 스타일
                            File musicFile = new File(music.getFilePath());
                            if (!musicFile.exists()) {
                                setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                                setText("❌ " + item.getDisplayText() + " (파일 없음)");
                            } else {
                                // 가사 파일 여부에 따른 아이콘 추가
                                String prefix = music.getLrcPath() != null ? "🎵 " : "";
                                setText(prefix + item.getDisplayText());
                                setStyle("-fx-text-fill: #2c3e50;");
                            }
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
        setPrefWidth(380);  // 폴더 버튼 추가로 너비 조금 증가
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
        
        // 첫 번째 줄: 파일 추가, 폴더 추가 버튼
        HBox musicControls1 = new HBox(5);
        musicControls1.setAlignment(Pos.CENTER_LEFT);
        musicControls1.getChildren().addAll(addButton, addFolderButton);
        
        // 두 번째 줄: 곡 제거, 전체 삭제 버튼
        HBox musicControls2 = new HBox(5);
        musicControls2.setAlignment(Pos.CENTER_LEFT);
        musicControls2.getChildren().addAll(removeButton, clearButton);

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
        MenuItem showFileItem = new MenuItem("파일 위치 열기");  // 새로 추가
        
        playItem.setOnAction(e -> {
            MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedMusic(selected.getMusicInfo());
            }
        });
        
        removeItem.setOnAction(e -> removeButton.fire());
        
        showFileItem.setOnAction(e -> {
            MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFileLocation(selected.getMusicInfo());
            }
        });
        
        musicContextMenu.getItems().addAll(playItem, new SeparatorMenuItem(), removeItem, showFileItem);
        musicListView.setContextMenu(musicContextMenu);
    }

    private void showFileLocation(MusicInfo music) {
        try {
            File musicFile = new File(music.getFilePath());
            if (musicFile.exists()) {
                // Windows
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    Runtime.getRuntime().exec("explorer.exe /select," + musicFile.getAbsolutePath());
                }
                // macOS
                else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Runtime.getRuntime().exec("open -R " + musicFile.getAbsolutePath());
                }
                // Linux
                else {
                    Runtime.getRuntime().exec("xdg-open " + musicFile.getParent());
                }
            } else {
                showAlert("오류", "파일이 존재하지 않습니다: " + musicFile.getAbsolutePath(), Alert.AlertType.WARNING);
            }
        } catch (Exception e) {
            log.error("파일 위치 열기 실패", e);
            showAlert("오류", "파일 위치를 열 수 없습니다: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void initializePlaylist() {
        Platform.runLater(() -> {
            updateCounts();
            updateStatusLabel("파일 추가나 폴더 추가 버튼으로 음악을 추가하세요", false);
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
            int totalSongs = playlistItems.size();
            long songsWithLyrics = playlistItems.stream()
                .mapToLong(item -> item.getMusicInfo().getLrcPath() != null ? 1 : 0)
                .sum();
            
            musicCountLabel.setText(String.format("곡: %d개 (가사: %d개)", totalSongs, songsWithLyrics));
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

    // ========== 현재 재생 중인 곡 추적 및 다음/이전 곡 관리 ==========
    
    /**
     * 현재 재생 중인 곡 설정
     */
    public void setCurrentPlayingMusic(MusicInfo music) {
        // 이전 재생 중인 곡의 하이라이트 제거
        if (currentPlayingIndex >= 0 && currentPlayingIndex < playlistItems.size()) {
            playlistItems.get(currentPlayingIndex).setCurrentlyPlaying(false);
        }
        
        currentPlayingMusic = music;
        currentPlayingIndex = -1;
        
        if (music != null) {
            // 새로운 재생 중인 곡 찾기
            for (int i = 0; i < playlistItems.size(); i++) {
                if (playlistItems.get(i).getMusicInfo().equals(music)) {
                    currentPlayingIndex = i;
                    playlistItems.get(i).setCurrentlyPlaying(true);
                    break;
                }
            }
        }
        
        // UI 업데이트
        Platform.runLater(() -> {
            musicListView.refresh();
            if (currentPlayingIndex >= 0) {
                musicListView.scrollTo(currentPlayingIndex);
                log.debug("현재 재생 중인 곡 설정: {} (인덱스: {})", music.getTitle(), currentPlayingIndex);
            }
        });
    }
    
    /**
     * 다음 곡 반환
     */
    public MusicInfo getNextMusic() {
        if (playlistItems.isEmpty()) {
            log.debug("재생목록이 비어있어 다음 곡이 없습니다.");
            return null;
        }
        
        int nextIndex;
        
        if (currentPlayingIndex < 0) {
            // 현재 재생 중인 곡이 없으면 첫 번째 곡
            nextIndex = 0;
        } else {
            // 다음 곡 계산
            nextIndex = (currentPlayingIndex + 1) % playlistItems.size();
            
            // 마지막 곡이었다면 더 이상 재생할 곡이 없음 (반복 재생 안함)
            if (nextIndex == 0 && currentPlayingIndex == playlistItems.size() - 1) {
                log.debug("마지막 곡이므로 다음 곡이 없습니다.");
                return null;
            }
        }
        
        MusicInfo nextMusic = playlistItems.get(nextIndex).getMusicInfo();
        log.debug("다음 곡 찾음: {} (인덱스: {} -> {})", nextMusic.getTitle(), currentPlayingIndex, nextIndex);
        return nextMusic;
    }

    /**
     * 이전 곡 반환
     */
    public MusicInfo getPreviousMusic() {
        if (playlistItems.isEmpty()) {
            log.debug("재생목록이 비어있어 이전 곡이 없습니다.");
            return null;
        }
        
        if (currentPlayingIndex <= 0) {
            log.debug("첫 번째 곡이거나 현재 곡이 없어 이전 곡이 없습니다.");
            return null;
        }
        
        int previousIndex = currentPlayingIndex - 1;
        MusicInfo previousMusic = playlistItems.get(previousIndex).getMusicInfo();
        log.debug("이전 곡 찾음: {} (인덱스: {} -> {})", previousMusic.getTitle(), currentPlayingIndex, previousIndex);
        return previousMusic;
    }
    
    /**
     * 특정 인덱스의 곡 반환
     */
    public MusicInfo getMusicAt(int index) {
        if (index >= 0 && index < playlistItems.size()) {
            return playlistItems.get(index).getMusicInfo();
        }
        return null;
    }
    
    /**
     * 현재 재생 중인 곡의 인덱스 반환
     */
    public int getCurrentPlayingIndex() {
        return currentPlayingIndex;
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
                int removeIndex = -1;
                
                for (int i = 0; i < playlistItems.size(); i++) {
                    MusicInfoItem item = playlistItems.get(i);
                    if (item.getMusicInfo().equals(music)) {
                        toRemove = item;
                        removeIndex = i;
                        break;
                    }
                }
                
                if (toRemove != null) {
                    playlistItems.remove(toRemove);
                    
                    // 현재 재생 중인 곡의 인덱스 조정
                    if (removeIndex == currentPlayingIndex) {
                        // 현재 재생 중인 곡이 제거됨
                        currentPlayingIndex = -1;
                        currentPlayingMusic = null;
                    } else if (removeIndex < currentPlayingIndex) {
                        // 현재 재생 중인 곡보다 앞의 곡이 제거됨
                        currentPlayingIndex--;
                    }
                    
                    updateStatusLabel("곡이 제거되었습니다: " + music.getTitle(), false);
                    updateButtonStates();
                    updateCounts();
                    
                    // 플레이리스트가 비었을 때 안내 메시지
                    if (playlistItems.isEmpty()) {
                        updateStatusLabel("재생목록이 비어있습니다. 파일 추가나 폴더 추가 버튼으로 음악을 추가하세요.", false);
                    }
                }
            });
        }
    }

    public void updatePlaylistItems(List<MusicInfo> musicList) {
        Platform.runLater(() -> {
            playlistItems.clear();
            currentPlayingIndex = -1;
            currentPlayingMusic = null;
            
            musicList.forEach(music -> playlistItems.add(new MusicInfoItem(music)));
            
            updateStatusLabel(String.format("재생목록 (%d곡)", playlistItems.size()), false);
            updateButtonStates();
            updateCounts();
        });
    }
    
    public void clearCurrentPlaylistItems() {
        Platform.runLater(() -> {
            playlistItems.clear();
            currentPlayingIndex = -1;
            currentPlayingMusic = null;
            updateCounts();
            updateButtonStates();
            updateStatusLabel("재생목록이 비워졌습니다. 파일 추가나 폴더 추가 버튼으로 음악을 추가하세요.", false);
        });
    }

    // Getter 메서드들 - 폴더 추가 버튼 포함
    public StyledButton getAddButton() { return addButton; }
    public StyledButton getAddFolderButton() { return addFolderButton; }  // 새로 추가
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
    
    public String getSelectedPlaylist() { 
        return "재생목록";
    }
    
    public boolean isDefaultPlaylist(String name) {
        return true;
    }
}