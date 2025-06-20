package ac.cwnu.synctune.ui;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.FileScanEvent;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.event.MediaInfoEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.ui.util.UIUtils;
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

    // ========== System Events ==========
    @EventListener
    public void onApplicationReady(SystemEvent.ApplicationReadyEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showApplicationReady();
            }
        });
        log.info("애플리케이션이 준비되었습니다.");
    }

    @EventListener
    public void onApplicationShutdown(SystemEvent.ApplicationShutdownEvent event) {
        log.info("ApplicationShutdownEvent를 수신했습니다. UI 종료를 준비합니다.");
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showShutdownMessage();
            }
        });
    }

    // ========== Error Events ==========
    @EventListener
    public void onError(ErrorEvent event) {
        Platform.runLater(() -> {
            if (event.isFatal()) {
                UIUtils.showError("치명적 오류", 
                    "치명적 오류가 발생했습니다: " + event.getMessage() + "\n애플리케이션이 종료됩니다.");
            } else {
                UIUtils.showError("오류", event.getMessage());
            }
        });
    }

    // ========== File Scan Events ==========
    @EventListener
    public void onFileScanStarted(FileScanEvent.ScanStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showScanStarted(event.getDirectoryPath());
            }
        });
        log.info("파일 스캔이 시작되었습니다: {}", event.getDirectoryPath());
    }

    @EventListener
    public void onFileFound(FileScanEvent.FileFoundEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateScanProgress(event.getFoundFile().getName());
            }
        });
    }

    @EventListener
    public void onFileScanCompleted(FileScanEvent.ScanCompletedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showScanCompleted(event.getTotalFilesFound());
            }
        });
        log.info("파일 스캔이 완료되었습니다. 총 {}개 파일 발견", event.getTotalFilesFound());
    }

    @EventListener
    public void onFileScanError(FileScanEvent.ScanErrorEvent event) {
        Platform.runLater(() -> {
            UIUtils.showError("스캔 오류", 
                "디렉토리 스캔 중 오류가 발생했습니다: " + event.getErrorMessage());
        });
    }

    // ========== Media Info Events ==========
    @EventListener
    public void onMediaScanStarted(MediaInfoEvent.MediaScanStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showMediaScanStarted(event.getDirectoryPath());
            }
        });
    }

    @EventListener
    public void onMediaScanProgress(MediaInfoEvent.MediaScanProgressEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                double progress = event.getTotalFiles() > 0 ? 
                    (double) event.getScannedFiles() / event.getTotalFiles() : 0.0;
                mainWindow.updateMediaScanProgress(progress, event.getScannedFiles(), event.getTotalFiles());
            }
        });
    }

    @EventListener
    public void onMediaScanCompleted(MediaInfoEvent.MediaScanCompletedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showMediaScanCompleted(event.getScannedMusicInfos().size());
                mainWindow.updateMusicLibrary(event.getScannedMusicInfos());
            }
        });
        log.info("미디어 스캔이 완료되었습니다. {}곡 발견", event.getScannedMusicInfos().size());
    }

    @EventListener
    public void onMetadataUpdated(MediaInfoEvent.MetadataUpdatedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateMusicMetadata(event.getUpdatedMusicInfo());
            }
        });
    }

    // ========== Playback Status Events ==========
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateCurrentMusic(event.getCurrentMusic());
                mainWindow.updatePlaybackState(true, false, false); // playing, not paused, not stopped
            }
        });
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaybackState(false, true, false); // not playing, paused, not stopped
            }
        });
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaybackState(false, false, true); // not playing, not paused, stopped
            }
        });
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateCurrentMusic(event.getNewMusic());
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

    // ========== Lyrics Events ==========
    @EventListener
    public void onLyricsFound(LyricsEvent.LyricsFoundEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showLyricsFound(event.getMusicFilePath(), event.getLrcFilePath());
            }
        });
        log.info("가사 파일을 찾았습니다: {}", event.getLrcFilePath());
    }

    @EventListener
    public void onLyricsNotFound(LyricsEvent.LyricsNotFoundEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showLyricsNotFound(event.getMusicFilePath());
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

    @EventListener
    public void onLyricsParseComplete(LyricsEvent.LyricsParseCompleteEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                if (event.isSuccess()) {
                    mainWindow.showLyricsParseSuccess(event.getMusicFilePath());
                } else {
                    mainWindow.showLyricsParseFailure(event.getMusicFilePath());
                }
            }
        });
    }

    // ========== Playlist Events ==========
    @EventListener
    public void onPlaylistCreated(PlaylistEvent.PlaylistCreatedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.addPlaylistToUI(event.getPlaylist());
            }
        });
        log.info("플레이리스트가 생성되었습니다: {}", event.getPlaylist().getName());
    }

    @EventListener
    public void onPlaylistDeleted(PlaylistEvent.PlaylistDeletedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.removePlaylistFromUI(event.getPlaylistName());
            }
        });
    }

    @EventListener
    public void onMusicAddedToPlaylist(PlaylistEvent.MusicAddedToPlaylistEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.addMusicToPlaylistUI(event.getPlaylistName(), event.getMusicInfo());
            }
        });
    }

    @EventListener
    public void onMusicRemovedFromPlaylist(PlaylistEvent.MusicRemovedFromPlaylistEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.removeMusicFromPlaylistUI(event.getPlaylistName(), event.getMusicInfo());
            }
        });
    }

    @EventListener
    public void onPlaylistOrderChanged(PlaylistEvent.PlaylistOrderChangedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaylistOrder(event.getPlaylist());
            }
        });
    }

    @EventListener
    public void onAllPlaylistsLoaded(PlaylistEvent.AllPlaylistsLoadedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.loadAllPlaylists(event.getPlaylists());
            }
        });
        log.info("모든 플레이리스트가 로드되었습니다. 총 {}개", event.getPlaylists().size());
    }

    // ========== Player UI Events ==========
    @EventListener
    public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
        log.info("다른 모듈에서 MainWindowClosed 이벤트를 받았습니다.");
        // UI 모듈에서는 자신이 발행한 이벤트를 받는 경우이므로 특별한 처리 없음
    }

    @EventListener
    public void onMainWindowRestored(PlayerUIEvent.MainWindowRestoredEvent event) {
        log.info("다른 모듈에서 MainWindowRestored 이벤트를 받았습니다.");
        // 필요시 다른 모듈들이 창 복원을 알 수 있도록 추가 UI 업데이트
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