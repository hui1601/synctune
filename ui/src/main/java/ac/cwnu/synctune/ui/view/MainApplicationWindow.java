package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * SyncTune의 메인 윈도우를 구성하는 JavaFX Stage 클래스
 */
public class MainApplicationWindow extends Stage {

    /**
     * @param publisher 이벤트 발행을 위한 EventPublisher
     */
    public MainApplicationWindow(EventPublisher publisher) {
        setTitle("SyncTune Player");
        initUI(publisher);
    }

    private void initUI(EventPublisher publisher) {
        // 최상위 레이아웃
        BorderPane root = new BorderPane();

        // 뷰 컴포넌트 생성
        PlayerControlsView controlsView = new PlayerControlsView();
        PlaylistView playlistView = new PlaylistView();
        LyricsView lyricsView = new LyricsView();

        // 하위 뷰 컴포넌트 배치
        root.setBottom(controlsView);
        root.setLeft(playlistView);
        root.setCenter(lyricsView);

        // 컨트롤러 초기화: 뷰와 퍼블리셔 연결
        new PlaybackController(controlsView, publisher);
        new PlaylistActionHandler(playlistView, publisher);
        new WindowStateManager(this, publisher);

        // 씬 설정
        Scene scene = new Scene(root, 1024, 768);
        setScene(scene);
    }
}
