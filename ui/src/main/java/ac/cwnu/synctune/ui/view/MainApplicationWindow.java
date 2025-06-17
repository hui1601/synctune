package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.controller.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
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
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        setScene(scene);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
        Menu fileMenu = new Menu("파일");
        MenuItem openFile = new MenuItem("파일 열기");
        MenuItem openFolder = new MenuItem("폴더 열기");
        MenuItem exit = new MenuItem("종료");
        exit.setOnAction(e -> close());
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
}