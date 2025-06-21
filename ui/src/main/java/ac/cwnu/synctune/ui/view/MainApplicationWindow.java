package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;
import java.util.Optional;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainApplicationWindow extends Stage {
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
        
        // 뷰 컴포넌트 생성
        controlsView = new PlayerControlsView();
        playlistView = new PlaylistView();
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
            // 폴더에서 음악 파일 스캔 및 플레이리스트에 추가
            scanFolderForMusic(selectedDirectory);
        }
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
    
    private void scanMusicLibrary() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 라이브러리 폴더 선택");
        
        File selectedDirectory = directoryChooser.showDialog(this);
        if (selectedDirectory != null) {
            // TODO: 전체 라이브러리 스캔 기능 구현
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("라이브러리 스캔");
            dialog.setHeaderText("라이브러리 스캔 (미구현)");
            dialog.setContentText("선택된 폴더: " + selectedDirectory.getAbsolutePath() + "\n\n" +
                                 "전체 라이브러리 스캔 기능은 향후 버전에서 제공될 예정입니다.");
            dialog.showAndWait();
        }
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
            
            MusicInfo musicInfo = new MusicInfo(
                baseName, "Unknown Artist", "Unknown Album", 
                musicFile.getAbsolutePath(), 180000L, null
            );
            
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(musicInfo));
            
        } catch (Exception e) {
            UIUtils.showError("오류", "음악 파일을 재생할 수 없습니다: " + e.getMessage());
        }
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
    
    private MusicInfo createMusicInfoFromFile(File file) {
        String fileName = file.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        return new MusicInfo(
            baseName, "Unknown Artist", "Unknown Album", 
            file.getAbsolutePath(), 180000L, null
        );
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
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(selectedMusic));
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