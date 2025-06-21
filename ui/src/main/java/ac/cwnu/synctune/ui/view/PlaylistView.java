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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlaylistView extends VBox {
    // UI 컴포넌트들
    private TextField playlistNameInput;
    private StyledButton createButton;
    private StyledButton deleteButton;
    private StyledButton addButton;
    private StyledButton addFolderButton; // 새로 추가
    private StyledButton removeButton;
    private StyledButton importButton;
    private StyledButton exportButton;
    private StyledButton clearButton;
    private StyledButton shufflePlaylistButton; // 새로 추가
    private StyledButton sortButton; // 새로 추가
    
    private ListView<String> playlistListView;
    private ListView<MusicInfoItem> musicListView;
    private Label statusLabel;
    private ProgressBar operationProgress;
    private Label playlistCountLabel; // 새로 추가
    private Label musicCountLabel; // 새로 추가
    
    // 검색 기능 추가
    private TextField searchField;
    private ToggleButton searchToggleButton;
    private boolean isSearchMode = false;
    private ObservableList<MusicInfoItem> originalMusicItems;
    
    // 데이터
    private final ObservableList<String> playlists;
    private final ObservableList<MusicInfoItem> currentPlaylistItems;
    
    // 현재 선택된 플레이리스트
    private String selectedPlaylistName;
    
    // 기본 플레이리스트 목록
    private static final List<String> DEFAULT_PLAYLISTS = List.of("즐겨찾기", "최근 재생");
    
    // 정렬 모드
    public enum SortMode {
        TITLE("제목순"),
        ARTIST("아티스트순"),
        ALBUM("앨범순"),
        DURATION("재생시간순"),
        FILE_NAME("파일명순"),
        DATE_ADDED("추가날짜순");
        
        private final String displayName;
        
        SortMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    private SortMode currentSortMode = SortMode.TITLE;
    private boolean sortAscending = true;
    
    // 음악 정보를 표시하기 위한 래퍼 클래스 (개선된 버전)
    public static class MusicInfoItem {
        private final MusicInfo musicInfo;
        private final String displayText;
        private final long addedTime; // 추가된 시간
        
        public MusicInfoItem(MusicInfo musicInfo) {
            this.musicInfo = musicInfo;
            this.displayText = formatDisplayText(musicInfo);
            this.addedTime = System.currentTimeMillis();
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
        public long getAddedTime() { return addedTime; }
        
        // 검색을 위한 메서드
        public boolean matches(String searchText) {
            if (searchText == null || searchText.trim().isEmpty()) return true;
            
            String lowerSearchText = searchText.toLowerCase();
            return musicInfo.getTitle().toLowerCase().contains(lowerSearchText) ||
                   musicInfo.getArtist().toLowerCase().contains(lowerSearchText) ||
                   musicInfo.getAlbum().toLowerCase().contains(lowerSearchText) ||
                   new File(musicInfo.getFilePath()).getName().toLowerCase().contains(lowerSearchText);
        }
        
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
        originalMusicItems = FXCollections.observableArrayList();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupContextMenus();
        setupKeyboardShortcuts();
        loadDefaultPlaylists();
    }

    private void initializeComponents() {
        // 텍스트 필드
        playlistNameInput = new TextField();
        playlistNameInput.setPromptText("새 플레이리스트 이름 입력...");
        playlistNameInput.setPrefWidth(200);

        // 검색 필드
        searchField = new TextField();
        searchField.setPromptText("곡 검색...");
        searchField.setVisible(false);
        searchField.setPrefWidth(200);

        // 버튼들
        createButton = new StyledButton("생성", StyledButton.ButtonStyle.PRIMARY);
        deleteButton = new StyledButton("삭제", StyledButton.ButtonStyle.DANGER);
        addButton = new StyledButton("파일 추가", StyledButton.ButtonStyle.SUCCESS);
        addFolderButton = new StyledButton("폴더 추가", StyledButton.ButtonStyle.SUCCESS);
        removeButton = new StyledButton("제거", StyledButton.ButtonStyle.WARNING);
        importButton = new StyledButton("가져오기", StyledButton.ButtonStyle.CONTROL);
        exportButton = new StyledButton("내보내기", StyledButton.ButtonStyle.CONTROL);
        clearButton = new StyledButton("전체 삭제", StyledButton.ButtonStyle.DANGER);
        shufflePlaylistButton = new StyledButton("섞기", StyledButton.ButtonStyle.CONTROL);
        sortButton = new StyledButton("정렬", StyledButton.ButtonStyle.CONTROL);
        searchToggleButton = new ToggleButton("🔍");

        // 버튼 크기 조정
        searchToggleButton.setPrefSize(30, 30);
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
        
        operationProgress = new ProgressBar();
        operationProgress.setVisible(false);
        operationProgress.setPrefWidth(Double.MAX_VALUE);

        // 초기 버튼 상태
        updateButtonStates();
    }

    private void setupButtonTooltips() {
        createButton.setTooltip(new Tooltip("새 플레이리스트 생성 (Ctrl+N)"));
        deleteButton.setTooltip(new Tooltip("선택한 플레이리스트 삭제 (Ctrl+Delete)"));
        addButton.setTooltip(new Tooltip("음악 파일 추가 (Ctrl+A)"));
        addFolderButton.setTooltip(new Tooltip("폴더에서 음악 추가"));
        removeButton.setTooltip(new Tooltip("선택한 곡 제거 (Delete)"));
        importButton.setTooltip(new Tooltip("M3U 플레이리스트 가져오기"));
        exportButton.setTooltip(new Tooltip("M3U 플레이리스트로 내보내기"));
        clearButton.setTooltip(new Tooltip("플레이리스트 비우기"));
        shufflePlaylistButton.setTooltip(new Tooltip("플레이리스트 순서 섞기"));
        sortButton.setTooltip(new Tooltip("정렬 옵션"));
        searchToggleButton.setTooltip(new Tooltip("곡 검색 (Ctrl+F)"));
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
                        
                        // 아이콘 추가
                        String prefix = isDefaultPlaylist(item) ? "⭐ " : "🎵 ";
                        setText(prefix + item);
                    }
                }
            };
            return cell;
        });
        
        // 음악 리스트뷰 셀 팩토리 (향상된 버전)
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
                            "제목: %s\n아티스트: %s\n앨범: %s\n파일: %s\n재생시간: %s\n추가일시: %s",
                            music.getTitle(),
                            music.getArtist(),
                            music.getAlbum(),
                            new File(music.getFilePath()).getName(),
                            UIUtils.formatTime(music.getDurationMillis()),
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(item.getAddedTime())
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
        setPrefWidth(400); // 약간 넓게 조정
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;");

        // 제목과 통계
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("플레이리스트");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleBox.getChildren().addAll(titleLabel, searchToggleButton);

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

        // 상태 및 진행 표시
        VBox statusSection = new VBox(5);
        statusSection.getChildren().addAll(statusLabel, operationProgress);

        getChildren().addAll(titleBox, statsBox, playlistSection, separator, musicSection, statusSection);
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
        
        // 관리 버튼들 (2줄로 배치)
        HBox manageBox1 = new HBox(5);
        manageBox1.setAlignment(Pos.CENTER_LEFT);
        manageBox1.getChildren().addAll(deleteButton, importButton, exportButton);
        
        section.getChildren().addAll(sectionTitle, createBox, playlistListView, manageBox1);
        return section;
    }

    private VBox createMusicSection() {
        VBox section = new VBox(10);
        
        // 섹션 제목과 검색
        HBox sectionTitleBox = new HBox(10);
        sectionTitleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label sectionTitle = new Label("재생 목록");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        HBox.setHgrow(sectionTitle, Priority.ALWAYS);
        sectionTitleBox.getChildren().addAll(sectionTitle);
        
        // 검색 필드 (토글)
        HBox searchBox = new HBox(5);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getChildren().add(searchField);
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        
        // 곡 관리 버튼들 (2줄로 배치)
        HBox musicControls1 = new HBox(5);
        musicControls1.setAlignment(Pos.CENTER_LEFT);
        musicControls1.getChildren().addAll(addButton, addFolderButton, removeButton);
        
        HBox musicControls2 = new HBox(5);
        musicControls2.setAlignment(Pos.CENTER_LEFT);
        musicControls2.getChildren().addAll(clearButton, shufflePlaylistButton, sortButton);

        section.getChildren().addAll(sectionTitleBox, searchBox, musicControls1, musicControls2, musicListView);
        
        // 검색 토글 처리
        searchToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isSearchMode = newVal;
            searchBox.setVisible(newVal);
            searchBox.setManaged(newVal);
            
            if (newVal) {
                searchField.requestFocus();
            } else {
                searchField.clear();
                showAllMusic();
            }
        });
        
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

        // 검색 기능
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterMusic(newVal);
        });

        // 정렬 버튼
        sortButton.setOnAction(e -> showSortMenu());
        
        // 섞기 버튼
        shufflePlaylistButton.setOnAction(e -> shuffleCurrentPlaylist());

        // Enter 키로 플레이리스트 생성
        playlistNameInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                createButton.fire();
            }
        });
        
        // 검색 필드 Enter 키 처리
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchToggleButton.setSelected(false);
            }
        });
    }

    private void setupKeyboardShortcuts() {
        setFocusTraversable(true);
        setOnKeyPressed(this::handleKeyboardShortcuts);
    }
    
    private void handleKeyboardShortcuts(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case N: // Ctrl+N: 새 플레이리스트
                    playlistNameInput.requestFocus();
                    event.consume();
                    break;
                case DELETE: // Ctrl+Del: 플레이리스트 삭제
                    if (getSelectedPlaylist() != null) {
                        deleteButton.fire();
                    }
                    event.consume();
                    break;
                case A: // Ctrl+A: 곡 추가
                    addButton.fire();
                    event.consume();
                    break;
                case F: // Ctrl+F: 검색
                    searchToggleButton.setSelected(!searchToggleButton.isSelected());
                    event.consume();
                    break;
                case S: // Ctrl+S: 정렬
                    if (event.isShiftDown()) {
                        shufflePlaylistButton.fire(); // Ctrl+Shift+S: 섞기
                    } else {
                        sortButton.fire(); // Ctrl+S: 정렬
                    }
                    event.consume();
                    break;
                default:
                    break;
            }
        } else if (event.getCode() == KeyCode.DELETE) {
            // Del: 선택된 곡 제거
            if (!musicListView.getSelectionModel().getSelectedItems().isEmpty()) {
                removeButton.fire();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.F3) {
            // F3: 다음 검색 결과 (검색 모드일 때)
            if (isSearchMode) {
                findNextMatch();
            }
            event.consume();
        }
    }

    private void setupContextMenus() {
        // 플레이리스트 리스트 컨텍스트 메뉴
        ContextMenu playlistContextMenu = new ContextMenu();
        MenuItem newPlaylistItem = new MenuItem("새 플레이리스트");
        MenuItem renameItem = new MenuItem("이름 변경");
        MenuItem duplicateItem = new MenuItem("복제");
        MenuItem deleteItem = new MenuItem("삭제");
        MenuItem exportItem = new MenuItem("내보내기");
        
        newPlaylistItem.setOnAction(e -> playlistNameInput.requestFocus());
        renameItem.setOnAction(e -> renameSelectedPlaylist());
        duplicateItem.setOnAction(e -> duplicateSelectedPlaylist());
        deleteItem.setOnAction(e -> deleteButton.fire());
        exportItem.setOnAction(e -> exportButton.fire());
        
        playlistContextMenu.getItems().addAll(
            newPlaylistItem, new SeparatorMenuItem(),
            renameItem, duplicateItem, new SeparatorMenuItem(),
            exportItem, deleteItem
        );
        playlistListView.setContextMenu(playlistContextMenu);
        
        // 음악 리스트 컨텍스트 메뉴
        ContextMenu musicContextMenu = new ContextMenu();
        MenuItem playItem = new MenuItem("재생");
        MenuItem addToQueueItem = new MenuItem("재생 대기열에 추가");
        MenuItem removeItem = new MenuItem("목록에서 제거");
        MenuItem showFileItem = new MenuItem("파일 위치 열기");
        MenuItem propertiesItem = new MenuItem("속성");
        MenuItem refreshItem = new MenuItem("파일 상태 새로고침");
        
        playItem.setOnAction(e -> {
            MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedMusic(selected.getMusicInfo());
            }
        });
        
        addToQueueItem.setOnAction(e -> addSelectedToQueue());
        removeItem.setOnAction(e -> removeButton.fire());
        showFileItem.setOnAction(e -> showSelectedMusicFile());
        propertiesItem.setOnAction(e -> showMusicProperties());
        refreshItem.setOnAction(e -> refreshSelectedMusicFiles());
        
        musicContextMenu.getItems().addAll(
            playItem, addToQueueItem, new SeparatorMenuItem(),
            removeItem, new SeparatorMenuItem(),
            showFileItem, refreshItem, propertiesItem
        );
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
            originalMusicItems.clear();
            updateStatusLabel("플레이리스트를 선택하세요", false);
            return;
        }
        
        // TODO: 실제 플레이리스트 데이터 로드
        // 현재는 샘플 데이터 생성
        currentPlaylistItems.clear();
        originalMusicItems.clear();
        
        if (playlistName.equals("즐겨찾기")) {
            addSampleMusic("My Favorite Song", "Favorite Artist", "Best Album");
            addSampleMusic("Amazing Grace", "Classical Artist", "Hymns Collection");
            addSampleMusic("Wonderful Tonight", "Eric Clapton", "Slowhand");
        } else if (playlistName.equals("최근 재생")) {
            addSampleMusic("Recently Played 1", "Recent Artist", "New Album");
            addSampleMusic("Recently Played 2", "Another Artist", "Latest Hits");
            addSampleMusic("Just Played", "Current Artist", "Now Playing");
        }
        
        // 원본 리스트 복사
        originalMusicItems.addAll(currentPlaylistItems);
        
        updateStatusLabel(String.format("%s (%d곡)", playlistName, currentPlaylistItems.size()), false);
    }

    private void addSampleMusic(String title, String artist, String album) {
        MusicInfo sampleMusic = new MusicInfo(title, artist, album, 
                                            "sample/" + title.replaceAll(" ", "_") + ".mp3", 
                                            (long)(Math.random() * 300000) + 120000, // 2-7분 랜덤
                                            null);
        MusicInfoItem item = new MusicInfoItem(sampleMusic);
        currentPlaylistItems.add(item);
    }

    private void filterMusic(String searchText) {
        if (!isSearchMode || searchText == null) {
            showAllMusic();
            return;
        }
        
        if (searchText.trim().isEmpty()) {
            showAllMusic();
        } else {
            List<MusicInfoItem> filteredItems = originalMusicItems.stream()
                .filter(item -> item.matches(searchText))
                .collect(Collectors.toList());
            
            currentPlaylistItems.setAll(filteredItems);
            updateStatusLabel(String.format("검색 결과: %d곡 (전체 %d곡 중)", 
                                           filteredItems.size(), originalMusicItems.size()), false);
        }
    }
    
    private void showAllMusic() {
        currentPlaylistItems.setAll(originalMusicItems);
        if (selectedPlaylistName != null) {
            updateStatusLabel(String.format("%s (%d곡)", selectedPlaylistName, currentPlaylistItems.size()), false);
        }
    }
    
    private void findNextMatch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) return;
        
        int currentIndex = musicListView.getSelectionModel().getSelectedIndex();
        int nextIndex = -1;
        
        for (int i = currentIndex + 1; i < currentPlaylistItems.size(); i++) {
            if (currentPlaylistItems.get(i).matches(searchText)) {
                nextIndex = i;
                break;
            }
        }
        
        // 끝에서 처음으로 돌아가기
        if (nextIndex == -1) {
            for (int i = 0; i <= currentIndex; i++) {
                if (currentPlaylistItems.get(i).matches(searchText)) {
                    nextIndex = i;
                    break;
                }
            }
        }
        
        if (nextIndex != -1) {
            musicListView.getSelectionModel().select(nextIndex);
            musicListView.scrollTo(nextIndex);
        }
    }

    private void showSortMenu() {
        ContextMenu sortMenu = new ContextMenu();
        
        for (SortMode mode : SortMode.values()) {
            CheckMenuItem item = new CheckMenuItem(mode.getDisplayName());
            item.setSelected(currentSortMode == mode);
            item.setOnAction(e -> sortMusic(mode));
            sortMenu.getItems().add(item);
        }
        
        sortMenu.getItems().add(new SeparatorMenuItem());
        
        CheckMenuItem ascendingItem = new CheckMenuItem("오름차순");
        CheckMenuItem descendingItem = new CheckMenuItem("내림차순");
        ascendingItem.setSelected(sortAscending);
        descendingItem.setSelected(!sortAscending);
        
        ascendingItem.setOnAction(e -> { sortAscending = true; sortMusic(currentSortMode); });
        descendingItem.setOnAction(e -> { sortAscending = false; sortMusic(currentSortMode); });
        
        sortMenu.getItems().addAll(ascendingItem, descendingItem);
        
        sortMenu.show(sortButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    
    private void sortMusic(SortMode mode) {
        currentSortMode = mode;
        
        List<MusicInfoItem> sortedItems = originalMusicItems.stream()
            .sorted((a, b) -> {
                int result = compareByMode(a, b, mode);
                return sortAscending ? result : -result;
            })
            .collect(Collectors.toList());
        
        originalMusicItems.setAll(sortedItems);
        
        // 검색 모드인 경우 필터 다시 적용
        if (isSearchMode) {
            filterMusic(searchField.getText());
        } else {
            currentPlaylistItems.setAll(sortedItems);
        }
        
        updateStatusLabel(String.format("정렬: %s (%s)", mode.getDisplayName(), 
                                       sortAscending ? "오름차순" : "내림차순"), false);
    }
    
    private int compareByMode(MusicInfoItem a, MusicInfoItem b, SortMode mode) {
        MusicInfo musicA = a.getMusicInfo();
        MusicInfo musicB = b.getMusicInfo();
        
        return switch (mode) {
            case TITLE -> musicA.getTitle().compareToIgnoreCase(musicB.getTitle());
            case ARTIST -> musicA.getArtist().compareToIgnoreCase(musicB.getArtist());
            case ALBUM -> musicA.getAlbum().compareToIgnoreCase(musicB.getAlbum());
            case DURATION -> Long.compare(musicA.getDurationMillis(), musicB.getDurationMillis());
            case FILE_NAME -> {
                String fileA = new File(musicA.getFilePath()).getName();
                String fileB = new File(musicB.getFilePath()).getName();
                yield fileA.compareToIgnoreCase(fileB);
            }
            case DATE_ADDED -> Long.compare(a.getAddedTime(), b.getAddedTime());
        };
    }
    
    private void shuffleCurrentPlaylist() {
        if (originalMusicItems.isEmpty()) {
            showAlert("정보", "플레이리스트가 비어있습니다.", Alert.AlertType.INFORMATION);
            return;
        }
        
        List<MusicInfoItem> shuffledItems = new java.util.ArrayList<>(originalMusicItems);
        java.util.Collections.shuffle(shuffledItems);
        
        originalMusicItems.setAll(shuffledItems);
        
        if (!isSearchMode) {
            currentPlaylistItems.setAll(shuffledItems);
        } else {
            filterMusic(searchField.getText());
        }
        
        updateStatusLabel("플레이리스트 순서가 섞였습니다.", false);
    }

    private void updateButtonStates() {
        boolean hasSelectedPlaylist = selectedPlaylistName != null;
        boolean hasSelectedMusic = !musicListView.getSelectionModel().getSelectedItems().isEmpty();
        boolean isDefaultPlaylist = hasSelectedPlaylist && isDefaultPlaylist(selectedPlaylistName);
        boolean hasMusic = !currentPlaylistItems.isEmpty();
        
        // 플레이리스트 관련 버튼
        deleteButton.setDisable(!hasSelectedPlaylist || isDefaultPlaylist);
        exportButton.setDisable(!hasSelectedPlaylist || !hasMusic);
        
        // 곡 관련 버튼
        addButton.setDisable(!hasSelectedPlaylist);
        addFolderButton.setDisable(!hasSelectedPlaylist);
        removeButton.setDisable(!hasSelectedMusic);
        clearButton.setDisable(!hasSelectedPlaylist || !hasMusic);
        shufflePlaylistButton.setDisable(!hasMusic);
        sortButton.setDisable(!hasMusic);
        searchToggleButton.setDisable(!hasMusic);
    }
    
    private void updateCounts() {
        Platform.runLater(() -> {
            playlistCountLabel.setText("플레이리스트: " + playlists.size() + "개");
            
            if (isSearchMode && !searchField.getText().trim().isEmpty()) {
                musicCountLabel.setText(String.format("곡: %d개 (전체 %d개)", 
                                                    currentPlaylistItems.size(), originalMusicItems.size()));
            } else {
                musicCountLabel.setText("곡: " + currentPlaylistItems.size() + "개");
            }
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

    public void showProgress(boolean show) {
    Platform.runLater(() -> operationProgress.setVisible(show));
}

    private void playSelectedMusic(MusicInfo music) {
        // TODO: 실제 재생 이벤트 발행
        updateStatusLabel("재생: " + music.getTitle(), false);
    }
    
    private void addSelectedToQueue() {
        List<MusicInfo> selectedMusic = getSelectedMusicList();
        if (!selectedMusic.isEmpty()) {
            // TODO: 재생 대기열 추가 이벤트 발행
            updateStatusLabel(selectedMusic.size() + "개 곡이 재생 대기열에 추가되었습니다.", false);
        }
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
                selectedPlaylistName = newName;
                updateStatusLabel("플레이리스트 이름이 변경되었습니다: " + newName, false);
            } else {
                showAlert("오류", "이미 존재하는 이름이거나 유효하지 않은 이름입니다.", Alert.AlertType.WARNING);
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
        List<MusicInfoItem> copiedItems = new java.util.ArrayList<>();
        for (MusicInfoItem item : originalMusicItems) {
            copiedItems.add(new MusicInfoItem(item.getMusicInfo()));
        }
        
        // TODO: 실제 구현에서는 플레이리스트 데이터 복사 이벤트 발행
        
        updateStatusLabel("플레이리스트가 복제되었습니다: " + newName, false);
        updateCounts();
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
    
    private void refreshSelectedMusicFiles() {
        List<MusicInfoItem> selectedItems = musicListView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) return;
        
        int refreshedCount = 0;
        for (MusicInfoItem item : selectedItems) {
            File musicFile = new File(item.getMusicInfo().getFilePath());
            if (musicFile.exists()) {
                refreshedCount++;
            }
        }
        
        // UI 새로고침
        musicListView.refresh();
        updateStatusLabel(String.format("%d개 파일 상태를 새로고침했습니다.", refreshedCount), false);
    }

    private void showMusicProperties() {
        MusicInfoItem selected = musicListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            MusicInfo music = selected.getMusicInfo();
            File musicFile = new File(music.getFilePath());
            
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("음악 속성");
            dialog.setHeaderText(music.getTitle());
            
            String content = String.format(
                "아티스트: %s\n" +
                "앨범: %s\n" +
                "재생 시간: %s\n" +
                "파일 경로: %s\n" +
                "파일 크기: %s\n" +
                "파일 존재: %s\n" +
                "가사 파일: %s\n" +
                "추가 일시: %s",
                music.getArtist(),
                music.getAlbum(),
                UIUtils.formatTime(music.getDurationMillis()),
                music.getFilePath(),
                musicFile.exists() ? formatFileSize(musicFile.length()) : "알 수 없음",
                musicFile.exists() ? "예" : "아니오",
                music.getLrcPath() != null ? music.getLrcPath() : "없음",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(selected.getAddedTime())
            );
            
            dialog.setContentText(content);
            dialog.showAndWait();
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
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
                    originalMusicItems.clear();
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
                if (!originalMusicItems.contains(item)) {
                    originalMusicItems.add(item);
                    if (!isSearchMode || item.matches(searchField.getText())) {
                        currentPlaylistItems.add(item);
                    }
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
                for (MusicInfoItem item : originalMusicItems) {
                    if (item.getMusicInfo().equals(music)) {
                        toRemove = item;
                        break;
                    }
                }
                if (toRemove != null) {
                    originalMusicItems.remove(toRemove);
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
            originalMusicItems.clear();
            musicList.forEach(music -> originalMusicItems.add(new MusicInfoItem(music)));
            
            if (isSearchMode) {
                filterMusic(searchField.getText());
            } else {
                currentPlaylistItems.setAll(originalMusicItems);
            }
            
            updateStatusLabel(String.format("%s (%d곡)", 
                selectedPlaylistName != null ? selectedPlaylistName : "플레이리스트", 
                originalMusicItems.size()), false);
            updateButtonStates();
            updateCounts();
        });
    }
    
    public void clearCurrentPlaylistItems() {
        Platform.runLater(() -> {
            originalMusicItems.clear();
            currentPlaylistItems.clear();
            updateCounts();
        });
    }

    // Getter 메서드들
    public Button getCreateButton() { return createButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getAddButton() { return addButton; }
    public Button getAddFolderButton() { return addFolderButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getImportButton() { return importButton; }
    public Button getExportButton() { return exportButton; }
    public Button getClearButton() { return clearButton; }
    public Button getShufflePlaylistButton() { return shufflePlaylistButton; }
    public Button getSortButton() { return sortButton; }
    
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
        return originalMusicItems.stream()
            .map(MusicInfoItem::getMusicInfo)
            .collect(Collectors.toList());
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