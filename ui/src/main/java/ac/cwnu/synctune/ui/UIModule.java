package ac.cwnu.synctune.ui;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.*;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;

@Module(name = "UI", version = "1.0.0")
public class UIModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(UIModule.class);
    private static UIModule instance;
    private MainApplicationWindow mainWindow;
    private boolean isJavaFXInitialized = false;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        instance = this;
        log.info("UIModule이 시작되었습니다.");

        // JavaFX 플랫폼이 이미 실행 중인지 확인
        if (Platform.isFxApplicationThread()) {
            initializeUI();
        } else {
            // JavaFX 애플리케이션을 별도 스레드에서 시작
            Thread javafxThread = new Thread(() -> {
                try {
                    System.setProperty("javafx.platform.exitOnClose", "false");
                    Application.launch(JavaFXApp.class);
                } catch (IllegalStateException e) {
                    // 이미 JavaFX가 시작된 경우
                    log.warn("JavaFX가 이미 시작되었습니다. Platform.runLater로 UI 초기화를 시도합니다.");
                    Platform.runLater(this::initializeUI);
                } catch (Exception e) {
                    log.error("JavaFX 애플리케이션 시작 중 오류", e);
                }
            });
            javafxThread.setDaemon(false);
            javafxThread.start();
        }
    }

    private void initializeUI() {
        if (!isJavaFXInitialized) {
            try {
                mainWindow = new MainApplicationWindow(eventPublisher);
                mainWindow.show();
                isJavaFXInitialized = true;
                log.info("메인 윈도우가 성공적으로 표시되었습니다.");
            } catch (Exception e) {
                log.error("UI 초기화 중 오류 발생", e);
            }
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다.");
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.close();
            }
        });
    }

    // 이벤트 리스너들
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateCurrentMusic(event.getCurrentMusic());
            }
        });
    }

    @EventListener
    public void onPlaybackProgressUpdate(PlaybackStatusEvent.PlaybackProgressUpdateEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateProgress(event.getCurrentTimeMillis(), event.getTotalTimeMillis());
            }
        });
    }

    @EventListener
    public void onNextLyrics(LyricsEvent.NextLyricsEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateLyrics(event.getLyricLine());
            }
        });
    }

    public static UIModule getInstance() {
        return instance;
    }

    /**
     * JavaFX Application 클래스
     */
    public static class JavaFXApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            if (instance != null) {
                instance.initializeUI();
            }
        }
    }
}