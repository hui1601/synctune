package ac.cwnu.synctune.ui;

import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Platform;
import org.slf4j.Logger;

/**
 * UI 모듈은 SyncTune의 사용자 인터페이스를 담당합니다.
 */
@Module(name = "UI", version = "1.0.0")
public class UIModule extends SyncTuneModule implements ModuleLifecycleListener {

    private static final Logger log = LogManager.getLogger(UIModule.class);
    private MainApplicationWindow mainWindow;

    @Override
    public void start(EventPublisher publisher) {
        this.eventPublisher = publisher;
        log.info("UIModule이 시작되었습니다.");

        // JavaFX 애플리케이션 실행
        Platform.startup(() -> {
            try {
                mainWindow = new MainApplicationWindow(publisher);
                mainWindow.show();
                log.info("메인 윈도우가 표시되었습니다.");
            } catch (Exception e) {
                log.error("UI 초기화 중 오류 발생", e);
            }
        });
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료되었습니다.");

        // JavaFX UI 종료 처리
        if (mainWindow != null) {
            Platform.runLater(() -> {
                mainWindow.close();
                Platform.exit();
            });
        }
    }

    @Override
    public String getModuleName() {
        return "UIModule";
    }
}
