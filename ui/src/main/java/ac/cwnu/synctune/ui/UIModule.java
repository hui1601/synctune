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
    private boolean isJavaFXInitialized = false;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        instance = this;
        log.info("UIModule이 시작되었습니다.");

        // JavaFX 애플리케이션 초기화
        initializeJavaFX();
    }

    private void initializeJavaFX() {
        // JavaFX 플랫폼이 이미 실행 중인지 확인
        try {
            if (Platform.isFxApplicationThread()) {
                log.info("이미 JavaFX 스레드에서 실행 중입니다. 즉시 UI를 초기화합니다.");
                initializeUI();
                return;
            }
        } catch (IllegalStateException e) {
            log.debug("JavaFX Platform이 아직 초기화되지 않음");
        }

        // JavaFX 애플리케이션을 별도 스레드에서 시작
        Thread javafxThread = new Thread(() -> {
            try {
                log.info("JavaFX 애플리케이션을 시작합니다...");
                System.setProperty("javafx.platform.exitOnClose", "false");
                Application.launch(JavaFXApp.class);
            } catch (IllegalStateException e) {
                log.info("JavaFX가 이미 시작되었습니다. Platform.runLater로 UI 초기화를 시도합니다.");
                Platform.runLater(() -> {
                    try {
                        initializeUI();
                    } catch (Exception ex) {
                        log.error("UI 초기화 중 오류 발생", ex);
                    }
                });
            } catch (Exception e) {
                log.error("JavaFX 애플리케이션 시작 중 오류", e);
            }
        });
        javafxThread.setName("JavaFX-Init-Thread");
        javafxThread.setDaemon(false);
        javafxThread.start();
    }

    private void initializeUI() {
        if (isJavaFXInitialized) {
            log.debug("UI가 이미 초기화되었습니다.");
            return;
        }
        
        try {
            log.info("UI 초기화를 시작합니다...");
            
            // Platform exit를 false로 설정하여 창을 닫아도 즉시 종료되지 않도록 함
            Platform.setImplicitExit(false);
            
            mainWindow = new MainApplicationWindow(eventPublisher);
            mainWindow.show();
            isJavaFXInitialized = true;
            
            log.info("메인 윈도우가 성공적으로 표시되었습니다.");
            
        } catch (Exception e) {
            log.error("UI 초기화 중 오류 발생", e);
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다.");
        
        if (Platform.isFxApplicationThread()) {
            cleanupAndExit();
        } else {
            Platform.runLater(this::cleanupAndExit);
        }
    }

    private void cleanupAndExit() {
        try {
            if (mainWindow != null) {
                log.debug("메인 윈도우를 닫습니다.");
                mainWindow.forceClose();
            }
            
            log.info("Platform.exit()를 호출하여 JavaFX 애플리케이션을 종료합니다.");
            Platform.exit();
            
        } catch (Exception e) {
            log.error("UI 정리 및 종료 중 오류 발생", e);
        }
    }

    // 시스템 이벤트 리스너
    @EventListener
    public void onApplicationShutdown(SystemEvent.ApplicationShutdownEvent event) {
        log.info("ApplicationShutdownEvent를 수신했습니다. UI 종료를 준비합니다.");
    }

    // 재생 상태 이벤트들을 UI에 전달
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.debug("PlaybackStartedEvent 수신: {}", event.getCurrentMusic().getTitle());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateCurrentMusic(event.getCurrentMusic());
                } catch (Exception e) {
                    log.error("PlaybackStartedEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.debug("PlaybackPausedEvent 수신");
        // 필요시 UI 상태 업데이트
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.debug("PlaybackStoppedEvent 수신");
        // 필요시 UI 상태 업데이트
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        log.debug("MusicChangedEvent 수신: {}", event.getNewMusic().getTitle());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateCurrentMusic(event.getNewMusic());
                } catch (Exception e) {
                    log.error("MusicChangedEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackProgressUpdate(PlaybackStatusEvent.PlaybackProgressUpdateEvent event) {
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateProgress(event.getCurrentTimeMillis(), event.getTotalTimeMillis());
                } catch (Exception e) {
                    log.error("PlaybackProgressUpdateEvent 처리 중 오류", e);
                }
            });
        }
    }

    // 가사 이벤트 처리
    @EventListener
    public void onNextLyrics(LyricsEvent.NextLyricsEvent event) {
        log.debug("NextLyricsEvent 수신: {}", event.getLyricLine());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateLyrics(event.getLyricLine());
                } catch (Exception e) {
                    log.error("NextLyricsEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onLyricsFound(LyricsEvent.LyricsFoundEvent event) {
        log.info("가사 파일 발견: {}", event.getLrcFilePath());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.showLyricsFound(event.getLrcFilePath());
                } catch (Exception e) {
                    log.error("LyricsFoundEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onLyricsNotFound(LyricsEvent.LyricsNotFoundEvent event) {
        log.info("가사 파일을 찾을 수 없음: {}", event.getMusicFilePath());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.showLyricsNotFound();
                } catch (Exception e) {
                    log.error("LyricsNotFoundEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onLyricsParseComplete(LyricsEvent.LyricsParseCompleteEvent event) {
        log.debug("가사 파싱 완료: {} (성공: {})", event.getMusicFilePath(), event.isSuccess());
        // 파싱 완료 상태는 별도 UI 업데이트 없이 로그만 남김
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
            // primaryStage는 사용하지 않고, UIModule에서 직접 MainApplicationWindow를 생성
            if (instance != null) {
                instance.initializeUI();
            }
        }
        
        @Override
        public void init() throws Exception {
            super.init();
            log.debug("JavaFX Application 초기화됨");
        }
        
        @Override
        public void stop() throws Exception {
            super.stop();
            log.debug("JavaFX Application 종료됨");
        }
    }
}