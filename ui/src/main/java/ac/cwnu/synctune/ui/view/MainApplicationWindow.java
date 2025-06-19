package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(1200);
        setHeight(800);
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
        MenuItem exit = new MenuItem("종료");
        
        // 종료 메뉴 클릭 시에도 안전한 종료 이벤트 발행
        exit.setOnAction(e -> {
            if (windowStateManager != null) {
                // WindowStateManager를 통해 안전한 종료 요청
                windowStateManager.handleCloseRequest(null);
            }
        });
        
        fileMenu.getItems().addAll(openFile, openFolder, exit);
        
        // 재생 메뉴
        Menu playMenu = new Menu("재생");
        MenuItem play = new MenuItem("재생");
        MenuItem pause = new MenuItem("일시정지");
        MenuItem stop = new MenuItem("정지");
        playMenu.getItems().addAll(play, pause, stop);
        
        // 도움말 메뉴
        Menu helpMenu = new Menu("도움말");
        MenuItem about = new MenuItem("정보");
        helpMenu.getItems().add(about);
        
        menuBar.getMenus().addAll(fileMenu, playMenu, helpMenu);
        return menuBar;
    }

    private void initControllers() {
        playbackController = new PlaybackController(controlsView, eventPublisher);
        playlistActionHandler = new PlaylistActionHandler(playlistView, eventPublisher);
        windowStateManager = new WindowStateManager(this, eventPublisher);
    }

    // UI 업데이트 메서드들
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            setTitle("SyncTune - " + music.getTitle() + " - " + music.getArtist());
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        controlsView.updateProgress(currentMs, totalMs);
    }

    public void updateLyrics(String lyrics) {
        lyricsView.updateLyrics(lyrics);
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