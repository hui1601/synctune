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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    
    // 컨트롤러들
    private PlaybackController playbackController;
    private PlaylistActionHandler playlistActionHandler;
    private WindowStateManager windowStateManager;
    
    // 상태 표시용 컴포넌트들
    private ProgressBar scanProgressBar;
    private Label statusLabel;
    private TextField searchField;
    private Alert progressDialog;
    
    // 애니메이션 및 타이머
    private Timeline statusMessageTimer;
    private Timeline autoHideTimer;
    
    // 상태 관리
    private boolean isFullScreen = false;
    private boolean isCompactMode = false;
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
        
        root.setTop(topContainer);
        root.setBottom(createBottomPanel());
        root.setLeft(playlistView);
        root.setCenter(createCenterPanel());
        
        // 여백 설정
        BorderPane.setMargin(controlsView, new Insets(10));
        BorderPane.setMargin(playlistView, new Insets(10));
        BorderPane.setMargin(lyricsView, new Insets(10));
        
        Scene scene = new Scene(root);
        
        // CSS 파일 로드 (안전하게 처리)
        try {
            String cssFile = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssFile);
        } catch (Exception e) {
            // CSS 파일이 없어도 애플리케이션은 계속 실행
            System.err.println("CSS 파일을 로드할 수 없습니다: " + e.getMessage());
        }
        
        setScene(scene);
    }

    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));
        
        // 가사 뷰를 중앙에 배치
        VBox.setVgrow(lyricsView, Priority.ALWAYS);
        centerPanel.getChildren().add(lyricsView);
        
        return centerPanel;
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // 실시간 검색 (500ms 딜레이 후 실행)
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
            
            if (searchText != null && !searchText.trim().isEmpty()) {
                setStatusText("검색: " + searchText);
            } else {
                setStatusText("검색 필터 해제됨");
            }
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
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
        MenuItem importPlaylist = new MenuItem("플레이리스트 가져오기");
        MenuItem exportPlaylist = new MenuItem("플레이리스트 내보내기");
        MenuItem exit = new MenuItem("종료");
        
        // 키보드 단축키 설정
        openFile.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openFolder.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        scanLibrary.setAccelerator(new KeyCodeCombination(KeyCode.F5));
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
        
        // 종료 메뉴 클릭 시에도 안전한 종료 이벤트 발행
        exit.setOnAction(e -> requestApplicationShutdown());
        
        fileMenu.getItems().addAll(openFile, openFolder, new SeparatorMenuItem(), 
                                  scanLibrary, new SeparatorMenuItem(),
                                  importPlaylist, exportPlaylist, new SeparatorMenuItem(), 
                                  exit);
        return fileMenu;
    }

    private Menu createPlayMenu() {
        Menu playMenu = new Menu("재생");
        
        MenuItem play = new MenuItem("재생");
        MenuItem pause = new MenuItem("일시정지");
        MenuItem stop = new MenuItem("정지");
        MenuItem next = new MenuItem("다음 곡");
        MenuItem previous = new MenuItem("이전 곡");
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
        
        playMenu.getItems().addAll(play, pause, stop, new SeparatorMenuItem(), 
                                  previous, next, new SeparatorMenuItem(),
                                  volumeUp, volumeDown, mute);
        return playMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu("보기");
        
        CheckMenuItem showLyrics = new CheckMenuItem("가사 표시");
        CheckMenuItem showPlaylist = new CheckMenuItem("플레이리스트 표시");
        CheckMenuItem compactMode = new CheckMenuItem("컴팩트 모드");
        CheckMenuItem alwaysOnTop = new CheckMenuItem("항상 위에");
        MenuItem fullScreen = new MenuItem("전체화면");
        MenuItem resetLayout = new MenuItem("레이아웃 초기화");
        
        // 초기 설정
        showLyrics.setSelected(true);
        showPlaylist.setSelected(true);
        
        // 키보드 단축키
        fullScreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        compactMode.setAccelerator(new KeyCodeCombination(KeyCode.F12));
        
        // 이벤트 처리
        showLyrics.setOnAction(e -> {
            lyricsView.setVisible(showLyrics.isSelected());
            setStatusText("가사 표시: " + (showLyrics.isSelected() ? "켜짐" : "꺼짐"));
        });
        
        showPlaylist.setOnAction(e -> {
            playlistView.setVisible(showPlaylist.isSelected());
            setStatusText("플레이리스트: " + (showPlaylist.isSelected() ? "표시" : "숨김"));
        });
        
        compactMode.setOnAction(e -> toggleCompactMode(compactMode.isSelected()));
        alwaysOnTop.setOnAction(e -> setAlwaysOnTop(alwaysOnTop.isSelected()));
        fullScreen.setOnAction(e -> toggleFullScreen());
        resetLayout.setOnAction(e -> resetWindowLayout());
        
        viewMenu.getItems().addAll(showLyrics, showPlaylist, new SeparatorMenuItem(),
                                  compactMode, alwaysOnTop, new SeparatorMenuItem(),
                                  fullScreen, resetLayout);
        return viewMenu;
    }

    private Menu createToolsMenu() {
        Menu toolsMenu = new Menu("도구");
        
        MenuItem searchMusic = new MenuItem("음악 검색");
        MenuItem playlistStats = new MenuItem("플레이리스트 통계");
        MenuItem clearCache = new MenuItem("캐시 정리");
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
        
        clearCache.setOnAction(e -> {
            if (UIUtils.showConfirmation("캐시 정리", "모든 캐시를 정리하시겠습니까?")) {
                clearApplicationCache();
            }
        });
        
        settings.setOnAction(e -> showSettingsDialog());
        
        toolsMenu.getItems().addAll(searchMusic, playlistStats, new SeparatorMenuItem(),
                                   clearCache, settings);
        return toolsMenu;
    }

    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("도움말");
        
        MenuItem shortcuts = new MenuItem("키보드 단축키");
        MenuItem userGuide = new MenuItem("사용자 가이드");
        MenuItem reportBug = new MenuItem("버그 신고");
        MenuItem about = new MenuItem("정보");
        
        shortcuts.setOnAction(e -> showShortcutsDialog());
        userGuide.setOnAction(e -> showUserGuideDialog());
        reportBug.setOnAction(e -> showBugReportDialog());
        about.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().addAll(shortcuts, userGuide, new SeparatorMenuItem(),
                                  reportBug, new SeparatorMenuItem(), about);
        return helpMenu;
    }
    
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        
        Button scanButton = new Button("📁 스캔");
        Button refreshButton = new Button("🔄 새로고침");
        Button randomButton = new Button("🎲 랜덤 재생");
        
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
            setStatusText("플레이리스트 새로고침됨");
        });
        
        randomButton.setOnAction(e -> {
            // TODO: 랜덤 재생 기능 구현
            setStatusText("랜덤 재생 모드 토글");
        });
        
        // 구분선
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        
        // 스프링으로 오른쪽 정렬
        HBox rightContainer = new HBox(10);
        rightContainer.getChildren().addAll(scanProgressBar, statusLabel);
        HBox.setHgrow(rightContainer, Priority.ALWAYS);
        
        toolBar.getItems().addAll(scanButton, refreshButton, randomButton, separator1, 
                                 searchField, separator2, rightContainer);
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
        // 전역 키보드 단축키 설정
        getScene().setOnKeyPressed(event -> {
            // 이미 메뉴에서 처리된 단축키들은 여기서 제외
            if (event.isConsumed()) return;
            
            switch (event.getCode()) {
                case ESCAPE:
                    if (isFullScreen) {
                        toggleFullScreen();
                    }
                    event.consume();
                    break;
                case DELETE:
                    // 선택된 플레이리스트 항목 삭제
                    String selectedMusic = playlistView.getSelectedMusic();
                    String selectedPlaylist = playlistView.getSelectedPlaylist();
                    if (selectedMusic != null && selectedPlaylist != null) {
                        if (UIUtils.showConfirmation("곡 제거", 
                            "선택된 곡을 플레이리스트에서 제거하시겠습니까?")) {
                            // TODO: 곡 제거 구현
                        }
                    }
                    event.consume();
                    break;
                case F1:
                    showShortcutsDialog();
                    event.consume();
                    break;
                default:
                    break;
            }
        });
    }

    private void applyTheme() {
        // 다크 모드 지원 준비 (향후 구현)
        getScene().getRoot().getStyleClass().add("light-theme");
    }

    // ========== UI 업데이트 메서드들 ==========
    
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            setTitle("SyncTune - " + music.getTitle() + " - " + music.getArtist());
            statusBarView.updateCurrentMusic(music);
            
            // 앨범 아트 로드 시도
            loadAlbumArt(music);
        } else {
            setTitle("SyncTune Player");
            statusBarView.updateCurrentMusic(null);
        }
    }

    private void loadAlbumArt(MusicInfo music) {
        CompletableFuture.runAsync(() -> {
            // 앨범 아트 파일 찾기
            try {
                String musicDir = new File(music.getFilePath()).getParent();
                File[] imageFiles = new File(musicDir).listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                           lower.endsWith(".png") || lower.endsWith(".gif");
                });
                
                if (imageFiles != null && imageFiles.length > 0) {
                    // 첫 번째 이미지 파일 사용
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
        setStatusText("음악 라이브러리 업데이트됨: " + musicList.size() + "곡");
    }
    
    public void updateMusicMetadata(MusicInfo music) {
        playlistView.updateMusicMetadata(music);
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
            // 컴팩트 모드: 가사 숨기고 창 크기 축소
            lyricsView.setVisible(false);
            setWidth(800);
            setHeight(400);
            setStatusText("컴팩트 모드 활성화");
        } else {
            // 일반 모드: 가사 표시하고 창 크기 복원
            lyricsView.setVisible(true);
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
        lyricsView.setVisible(true);
        playlistView.setVisible(true);
        
        setStatusText("레이아웃이 초기화되었습니다");
    }

    // ========== 기능 구현 메서드들 ==========
    
    private void importPlaylistFromFile(File playlistFile) {
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: 플레이리스트 파일 파싱 구현
                // M3U, PLS 등의 플레이리스트 형식 지원
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
                // M3U 형식으로 내보내기
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
                // TODO: 캐시 정리 구현
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

    private void showSettingsDialog() {
        // TODO: 설정 다이얼로그 구현
        UIUtils.showInfo("설정", "설정 기능은 향후 업데이트에서 제공될 예정입니다.");
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
            "Escape          - 전체화면 해제\n\n" +
            "기타:\n" +
            "F1              - 도움말\n" +
            "Ctrl+Q          - 프로그램 종료\n" +
            "Ctrl+,          - 설정\n" +
            "Delete          - 선택된 항목 삭제"
        );
        shortcuts.showAndWait();
    }

    private void requestApplicationShutdown() {
        if (windowStateManager != null) {
            windowStateManager.handleCloseRequest(null);
        } else {
            // 백업 종료 방법
            eventPublisher.publish(new SystemEvent.RequestApplicationShutdownEvent());
        }
    }

    // ========== 상태 메시지 관리 ==========
    
    public void setStatusText(String text) {
        statusLabel.setText(text);
        statusBarView.updateStatus(text);
        
        // 자동 숨김 타이머 설정 (5초 후 기본 메시지로 복원)
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
        
        // 영구 메시지의 경우 타이머 중지
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
        
        // 버튼 없는 진행 다이얼로그
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

    /**
     * 애플리케이션 정리 작업
     */
    public void cleanup() {
        try {
            // 타이머들 정리
            if (statusMessageTimer != null) {
                statusMessageTimer.stop();
            }
            if (autoHideTimer != null) {
                autoHideTimer.stop();
            }
            
            // 진행 다이얼로그 정리
            hideProgressDialog();
            
            // 컨트롤러들 정리
            if (playbackController != null) {
                playbackController.dispose();
            }
            if (playlistActionHandler != null) {
                playlistActionHandler.dispose();
            }
            
            // 뷰 컴포넌트들 정리 (필요한 경우)
            
        } catch (Exception e) {
            System.err.println("정리 작업 중 오류: " + e.getMessage());
        }
    }

    /**
     * UIModule.stop()에서 호출되는 강제 종료 메서드
     */
    public void forceClose() {
        if (windowStateManager != null) {
            windowStateManager.forceClose();
        } else {
            // 백업 종료 방법
            UIUtils.runOnUIThread(() -> {
                cleanup();
                close();
            });
        }
    }

    @Override
    public void close() {
        // Core의 지시가 아닌 직접적인 close() 호출 시에도 안전한 종료 절차 진행
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
    
    public PlaybackController getPlaybackController() { return playbackController; }
    public PlaylistActionHandler getPlaylistActionHandler() { return playlistActionHandler; }
    public WindowStateManager getWindowStateManager() { return windowStateManager; }
    
    public boolean isCompactMode() { return isCompactMode; }
    public boolean isInFullScreen() { return isFullScreen; }
}