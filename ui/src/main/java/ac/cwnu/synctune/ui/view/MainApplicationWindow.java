package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.FileScanEvent;
import ac.cwnu.synctune.sdk.event.MediaInfoEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.util.UIUtils;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

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
    private Alert progressDialog;

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(1400);
        setHeight(900);
        initUI();
        initControllers();
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
        
        // 상단 도구모음 생성
        HBox toolBar = createToolBar();
        
        // 레이아웃 구성
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        
        root.setTop(topContainer);
        root.setBottom(createBottomPanel());
        root.setLeft(playlistView);
        root.setCenter(lyricsView);
        
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
        Menu fileMenu = new Menu("파일");
        MenuItem openFile = new MenuItem("파일 열기");
        MenuItem openFolder = new MenuItem("폴더 열기");
        MenuItem scanLibrary = new MenuItem("라이브러리 스캔");
        MenuItem exit = new MenuItem("종료");
        
        // 파일 열기
        openFile.setOnAction(e -> {
            List<File> files = UIUtils.showMusicFileChooser(this);
            if (files != null && !files.isEmpty()) {
                files.forEach(file -> {
                    eventPublisher.publish(new FileScanEvent.FileFoundEvent(file));
                });
                UIUtils.showInfo("파일 추가", files.size() + "개의 파일이 추가되었습니다.");
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
        
        // 종료 메뉴 클릭 시에도 안전한 종료 이벤트 발행
        exit.setOnAction(e -> {
            if (windowStateManager != null) {
                windowStateManager.handleCloseRequest(null);
            }
        });
        
        fileMenu.getItems().addAll(openFile, openFolder, new SeparatorMenuItem(), 
                                  scanLibrary, new SeparatorMenuItem(), exit);
        
        // 재생 메뉴
        Menu playMenu = new Menu("재생");
        MenuItem play = new MenuItem("재생");
        MenuItem pause = new MenuItem("일시정지");
        MenuItem stop = new MenuItem("정지");
        MenuItem next = new MenuItem("다음 곡");
        MenuItem previous = new MenuItem("이전 곡");
        
        // 재생 메뉴 이벤트 연결
        play.setOnAction(e -> playbackController.requestPlay());
        pause.setOnAction(e -> playbackController.requestPause());
        stop.setOnAction(e -> playbackController.requestStop());
        next.setOnAction(e -> playbackController.requestNext());
        previous.setOnAction(e -> playbackController.requestPrevious());
        
        playMenu.getItems().addAll(play, pause, stop, new SeparatorMenuItem(), previous, next);
        
        // 보기 메뉴
        Menu viewMenu = new Menu("보기");
        CheckMenuItem showLyrics = new CheckMenuItem("가사 표시");
        CheckMenuItem showPlaylist = new CheckMenuItem("플레이리스트 표시");
        CheckMenuItem alwaysOnTop = new CheckMenuItem("항상 위에");
        
        showLyrics.setSelected(true);
        showPlaylist.setSelected(true);
        
        showLyrics.setOnAction(e -> lyricsView.setVisible(showLyrics.isSelected()));
        showPlaylist.setOnAction(e -> playlistView.setVisible(showPlaylist.isSelected()));
        alwaysOnTop.setOnAction(e -> setAlwaysOnTop(alwaysOnTop.isSelected()));
        
        viewMenu.getItems().addAll(showLyrics, showPlaylist, new SeparatorMenuItem(), alwaysOnTop);
        
        // 도움말 메뉴
        Menu helpMenu = new Menu("도움말");
        MenuItem about = new MenuItem("정보");
        MenuItem shortcuts = new MenuItem("키보드 단축키");
        
        about.setOnAction(e -> showAboutDialog());
        shortcuts.setOnAction(e -> showShortcutsDialog());
        
        helpMenu.getItems().addAll(shortcuts, new SeparatorMenuItem(), about);
        
        menuBar.getMenus().addAll(fileMenu, playMenu, viewMenu, helpMenu);
        return menuBar;
    }
    
    private HBox createToolBar() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(5));
        toolBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        Button scanButton = new Button("📁 스캔");
        Button refreshButton = new Button("🔄 새로고침");
        
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
            statusLabel.setText("플레이리스트 새로고침됨");
        });
        
        // 구분선
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        toolBar.getChildren().addAll(scanButton, refreshButton, separator, scanProgressBar, statusLabel);
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

    // ========== UI 업데이트 메서드들 ==========
    
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            setTitle("SyncTune - " + music.getTitle() + " - " + music.getArtist());
            statusBarView.updateCurrentMusic(music);
        }
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
        statusLabel.setText("스캔 시작: " + getShortPath(directoryPath));
        scanProgressBar.setProgress(0);
        scanProgressBar.setVisible(true);
    }
    
    public void updateScanProgress(String currentFileName) {
        statusLabel.setText("스캔 중: " + currentFileName);
    }
    
    public void showScanCompleted(int totalFiles) {
        statusLabel.setText("스캔 완료: " + totalFiles + "개 파일 발견");
        scanProgressBar.setVisible(false);
        UIUtils.showInfo("스캔 완료", totalFiles + "개의 파일을 발견했습니다.");
    }
    
    public void showMediaScanStarted(String directoryPath) {
        statusLabel.setText("미디어 스캔 시작: " + getShortPath(directoryPath));
        scanProgressBar.setProgress(0);
        scanProgressBar.setVisible(true);
    }
    
    public void updateMediaScanProgress(double progress, int scanned, int total) {
        scanProgressBar.setProgress(progress);
        statusLabel.setText(String.format("미디어 스캔 중: %d/%d (%.1f%%)", scanned, total, progress * 100));
    }
    
    public void showMediaScanCompleted(int totalMusic) {
        statusLabel.setText("미디어 스캔 완료: " + totalMusic + "곡 발견");
        scanProgressBar.setVisible(false);
        UIUtils.showInfo("미디어 스캔 완료", totalMusic + "곡이 라이브러리에 추가되었습니다.");
    }

    // ========== 가사 관련 메서드들 ==========
    
    public void showLyricsFound(String musicPath, String lrcPath) {
        statusLabel.setText("가사 파일 발견: " + getShortPath(lrcPath));
        lyricsView.showLyricsAvailable(true);
    }
    
    public void showLyricsNotFound(String musicPath) {
        statusLabel.setText("가사 파일 없음: " + getShortPath(musicPath));
        lyricsView.showLyricsAvailable(false);
        lyricsView.updateLyrics("가사를 찾을 수 없습니다");
    }
    
    public void showLyricsParseSuccess(String musicPath) {
        statusLabel.setText("가사 파싱 완료: " + getShortPath(musicPath));
    }
    
    public void showLyricsParseFailure(String musicPath) {
        statusLabel.setText("가사 파싱 실패: " + getShortPath(musicPath));
        UIUtils.showError("가사 오류", "가사 파일을 읽는 중 오류가 발생했습니다.");
    }

    // ========== 플레이리스트 관련 메서드들 ==========
    
    public void addPlaylistToUI(Playlist playlist) {
        playlistView.addPlaylist(playlist.getName());
        statusLabel.setText("플레이리스트 생성됨: " + playlist.getName());
    }
    
    public void removePlaylistFromUI(String playlistName) {
        playlistView.removePlaylist(playlistName);
        statusLabel.setText("플레이리스트 삭제됨: " + playlistName);
    }
    
    public void addMusicToPlaylistUI(String playlistName, MusicInfo music) {
        playlistView.addMusicToPlaylist(playlistName, music);
        statusLabel.setText("곡 추가됨: " + music.getTitle() + " → " + playlistName);
    }
    
    public void removeMusicFromPlaylistUI(String playlistName, MusicInfo music) {
        playlistView.removeMusicFromPlaylist(playlistName, music);
        statusLabel.setText("곡 제거됨: " + music.getTitle() + " ← " + playlistName);
    }
    
    public void updatePlaylistOrder(Playlist playlist) {
        playlistView.updatePlaylistOrder(playlist);
        statusLabel.setText("플레이리스트 순서 변경됨: " + playlist.getName());
    }
    
    public void loadAllPlaylists(List<Playlist> playlists) {
        playlistView.loadPlaylists(playlists);
        statusLabel.setText("플레이리스트 로드됨: " + playlists.size() + "개");
    }

    // ========== 라이브러리 관련 메서드들 ==========
    
    public void updateMusicLibrary(List<MusicInfo> musicList) {
        playlistView.updateMusicLibrary(musicList);
        statusLabel.setText("음악 라이브러리 업데이트됨: " + musicList.size() + "곡");
    }
    
    public void updateMusicMetadata(MusicInfo music) {
        playlistView.updateMusicMetadata(music);
        statusLabel.setText("메타데이터 업데이트됨: " + music.getTitle());
    }

    // ========== 애플리케이션 상태 관련 메서드들 ==========
    
    public void showApplicationReady() {
        statusLabel.setText("SyncTune 준비 완료");
        UIUtils.showInfo("시작 완료", "SyncTune이 성공적으로 시작되었습니다!");
    }
    
    public void showShutdownMessage() {
        statusLabel.setText("종료 중...");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

    // ========== 다이얼로그 메서드들 ==========
    
    private void showAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("SyncTune 정보");
        about.setHeaderText("SyncTune Player v1.0");
        about.setContentText(
            "고급 음악 플레이어 및 가사 동기화 소프트웨어\n\n" +
            "기능:\n" +
            "• 다양한 오디오 형식 지원\n" +
            "• LRC 가사 파일 동기화\n" +
            "• 플레이리스트 관리\n" +
            "• 모듈형 아키텍처\n\n" +
            "개발: SyncTune Team"
        );
        about.showAndWait();
    }
    
    private void showShortcutsDialog() {
        Alert shortcuts = new Alert(Alert.AlertType.INFORMATION);
        shortcuts.setTitle("키보드 단축키");
        shortcuts.setHeaderText("사용 가능한 키보드 단축키");
        shortcuts.setContentText(
            "재생 제어:\n" +
            "Space - 재생/일시정지\n" +
            "Ctrl+S - 정지\n" +
            "Ctrl+→ - 다음 곡\n" +
            "Ctrl+← - 이전 곡\n\n" +
            "파일 관리:\n" +
            "Ctrl+O - 파일 열기\n" +
            "Ctrl+Shift+O - 폴더 열기\n" +
            "F5 - 라이브러리 새로고침\n\n" +
            "창 관리:\n" +
            "F11 - 전체화면\n" +
            "Ctrl+M - 최소화\n" +
            "Ctrl+Q - 종료"
        );
        shortcuts.showAndWait();
    }

    // ========== 유틸리티 메서드들 ==========
    
    private String getShortPath(String fullPath) {
        if (fullPath == null || fullPath.length() <= 50) {
            return fullPath;
        }
        return "..." + fullPath.substring(fullPath.length() - 47);
    }
    
    public void setStatusText(String text) {
        statusLabel.setText(text);
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

    /**
     * UIModule.stop()에서 호출되는 강제 종료 메서드
     */
    public void forceClose() {
        if (windowStateManager != null) {
            windowStateManager.forceClose();
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