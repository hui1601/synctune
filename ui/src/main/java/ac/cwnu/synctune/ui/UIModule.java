package ac.cwnu.synctune.ui;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

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

        // JavaFX 플랫폼 초기화를 별도 스레드에서 수행
        Thread javafxThread = new Thread(() -> {
            try {
                // JavaFX 플랫폼이 이미 실행 중인지 확인
                if (!Platform.isFxApplicationThread()) {
                    // JavaFX 애플리케이션 시작
                    System.setProperty("javafx.platform.exitOnClose", "false");
                    Application.launch(JavaFXApp.class);
                }
            } catch (IllegalStateException e) {
                // 이미 JavaFX가 시작된 경우
                log.info("JavaFX가 이미 시작되었습니다. Platform.runLater로 UI 초기화를 시도합니다.");
                Platform.runLater(this::initializeUI);
            } catch (Exception e) {
                log.error("JavaFX 애플리케이션 시작 중 오류", e);
            }
        });
        javafxThread.setName("JavaFX-Init-Thread");
        javafxThread.setDaemon(false); // 메인 스레드가 종료되어도 UI가 유지되도록
        javafxThread.start();
    }

    private void initializeUI() {
        if (!isJavaFXInitialized) {
            try {
                // Platform implicit exit를 false로 설정하여 창을 닫아도 즉시 종료되지 않도록 함
                Platform.setImplicitExit(false);
                
                mainWindow = new MainApplicationWindow(eventPublisher);
                mainWindow.show();
                isJavaFXInitialized = true;
                log.info("메인 윈도우가 성공적으로 표시되었습니다. (ImplicitExit=false)");
            } catch (Exception e) {
                log.error("UI 초기화 중 오류 발생", e);
            }
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다. Core로부터 종료 신호를 받았습니다.");
        
        if (Platform.isFxApplicationThread()) {
            // UI 스레드에서 호출된 경우
            cleanupAndExit();
        } else {
            // 다른 스레드에서 호출된 경우 UI 스레드에서 실행
            Platform.runLater(this::cleanupAndExit);
        }
    }

    private void cleanupAndExit() {
        try {
            // 메인 윈도우 정리
            if (mainWindow != null) {
                log.debug("메인 윈도우를 닫습니다.");
                mainWindow.close();
            }
            
            log.info("Platform.exit()를 호출하여 JavaFX 애플리케이션을 종료합니다.");
            Platform.exit();
            
        } catch (Exception e) {
            log.error("UI 정리 및 종료 중 오류 발생", e);
        }
    }

    // SystemEvent.ApplicationShutdownEvent 리스너 추가
    @EventListener
    public void onApplicationShutdown(SystemEvent.ApplicationShutdownEvent event) {
        log.info("ApplicationShutdownEvent를 수신했습니다. UI 종료를 준비합니다.");
        // Core에서 전체 애플리케이션 종료를 알릴 때 받는 이벤트
        // 실제 정리는 stop() 메서드에서 수행
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