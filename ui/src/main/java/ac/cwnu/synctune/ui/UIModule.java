package ac.cwnu.synctune.ui;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

@Module(name = "UI", version = "1.0.0")
public class UIModule extends SyncTuneModule {
    private static final Logger log = LogManager.getLogger(UIModule.class);
    private static UIModule instance;
    private MainApplicationWindow mainWindow;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        instance = this;
        log.info("UIModule이 시작되었습니다.");

        // 간소화된 JavaFX 초기화
        initializeJavaFX();
    }

    private void initializeJavaFX() {
        // JavaFX 툴킷이 이미 초기화되었는지 확인
        try {
            Platform.runLater(() -> createMainWindow());
        } catch (IllegalStateException e) {
            // JavaFX가 아직 초기화되지 않은 경우
            Thread javafxThread = new Thread(() -> {
                Application.launch(SimpleJavaFXApp.class);
            });
            javafxThread.setName("JavaFX-Thread");
            javafxThread.setDaemon(false);
            javafxThread.start();
        }
    }

    private void createMainWindow() {
        try {
            Platform.setImplicitExit(false);
            mainWindow = new MainApplicationWindow(eventPublisher);
            mainWindow.show();
            log.info("메인 윈도우가 성공적으로 표시되었습니다.");
        } catch (Exception e) {
            log.error("UI 초기화 중 오류 발생", e);
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다.");
        if (mainWindow != null) {
            Platform.runLater(() -> {
                mainWindow.forceClose();
                Platform.exit();
            });
        }
    }

    // 이벤트 리스너들 - 간소화
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.info("재생 시작: {}", event.getCurrentMusic().getTitle());
        if (mainWindow != null) {
            Platform.runLater(() -> mainWindow.updateCurrentMusic(event.getCurrentMusic()));
        }
    }

    @EventListener
    public void onPlaybackProgressUpdate(PlaybackStatusEvent.PlaybackProgressUpdateEvent event) {
        if (mainWindow != null) {
            Platform.runLater(() -> mainWindow.updateProgress(event.getCurrentTimeMillis(), event.getTotalTimeMillis()));
        }
    }

    @EventListener
    public void onNextLyrics(LyricsEvent.NextLyricsEvent event) {
        log.info("가사 업데이트: {}", event.getLyricLine());
        if (mainWindow != null) {
            Platform.runLater(() -> mainWindow.updateLyrics(event.getLyricLine()));
        }
    }

    @EventListener
    public void onApplicationShutdown(SystemEvent.ApplicationShutdownEvent event) {
        log.info("ApplicationShutdownEvent를 수신했습니다. UI 종료를 준비합니다.");
    }

    public static UIModule getInstance() {
        return instance;
    }

    /**
     * 간소화된 JavaFX Application 클래스
     */
    public static class SimpleJavaFXApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            if (instance != null) {
                instance.createMainWindow();
            }
        }
    }
}