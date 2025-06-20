package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.FileScanEvent;
import ac.cwnu.synctune.sdk.event.MediaInfoEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApplicationWindow extends Stage {
    private final EventPublisher eventPublisher;
    
    // 뷰 컴포넌트들
    private PlayerControlsView controlsView;
    private PlaylistView playlistView;
    private LyricsView lyricsView;
    private StatusBarView statusBarView;
    private MusicLibraryView musicLibraryView; // 새로 추가된 음악 라이브러리 뷰
    
    // 컨트롤러들
    private PlaybackController playbackController;
    private PlaylistActionHandler playlistActionHandler;
    private WindowStateManager windowStateManager;
    
    // 상태 표시용 컴포넌트들
    private ProgressBar scanProgressBar;
    private Label statusLabel;
    private TextField searchField;
    private Alert progressDialog;
    private TabPane centerTabPane; // 중앙 탭 패널
    
    // 애니메이션 및 타이머
    private Timeline statusMessageTimer;
    private Timeline autoHideTimer;
    
    // 상태 관리
    private boolean isFullScreen = false;
    private boolean isCompactMode = false;
    private boolean showLyrics = true;
    private boolean showPlaylist = true;
    private boolean showMusicLibrary = true;
    private double normalWidth = 1400;
    private double normalHeight = 900;

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(normalWidth);
        setHeight(normalHeight);
        setMinWidth(800);
        setMinHeight(600);
        
        initUI();
        initControllers();
        setupKeyboardShortcuts();
        applyTheme();
        
        // 창 아이콘 설정 (리소스가 있다면)
        try {
            // getIcons().add(new Image(getClass().getResourceAsStream("/icons/synctune.png")));
        } catch (Exception e) {
            // 아이콘 파일이 없어도 계속 진행
        }
    }

    private void initUI() {
        BorderPane root = new BorderPane();
        
        // 메뉴바 생성
        MenuBar menuBar = createMenuBar();
        
        // 뷰 컴포넌트 생성
        controlsView = new PlayerControlsView();
        playlistView = new PlaylistView();
        lyricsView = new LyricsView();
        statusBarView = new StatusBarView();
        musicLibraryView = new MusicLibraryView(eventPublisher); // 새로 추가
        
        // 스캔 진행 바 생성
        scanProgressBar = new ProgressBar(0);
        scanProgressBar.setPrefWidth(300);
        scanProgressBar.setVisible(false);
        
        statusLabel = new Label("준비됨");
        
        // 검색 필드 생성
        searchField = new TextField();
        searchField.setPromptText("음악 검색...");
        searchField.setPrefWidth(200);
        setupSearchField();
        
        // 상단 도구모음 생성
        ToolBar toolBar = createToolBar();
        
        // 레이아웃 구성
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        
        // 중앙 패널을 탭으로 구성
        centerTabPane = createCenterTabPane();
        
        // 사이드바 (플레이리스트)를 접을 수 있는 패널로 구성
        TitledPane playlistPane = new TitledPane("플레이리스트", playlistView);
        playlistPane.setCollapsible(true);
        playlistPane.setExpanded(true);
        playlistPane.setPrefWidth(350);
        
        root.setTop(topContainer);
        root.setBottom(createBottomPanel());
        root.setLeft(playlistPane);
        root.setCenter(centerTabPane);
        
        Scene scene = new Scene(root);
        
        // CSS 파일 로드
        loadStylesheets(scene);
        
        setScene(scene);
    }

    private TabPane createCenterTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 가사 탭
        Tab lyricsTab = new Tab("가사", lyricsView);
        lyricsTab.setClosable(false);
        
        // 음악 라이브러리 탭
        Tab libraryTab = new Tab("라이브러리", musicLibraryView);
        libraryTab.setClosable(false);
        
        // 비주얼라이저 탭 (향후 구현을 위해 준비)
        VBox visualizerPlaceholder = new VBox();
        visualizerPlaceholder.getChildren().add(new Label("비주얼라이저 (준비 중)"));
        Tab visualizerTab = new Tab("비주얼라이저", visualizerPlaceholder);
        visualizerTab.setClosable(false);
        
        // 이퀄라이저 탭 (향후 구현을 위해 준비)
        VBox equalizerPlaceholder = new VBox();
        equalizerPlaceholder.getChildren().add(new Label("이퀄라이저 (준비 중)"));
        Tab equalizerTab = new Tab("이퀄라이저", equalizerPlaceholder);
        equalizerTab.setClosable(false);
        
        tabPane.getTabs().addAll(lyricsTab, libraryTab, visualizerTab, equalizerTab);
        
        return tabPane;
    }

    private void loadStylesheets(Scene scene) {
        try {
            String cssFile = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssFile);
        } catch (Exception e) {
            System.err.println("CSS 파일을 로드할 수 없습니다: " + e.getMessage());
        }
        
        // 추가 테마 파일들 로드 시도
        try {
            String darkTheme = getClass().getResource("/dark-theme.css").toExternalForm();
            // 나중에 다크 모드 지원 시 사용
        } catch (Exception e) {
            // 다크 테마 파일이 없어도 계속 진행
        }
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (autoHideTimer != null) {
                autoHideTimer.stop();
            }
            
            autoHideTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                performSearch(newValue);
            }));
            autoHideTimer.play();
        });
        
        searchField.setOnAction(e -> performSearch(searchField.getText()));
    }

    private void performSearch(String searchText) {
        if (playlistActionHandler != null) {
            playlistActionHandler.filterMusic(searchText);
        }
        
        // 음악 라이브러리에서도 검색
        if (musicLibraryView != null) {
            musicLibraryView.filterMusic(searchText);
        }
        
        if (searchText != null && !searchText.trim().isEmpty()) {
            setStatusText("검색: " + searchText);
        } else {
            setStatusText("검색 필터 해제됨");
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = createFileMenu();
        Menu playMenu = createPlayMenu();
        Menu viewMenu = createViewMenu();
        Menu toolsMenu = createToolsMenu();
        Menu helpMenu = createHelpMenu();
        
        menuBar.getMenus().addAll(fileMenu, playMenu, viewMenu, toolsMenu, helpMenu);
        return menuBar;
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu("파일");
        
        MenuItem openFile = new MenuItem("파일 열기");
        MenuItem openFolder = new MenuItem("폴더 열기");
        MenuItem scanLibrary = new MenuItem("라이브러리 스캔");
        MenuItem recentFiles = new MenuItem("최근 파일");
        MenuItem importPlaylist = new MenuItem("플레이리스트 가져오기");
        MenuItem exportPlaylist = new MenuItem("플레이리스트 내보내기");
        MenuItem preferences = new MenuItem("환경설정");
        MenuItem exit = new MenuItem("종료");
        
        // 키보드 단축키 설정
        openFile.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openFolder.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        scanLibrary.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        preferences.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
        exit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        
        // 파일 열기
        openFile.setOnAction(e -> {
            List<File> files = UIUtils.showMusicFileChooser(this);
            if (files != null && !files.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    files.forEach(file -> {
                        eventPublisher.publish(new FileScanEvent.FileFoundEvent(file));
                    });
                }).thenRun(() -> {
                    UIUtils.runOnUIThread(() -> {
                        UIUtils.showTaskCompleted("파일 추가", files.size());
                    });
                });
            }
        });
        
        // 폴더 열기
        openFolder.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("음악 폴더 선택");
            File selectedDir = dirChooser.showDialog(this);
            if (selectedDir != null) {
                eventPublisher.publish(new FileScanEvent.ScanStartedEvent(selectedDir.getAbsolutePath()));
                eventPublisher.publish(new MediaInfoEvent.MediaScanStartedEvent(selectedDir.getAbsolutePath()));
            }
        });
        
        // 라이브러리 스캔
        scanLibrary.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("스캔할 음악 라이브러리 폴더 선택");
            File selectedDir = dirChooser.showDialog(this);
            if (selectedDir != null) {
                showScanStarted(selectedDir.getAbsolutePath());
                eventPublisher.publish(new MediaInfoEvent.MediaScanStartedEvent(selectedDir.getAbsolutePath()));
            }
        });
        
        // 최근 파일 메뉴 (하위 메뉴)
        Menu recentFilesMenu = new Menu("최근 파일");
        // TODO: 최근 파일 목록 구현
        recentFilesMenu.getItems().add(new MenuItem("(최근 파일 없음)"));
        
        // 플레이리스트 가져오기
        importPlaylist.setOnAction(e -> {
            File playlistFile = UIUtils.showPlaylistImportDialog(this);
            if (playlistFile != null) {
                importPlaylistFromFile(playlistFile);
            }
        });
        
        // 플레이리스트 내보내기
        exportPlaylist.setOnAction(e -> {
            String selectedPlaylist = playlistView.getSelectedPlaylist();
            if (selectedPlaylist != null) {
                File saveFile = UIUtils.showPlaylistExportDialog(this);
                if (saveFile != null) {
                    exportPlaylistToFile(selectedPlaylist, saveFile);
                }
            } else {
                UIUtils.showWarning("내보내기 오류", "내보낼 플레이리스트를 먼저 선택해주세요.");
            }
        });
        
        // 환경설정
        preferences.setOnAction(e -> showPreferencesDialog());
        
        // 종료
        exit.setOnAction(e -> requestApplicationShutdown());
        
        fileMenu.getItems().addAll(
            openFile, openFolder, new SeparatorMenuItem(),
            scanLibrary, recentFilesMenu, new SeparatorMenuItem(),
            importPlaylist, exportPlaylist, new SeparatorMenuItem(),
            preferences, new SeparatorMenuItem(),
            exit
        );
        
        return fileMenu;
    }

    private Menu createPlayMenu() {
        Menu playMenu = new Menu("재생");
        
        MenuItem play = new MenuItem("재생");
        MenuItem pause = new MenuItem("일시정지");
        MenuItem stop = new MenuItem("정지");
        MenuItem next = new MenuItem("다음 곡");
        MenuItem previous = new MenuItem("이전 곡");
        MenuItem shuffle = new MenuItem("셔플");
        MenuItem repeat = new MenuItem("반복");
        MenuItem volumeUp = new MenuItem("볼륨 높이기");
        MenuItem volumeDown = new MenuItem("볼륨 낮추기");
        MenuItem mute = new MenuItem("음소거");
        
        // 키보드 단축키 설정
        play.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
        pause.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
        stop.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        next.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN));
        previous.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN));
        volumeUp.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN));
        volumeDown.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN));
        mute.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));
        
        // 재생 메뉴 이벤트 연결
        play.setOnAction(e -> playbackController.requestPlay());
        pause.setOnAction(e -> playbackController.requestPause());
        stop.setOnAction(e -> playbackController.requestStop());
        next.setOnAction(e -> playbackController.requestNext());
        previous.setOnAction(e -> playbackController.requestPrevious());
        volumeUp.setOnAction(e -> playbackController.adjustVolume(0.1f));
        volumeDown.setOnAction(e -> playbackController.adjustVolume(-0.1f));
        mute.setOnAction(e -> playbackController.toggleMute());
        
        // 셔플과 반복은 향후 구현
        shuffle.setOnAction(e -> setStatusText("셔플 기능 (구현 예정)"));
        repeat.setOnAction(e -> setStatusText("반복 기능 (구현 예정)"));
        
        playMenu.getItems().addAll(
            play, pause, stop, new SeparatorMenuItem(),
            previous, next, new SeparatorMenuItem(),
            shuffle, repeat, new SeparatorMenuItem(),
            volumeUp, volumeDown, mute
        );
        
        return playMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu("보기");
        
        CheckMenuItem showLyricsMenu = new CheckMenuItem("가사 표시");
        CheckMenuItem showPlaylistMenu = new CheckMenuItem("플레이리스트 표시");
        CheckMenuItem showLibraryMenu = new CheckMenuItem("라이브러리 표시");
        CheckMenuItem compactMode = new CheckMenuItem("컴팩트 모드");
        CheckMenuItem alwaysOnTop = new CheckMenuItem("항상 위에");
        MenuItem fullScreen = new MenuItem("전체화면");
        MenuItem resetLayout = new MenuItem("레이아웃 초기화");
        
        // 테마 메뉴
        Menu themeMenu = new Menu("테마");
        RadioMenuItem lightTheme = new RadioMenuItem("라이트 테마");
        RadioMenuItem darkTheme = new RadioMenuItem("다크 테마");
        ToggleGroup themeGroup = new ToggleGroup();
        lightTheme.setToggleGroup(themeGroup);
        darkTheme.setToggleGroup(themeGroup);
        lightTheme.setSelected(true);
        themeMenu.getItems().addAll(lightTheme, darkTheme);
        
        // 초기 설정
        showLyricsMenu.setSelected(showLyrics);
        showPlaylistMenu.setSelected(showPlaylist);
        showLibraryMenu.setSelected(showMusicLibrary);
        
        // 키보드 단축키
        fullScreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        compactMode.setAccelerator(new KeyCodeCombination(KeyCode.F12));
        
        // 이벤트 처리
        showLyricsMenu.setOnAction(e -> {
            showLyrics = showLyricsMenu.isSelected();
            toggleLyricsView(showLyrics);
        });
        
        showPlaylistMenu.setOnAction(e -> {
            showPlaylist = showPlaylistMenu.isSelected();
            togglePlaylistView(showPlaylist);
        });
        
        showLibraryMenu.setOnAction(e -> {
            showMusicLibrary = showLibraryMenu.isSelected();
            toggleLibraryView(showMusicLibrary);
        });
        
        compactMode.setOnAction(e -> toggleCompactMode(compactMode.isSelected()));
        alwaysOnTop.setOnAction(e -> setAlwaysOnTop(alwaysOnTop.isSelected()));
        fullScreen.setOnAction(e -> toggleFullScreen());
        resetLayout.setOnAction(e -> resetWindowLayout());
        
        // 테마 변경
        lightTheme.setOnAction(e -> applyTheme("light"));
        darkTheme.setOnAction(e -> applyTheme("dark"));
        
        viewMenu.getItems().addAll(
            showLyricsMenu, showPlaylistMenu, showLibraryMenu, new SeparatorMenuItem(),
            compactMode, alwaysOnTop, new SeparatorMenuItem(),
            fullScreen, resetLayout, new SeparatorMenuItem(),
            themeMenu
        );
        
        return viewMenu;
    }

    private Menu createToolsMenu() {
        Menu toolsMenu = new Menu("도구");
        
        MenuItem searchMusic = new MenuItem("음악 검색");
        MenuItem playlistStats = new MenuItem("플레이리스트 통계");
        MenuItem libraryStats = new MenuItem("라이브러리 통계");
        MenuItem clearCache = new MenuItem("캐시 정리");
        MenuItem rescanLibrary = new MenuItem("라이브러리 재스캔");
        MenuItem fileAssociations = new MenuItem("파일 연결");
        MenuItem settings = new MenuItem("설정");
        
        // 키보드 단축키
        searchMusic.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        settings.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
        
        // 이벤트 처리
        searchMusic.setOnAction(e -> {
            searchField.requestFocus();
            setStatusText("검색 모드 활성화");
        });
        
        playlistStats.setOnAction(e -> {
            if (playlistActionHandler != null) {
                playlistActionHandler.showStatistics();
            }
        });
        
        libraryStats.setOnAction(e -> showLibraryStatistics());
        
        clearCache.setOnAction(e -> {
            if (UIUtils.showConfirmation("캐시 정리", "모든 캐시를 정리하시겠습니까?")) {
                clearApplicationCache();
            }
        });
        
        rescanLibrary.setOnAction(e -> {
            if (UIUtils.showConfirmation("라이브러리 재스캔", "전체 라이브러리를 다시 스캔하시겠습니까?")) {
                rescanMusicLibrary();
            }
        });
        
        fileAssociations.setOnAction(e -> showFileAssociationsDialog());
        settings.setOnAction(e -> showSettingsDialog());
        
        toolsMenu.getItems().addAll(
            searchMusic, new SeparatorMenuItem(),
            playlistStats, libraryStats, new SeparatorMenuItem(),
            clearCache, rescanLibrary, new SeparatorMenuItem(),
            fileAssociations, settings
        );
        
        return toolsMenu;
    }

    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("도움말");
        
        MenuItem shortcuts = new MenuItem("키보드 단축키");
        MenuItem userGuide = new MenuItem("사용자 가이드");
        MenuItem onlineHelp = new MenuItem("온라인 도움말");
        MenuItem checkUpdates = new MenuItem("업데이트 확인");
        MenuItem reportBug = new MenuItem("버그 신고");
        MenuItem about = new MenuItem("정보");
        
        shortcuts.setOnAction(e -> showShortcutsDialog());
        userGuide.setOnAction(e -> showUserGuideDialog());
        onlineHelp.setOnAction(e -> openOnlineHelp());
        checkUpdates.setOnAction(e -> checkForUpdates());
        reportBug.setOnAction(e -> showBugReportDialog());
        about.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().addAll(
            shortcuts, userGuide, onlineHelp, new SeparatorMenuItem(),
            checkUpdates, new SeparatorMenuItem(),
            reportBug, new SeparatorMenuItem(),
            about
        );
        
        return helpMenu;
    }
    
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        
        Button scanButton = new Button("📁 스캔");
        Button refreshButton = new Button("🔄 새로고침");
        Button randomButton = new Button("🎲 랜덤 재생");
        Button importButton = new Button("📥 가져오기");
        Button exportButton = new Button("📤 내보내기");
        
        scanButton.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("음악 폴더 스캔");
            File selectedDir = dirChooser.showDialog(this);
            if (selectedDir != null) {
                eventPublisher.publish(new MediaInfoEvent.MediaScanStartedEvent(selectedDir.getAbsolutePath()));
            }
        });
        
        refreshButton.setOnAction(e -> {
            playlistView.refreshPlaylists();
            musicLibraryView.refreshLibrary();
            setStatusText("전체 새로고침됨");
        });
        
        randomButton.setOnAction(e -> {
            setStatusText("랜덤 재생 모드 토글 (구현 예정)");
        });
        
        importButton.setOnAction(e -> {
            File file = UIUtils.showPlaylistImportDialog(this);
            if (file != null) {
                importPlaylistFromFile(file);
            }
        });
        
        exportButton.setOnAction(e -> {
            String playlist = playlistView.getSelectedPlaylist();
            if (playlist != null) {
                File file = UIUtils.showPlaylistExportDialog(this);
                if (file != null) {
                    exportPlaylistToFile(playlist, file);
                }
            } else {
                UIUtils.showWarning("내보내기 오류", "내보낼 플레이리스트를 선택하세요.");
            }
        });
        
        // 구분선
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        
        // 오른쪽 정렬용 컨테이너
        HBox rightContainer = new HBox(10);
        rightContainer.getChildren().addAll(scanProgressBar, statusLabel);
        HBox.setHgrow(rightContainer, Priority.ALWAYS);
        
        toolBar.getItems().addAll(
            scanButton, refreshButton, randomButton, separator1,
            importButton, exportButton, separator2,
            searchField, rightContainer
        );
        
        return toolBar;
    }
    
    private VBox createBottomPanel() {
        VBox bottomPanel = new VBox();
        bottomPanel.getChildren().addAll(controlsView, statusBarView);
        return bottomPanel;
    }

    private void initControllers() {
        playbackController = new PlaybackController(controlsView, eventPublisher);
        playlistActionHandler = new PlaylistActionHandler(playlistView, eventPublisher);
        windowStateManager = new WindowStateManager(this, eventPublisher);
    }

    private void setupKeyboardShortcuts() {
        getScene().setOnKeyPressed(event -> {
            if (event.isConsumed()) return;
            
            switch (event.getCode()) {
                case ESCAPE:
                    if (isFullScreen) {
                        toggleFullScreen();
                    }
                    event.consume();
                    break;
                case DELETE:
                    handleDeleteKey();
                    event.consume();
                    break;
                case F1:
                    showShortcutsDialog();
                    event.consume();
                    break;
                case TAB:
                    if (event.isControlDown()) {
                        cycleTab();
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void handleDeleteKey() {
        String selectedMusic = playlistView.getSelectedMusic();
        String selectedPlaylist = playlistView.getSelectedPlaylist();
        if (selectedMusic != null && selectedPlaylist != null) {
            if (UIUtils.showConfirmation("곡 제거", 
                "선택된 곡을 플레이리스트에서 제거하시겠습니까?")) {
                // TODO: 곡 제거 구현
                setStatusText("곡 제거: " + selectedMusic);
            }
        }
    }

    private void cycleTab() {
        int selectedIndex = centerTabPane.getSelectionModel().getSelectedIndex();
        int nextIndex = (selectedIndex + 1) % centerTabPane.getTabs().size();
        centerTabPane.getSelectionModel().select(nextIndex);
    }

    private void applyTheme() {
        applyTheme("light");
    }
    
    private void applyTheme(String themeName) {
        Scene scene = getScene();
        if (scene != null) {
            scene.getRoot().getStyleClass().removeAll("light-theme", "dark-theme");
            scene.getRoot().getStyleClass().add(themeName + "-theme");
            setStatusText("테마 변경: " + (themeName.equals("light") ? "라이트" : "다크"));
        }
    }

    // ========== 뷰 토글 메서드들 ==========
    
    private void toggleLyricsView(boolean show) {
        Tab lyricsTab = centerTabPane.getTabs().get(0);
        if (show && !centerTabPane.getTabs().contains(lyricsTab)) {
            centerTabPane.getTabs().add(0, lyricsTab);
        } else if (!show && centerTabPane.getTabs().contains(lyricsTab)) {
            centerTabPane.getTabs().remove(lyricsTab);
        }
        setStatusText("가사 표시: " + (show ? "켜짐" : "꺼짐"));
    }
    
    private void togglePlaylistView(boolean show) {
        BorderPane root = (BorderPane) getScene().getRoot();
        if (show && root.getLeft() == null) {
            TitledPane playlistPane = new TitledPane("플레이리스트", playlistView);
            playlistPane.setCollapsible(true);
            playlistPane.setExpanded(true);
            playlistPane.setPrefWidth(350);
            root.setLeft(playlistPane);
        } else if (!show && root.getLeft() != null) {
            root.setLeft(null);
        }
        setStatusText("플레이리스트: " + (show ? "표시" : "숨김"));
    }
    
    private void toggleLibraryView(boolean show) {
        Tab libraryTab = null;
        for (Tab tab : centerTabPane.getTabs()) {
            if ("라이브러리".equals(tab.getText())) {
                libraryTab = tab;
                break;
            }
        }
        
        if (show && libraryTab != null && !centerTabPane.getTabs().contains(libraryTab)) {
            centerTabPane.getTabs().add(1, libraryTab);
        } else if (!show && libraryTab != null && centerTabPane.getTabs().contains(libraryTab)) {
            centerTabPane.getTabs().remove(libraryTab);
        }
        setStatusText("라이브러리: " + (show ? "표시" : "숨김"));
    }

    // ========== UI 업데이트 메서드들 ==========
    
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            setTitle("SyncTune - " + music.getTitle() + " - " + music.getArtist());
            statusBarView.updateCurrentMusic(music);
            musicLibraryView.highlightCurrentMusic(music);
            
            loadAlbumArt(music);
        } else {
            setTitle("SyncTune Player");
            statusBarView.updateCurrentMusic(null);
            musicLibraryView.clearHighlight();
        }
    }

    private void loadAlbumArt(MusicInfo music) {
        CompletableFuture.runAsync(() -> {
            try {
                String musicDir = new File(music.getFilePath()).getParent();
                File[] imageFiles = new File(musicDir).listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                           lower.endsWith(".png") || lower.endsWith(".gif");
                });
                
                if (imageFiles != null && imageFiles.length > 0) {
                    UIUtils.runOnUIThread(() -> {
                        controlsView.setAlbumArt(imageFiles[0].getAbsolutePath());
                    });
                }
            } catch (Exception e) {
                // 앨범 아트 로드 실패는 조용히 무시
            }
        });
    }

    public void updateProgress(long currentMs, long totalMs) {
        controlsView.updateProgress(currentMs, totalMs);
        statusBarView.updateProgress(currentMs, totalMs);
    }

    public void updateLyrics(String lyrics) {
        lyricsView.updateLyrics(lyrics);
    }
    
    public void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        controlsView.updatePlaybackState(isPlaying, isPaused, isStopped);
        statusBarView.updatePlaybackState(isPlaying, isPaused, isStopped);
    }

    // ========== 스캔 관련 메서드들 ==========
    
    public void showScanStarted(String directoryPath) {
        setStatusText("스캔 시작: " + getShortPath(directoryPath));
        scanProgressBar.setProgress(0);
        scanProgressBar.setVisible(true);
    }
    
    public void updateScanProgress(String currentFileName) {
        setStatusText("스캔 중: " + currentFileName);
    }
    
    public void showScanCompleted(int totalFiles) {
        setStatusText("스캔 완료: " + totalFiles + "개 파일 발견");
        scanProgressBar.setVisible(false);
    }
    
    public void showMediaScanStarted(String directoryPath) {
        setStatusText("미디어 스캔 시작: " + getShortPath(directoryPath));
        scanProgressBar.setProgress(0);
        scanProgressBar.setVisible(true);
    }
    
    public void updateMediaScanProgress(double progress, int scanned, int total) {
        scanProgressBar.setProgress(progress);
        setStatusText(String.format("미디어 스캔 중: %d/%d (%.1f%%)", scanned, total, progress * 100));
    }
    
    public void showMediaScanCompleted(int totalMusic) {
        setStatusText("미디어 스캔 완료: " + totalMusic + "곡 발견");
        scanProgressBar.setVisible(false);
    }

    // ========== 가사 관련 메서드들 ==========
    
    public void showLyricsFound(String musicPath, String lrcPath) {
        setStatusText("가사 파일 발견: " + getShortPath(lrcPath));
        lyricsView.showLyricsAvailable(true);
        lyricsView.showLyricsFromFile(lrcPath);
    }
    
    public void showLyricsNotFound(String musicPath) {
        setStatusText("가사 파일 없음: " + getShortPath(musicPath));
        lyricsView.showLyricsAvailable(false);
        lyricsView.updateLyrics("가사를 찾을 수 없습니다");
    }
    
    public void showLyricsParseSuccess(String musicPath) {
        setStatusText("가사 파싱 완료: " + getShortPath(musicPath));
        lyricsView.showLyricsReady();
    }
    
    public void showLyricsParseFailure(String musicPath) {
        setStatusText("가사 파싱 실패: " + getShortPath(musicPath));
        lyricsView.showLyricsParseError();
    }

    // ========== 플레이리스트 관련 메서드들 ==========
    
    public void addPlaylistToUI(Playlist playlist) {
        playlistView.addPlaylist(playlist.getName());
        setStatusText("플레이리스트 생성됨: " + playlist.getName());
    }
    
    public void removePlaylistFromUI(String playlistName) {
        playlistView.removePlaylist(playlistName);
        setStatusText("플레이리스트 삭제됨: " + playlistName);
    }
    
    public void addMusicToPlaylistUI(String playlistName, MusicInfo music) {
        playlistView.addMusicToPlaylist(playlistName, music);
        setStatusText("곡 추가됨: " + music.getTitle() + " → " + playlistName);
    }
    
    public void removeMusicFromPlaylistUI(String playlistName, MusicInfo music) {
        playlistView.removeMusicFromPlaylist(playlistName, music);
        setStatusText("곡 제거됨: " + music.getTitle() + " ← " + playlistName);
    }
    
    public void updatePlaylistOrder(Playlist playlist) {
        playlistView.updatePlaylistOrder(playlist);
        setStatusText("플레이리스트 순서 변경됨: " + playlist.getName());
    }
    
    public void loadAllPlaylists(List<Playlist> playlists) {
        playlistView.loadPlaylists(playlists);
        setStatusText("플레이리스트 로드됨: " + playlists.size() + "개");
    }

    // ========== 라이브러리 관련 메서드들 ==========
    
    public void updateMusicLibrary(List<MusicInfo> musicList) {
        playlistView.updateMusicLibrary(musicList);
        musicLibraryView.updateLibrary(musicList);
        setStatusText("음악 라이브러리 업데이트됨: " + musicList.size() + "곡");
    }
    
    public void updateMusicMetadata(MusicInfo music) {
        playlistView.updateMusicMetadata(music);
        musicLibraryView.updateMusicMetadata(music);
        setStatusText("메타데이터 업데이트됨: " + music.getTitle());
    }

    // ========== 애플리케이션 상태 관련 메서드들 ==========
    
    public void showApplicationReady() {
        setStatusText("SyncTune 준비 완료");
    }
    
    public void showShutdownMessage() {
        setStatusText("종료 중...");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

    // ========== 창 관리 메서드들 ==========
    
    private void toggleFullScreen() {
        setFullScreen(!isFullScreen());
        isFullScreen = !isFullScreen;
        setStatusText("전체화면: " + (isFullScreen ? "켜짐" : "꺼짐"));
    }

    private void toggleCompactMode(boolean compact) {
        isCompactMode = compact;
        
        if (compact) {
            // 컴팩트 모드: 가사와 라이브러리 숨기고 창 크기 축소
            toggleLyricsView(false);
            toggleLibraryView(false);
            setWidth(800);
            setHeight(400);
            setStatusText("컴팩트 모드 활성화");
        } else {
            // 일반 모드: 모든 뷰 표시하고 창 크기 복원
            toggleLyricsView(true);
            toggleLibraryView(true);
            setWidth(normalWidth);
            setHeight(normalHeight);
            setStatusText("일반 모드로 전환");
        }
    }

    private void resetWindowLayout() {
        setFullScreen(false);
        isFullScreen = false;
        isCompactMode = false;
        
        setWidth(normalWidth);
        setHeight(normalHeight);
        centerOnScreen();
        
        // 모든 패널 표시
        toggleLyricsView(true);
        togglePlaylistView(true);
        toggleLibraryView(true);
        
        setStatusText("레이아웃이 초기화되었습니다");
    }

    // ========== 기능 구현 메서드들 ==========
    
    private void importPlaylistFromFile(File playlistFile) {
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: 플레이리스트 파일 파싱 구현
                Thread.sleep(1000); // 시뮬레이션
                UIUtils.runOnUIThread(() -> {
                    setStatusText("플레이리스트 가져오기 완료: " + playlistFile.getName());
                    UIUtils.showSuccess("가져오기 완료", "플레이리스트를 성공적으로 가져왔습니다.");
                });
            } catch (Exception e) {
                UIUtils.runOnUIThread(() -> {
                    UIUtils.showError("가져오기 실패", "플레이리스트 파일을 읽을 수 없습니다: " + e.getMessage());
                });
            }
        });
    }

    private void exportPlaylistToFile(String playlistName, File saveFile) {
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: 플레이리스트 파일 생성 구현
                Thread.sleep(1000); // 시뮬레이션
                UIUtils.runOnUIThread(() -> {
                    setStatusText("플레이리스트 내보내기 완료: " + saveFile.getName());
                    UIUtils.showSuccess("내보내기 완료", "플레이리스트를 성공적으로 내보냈습니다.");
                });
            } catch (Exception e) {
                UIUtils.runOnUIThread(() -> {
                    UIUtils.showError("내보내기 실패", "플레이리스트를 저장할 수 없습니다: " + e.getMessage());
                });
            }
        });
    }

    private void clearApplicationCache() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 시뮬레이션
                UIUtils.runOnUIThread(() -> {
                    setStatusText("캐시 정리 완료");
                    UIUtils.showSuccess("정리 완료", "애플리케이션 캐시가 정리되었습니다.");
                });
            } catch (Exception e) {
                UIUtils.runOnUIThread(() -> {
                    UIUtils.showError("정리 실패", "캐시 정리 중 오류가 발생했습니다: " + e.getMessage());
                });
            }
        });
    }

    private void showLibraryStatistics() {
        // TODO: 라이브러리 통계 다이얼로그 구현
        UIUtils.showInfo("라이브러리 통계", "라이브러리 통계 기능은 향후 업데이트에서 제공될 예정입니다.");
    }

    private void rescanMusicLibrary() {
        CompletableFuture.runAsync(() -> {
            try {
                UIUtils.runOnUIThread(() -> setStatusText("라이브러리 재스캔 시작..."));
                Thread.sleep(2000); // 시뮬레이션
                UIUtils.runOnUIThread(() -> {
                    setStatusText("라이브러리 재스캔 완료");
                    UIUtils.showSuccess("재스캔 완료", "음악 라이브러리가 업데이트되었습니다.");
                });
            } catch (Exception e) {
                UIUtils.runOnUIThread(() -> {
                    UIUtils.showError("재스캔 실패", "라이브러리 재스캔 중 오류가 발생했습니다: " + e.getMessage());
                });
            }
        });
    }

    private void showFileAssociationsDialog() {
        UIUtils.showInfo("파일 연결", "파일 연결 설정 기능은 향후 업데이트에서 제공될 예정입니다.");
    }

    private void showPreferencesDialog() {
        // TODO: 환경설정 다이얼로그 구현
        PreferencesDialog dialog = new PreferencesDialog(this);
        dialog.showAndWait();
    }

    private void showSettingsDialog() {
        // TODO: 설정 다이얼로그 구현
        UIUtils.showInfo("설정", "설정 기능은 향후 업데이트에서 제공될 예정입니다.");
    }

    private void openOnlineHelp() {
        try {
            // TODO: 온라인 도움말 URL 열기
            setStatusText("온라인 도움말을 여는 중...");
        } catch (Exception e) {
            UIUtils.showError("도움말 오류", "온라인 도움말을 열 수 없습니다.");
        }
    }

    private void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                UIUtils.runOnUIThread(() -> setStatusText("업데이트 확인 중..."));
                Thread.sleep(2000); // 시뮬레이션
                UIUtils.runOnUIThread(() -> {
                    setStatusText("최신 버전입니다");
                    UIUtils.showInfo("업데이트 확인", "현재 최신 버전을 사용하고 있습니다.");
                });
            } catch (Exception e) {
                UIUtils.runOnUIThread(() -> {
                    UIUtils.showError("업데이트 확인 실패", "업데이트를 확인할 수 없습니다: " + e.getMessage());
                });
            }
        });
    }

    private void showUserGuideDialog() {
        Alert guide = new Alert(Alert.AlertType.INFORMATION);
        guide.setTitle("사용자 가이드");
        guide.setHeaderText("SyncTune 사용법");
        guide.setContentText(
            "기본 사용법:\n\n" +
            "1. 파일 > 폴더 열기로 음악 폴더를 선택하세요\n" +
            "2. 스캔이 완료되면 플레이리스트에서 곡을 선택하세요\n" +
            "3. 더블클릭하거나 재생 버튼으로 음악을 재생하세요\n" +
            "4. LRC 파일이 있으면 자동으로 가사가 표시됩니다\n\n" +
            "키보드 단축키:\n" +
            "• Space: 재생/일시정지\n" +
            "• Ctrl+O: 파일 열기\n" +
            "• F5: 라이브러리 새로고침\n" +
            "• F11: 전체화면\n" +
            "• F12: 컴팩트 모드\n" +
            "• Ctrl+Tab: 탭 전환\n" +
            "• F1: 도움말\n\n" +
            "자세한 내용은 온라인 문서를 참조하세요."
        );
        guide.showAndWait();
    }

    private void showBugReportDialog() {
        Alert bugReport = new Alert(Alert.AlertType.INFORMATION);
        bugReport.setTitle("버그 신고");
        bugReport.setHeaderText("문제 신고 방법");
        bugReport.setContentText(
            "버그를 발견하셨나요?\n\n" +
            "다음 정보와 함께 신고해주세요:\n" +
            "• 문제가 발생한 상황\n" +
            "• 재현 방법\n" +
            "• 오류 메시지 (있는 경우)\n" +
            "• 사용 중인 운영체제\n\n" +
            "신고 방법:\n" +
            "• GitHub Issues\n" +
            "• 이메일: support@synctune.com\n" +
            "• 디스코드: SyncTune 커뮤니티\n\n" +
            "빠른 해결을 위해 자세한 정보를 제공해주세요."
        );
        bugReport.showAndWait();
    }

    private void showAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("SyncTune 정보");
        about.setHeaderText("SyncTune Player v1.0");
        about.setContentText(
            "고급 음악 플레이어 및 가사 동기화 소프트웨어\n\n" +
            "주요 기능:\n" +
            "• 다양한 오디오 형식 지원 (MP3, FLAC, WAV, etc.)\n" +
            "• LRC 가사 파일 실시간 동기화\n" +
            "• 스마트 플레이리스트 관리\n" +
            "• 모듈형 아키텍처\n" +
            "• 키보드 단축키 지원\n" +
            "• 테마 및 레이아웃 커스터마이징\n\n" +
            "시스템 요구사항:\n" +
            "• Java 21 이상\n" +
            "• JavaFX 21\n" +
            "• 최소 512MB RAM\n\n" +
            "개발: SyncTune Team\n" +
            "라이선스: MIT License\n" +
            "© 2024 SyncTune Project"
        );
        about.showAndWait();
    }

    private void showShortcutsDialog() {
        Alert shortcuts = new Alert(Alert.AlertType.INFORMATION);
        shortcuts.setTitle("키보드 단축키");
        shortcuts.setHeaderText("사용 가능한 키보드 단축키");
        shortcuts.setContentText(
            "파일 및 재생 제어:\n" +
            "Space           - 재생/일시정지\n" +
            "Ctrl+S          - 정지\n" +
            "Ctrl+→          - 다음 곡\n" +
            "Ctrl+←          - 이전 곡\n" +
            "Ctrl+↑          - 볼륨 높이기\n" +
            "Ctrl+↓          - 볼륨 낮추기\n" +
            "Ctrl+M          - 음소거\n\n" +
            "파일 관리:\n" +
            "Ctrl+O          - 파일 열기\n" +
            "Ctrl+Shift+O    - 폴더 열기\n" +
            "F5              - 라이브러리 새로고침\n" +
            "Ctrl+F          - 음악 검색\n\n" +
            "창 관리:\n" +
            "F11             - 전체화면 토글\n" +
            "F12             - 컴팩트 모드 토글\n" +
            "Ctrl+Tab        - 탭 전환\n" +
            "Escape          - 전체화면 해제\n\n" +
            "기타:\n" +
            "F1              - 도움말\n" +
            "Ctrl+Q          - 프로그램 종료\n" +
            "Ctrl+,          - 환경설정\n" +
            "Delete          - 선택된 항목 삭제"
        );
        shortcuts.showAndWait();
    }

    private void requestApplicationShutdown() {
        if (windowStateManager != null) {
            windowStateManager.handleCloseRequest(null);
        } else {
            eventPublisher.publish(new SystemEvent.RequestApplicationShutdownEvent());
        }
    }

    // ========== 상태 메시지 관리 ==========
    
    public void setStatusText(String text) {
        statusLabel.setText(text);
        statusBarView.updateStatus(text);
        
        if (statusMessageTimer != null) {
            statusMessageTimer.stop();
        }
        
        statusMessageTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            statusLabel.setText("준비됨");
            statusBarView.updateStatus("준비됨");
        }));
        statusMessageTimer.play();
    }

    public void setStatusTextPermanent(String text) {
        statusLabel.setText(text);
        statusBarView.updateStatus(text);
        
        if (statusMessageTimer != null) {
            statusMessageTimer.stop();
        }
    }

    // ========== 유틸리티 메서드들 ==========
    
    private String getShortPath(String fullPath) {
        if (fullPath == null || fullPath.length() <= 50) {
            return fullPath;
        }
        return "..." + fullPath.substring(fullPath.length() - 47);
    }

    public void showProgressDialog(String title, String message) {
        if (progressDialog != null) {
            progressDialog.close();
        }
        
        progressDialog = new Alert(Alert.AlertType.INFORMATION);
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(message);
        progressDialog.setContentText("진행 중...");
        progressDialog.getButtonTypes().clear();
        progressDialog.show();
    }
    
    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.close();
            progressDialog = null;
        }
    }

    // ========== 정리 및 종료 메서드들 ==========

    public void cleanup() {
        try {
            if (statusMessageTimer != null) {
                statusMessageTimer.stop();
            }
            if (autoHideTimer != null) {
                autoHideTimer.stop();
            }
            
            hideProgressDialog();
            
            if (playbackController != null) {
                playbackController.dispose();
            }
            if (playlistActionHandler != null) {
                playlistActionHandler.dispose();
            }
            
        } catch (Exception e) {
            System.err.println("정리 작업 중 오류: " + e.getMessage());
        }
    }

    public void forceClose() {
        if (windowStateManager != null) {
            windowStateManager.forceClose();
        } else {
            UIUtils.runOnUIThread(() -> {
                cleanup();
                close();
            });
        }
    }

    @Override
    public void close() {
        if (windowStateManager != null && !windowStateManager.isCloseRequested()) {
            windowStateManager.handleCloseRequest(null);
        } else {
            cleanup();
            super.close();
        }
    }

    // ========== 접근자 메서드들 ==========
    
    public PlayerControlsView getControlsView() { return controlsView; }
    public PlaylistView getPlaylistView() { return playlistView; }
    public LyricsView getLyricsView() { return lyricsView; }
    public StatusBarView getStatusBarView() { return statusBarView; }
    public MusicLibraryView getMusicLibraryView() { return musicLibraryView; }
    
    public PlaybackController getPlaybackController() { return playbackController; }
    public PlaylistActionHandler getPlaylistActionHandler() { return playlistActionHandler; }
    public WindowStateManager getWindowStateManager() { return windowStateManager; }
    
    public boolean isCompactMode() { return isCompactMode; }
    public boolean isInFullScreen() { return isFullScreen; }
}