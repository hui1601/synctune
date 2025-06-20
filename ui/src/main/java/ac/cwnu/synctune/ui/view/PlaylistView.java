package ac.cwnu.synctune.ui.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.component.StyledButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlaylistView extends VBox {
    private TextField playlistNameInput;
    private StyledButton createButton;
    private StyledButton deleteButton;
    private StyledButton addButton;
    private StyledButton removeButton;
    private StyledButton refreshButton;
    private ListView<String> playlistListView;
    private ListView<String> musicListView;
    private Label libraryCountLabel;
    private Label playlistInfoLabel;
    
    private final ObservableList<String> playlistItems;
    private final ObservableList<String> playlistNames;
    
    // 데이터 저장
    private final Map<String, List<MusicInfo>> playlistData = new HashMap<>();
    private List<MusicInfo> musicLibrary = FXCollections.observableArrayList();

    public PlaylistView() {
        playlistItems = FXCollections.observableArrayList();
        playlistNames = FXCollections.observableArrayList();
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
        refreshButton = new StyledButton("🔄", StyledButton.ButtonStyle.CONTROL);
        refreshButton.setPrefWidth(30);

        // 정보 라벨들
        libraryCountLabel = new Label("라이브러리: 0곡");
        libraryCountLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        playlistInfoLabel = new Label("플레이리스트를 선택하세요");
        playlistInfoLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        // 리스트 뷰
        playlistListView = new ListView<>();
        playlistListView.setPrefHeight(200);
        playlistListView.setItems(playlistNames);
        playlistListView.setPlaceholder(new Label("플레이리스트가 없습니다\n'생성' 버튼을 눌러 만들어보세요"));

        musicListView = new ListView<>();
        musicListView.setPrefHeight(300);
        musicListView.setItems(playlistItems);
        musicListView.setPlaceholder(new Label("플레이리스트를 선택하거나\n곡을 추가해주세요"));

        // 기본 플레이리스트 추가
        addDefaultPlaylists();
    }

    private void layoutComponents() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(350);

        // 라이브러리 정보 영역
        VBox librarySection = new VBox(5);
        Label libraryTitle = new Label("음악 라이브러리");
        libraryTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        HBox libraryInfo = new HBox(10);
        libraryInfo.getChildren().addAll(libraryCountLabel, refreshButton);
        
        librarySection.getChildren().addAll(libraryTitle, libraryInfo);

        // 플레이리스트 관리 영역
        Label playlistTitle = new Label("플레이리스트");
        playlistTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        HBox playlistControls = new HBox(5);
        playlistControls.getChildren().addAll(playlistNameInput, createButton, deleteButton);

        // 곡 관리 영역
        HBox musicControls = new HBox(5);
        musicControls.getChildren().addAll(addButton, removeButton);

        getChildren().addAll(
            librarySection,
            new Separator(),
            playlistTitle,
            playlistControls,
            playlistListView,
            playlistInfoLabel,
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
                updatePlaylistInfo(newVal);
            }
        });

        // 더블클릭으로 곡 재생
        musicListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedSong = musicListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    // TODO: 곡 재생 이벤트 발행
                    playSelectedMusic(selectedSong);
                }
            }
        });

        // 새로고침 버튼
        refreshButton.setOnAction(e -> refreshPlaylists());
    }

    private void addDefaultPlaylists() {
        playlistNames.addAll("즐겨찾기", "최근 재생", "내가 만든 목록");
        
        // 기본 데이터 초기화
        playlistData.put("즐겨찾기", FXCollections.observableArrayList());
        playlistData.put("최근 재생", FXCollections.observableArrayList());
        playlistData.put("내가 만든 목록", FXCollections.observableArrayList());
    }

    private void loadPlaylistSongs(String playlistName) {
        playlistItems.clear();
        
        List<MusicInfo> songs = playlistData.get(playlistName);
        if (songs != null) {
            songs.forEach(music -> 
                playlistItems.add(formatMusicDisplay(music))
            );
        }
    }

    private void updatePlaylistInfo(String playlistName) {
        List<MusicInfo> songs = playlistData.get(playlistName);
        if (songs != null) {
            long totalDuration = songs.stream()
                .mapToLong(MusicInfo::getDurationMillis)
                .sum();
            
            String durationText = formatDuration(totalDuration);
            playlistInfoLabel.setText(String.format("%s: %d곡, %s", 
                playlistName, songs.size(), durationText));
        } else {
            playlistInfoLabel.setText("플레이리스트 정보를 불러올 수 없습니다");
        }
    }

    private String formatMusicDisplay(MusicInfo music) {
        String display = music.getTitle();
        if (!music.getArtist().equals("Unknown Artist")) {
            display += " - " + music.getArtist();
        }
        
        // 재생 시간 추가
        if (music.getDurationMillis() > 0) {
            display += " (" + formatDuration(music.getDurationMillis()) + ")";
        }
        
        return display;
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%d:%02d", minutes, seconds % 60);
        }
    }

    private void playSelectedMusic(String displayText) {
        // 선택된 곡 정보에서 실제 MusicInfo 찾기
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist != null) {
            List<MusicInfo> songs = playlistData.get(selectedPlaylist);
            if (songs != null) {
                for (MusicInfo music : songs) {
                    if (formatMusicDisplay(music).equals(displayText)) {
                        // TODO: 재생 요청 이벤트 발행
                        System.out.println("재생 요청: " + music.getTitle());
                        break;
                    }
                }
            }
        }
    }

    // ========== 공개 메서드들 (컨트롤러에서 사용) ==========

    public Button getCreateButton() { return createButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    
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
        return playlistItems; 
    }
    
    public void clearPlaylistNameInput() {
        playlistNameInput.clear();
    }

    // ========== 데이터 업데이트 메서드들 ==========

    public void addPlaylist(String playlistName) {
        if (!playlistNames.contains(playlistName)) {
            playlistNames.add(playlistName);
            playlistData.put(playlistName, FXCollections.observableArrayList());
        }
    }

    public void removePlaylist(String playlistName) {
        playlistNames.remove(playlistName);
        playlistData.remove(playlistName);
        
        // 선택된 플레이리스트가 삭제된 경우 초기화
        if (playlistName.equals(getSelectedPlaylist())) {
            playlistItems.clear();
            playlistInfoLabel.setText("플레이리스트를 선택하세요");
        }
    }

    public void addMusicToPlaylist(String playlistName, MusicInfo music) {
        List<MusicInfo> playlist = playlistData.get(playlistName);
        if (playlist != null && !playlist.contains(music)) {
            playlist.add(music);
            
            // 현재 선택된 플레이리스트라면 UI 업데이트
            if (playlistName.equals(getSelectedPlaylist())) {
                playlistItems.add(formatMusicDisplay(music));
                updatePlaylistInfo(playlistName);
            }
        }
    }

    public void removeMusicFromPlaylist(String playlistName, MusicInfo music) {
        List<MusicInfo> playlist = playlistData.get(playlistName);
        if (playlist != null) {
            playlist.remove(music);
            
            // 현재 선택된 플레이리스트라면 UI 업데이트
            if (playlistName.equals(getSelectedPlaylist())) {
                playlistItems.remove(formatMusicDisplay(music));
                updatePlaylistInfo(playlistName);
            }
        }
    }

    public void updatePlaylistOrder(Playlist playlist) {
        playlistData.put(playlist.getName(), playlist.getMusicList());
        
        // 현재 선택된 플레이리스트라면 UI 업데이트
        if (playlist.getName().equals(getSelectedPlaylist())) {
            loadPlaylistSongs(playlist.getName());
            updatePlaylistInfo(playlist.getName());
        }
    }

    public void loadPlaylists(List<Playlist> playlists) {
        playlistNames.clear();
        playlistData.clear();
        
        playlists.forEach(playlist -> {
            playlistNames.add(playlist.getName());
            playlistData.put(playlist.getName(), playlist.getMusicList());
        });
        
        playlistInfoLabel.setText("플레이리스트 " + playlists.size() + "개 로드됨");
    }

    public void updateMusicLibrary(List<MusicInfo> musicList) {
        this.musicLibrary = musicList;
        libraryCountLabel.setText("라이브러리: " + musicList.size() + "곡");
        
        // "최근 재생" 플레이리스트에 최근 스캔된 곡들 일부 추가 (시뮬레이션)
        if (!musicList.isEmpty()) {
            List<MusicInfo> recentPlaylist = playlistData.get("최근 재생");
            if (recentPlaylist != null) {
                recentPlaylist.clear();
                // 최대 10곡까지만 추가
                int count = Math.min(10, musicList.size());
                for (int i = 0; i < count; i++) {
                    recentPlaylist.add(musicList.get(i));
                }
                
                // 현재 "최근 재생"이 선택되어 있다면 업데이트
                if ("최근 재생".equals(getSelectedPlaylist())) {
                    loadPlaylistSongs("최근 재생");
                    updatePlaylistInfo("최근 재생");
                }
            }
        }
    }

    public void updateMusicMetadata(MusicInfo updatedMusic) {
        // 모든 플레이리스트에서 해당 음악의 메타데이터 업데이트
        playlistData.forEach((playlistName, musicList) -> {
            for (int i = 0; i < musicList.size(); i++) {
                MusicInfo music = musicList.get(i);
                if (music.getFilePath().equals(updatedMusic.getFilePath())) {
                    musicList.set(i, updatedMusic);
                    
                    // 현재 선택된 플레이리스트라면 UI 업데이트
                    if (playlistName.equals(getSelectedPlaylist())) {
                        loadPlaylistSongs(playlistName);
                        updatePlaylistInfo(playlistName);
                    }
                    break;
                }
            }
        });
    }

    public void refreshPlaylists() {
        String selectedPlaylist = getSelectedPlaylist();
        if (selectedPlaylist != null) {
            loadPlaylistSongs(selectedPlaylist);
            updatePlaylistInfo(selectedPlaylist);
        }
        
        // 라이브러리 카운트 업데이트
        libraryCountLabel.setText("라이브러리: " + musicLibrary.size() + "곡 (새로고침됨)");
        
        // 잠시 후 원래 텍스트로 복원
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(2),
                e -> libraryCountLabel.setText("라이브러리: " + musicLibrary.size() + "곡")
            )
        );
        timeline.play();
    }

    // ========== 검색 및 필터링 기능 ==========

    public void filterMusicLibrary(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            updateMusicLibrary(musicLibrary);
            return;
        }
        
        String lowerCaseSearch = searchText.toLowerCase().trim();
        List<MusicInfo> filteredList = musicLibrary.stream()
            .filter(music -> 
                music.getTitle().toLowerCase().contains(lowerCaseSearch) ||
                music.getArtist().toLowerCase().contains(lowerCaseSearch) ||
                music.getAlbum().toLowerCase().contains(lowerCaseSearch)
            )
            .toList();
        
        libraryCountLabel.setText("검색 결과: " + filteredList.size() + "곡");
    }

    public void showPlaylistStatistics() {
        StringBuilder stats = new StringBuilder("플레이리스트 통계:\n\n");
        
        playlistData.forEach((name, musicList) -> {
            long totalDuration = musicList.stream()
                .mapToLong(MusicInfo::getDurationMillis)
                .sum();
            
            stats.append(String.format("• %s: %d곡, %s\n", 
                name, musicList.size(), formatDuration(totalDuration)));
        });
        
        stats.append(String.format("\n총 라이브러리: %d곡", musicLibrary.size()));
        
        Alert statsDialog = new Alert(Alert.AlertType.INFORMATION);
        statsDialog.setTitle("플레이리스트 통계");
        statsDialog.setHeaderText("현재 플레이리스트 현황");
        statsDialog.setContentText(stats.toString());
        statsDialog.showAndWait();
    }
}