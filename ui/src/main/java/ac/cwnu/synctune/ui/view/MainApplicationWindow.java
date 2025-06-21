package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.UIModule;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.scanner.MusicFolderScanner;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class MainApplicationWindow extends Stage {
    private static final Logger log = LoggerFactory.getLogger(MainApplicationWindow.class);
    private final EventPublisher eventPublisher;
    
    // 뷰 컴포넌트들
    private PlayerControlsView controlsView;
    private PlaylistView playlistView;
    private LyricsView lyricsView;
    
    // 컨트롤러들
    private PlaybackController playbackController;
    private PlaylistActionHandler playlistActionHandler;
    private WindowStateManager windowStateManager;
    
    // 메뉴 항목들
    private MenuItem openFileMenuItem;
    private MenuItem openFolderMenuItem;
    private MenuItem exitMenuItem;
    private MenuItem playMenuItem;
    private MenuItem pauseMenuItem;
    private MenuItem stopMenuItem;
    private MenuItem nextMenuItem;
    private MenuItem previousMenuItem;
    private MenuItem aboutMenuItem;
    private CheckMenuItem alwaysOnTopMenuItem;
    private CheckMenuItem miniPlayerMenuItem;
    
    // 상태 추적
    private boolean isMiniPlayerMode = false;
    private double normalWidth, normalHeight;

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(1200);
        setHeight(800);
        setMinWidth(800);
        setMinHeight(600);
        
        // 정상 크기 저장
        normalWidth = getWidth();
        normalHeight = getHeight();
        
        initUI();
        initControllers();
        setupGlobalKeyboardShortcuts();
    }

    private void initUI() {
    BorderPane root = new BorderPane();
    
    // 메뉴바 생성
    MenuBar menuBar = createMenuBar();
    
    // 뷰 컴포넌트 생성 - EventPublisher 전달
    controlsView = new PlayerControlsView();
    playlistView = new PlaylistView(eventPublisher);  // EventPublisher 전달
    lyricsView = new LyricsView();
    
    // 레이아웃 구성
    VBox topContainer = new VBox();
    topContainer.getChildren().addAll(menuBar);
    
    root.setTop(topContainer);
    root.setBottom(controlsView);
    root.setLeft(playlistView);
    root.setCenter(lyricsView);
    
    // 여백 설정
    BorderPane.setMargin(controlsView, new Insets(10));
    BorderPane.setMargin(playlistView, new Insets(10));
    BorderPane.setMargin(lyricsView, new Insets(10));
    
    Scene scene = new Scene(root);
    
    // CSS 파일 로드 (안전하게 처리)
    try {
        var cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            String cssFile = cssResource.toExternalForm();
            scene.getStylesheets().add(cssFile);
        }
    } catch (Exception e) {
        // CSS 파일이 없어도 애플리케이션은 계속 실행
        System.err.println("CSS 파일을 로드할 수 없습니다: " + e.getMessage());
    }
    
    setScene(scene);
}
    public PlaybackController getPlaybackController() {
    return playbackController;
    }

    public void updatePlaybackButtonStates(boolean isPlaying, boolean isPaused) {
    if (controlsView != null) {
        controlsView.setPlaybackState(isPlaying, isPaused);
        }
    }

    public void setPlaybackActive(boolean playing) {
    if (controlsView != null) {
        // 재생 중일 때: 재생 버튼 비활성화, 일시정지/정지 버튼 활성화
        // 정지 중일 때: 재생 버튼 활성화, 일시정지/정지 버튼 비활성화
        controlsView.getPlayButton().setDisable(playing);
        controlsView.getPauseButton().setDisable(!playing);
        controlsView.getStopButton().setDisable(!playing);
        
        // 이전/다음 곡 버튼은 항상 활성화 (플레이리스트가 있는 경우)
        // TODO: 실제로는 플레이리스트 상태에 따라 결정해야 함
        controlsView.getPrevButton().setDisable(false);
        controlsView.getNextButton().setDisable(false);
        
        log.debug("재생 버튼 상태 업데이트 - playing: {}", playing);
        }
    }

    public void setPausedState() {
    if (controlsView != null) {
        // 일시정지 상태: 재생 버튼 활성화, 일시정지 버튼 비활성화, 정지 버튼 활성화
        controlsView.getPlayButton().setDisable(false);
        controlsView.getPauseButton().setDisable(true);
        controlsView.getStopButton().setDisable(false);
        
        log.debug("일시정지 버튼 상태 업데이트");
        }
    }

    public void setStoppedState() {
    if (controlsView != null) {
        // 정지 상태: 재생 버튼 활성화, 일시정지/정지 버튼 비활성화
        controlsView.getPlayButton().setDisable(false);
        controlsView.getPauseButton().setDisable(true);
        controlsView.getStopButton().setDisable(true);
        
        // 진행바도 초기화
        controlsView.updateProgress(0, (long) controlsView.getProgressSlider().getMax());
        
        log.debug("정지 버튼 상태 업데이트");
    }
}

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
        Menu fileMenu = createFileMenu();
        
        // 재생 메뉴
        Menu playMenu = createPlayMenu();
        
        // 보기 메뉴
        Menu viewMenu = createViewMenu();
        
        // 도구 메뉴
        Menu toolsMenu = createToolsMenu();
        
        // 도움말 메뉴
        Menu helpMenu = createHelpMenu();
        
        menuBar.getMenus().addAll(fileMenu, playMenu, viewMenu, toolsMenu, helpMenu);
        return menuBar;
    }
    
    private Menu createFileMenu() {
        Menu fileMenu = new Menu("파일");
        
        openFileMenuItem = new MenuItem("파일 열기...");
        openFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openFileMenuItem.setOnAction(e -> openMusicFiles());
        
        openFolderMenuItem = new MenuItem("폴더 열기...");
        openFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, 
            KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openFolderMenuItem.setOnAction(e -> openMusicFolder());
        
        MenuItem newPlaylistMenuItem = new MenuItem("새 플레이리스트...");
        newPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newPlaylistMenuItem.setOnAction(e -> createNewPlaylist());
        
        MenuItem importPlaylistMenuItem = new MenuItem("플레이리스트 가져오기...");
        importPlaylistMenuItem.setOnAction(e -> importPlaylist());
        
        MenuItem exportPlaylistMenuItem = new MenuItem("플레이리스트 내보내기...");
        exportPlaylistMenuItem.setOnAction(e -> exportPlaylist());
        
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        MenuItem preferencesMenuItem = new MenuItem("환경설정...");
        preferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
        preferencesMenuItem.setOnAction(e -> showPreferences());
        
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        
        exitMenuItem = new MenuItem("종료");
        exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitMenuItem.setOnAction(e -> requestClose());
        
        fileMenu.getItems().addAll(
            openFileMenuItem, openFolderMenuItem, separator1,
            newPlaylistMenuItem, importPlaylistMenuItem, exportPlaylistMenuItem, separator1,
            preferencesMenuItem, separator2,
            exitMenuItem
        );
        
        return fileMenu;
    }
    
    private Menu createPlayMenu() {
        Menu playMenu = new Menu("재생");
        
        playMenuItem = new MenuItem("재생/일시정지");
        playMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
        playMenuItem.setOnAction(e -> togglePlayPause());
        
        stopMenuItem = new MenuItem("정지");
        stopMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        stopMenuItem.setOnAction(e -> stopPlayback());
        
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        previousMenuItem = new MenuItem("이전 곡");
        previousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN));
        previousMenuItem.setOnAction(e -> playPrevious());
        
        nextMenuItem = new MenuItem("다음 곡");
        nextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN));
        nextMenuItem.setOnAction(e -> playNext());
        
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        
        MenuItem volumeUpMenuItem = new MenuItem("볼륨 증가");
        volumeUpMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN));
        volumeUpMenuItem.setOnAction(e -> adjustVolume(10));
        
        MenuItem volumeDownMenuItem = new MenuItem("볼륨 감소");
        volumeDownMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN));
        volumeDownMenuItem.setOnAction(e -> adjustVolume(-10));
        
        MenuItem muteMenuItem = new MenuItem("음소거 토글");
        muteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));
        muteMenuItem.setOnAction(e -> toggleMute());
        
        playMenu.getItems().addAll(
            playMenuItem, stopMenuItem, separator1,
            previousMenuItem, nextMenuItem, separator2,
            volumeUpMenuItem, volumeDownMenuItem, muteMenuItem
        );
        
        return playMenu;
    }
    
    private Menu createViewMenu() {
        Menu viewMenu = new Menu("보기");
        
        alwaysOnTopMenuItem = new CheckMenuItem("항상 위에");
        alwaysOnTopMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        alwaysOnTopMenuItem.setOnAction(e -> toggleAlwaysOnTop());
        
        miniPlayerMenuItem = new CheckMenuItem("미니 플레이어");
        miniPlayerMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, 
            KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        miniPlayerMenuItem.setOnAction(e -> toggleMiniPlayer());
        
        MenuItem fullScreenMenuItem = new MenuItem("전체화면");
        fullScreenMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        fullScreenMenuItem.setOnAction(e -> toggleFullScreen());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem showPlaylistMenuItem = new MenuItem("플레이리스트 표시/숨김");
        showPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
        showPlaylistMenuItem.setOnAction(e -> togglePlaylistView());
        
        MenuItem showLyricsMenuItem = new MenuItem("가사 표시/숨김");
        showLyricsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        showLyricsMenuItem.setOnAction(e -> toggleLyricsView());
        
        viewMenu.getItems().addAll(
            alwaysOnTopMenuItem, miniPlayerMenuItem, fullScreenMenuItem, separator,
            showPlaylistMenuItem, showLyricsMenuItem
        );
        
        return viewMenu;
    }
    
    private Menu createToolsMenu() {
        Menu toolsMenu = new Menu("도구");
        
        MenuItem scanLibraryMenuItem = new MenuItem("음악 라이브러리 스캔...");
        scanLibraryMenuItem.setOnAction(e -> scanMusicLibrary());
        
        MenuItem findDuplicatesMenuItem = new MenuItem("중복 파일 찾기...");
        findDuplicatesMenuItem.setOnAction(e -> findDuplicateFiles());
        
        MenuItem cleanupMenuItem = new MenuItem("플레이리스트 정리...");
        cleanupMenuItem.setOnAction(e -> cleanupPlaylists());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem mediaInfoMenuItem = new MenuItem("미디어 정보 새로고침");
        mediaInfoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        mediaInfoMenuItem.setOnAction(e -> refreshMediaInfo());
        
        toolsMenu.getItems().addAll(
            scanLibraryMenuItem, findDuplicatesMenuItem, cleanupMenuItem, separator,
            mediaInfoMenuItem
        );
        
        return toolsMenu;
    }
    
    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("도움말");
        
        MenuItem keyboardShortcutsMenuItem = new MenuItem("키보드 단축키...");
        keyboardShortcutsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        keyboardShortcutsMenuItem.setOnAction(e -> showKeyboardShortcuts());
        
        MenuItem userGuideMenuItem = new MenuItem("사용자 가이드...");
        userGuideMenuItem.setOnAction(e -> showUserGuide());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        aboutMenuItem = new MenuItem("SyncTune 정보...");
        aboutMenuItem.setOnAction(e -> showAbout());
        
        helpMenu.getItems().addAll(
            keyboardShortcutsMenuItem, userGuideMenuItem, separator,
            aboutMenuItem
        );
        
        return helpMenu;
    }

    private void initControllers() {
        playbackController = new PlaybackController(controlsView, eventPublisher);
        playlistActionHandler = new PlaylistActionHandler(playlistView, eventPublisher);
        windowStateManager = new WindowStateManager(this, eventPublisher);
        
        // PlaybackController를 CoreModule의 EventBus에 등록하여 이벤트를 받을 수 있도록 함
        // 이는 UIModule에서 처리되어야 하므로, UIModule에서 등록하도록 수정 필요
        log.debug("컨트롤러들 초기화 완료");
    }
    
    private void setupGlobalKeyboardShortcuts() {
        getScene().setOnKeyPressed(event -> {
            // 전역 키보드 단축키 처리
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case SPACE:
                        togglePlayPause();
                        event.consume();
                        break;
                    case ENTER:
                        // 선택된 곡 재생
                        playSelectedMusic();
                        event.consume();
                        break;
                    default:
                        break;
                }
            } else {
                switch (event.getCode()) {
                    case ESCAPE:
                        // ESC로 전체화면 해제
                        if (isFullScreen()) {
                            setFullScreen(false);
                            event.consume();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    // 메뉴 액션 구현들
    private void openMusicFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 열기");
        
        // 확장자 필터 설정
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("음악 파일", "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg"),
            new FileChooser.ExtensionFilter("MP3 파일", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV 파일", "*.wav"),
            new FileChooser.ExtensionFilter("FLAC 파일", "*.flac"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(this);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            // 첫 번째 파일을 즉시 재생하고 나머지는 플레이리스트에 추가
            if (selectedFiles.size() == 1) {
                playMusicFile(selectedFiles.get(0));
            } else {
                // 여러 파일 선택 시 임시 플레이리스트 생성 옵션 제공
                showMultipleFilesDialog(selectedFiles);
            }
        }
    }
    
    private void openMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 폴더 선택");
    
        File selectedDirectory = directoryChooser.showDialog(this);
        if (selectedDirectory != null) {
            // UIModule의 MusicFolderScanner를 사용하여 폴더 스캔
            scanFolderForMusicWithScanner(selectedDirectory);
        }
    }

    private void scanFolderForMusicWithScanner(File folder) {
    UIModule uiModule = UIModule.getInstance();
    if (uiModule == null || uiModule.getMusicFolderScanner() == null) {
        // 폴백: 기존 방식으로 처리
        scanFolderForMusic(folder);
        return;
    }

    // 진행 상황 다이얼로그 생성
    Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);
    progressDialog.setTitle("폴더 스캔");
    progressDialog.setHeaderText("음악 파일을 검색하고 있습니다...");
    progressDialog.setContentText("폴더: " + folder.getName());
    
    ButtonType cancelButton = new ButtonType("취소");
    progressDialog.getButtonTypes().setAll(cancelButton);
    
    // 비동기적으로 다이얼로그 표시
    progressDialog.show();
    
    // 스캔 진행 상황 콜백
    MusicFolderScanner.ScanProgressCallback callback = new MusicFolderScanner.ScanProgressCallback() {
        @Override
        public void onProgress(int scannedFiles, int foundMusic, String currentFile) {
            Platform.runLater(() -> {
                progressDialog.setContentText(String.format(
                    "스캔 중... %d개 파일 검사 완료\n%d개 음악 파일 발견\n현재: %s", 
                    scannedFiles, foundMusic, truncateFileName(currentFile, 40)));
            });
        }
        
        @Override
        public void onDirectoryEntered(String directoryPath) {
            Platform.runLater(() -> {
                progressDialog.setHeaderText("검색 중: " + truncateFileName(directoryPath, 50));
            });
        }
        
        @Override
        public void onComplete(MusicFolderScanner.ScanResult result) {
            Platform.runLater(() -> {
                progressDialog.close();
                handleScanResult(result, folder);
            });
        }
        
        @Override
        public void onError(String error) {
            Platform.runLater(() -> {
                progressDialog.close();
                UIUtils.showError("스캔 오류", "폴더 스캔 중 오류가 발생했습니다: " + error);
            });
        }
    };
    
    // Task 생성 및 실행
    Task<MusicFolderScanner.ScanResult> scanTask = uiModule.createMusicScanTask(folder, callback);
    
    // 취소 버튼 처리
    progressDialog.setOnCloseRequest(e -> {
        scanTask.cancel();
        uiModule.cancelCurrentScan();
    });
    
    progressDialog.showAndWait().ifPresent(result -> {
        if (result == cancelButton) {
            scanTask.cancel();
            uiModule.cancelCurrentScan();
        }
    });
    
    // 백그라운드에서 실행
    Thread scanThread = new Thread(scanTask);
    scanThread.setDaemon(true);
    scanThread.start();
}
    private void handleScanResult(MusicFolderScanner.ScanResult result, File scannedFolder) {
    if (result.isCancelled()) {
        UIUtils.showInfo("스캔 취소", "폴더 스캔이 취소되었습니다.");
        return;
    }
    
    if (!result.isSuccess()) {
        String errorMsg = result.getErrorMessage() != null ? 
            result.getErrorMessage() : "알 수 없는 오류가 발생했습니다.";
        UIUtils.showError("스캔 실패", "폴더 스캔에 실패했습니다: " + errorMsg);
        return;
    }
    
    List<MusicInfo> foundMusic = result.getMusicFiles();
    
    if (foundMusic.isEmpty()) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("스캔 완료");
        infoAlert.setHeaderText("음악 파일을 찾을 수 없습니다");
        infoAlert.setContentText(String.format(
            "폴더: %s\n검사한 파일: %d개\n검사한 폴더: %d개\n소요 시간: %.1f초",
            scannedFolder.getName(),
            result.getTotalFilesScanned(),
            result.getDirectoriesScanned(),
            result.getScanTimeMs() / 1000.0));
        infoAlert.showAndWait();
        return;
    }
    
    // 발견된 음악 파일들을 플레이리스트에 추가할지 확인
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("음악 파일 발견");
    confirmAlert.setHeaderText("음악 파일을 발견했습니다!");
    
    String content = String.format(
        "폴더: %s\n" +
        "발견된 음악 파일: %d개\n" +
        "검사한 파일: %d개\n" +
        "검사한 폴더: %d개\n" +
        "소요 시간: %.1f초\n\n" +
        "플레이리스트에 추가하시겠습니까?",
        scannedFolder.getName(),
        foundMusic.size(),
        result.getTotalFilesScanned(),
        result.getDirectoriesScanned(),
        result.getScanTimeMs() / 1000.0);
    
    confirmAlert.setContentText(content);
    
    ButtonType addToCurrentButton = new ButtonType("현재 플레이리스트에 추가");
    ButtonType createNewButton = new ButtonType("새 플레이리스트 생성");
    ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    confirmAlert.getButtonTypes().setAll(addToCurrentButton, createNewButton, cancelButton);
    
    Optional<ButtonType> choice = confirmAlert.showAndWait();
    
    if (choice.isPresent()) {
        if (choice.get() == addToCurrentButton) {
            addFoundMusicToCurrentPlaylist(foundMusic);
        } else if (choice.get() == createNewButton) {
            createNewPlaylistWithFoundMusic(foundMusic, scannedFolder.getName());
        }
    }
    
    // 스캔 결과에 대한 추가 정보 표시 (경고나 오류가 있는 경우)
    if (result.getScanTimeMs() > 30000) { // 30초 이상 걸린 경우
        UIUtils.showInfo("스캔 완료", 
            String.format("대용량 폴더 스캔이 완료되었습니다. 소요 시간: %.1f초", 
                         result.getScanTimeMs() / 1000.0));
    }
}
    private void addFoundMusicToCurrentPlaylist(List<MusicInfo> musicList) {
    String selectedPlaylist = playlistView.getSelectedPlaylist();
    String currentPlaylist;
    
    if (selectedPlaylist == null) {
        // 현재 선택된 플레이리스트가 없으면 기본 플레이리스트 생성
        String defaultName = "새 플레이리스트";
        playlistView.addPlaylist(defaultName);
        playlistView.selectPlaylist(defaultName);
        currentPlaylist = defaultName;
    } else {
        currentPlaylist = selectedPlaylist;
    }
    
    // 진행 상황 표시
    playlistView.showProgress(true);
    playlistView.updateStatusLabel("음악 파일을 추가하는 중...", false);
    
    final String finalCurrentPlaylist = currentPlaylist;
    // 백그라운드에서 추가 작업 수행
    CompletableFuture.runAsync(() -> {
        int addedCount = 0;
        
        for (int i = 0; i < musicList.size(); i++) {
            MusicInfo music = musicList.get(i);
            
            Platform.runLater(() -> {
                playlistView.addMusicToCurrentPlaylist(music);
            });
            
            addedCount++;
            
            // 진행 상황 업데이트
            final int currentIndex = i + 1;
            final int finalAddedCount = addedCount;
            Platform.runLater(() -> {
                playlistView.updateStatusLabel(
                    String.format("음악 파일 추가 중... (%d/%d)", currentIndex, musicList.size()), 
                    false);
            });
            
            // UI 업데이트를 위한 짧은 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 완료 처리
        final int finalTotalAdded = addedCount;
        Platform.runLater(() -> {
            playlistView.showProgress(false);
            playlistView.updateStatusLabel(
                String.format("%d개의 음악 파일이 '%s'에 추가되었습니다.", 
                             finalTotalAdded, finalCurrentPlaylist), 
                false);
        });
    });
    }
    private void createNewPlaylistWithFoundMusic(List<MusicInfo> musicList, String folderName) {
    TextInputDialog dialog = new TextInputDialog(folderName + " 음악");
    dialog.setTitle("새 플레이리스트 생성");
    dialog.setHeaderText("발견된 음악 파일들로 새 플레이리스트를 만듭니다");
    dialog.setContentText("플레이리스트 이름:");
    
    Optional<String> result = dialog.showAndWait();
    result.ifPresent(playlistName -> {
        if (!playlistName.trim().isEmpty()) {
            // 중복 이름 확인 및 처리
            String finalName = playlistName.trim();
            int counter = 1;
            while (playlistView.getPlaylists().contains(finalName)) {
                finalName = playlistName.trim() + " (" + counter + ")";
                counter++;
            }
            
            // 플레이리스트 생성 및 선택
            playlistView.addPlaylist(finalName);
            playlistView.selectPlaylist(finalName);
            
            // 음악 파일들 추가
            addFoundMusicToCurrentPlaylist(musicList);
        }
    });
    }
    private void scanMusicLibrary() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("음악 라이브러리 폴더 선택");
    
    // 기본 음악 폴더로 시작
    String defaultMusicPath = System.getProperty("user.home") + "/Music";
    File defaultMusicDir = new File(defaultMusicPath);
    if (defaultMusicDir.exists()) {
        directoryChooser.setInitialDirectory(defaultMusicDir);
    }
    
    File selectedDirectory = directoryChooser.showDialog(this);
    if (selectedDirectory != null) {
        scanMusicLibraryAdvanced(selectedDirectory);
    }
}
    private void scanMusicLibraryAdvanced(File rootDirectory) {
    UIModule uiModule = UIModule.getInstance();
    if (uiModule == null || uiModule.getMusicFolderScanner() == null) {
        UIUtils.showError("오류", "음악 스캐너를 사용할 수 없습니다.");
        return;
    }
    
    // 스캔 옵션 선택 다이얼로그
    Alert optionsDialog = new Alert(Alert.AlertType.CONFIRMATION);
    optionsDialog.setTitle("라이브러리 스캔 옵션");
    optionsDialog.setHeaderText("스캔 옵션을 선택하세요");
    optionsDialog.setContentText("선택한 폴더: " + rootDirectory.getAbsolutePath());
    
    ButtonType quickScanButton = new ButtonType("빠른 스캔 (최대 3단계 깊이)");
    ButtonType fullScanButton = new ButtonType("전체 스캔 (모든 하위 폴더)");
    ButtonType customScanButton = new ButtonType("사용자 정의 스캔");
    ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    optionsDialog.getButtonTypes().setAll(quickScanButton, fullScanButton, customScanButton, cancelButton);
    
    Optional<ButtonType> choice = optionsDialog.showAndWait();
    
    if (choice.isPresent() && choice.get() != cancelButton) {
        MusicFolderScanner.ScanOptions scanOptions;
        
        if (choice.get() == quickScanButton) {
            scanOptions = uiModule.getQuickScanOptions();
        } else if (choice.get() == fullScanButton) {
            scanOptions = uiModule.getDefaultScanOptions();
        } else {
            // 사용자 정의 스캔 옵션
            scanOptions = showCustomScanOptionsDialog(uiModule.getDefaultScanOptions());
            if (scanOptions == null) return; // 사용자가 취소함
        }
        
        performLibraryScan(rootDirectory, scanOptions);
    }
}
    private MusicFolderScanner.ScanOptions showCustomScanOptionsDialog(MusicFolderScanner.ScanOptions defaultOptions) {
    // 커스텀 다이얼로그 생성 (간단한 구현)
    Alert customDialog = new Alert(Alert.AlertType.CONFIRMATION);
    customDialog.setTitle("사용자 정의 스캔 설정");
    customDialog.setHeaderText("스캔 설정을 선택하세요");
    
    VBox content = new VBox(10);
    
    CheckBox recursiveCheck = new CheckBox("하위 폴더까지 스캔");
    recursiveCheck.setSelected(defaultOptions.isRecursive());
    
    CheckBox hiddenFilesCheck = new CheckBox("숨김 파일 포함");
    hiddenFilesCheck.setSelected(defaultOptions.isIncludeHiddenFiles());
    
    CheckBox symlinksCheck = new CheckBox("심볼릭 링크 따라가기");
    symlinksCheck.setSelected(defaultOptions.isFollowSymlinks());
    
    HBox depthBox = new HBox(10);
    depthBox.getChildren().addAll(
        new Label("최대 깊이:"),
        new Spinner<>(1, 50, Math.min(defaultOptions.getMaxDepth(), 20))
    );
    
    content.getChildren().addAll(recursiveCheck, hiddenFilesCheck, symlinksCheck, depthBox);
    
    customDialog.getDialogPane().setContent(content);
    
    Optional<ButtonType> result = customDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        Spinner<Integer> depthSpinner = (Spinner<Integer>) ((HBox) content.getChildren().get(3)).getChildren().get(1);
        
        return new MusicFolderScanner.ScanOptions()
            .setRecursive(recursiveCheck.isSelected())
            .setIncludeHiddenFiles(hiddenFilesCheck.isSelected())
            .setFollowSymlinks(symlinksCheck.isSelected())
            .setMaxDepth(depthSpinner.getValue())
            .setMaxFileSize(defaultOptions.getMaxFileSize());
    }
    
    return null;
}
    private void performLibraryScan(File directory, MusicFolderScanner.ScanOptions options) {
    UIModule uiModule = UIModule.getInstance();
    
    // 진행 상황 다이얼로그
    Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);
    progressDialog.setTitle("라이브러리 스캔");
    progressDialog.setHeaderText("음악 라이브러리를 스캔하고 있습니다...");
    progressDialog.setContentText("준비 중...");
    
    ButtonType cancelButton = new ButtonType("취소");
    progressDialog.getButtonTypes().setAll(cancelButton);
    
    ProgressBar progressBar = new ProgressBar();
    progressBar.setPrefWidth(300);
    
    VBox progressContent = new VBox(10);
    progressContent.getChildren().addAll(new Label("진행 상황:"), progressBar);
    progressDialog.getDialogPane().setExpandableContent(progressContent);
    progressDialog.getDialogPane().setExpanded(true);
    
    progressDialog.show();
    
    // 스캔 콜백
    MusicFolderScanner.ScanProgressCallback callback = new MusicFolderScanner.ScanProgressCallback() {
        private long startTime = System.currentTimeMillis();
        
        @Override
        public void onProgress(int scannedFiles, int foundMusic, String currentFile) {
            Platform.runLater(() -> {
                long elapsed = System.currentTimeMillis() - startTime;
                progressDialog.setContentText(String.format(
                    "검사 완료: %d개 파일\n발견: %d개 음악 파일\n경과 시간: %.1f초\n현재: %s",
                    scannedFiles, foundMusic, elapsed / 1000.0, 
                    truncateFileName(currentFile, 50)));
                
                // 대략적인 진행률 표시
                if (scannedFiles > 0) {
                    progressBar.setProgress(Math.min(0.95, scannedFiles / 10000.0));
                }
            });
        }
        
        @Override
        public void onDirectoryEntered(String directoryPath) {
            Platform.runLater(() -> {
                progressDialog.setHeaderText("스캔 중: " + truncateFileName(directoryPath, 60));
            });
        }
        
        @Override
        public void onComplete(MusicFolderScanner.ScanResult result) {
            Platform.runLater(() -> {
                progressDialog.close();
                handleLibraryScanResult(result, directory);
            });
        }
        
        @Override
        public void onError(String error) {
            Platform.runLater(() -> {
                progressDialog.close();
                UIUtils.showError("라이브러리 스캔 오류", 
                    "라이브러리 스캔 중 오류가 발생했습니다: " + error);
            });
        }
    };
    
    // Task 생성 및 실행
    Task<MusicFolderScanner.ScanResult> scanTask = uiModule.createCustomMusicScanTask(directory, options, callback);
    
    // 취소 처리
    progressDialog.setOnCloseRequest(e -> {
        scanTask.cancel();
        uiModule.cancelCurrentScan();
    });
    
    progressDialog.showAndWait().ifPresent(result -> {
        if (result == cancelButton) {
            scanTask.cancel();
            uiModule.cancelCurrentScan();
        }
    });
    
    // 백그라운드 실행
    Thread scanThread = new Thread(scanTask);
    scanThread.setDaemon(true);
    scanThread.start();
}
private void handleLibraryScanResult(MusicFolderScanner.ScanResult result, File scannedDirectory) {
    if (result.isCancelled()) {
        UIUtils.showInfo("스캔 취소", "라이브러리 스캔이 취소되었습니다.");
        return;
    }
    
    if (!result.isSuccess()) {
        String errorMsg = result.getErrorMessage() != null ? 
            result.getErrorMessage() : "알 수 없는 오류가 발생했습니다.";
        UIUtils.showError("스캔 실패", "라이브러리 스캔에 실패했습니다: " + errorMsg);
        return;
    }
    
    List<MusicInfo> foundMusic = result.getMusicFiles();
    
    // 결과 표시
    Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
    resultDialog.setTitle("라이브러리 스캔 완료");
    resultDialog.setHeaderText("음악 라이브러리 스캔이 완료되었습니다");
    
    String content = String.format(
        "스캔 대상: %s\n" +
        "발견된 음악 파일: %d개\n" +
        "검사한 파일: %d개\n" +
        "검사한 폴더: %d개\n" +
        "소요 시간: %.1f초",
        scannedDirectory.getAbsolutePath(),
        foundMusic.size(),
        result.getTotalFilesScanned(),
        result.getDirectoriesScanned(),
        result.getScanTimeMs() / 1000.0);
    
    resultDialog.setContentText(content);
    
    if (!foundMusic.isEmpty()) {
        ButtonType addAllButton = new ButtonType("모든 음악 추가");
        ButtonType selectiveButton = new ButtonType("선택적 추가");
        ButtonType closeButton = new ButtonType("닫기", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        resultDialog.getButtonTypes().setAll(addAllButton, selectiveButton, closeButton);
        
        Optional<ButtonType> choice = resultDialog.showAndWait();
        if (choice.isPresent()) {
            if (choice.get() == addAllButton) {
                createNewPlaylistWithFoundMusic(foundMusic, scannedDirectory.getName() + " 라이브러리");
            } else if (choice.get() == selectiveButton) {
                showSelectiveMusicAddDialog(foundMusic);
            }
        }
    } else {
        resultDialog.showAndWait();
    }
}
    private void showSelectiveMusicAddDialog(List<MusicInfo> musicList) {
    // 간단한 구현: 아티스트별로 그룹화하여 선택할 수 있는 다이얼로그
    Alert selectDialog = new Alert(Alert.AlertType.CONFIRMATION);
    selectDialog.setTitle("음악 파일 선택");
    selectDialog.setHeaderText("추가할 음악 파일을 선택하세요");
    
    ListView<MusicInfo> musicListView = new ListView<>();
    musicListView.getItems().addAll(musicList);
    musicListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    musicListView.setPrefHeight(300);
    
    // 전체 선택/해제 버튼
    CheckBox selectAllCheck = new CheckBox("전체 선택/해제");
    selectAllCheck.setOnAction(e -> {
        if (selectAllCheck.isSelected()) {
            musicListView.getSelectionModel().selectAll();
        } else {
            musicListView.getSelectionModel().clearSelection();
        }
    });
    
    VBox content = new VBox(10);
    content.getChildren().addAll(selectAllCheck, musicListView);
    content.setPrefWidth(500);
    
    selectDialog.getDialogPane().setContent(content);
    
    Optional<ButtonType> result = selectDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        List<MusicInfo> selectedMusic = musicListView.getSelectionModel().getSelectedItems();
        if (!selectedMusic.isEmpty()) {
            createNewPlaylistWithFoundMusic(selectedMusic, "선택된 음악");
        }
    }
}
    private String truncateFileName(String fileName, int maxLength) {
    if (fileName == null || fileName.length() <= maxLength) {
        return fileName;
    }
    return "..." + fileName.substring(fileName.length() - maxLength + 3);
}
    
    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("새 플레이리스트");
        dialog.setHeaderText("새 플레이리스트를 만듭니다");
        dialog.setContentText("플레이리스트 이름:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                playlistView.addPlaylist(name.trim());
            }
        });
    }
    
    private void importPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("플레이리스트 가져오기");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("M3U 플레이리스트", "*.m3u", "*.m3u8"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(this);
        if (selectedFile != null) {
            // TODO: 실제 M3U 파일 파싱 기능 구현
            UIUtils.showInfo("가져오기", "플레이리스트 가져오기 기능은 구현 중입니다.");
        }
    }
    
    private void exportPlaylist() {
        String selectedPlaylist = playlistView.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            UIUtils.showError("오류", "내보낼 플레이리스트를 선택해주세요.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("플레이리스트 내보내기");
        fileChooser.setInitialFileName(selectedPlaylist + ".m3u");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("M3U 플레이리스트", "*.m3u")
        );
        
        File saveFile = fileChooser.showSaveDialog(this);
        if (saveFile != null) {
            // TODO: 실제 M3U 파일 생성 기능 구현
            UIUtils.showInfo("내보내기", "플레이리스트 내보내기 기능은 구현 중입니다.");
        }
    }
    
    private void showPreferences() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("환경설정");
        dialog.setHeaderText("환경설정 (미구현)");
        dialog.setContentText("환경설정 기능은 향후 버전에서 제공될 예정입니다.\n" +
                             "현재는 기본 설정으로 동작합니다.");
        dialog.showAndWait();
    }
    
    private void requestClose() {
        if (windowStateManager != null) {
            windowStateManager.handleCloseRequest(null);
        } else {
            close();
        }
    }
    
    private void togglePlayPause() {
        if (playbackController != null) {
            if (playbackController.isPlaybackActive()) {
                eventPublisher.publish(new MediaControlEvent.RequestPauseEvent());
            } else {
                eventPublisher.publish(new MediaControlEvent.RequestPlayEvent());
            }
        }
    }
    
    private void stopPlayback() {
        eventPublisher.publish(new MediaControlEvent.RequestStopEvent());
    }
    
    private void playPrevious() {
        eventPublisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
    }
    
    private void playNext() {
        eventPublisher.publish(new MediaControlEvent.RequestNextMusicEvent());
    }
    
    private void adjustVolume(int delta) {
        if (controlsView != null) {
            double currentVolume = controlsView.getVolumeSlider().getValue();
            double newVolume = Math.max(0, Math.min(100, currentVolume + delta));
            controlsView.getVolumeSlider().setValue(newVolume);
        }
    }
    
    private void toggleMute() {
        if (controlsView != null) {
            controlsView.getMuteButton().setSelected(!controlsView.getMuteButton().isSelected());
        }
    }
    
    private void toggleAlwaysOnTop() {
        setAlwaysOnTop(alwaysOnTopMenuItem.isSelected());
    }
    
    private void toggleMiniPlayer() {
        isMiniPlayerMode = miniPlayerMenuItem.isSelected();
        
        if (isMiniPlayerMode) {
            // 미니 플레이어 모드로 전환
            normalWidth = getWidth();
            normalHeight = getHeight();
            
            // 플레이리스트와 가사 뷰 숨기기
            playlistView.setVisible(false);
            playlistView.setManaged(false);
            lyricsView.setVisible(false);
            lyricsView.setManaged(false);
            
            // 창 크기 조정
            setWidth(400);
            setHeight(200);
            setResizable(false);
            
        } else {
            // 일반 모드로 전환
            playlistView.setVisible(true);
            playlistView.setManaged(true);
            lyricsView.setVisible(true);
            lyricsView.setManaged(true);
            
            setWidth(normalWidth);
            setHeight(normalHeight);
            setResizable(true);
        }
    }
    
    private void toggleFullScreen() {
        setFullScreen(!isFullScreen());
    }
    
    private void togglePlaylistView() {
        boolean isVisible = playlistView.isVisible();
        playlistView.setVisible(!isVisible);
        playlistView.setManaged(!isVisible);
    }
    
    private void toggleLyricsView() {
        boolean isVisible = lyricsView.isVisible();
        lyricsView.setVisible(!isVisible);
        lyricsView.setManaged(!isVisible);
    }
    
    private void findDuplicateFiles() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("중복 파일 찾기");
        dialog.setHeaderText("중복 파일 찾기 (미구현)");
        dialog.setContentText("중복 파일 찾기 기능은 향후 버전에서 제공될 예정입니다.\n" +
                             "파일명, 크기, 해시값 등을 비교하여 중복 파일을 찾을 예정입니다.");
        dialog.showAndWait();
    }
    
    private void cleanupPlaylists() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("플레이리스트 정리");
        confirmDialog.setHeaderText("플레이리스트를 정리하시겠습니까?");
        confirmDialog.setContentText("존재하지 않는 파일들을 플레이리스트에서 제거합니다.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: 실제 정리 기능 구현
            Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
            resultDialog.setTitle("정리 완료");
            resultDialog.setHeaderText("플레이리스트 정리가 완료되었습니다");
            resultDialog.setContentText("정리 기능은 향후 버전에서 구현될 예정입니다.");
            resultDialog.showAndWait();
        }
    }
    
    private void refreshMediaInfo() {
        // TODO: 미디어 정보 새로고침 이벤트 발행
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("새로고침");
        dialog.setHeaderText("미디어 정보 새로고침");
        dialog.setContentText("미디어 정보가 새로고침되었습니다.");
        dialog.showAndWait();
    }
    
    private void showKeyboardShortcuts() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("키보드 단축키");
        dialog.setHeaderText("SyncTune 키보드 단축키");
        
        String shortcuts = """
            재생/제어:
            Space - 재생/일시정지
            Ctrl+S - 정지
            Ctrl+← - 이전 곡
            Ctrl+→ - 다음 곡
            Ctrl+↑ - 볼륨 증가
            Ctrl+↓ - 볼륨 감소
            Ctrl+M - 음소거
            
            파일:
            Ctrl+O - 파일 열기
            Ctrl+Shift+O - 폴더 열기
            Ctrl+N - 새 플레이리스트
            Ctrl+Q - 종료
            
            보기:
            F11 - 전체화면
            Ctrl+T - 항상 위에
            Ctrl+Alt+M - 미니 플레이어
            Ctrl+P - 플레이리스트 표시/숨김
            Ctrl+L - 가사 표시/숨김
            
            기타:
            F1 - 이 도움말
            F5 - 새로고침
            ESC - 전체화면 해제
            """;
        
        dialog.setContentText(shortcuts);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.showAndWait();
    }
    
    private void showUserGuide() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("사용자 가이드");
        dialog.setHeaderText("SyncTune 사용자 가이드");
        
        String guide = """
            SyncTune 음악 플레이어에 오신 것을 환영합니다!
            
            기본 사용법:
            1. '파일 > 파일 열기'로 음악 파일을 추가하세요
            2. '파일 > 폴더 열기'로 폴더 전체를 추가할 수 있습니다
            3. 왼쪽 플레이리스트에서 재생목록을 관리하세요
            4. 오른쪽에서 현재 재생 중인 곡의 가사를 확인하세요
            5. 하단 컨트롤로 재생을 제어하세요
            
            플레이리스트 관리:
            - 새 플레이리스트 생성
            - 곡 추가/제거
            - M3U 형식으로 가져오기/내보내기
            - 플레이리스트 정렬 및 섞기
            
            가사 기능:
            - LRC 파일 자동 인식
            - 실시간 가사 동기화
            - 시간대별 가사 하이라이트
            
            더 자세한 정보는 F1을 눌러 키보드 단축키를 확인하세요.
            """;
        
        dialog.setContentText(guide);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.showAndWait();
    }
    
    private void showAbout() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("SyncTune 정보");
        dialog.setHeaderText("SyncTune Music Player");
        
        String about = """
            SyncTune v1.0.0
            
            확장 가능하고 유지보수가 용이한 음악 플레이어
            
            주요 기능:
            • 다양한 음악 형식 지원 (MP3, WAV, FLAC, M4A, AAC, OGG)
            • LRC 가사 파일 지원 및 실시간 동기화
            • 플레이리스트 관리 (M3U 가져오기/내보내기)
            • 모듈형 아키텍처로 확장 가능
            • 이벤트 기반 모듈 간 통신
            
            개발: OOP 1팀
            팀원: 동헌희, 김민재, 김대영, 임민수
            
            아키텍처: SOLID 원칙 기반 모듈형 설계
            사용 기술: Java, JavaFX, SLF4J, Gradle
            """;
        
        dialog.setContentText(about);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.showAndWait();
    }
    
    // 헬퍼 메서드들
    private void playMusicFile(File musicFile) {
    try {
        // 파일에서 MusicInfo 생성
        String fileName = musicFile.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // 파일명 파싱 시도 (예: "Artist - Title.mp3")
        String title = baseName;
        String artist = "Unknown Artist";
        
        if (baseName.contains(" - ")) {
            String[] parts = baseName.split(" - ", 2);
            if (parts.length == 2) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }
        
        // 파일 크기 기반 대략적인 재생 시간 계산
        long fileSize = musicFile.length();
        long estimatedDuration = estimateDuration(fileSize, fileName);
        
        // LRC 파일 찾기
        String lrcPath = findLrcFileForMusic(musicFile);
        
        MusicInfo musicInfo = new MusicInfo(
            title, artist, "Unknown Album", 
            musicFile.getAbsolutePath(), estimatedDuration, lrcPath
        );
        
        log.info("음악 파일 재생 요청: {} - {}", artist, title);
        eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(musicInfo));
        
    } catch (Exception e) {
        UIUtils.showError("오류", "음악 파일을 재생할 수 없습니다: " + e.getMessage());
        log.error("음악 파일 재생 실패: {}", musicFile.getAbsolutePath(), e);
    }
}

    private String findLrcFileForMusic(File musicFile) {
    try {
        String baseName = musicFile.getName();
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = baseName.substring(0, lastDot);
        }
        
        // 같은 디렉토리에서 찾기
        File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
        if (lrcFile.exists()) {
            log.debug("LRC 파일 발견: {}", lrcFile.getAbsolutePath());
            return lrcFile.getAbsolutePath();
        }
        
        // lyrics 폴더에서 찾기
        File lyricsDir = new File("lyrics");
        if (lyricsDir.exists()) {
            File lrcInLyricsDir = new File(lyricsDir, baseName + ".lrc");
            if (lrcInLyricsDir.exists()) {
                log.debug("lyrics 폴더에서 LRC 파일 발견: {}", lrcInLyricsDir.getAbsolutePath());
                return lrcInLyricsDir.getAbsolutePath();
            }
        }
        
        } catch (Exception e) {
        log.debug("LRC 파일 찾기 실패: {}", musicFile.getName());
        }
    
    return null;
}
    
    private void showMultipleFilesDialog(List<File> files) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("여러 파일 선택");
        dialog.setHeaderText(files.size() + "개의 파일이 선택되었습니다");
        dialog.setContentText("어떻게 처리하시겠습니까?");
        
        ButtonType playFirstButton = new ButtonType("첫 번째 파일 재생");
        ButtonType addToPlaylistButton = new ButtonType("플레이리스트에 추가");
        ButtonType createPlaylistButton = new ButtonType("새 플레이리스트 생성");
        ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getButtonTypes().setAll(playFirstButton, addToPlaylistButton, createPlaylistButton, cancelButton);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == playFirstButton) {
                playMusicFile(files.get(0));
            } else if (result.get() == addToPlaylistButton) {
                // 현재 선택된 플레이리스트에 추가
                String selectedPlaylist = playlistView.getSelectedPlaylist();
                if (selectedPlaylist != null) {
                    addFilesToPlaylist(files);
                } else {
                    UIUtils.showError("오류", "먼저 플레이리스트를 선택해주세요.");
                }
            } else if (result.get() == createPlaylistButton) {
                // 새 플레이리스트 생성 후 추가
                createNewPlaylistWithFiles(files);
            }
        }
    }

    private long estimateDuration(long fileSize, String fileName) {
        String ext = fileName.toLowerCase();
    if (ext.endsWith(".mp3")) {
        // MP3: 대략 1MB당 1분으로 추정
        return Math.max((fileSize / (1024 * 1024)) * 60 * 1000, 30000); // 최소 30초
    } else if (ext.endsWith(".wav")) {
        // WAV: 무압축이므로 더 짧게 추정
        return Math.max((fileSize / (10 * 1024 * 1024)) * 60 * 1000, 30000);
    } else {
        // 기본값: 3분
        return 180000L;
    }
}

    private MusicInfo createMusicInfoFromFile(File file) {
    try {
        // 파일명에서 기본 정보 추출
        String fileName = file.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // 파일명 파싱 시도
        String title = baseName;
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        
        if (baseName.contains(" - ")) {
            String[] parts = baseName.split(" - ", 2);
            if (parts.length == 2) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }
        
        // 재생 시간 추정
        long duration = estimateDuration(file.length(), fileName);
        
        // LRC 파일 찾기
        String lrcPath = findLrcFileForMusic(file);
        
        log.debug("MusicInfo 생성: {} - {} ({})", artist, title, UIUtils.formatTime(duration));
        
        return new MusicInfo(title, artist, album, file.getAbsolutePath(), duration, lrcPath);
        
    } catch (Exception e) {
        log.error("MusicInfo 생성 중 오류: {}", file.getName(), e);
        // 기본값으로 반환
        return new MusicInfo(file.getName(), "Unknown Artist", "Unknown Album", 
                           file.getAbsolutePath(), 180000L, null);
    }
}
    
    private void scanFolderForMusic(File folder) {
        // TODO: 실제 폴더 스캔 기능 구현
        UIUtils.showInfo("폴더 스캔", "폴더 스캔 기능은 구현 중입니다.\n선택된 폴더: " + folder.getAbsolutePath());
    }
    
    private void addFilesToPlaylist(List<File> files) {
        int addedCount = 0;
        for (File file : files) {
            if (isSupportedAudioFile(file)) {
                MusicInfo musicInfo = createMusicInfoFromFile(file);
                playlistView.addMusicToCurrentPlaylist(musicInfo);
                addedCount++;
            }
        }
        UIUtils.showInfo("추가 완료", addedCount + "개의 파일이 플레이리스트에 추가되었습니다.");
    }
    
    private void createNewPlaylistWithFiles(List<File> files) {
        TextInputDialog dialog = new TextInputDialog("새 플레이리스트");
        dialog.setTitle("새 플레이리스트 생성");
        dialog.setHeaderText("파일들을 포함할 새 플레이리스트를 만듭니다");
        dialog.setContentText("플레이리스트 이름:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                playlistView.addPlaylist(name.trim());
                playlistView.selectPlaylist(name.trim());
                addFilesToPlaylist(files);
            }
        });
    }
    
    
    private boolean isSupportedAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
               fileName.endsWith(".flac") || fileName.endsWith(".m4a") ||
               fileName.endsWith(".aac") || fileName.endsWith(".ogg");
    }
    
    private void playSelectedMusic() {
    MusicInfo selectedMusic = playlistView.getSelectedMusic();
    if (selectedMusic != null) {
        log.debug("메인 윈도우에서 곡 재생 요청: {}", selectedMusic.getTitle());
        eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(selectedMusic));
    } else {
        // 선택된 곡이 없으면 기본 재생 요청
        eventPublisher.publish(new MediaControlEvent.RequestPlayEvent());
    }
}

    // UI 업데이트 메서드들
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            String title = isMiniPlayerMode ? 
                music.getTitle() : 
                "SyncTune - " + music.getTitle() + " - " + music.getArtist();
            setTitle(title);
            
            // 가사 뷰에 로딩 상태 표시
            lyricsView.showLyricsLoading();
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (controlsView != null) {
            controlsView.updateProgress(currentMs, totalMs);
        }
    }

    public void updateLyrics(String lyrics) {
        if (lyricsView != null) {
            if (lyrics == null || lyrics.trim().isEmpty() || lyrics.equals("가사를 찾을 수 없습니다")) {
                lyricsView.showLyricsNotFound();
            } else {
                lyricsView.updateLyrics(lyrics);
            }
        }
    }

    public void showLyricsFound(String lrcPath) {
        if (lyricsView != null) {
            lyricsView.showLyricsLoading();
        }
    }

    public void showLyricsNotFound() {
        if (lyricsView != null) {
            lyricsView.showLyricsNotFound();
        }
    }

    /**
     * UIModule.stop()에서 호출되는 강제 종료 메서드
     */
    public void forceClose() {
        try {
            // 이벤트 핸들러 제거하여 재귀 호출 방지
            setOnCloseRequest(null);
            
            // 컨트롤러 정리
            if (windowStateManager != null) {
                windowStateManager = null;
            }
            
            // 윈도우 닫기
            super.close();
        } catch (Exception e) {
            System.err.println("윈도우 강제 종료 중 오류: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // Core의 지시가 아닌 직접적인 close() 호출 시에도 안전한 종료 절차 진행
        if (windowStateManager != null && !windowStateManager.isCloseRequested()) {
            windowStateManager.handleCloseRequest(null);
        } else {
            super.close();
        }
    }
}