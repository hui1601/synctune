package ac.cwnu.synctune.ui.view;

import java.io.File;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PlaylistView extends VBox {
    private TextField playlistNameInput;
    private StyledButton createButton;
    private StyledButton deleteButton;
    private StyledButton addButton;
    private StyledButton removeButton;
    private ListView<String> playlistListView;
    private ListView<String> musicListView;
    
    // 실제 데이터 저장소
    private final Map<String, Playlist> playlists = new HashMap<>();
    private final ObservableList<String> playlistNames = FXCollections.observableArrayList();
    private final ObservableList<String> currentPlaylistSongs = FXCollections.observableArrayList();

    public PlaylistView() {
        initializeData();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeData() {
        // 기본 플레이리스트 생성
        Playlist defaultPlaylist = new Playlist("기본 플레이리스트");
        Playlist favoritePlaylist = new Playlist("즐겨찾기");
        
        playlists.put("기본 플레이리스트", defaultPlaylist);
        playlists.put("즐겨찾기", favoritePlaylist);
        
        playlistNames.addAll("기본 플레이리스트", "즐겨찾기");
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
        playlistListView = new ListView<>(playlistNames);
        playlistListView.setPrefHeight(200);

        musicListView = new ListView<>(currentPlaylistSongs);
        musicListView.setPrefHeight(300);
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
        // 플레이리스트 생성
        createButton.setOnAction(e -> createNewPlaylist());
        
        // 플레이리스트 삭제
        deleteButton.setOnAction(e -> deleteSelectedPlaylist());
        
        // 곡 추가
        addButton.setOnAction(e -> addMusicToPlaylist());
        
        // 곡 제거
        removeButton.setOnAction(e -> removeMusicFromPlaylist());

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
                    showInfo("재생", "'" + selectedSong + "' 재생 (구현 예정)");
                }
            }
        });
    }

    private void createNewPlaylist() {
        String name = playlistNameInput.getText().trim();
        if (name.isEmpty()) {
            showAlert("오류", "플레이리스트 이름을 입력해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        if (playlists.containsKey(name)) {
            showAlert("오류", "이미 존재하는 플레이리스트 이름입니다.", Alert.AlertType.WARNING);
            return;
        }
        
        // 새 플레이리스트 생성
        Playlist newPlaylist = new Playlist(name);
        playlists.put(name, newPlaylist);
        playlistNames.add(name);
        
        // 입력 필드 초기화
        playlistNameInput.clear();
        
        // 새로 만든 플레이리스트 선택
        playlistListView.getSelectionModel().select(name);
        
        showInfo("성공", "플레이리스트 '" + name + "'이 생성되었습니다.");
    }

    private void deleteSelectedPlaylist() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            showAlert("오류", "삭제할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 기본 플레이리스트는 삭제 불가
        if ("기본 플레이리스트".equals(selectedPlaylist) || "즐겨찾기".equals(selectedPlaylist)) {
            showAlert("오류", "기본 플레이리스트는 삭제할 수 없습니다.", Alert.AlertType.WARNING);
            return;
        }
        
        // 삭제 확인
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("플레이리스트 삭제");
        confirmAlert.setHeaderText("플레이리스트를 삭제하시겠습니까?");
        confirmAlert.setContentText("플레이리스트 '" + selectedPlaylist + "'을(를) 삭제하시겠습니까?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            playlists.remove(selectedPlaylist);
            playlistNames.remove(selectedPlaylist);
            currentPlaylistSongs.clear();
            
            showInfo("성공", "플레이리스트 '" + selectedPlaylist + "'이 삭제되었습니다.");
        }
    }

    private void addMusicToPlaylist() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 파일 선택 다이얼로그
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 선택");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("음악 파일", "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        // 현재 윈도우를 owner로 설정
        Stage currentStage = (Stage) getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(currentStage);
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            Playlist playlist = playlists.get(selectedPlaylist);
            int addedCount = 0;
            
            for (File file : selectedFiles) {
                // 간단한 MusicInfo 생성 (실제로는 메타데이터 추출 필요)
                String fileName = file.getName();
                String title = getFileNameWithoutExtension(fileName);
                MusicInfo musicInfo = new MusicInfo(title, "Unknown Artist", "Unknown Album", 
                                                  file.getAbsolutePath(), 0);
                
                // 플레이리스트에 추가 (중복 체크)
                if (!isAlreadyInPlaylist(playlist, musicInfo)) {
                    // 새로운 플레이리스트 생성 (불변 객체이므로)
                    playlists.put(selectedPlaylist, addPlaylistItem(playlist, musicInfo));
                    addedCount++;
                }
            }
            
            // UI 업데이트
            loadPlaylistSongs(selectedPlaylist);
            
            if (addedCount > 0) {
                showInfo("성공", addedCount + "개의 곡이 플레이리스트에 추가되었습니다.");
            } else {
                showInfo("정보", "추가할 새로운 곡이 없습니다 (중복 제외).");
            }
        }
    }

    private void removeMusicFromPlaylist() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        String selectedSong = musicListView.getSelectionModel().getSelectedItem();
        
        if (selectedPlaylist == null) {
            showAlert("오류", "플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        if (selectedSong == null) {
            showAlert("오류", "제거할 곡을 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        Playlist playlist = playlists.get(selectedPlaylist);
        if (playlist != null) {
            // 선택된 곡 찾기
            int selectedIndex = musicListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < playlist.getMusicList().size()) {
                // 새로운 플레이리스트 생성 (선택된 곡 제외)
                playlists.put(selectedPlaylist, removePlaylistItem(playlist, selectedIndex));
                
                // UI 업데이트
                loadPlaylistSongs(selectedPlaylist);
                
                showInfo("성공", "곡이 플레이리스트에서 제거되었습니다.");
            }
        }
    }

    private void loadPlaylistSongs(String playlistName) {
        currentPlaylistSongs.clear();
        Playlist playlist = playlists.get(playlistName);
        if (playlist != null) {
            for (MusicInfo music : playlist.getMusicList()) {
                currentPlaylistSongs.add(music.getTitle() + " - " + music.getArtist());
            }
        }
    }

    // 유틸리티 메서드들
    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private boolean isAlreadyInPlaylist(Playlist playlist, MusicInfo musicInfo) {
        return playlist.getMusicList().stream()
                .anyMatch(existing -> existing.getFilePath().equals(musicInfo.getFilePath()));
    }

    private Playlist addPlaylistItem(Playlist playlist, MusicInfo musicInfo) {
        var newList = new java.util.ArrayList<>(playlist.getMusicList());
        newList.add(musicInfo);
        return new Playlist(playlist.getName(), newList);
    }

    private Playlist removePlaylistItem(Playlist playlist, int index) {
        var newList = new java.util.ArrayList<>(playlist.getMusicList());
        newList.remove(index);
        return new Playlist(playlist.getName(), newList);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }

    // Getter 메서드들
    public Button getCreateButton() { return createButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public String getPlaylistNameInput() { return playlistNameInput.getText().trim(); }
    public String getSelectedPlaylist() { return playlistListView.getSelectionModel().getSelectedItem(); }
    public String getSelectedMusic() { return musicListView.getSelectionModel().getSelectedItem(); }
    public ObservableList<String> getPlaylistItems() { return currentPlaylistSongs; }
    
    public void clearPlaylistNameInput() {
        playlistNameInput.clear();
    }
    
    // 외부에서 플레이리스트 데이터에 접근할 수 있는 메서드
    public Map<String, Playlist> getPlaylists() {
        return new HashMap<>(playlists);
    }
    
    public Playlist getCurrentPlaylist() {
        String selected = getSelectedPlaylist();
        return selected != null ? playlists.get(selected) : null;
    }
}