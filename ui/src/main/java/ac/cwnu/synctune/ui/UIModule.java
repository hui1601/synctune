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
    private volatile boolean javaFXReady = false;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        instance = this;
        log.info("UIModule이 시작되었습니다.");

        // 🔧 핵심: JavaFX 초기화를 메인 스레드에서 실행하고 대기
        initializeJavaFXSync();
    }

    @EventListener
    public void onLyricsFullText(LyricsEvent.LyricsFullTextEvent event) {
        log.info("전체 가사(FullText) 이벤트 수신: {}줄", event.getFullLyricsLines().size());
        if (mainWindow != null) {
            Platform.runLater(() -> mainWindow.setFullLyrics(event.getFullLyricsLines()));
        }
    }

    private void initializeJavaFXSync() {
        try {
            // JavaFX 툴킷이 이미 초기화되었는지 확인
            if (isJavaFXInitialized()) {
                log.info("JavaFX가 이미 초기화되어 있습니다.");
                createMainWindow();
                return;
            }

            log.info("JavaFX 초기화를 시작합니다...");
            
            // 🔧 중요: Non-daemon 스레드로 JavaFX 실행
            Thread javafxThread = new Thread(() -> {
                System.setProperty("javafx.application.Thread.currentThread.setName", "JavaFX-Application-Thread");
                Application.launch(SimpleJavaFXApp.class);
            });
            javafxThread.setName("JavaFX-Launcher");
            javafxThread.setDaemon(false); // 🔧 Non-daemon으로 설정
            javafxThread.start();

            // JavaFX 초기화 완료까지 대기
            waitForJavaFXReady();
            
        } catch (Exception e) {
            log.error("JavaFX 초기화 실패", e);
        }
    }

    private boolean isJavaFXInitialized() {
        try {
            // Platform.runLater()를 호출해서 예외가 발생하지 않으면 초기화됨
            Platform.runLater(() -> {});
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void waitForJavaFXReady() {
        int attempts = 0;
        int maxAttempts = 100; // 10초 대기
        
        while (!javaFXReady && attempts < maxAttempts) {
            try {
                Thread.sleep(100);
                attempts++;
                log.debug("JavaFX 초기화 대기 중... ({}/{})", attempts, maxAttempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("JavaFX 초기화 대기 중 인터럽트됨", e);
                break;
            }
        }
        
        if (!javaFXReady) {
            log.error("JavaFX 초기화 시간 초과!");
        } else {
            log.info("JavaFX 초기화 완료!");
        }
    }

    private void createMainWindow() {
        Platform.runLater(() -> {
            try {
                // 🔧 매우 중요: ImplicitExit를 false로 설정
                Platform.setImplicitExit(false);
                
                mainWindow = new MainApplicationWindow(eventPublisher);
                mainWindow.show();
                javaFXReady = true;
                
                log.info("메인 윈도우가 성공적으로 표시되었습니다.");
                
                
            } catch (Exception e) {
                log.error("UI 초기화 중 오류 발생", e);
            }
        });
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다.");
        if (mainWindow != null) {
            Platform.runLater(() -> {
                mainWindow.forceClose();
                // JavaFX 플랫폼도 종료
                Platform.exit();
            });
        }
    }

    // 🔧 이벤트 리스너들을 UIModule에서 직접 처리
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.info("재생 시작: {}", event.getCurrentMusic().getTitle());
        if (mainWindow != null) {
            Platform.runLater(() -> {
                mainWindow.updateCurrentMusic(event.getCurrentMusic());
                // 버튼 상태 업데이트
                if (mainWindow.getControlsView() != null) {
                    mainWindow.getControlsView().setPlaybackState(true, false);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.info("일시정지됨");
        if (mainWindow != null) {
            Platform.runLater(() -> {
                if (mainWindow.getControlsView() != null) {
                    mainWindow.getControlsView().setPlaybackState(false, true);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.info("정지됨");
        if (mainWindow != null) {
            Platform.runLater(() -> {
                if (mainWindow.getControlsView() != null) {
                    mainWindow.getControlsView().setPlaybackState(false, false);
                }
            });
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
            Platform.runLater(() -> {
                int currentIndex = mainWindow.findCurrentLyricIndex(event.getLyricLine(), event.getStartTimeMillis());
                log.info("findCurrentLyricIndex: lyric='{}', timestamp={}, index={}",
                     event.getLyricLine(), event.getStartTimeMillis(), currentIndex);
                mainWindow.updateLyrics(event.getLyricLine(), currentIndex);
            });
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
     * 🔧 수정된 JavaFX Application 클래스
     */
    public static class SimpleJavaFXApp extends Application {
        
        @Override
        public void init() throws Exception {
            super.init();
            log.info("JavaFX Application.init() 호출됨");
        }
        
        @Override
        public void start(Stage primaryStage) throws Exception {
            log.info("JavaFX Application.start() 호출됨");
            
            // 🔧 매우 중요: ImplicitExit를 여기서도 설정
            Platform.setImplicitExit(false);
            
            if (instance != null) {
                instance.createMainWindow();
            } else {
                log.error("UIModule instance가 null입니다!");
            }
        }
        
        @Override
        public void stop() throws Exception {
            log.info("JavaFX Application.stop() 호출됨");
            super.stop();
        }
    }
}