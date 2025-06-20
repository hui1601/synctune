package ac.cwnu.synctune.ui;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.FileScanEvent;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
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
import javafx.concurrent.Task;

@Module(name = "UI", version = "1.0.0")
public class UIModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(UIModule.class);
    private static UIModule instance;
    private MainApplicationWindow mainWindow;
    private boolean isJavaFXInitialized = false;
    private boolean isShuttingDown = false;

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
                publish(new ErrorEvent("JavaFX 초기화 실패: " + e.getMessage(), e, false));
            }
        });
        javafxThread.setName("JavaFX-Init-Thread");
        javafxThread.setDaemon(false); // 메인 스레드가 종료되어도 UI가 유지되도록
        javafxThread.start();
    }

    private void initializeUI() {
        if (!isJavaFXInitialized && !isShuttingDown) {
            try {
                // Platform implicit exit를 false로 설정하여 창을 닫아도 즉시 종료되지 않도록 함
                Platform.setImplicitExit(false);
                
                mainWindow = new MainApplicationWindow(eventPublisher);
                mainWindow.show();
                isJavaFXInitialized = true;
                log.info("메인 윈도우가 성공적으로 표시되었습니다. (ImplicitExit=false)");
                
                // UI 준비 완료 이벤트 발행
                publish(new PlayerUIEvent.MainWindowRestoredEvent());
                
            } catch (Exception e) {
                log.error("UI 초기화 중 오류 발생", e);
                publish(new ErrorEvent("UI 초기화 실패: " + e.getMessage(), e, false));
            }
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다. Core로부터 종료 신호를 받았습니다.");
        isShuttingDown = true;
        
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
            // 진행 중인 작업들 정리
            if (mainWindow != null) {
                log.debug("메인 윈도우를 닫습니다.");
                mainWindow.cleanup();
                mainWindow.forceClose();
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
                UIUtils.showToast(mainWindow, "SyncTune이 준비되었습니다!", 3000);
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
                UIUtils.showProgressDialog("종료 중", "안전한 종료를 진행하고 있습니다...");
            }
        });
    }

    // ========== Error Events ==========
    @EventListener
    public void onError(ErrorEvent event) {
        Platform.runLater(() -> {
            if (event.isFatal()) {
                UIUtils.showDetailedError("치명적 오류", 
                    "치명적 오류가 발생했습니다: " + event.getMessage() + "\n애플리케이션이 종료됩니다.",
                    event.getException() instanceof Exception ? (Exception)event.getException() : null);
            } else {
                UIUtils.showError("오류", event.getMessage());
                if (mainWindow != null) {
                    mainWindow.setStatusText("오류: " + event.getMessage());
                }
            }
        });
    }

    // ========== File Scan Events ==========
    @EventListener
    public void onFileScanStarted(FileScanEvent.ScanStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showScanStarted(event.getDirectoryPath());
                UIUtils.showProgressDialog("파일 스캔", "파일을 스캔하고 있습니다...");
            }
        });
        log.info("파일 스캔이 시작되었습니다: {}", event.getDirectoryPath());
    }

    @EventListener
    public void onFileFound(FileScanEvent.FileFoundEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateScanProgress(event.getFoundFile().getName());
                UIUtils.updateProgressDialog("파일 발견: " + event.getFoundFile().getName());
            }
        });
    }

    @EventListener
    public void onFileScanCompleted(FileScanEvent.ScanCompletedEvent event) {
        Platform.runLater(() -> {
            UIUtils.hideProgressDialog();
            if (mainWindow != null) {
                mainWindow.showScanCompleted(event.getTotalFilesFound());
            }
            UIUtils.showTaskCompleted("파일 스캔", event.getTotalFilesFound());
        });
        log.info("파일 스캔이 완료되었습니다. 총 {}개 파일 발견", event.getTotalFilesFound());
    }

    @EventListener
    public void onFileScanError(FileScanEvent.ScanErrorEvent event) {
        Platform.runLater(() -> {
            UIUtils.hideProgressDialog();
            UIUtils.showError("스캔 오류", 
                "디렉토리 스캔 중 오류가 발생했습니다: " + event.getErrorMessage());
            if (mainWindow != null) {
                mainWindow.setStatusText("스캔 오류: " + event.getErrorMessage());
            }
        });
    }

    // ========== Media Info Events ==========
    @EventListener
    public void onMediaScanStarted(MediaInfoEvent.MediaScanStartedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.showMediaScanStarted(event.getDirectoryPath());
                UIUtils.showProgressDialog("미디어 스캔", "미디어 파일을 분석하고 있습니다...");
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
                
                String progressText = String.format("진행률: %d/%d (%.1f%%)", 
                    event.getScannedFiles(), event.getTotalFiles(), progress * 100);
                UIUtils.updateProgressDialog(progressText);
            }
        });
    }

    @EventListener
    public void onMediaScanCompleted(MediaInfoEvent.MediaScanCompletedEvent event) {
        Platform.runLater(() -> {
            UIUtils.hideProgressDialog();
            if (mainWindow != null) {
                mainWindow.showMediaScanCompleted(event.getScannedMusicInfos().size());
                mainWindow.updateMusicLibrary(event.getScannedMusicInfos());
            }
            UIUtils.showTaskCompleted("미디어 스캔", event.getScannedMusicInfos().size());
        });
        log.info("미디어 스캔이 완료되었습니다. {}곡 발견", event.getScannedMusicInfos().size());
    }

    @EventListener
    public void onMetadataUpdated(MediaInfoEvent.MetadataUpdatedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateMusicMetadata(event.getUpdatedMusicInfo());
                String message = "메타데이터 업데이트: " + event.getUpdatedMusicInfo().getTitle();
                mainWindow.setStatusText(message);
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
                
                if (event.getCurrentMusic() != null) {
                    String message = "재생 시작: " + event.getCurrentMusic().getTitle();
                    mainWindow.setStatusText(message);
                    UIUtils.showToast(mainWindow, message, 2000);
                }
            }
        });
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaybackState(false, true, false); // not playing, paused, not stopped
                mainWindow.setStatusText("재생 일시정지됨");
            }
        });
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaybackState(false, false, true); // not playing, not paused, stopped
                mainWindow.setStatusText("재생 정지됨");
            }
        });
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updateCurrentMusic(event.getNewMusic());
                
                if (event.getNewMusic() != null) {
                    String message = "곡 변경: " + event.getNewMusic().getTitle();
                    mainWindow.setStatusText(message);
                    UIUtils.showToast(mainWindow, message, 2000);
                }
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
                String message = "가사 파일 발견: " + UIUtils.getFileNameWithoutExtension(event.getLrcFilePath());
                UIUtils.showToast(mainWindow, message, 2000);
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
                    UIUtils.showToast(mainWindow, "가사 파일 로드 완료", 1500);
                } else {
                    mainWindow.showLyricsParseFailure(event.getMusicFilePath());
                    UIUtils.showWarning("가사 오류", "가사 파일을 읽는 중 오류가 발생했습니다.");
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
                String message = "플레이리스트 생성: " + event.getPlaylist().getName();
                UIUtils.showToast(mainWindow, message, 2000);
            }
        });
        log.info("플레이리스트가 생성되었습니다: {}", event.getPlaylist().getName());
    }

    @EventListener
    public void onPlaylistDeleted(PlaylistEvent.PlaylistDeletedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.removePlaylistFromUI(event.getPlaylistName());
                String message = "플레이리스트 삭제: " + event.getPlaylistName();
                UIUtils.showToast(mainWindow, message, 2000);
            }
        });
    }

    @EventListener
    public void onMusicAddedToPlaylist(PlaylistEvent.MusicAddedToPlaylistEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.addMusicToPlaylistUI(event.getPlaylistName(), event.getMusicInfo());
                String message = String.format("'%s'를 '%s'에 추가", 
                    event.getMusicInfo().getTitle(), event.getPlaylistName());
                UIUtils.showToast(mainWindow, message, 2000);
            }
        });
    }

    @EventListener
    public void onMusicRemovedFromPlaylist(PlaylistEvent.MusicRemovedFromPlaylistEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.removeMusicFromPlaylistUI(event.getPlaylistName(), event.getMusicInfo());
                String message = String.format("'%s'를 '%s'에서 제거", 
                    event.getMusicInfo().getTitle(), event.getPlaylistName());
                UIUtils.showToast(mainWindow, message, 2000);
            }
        });
    }

    @EventListener
    public void onPlaylistOrderChanged(PlaylistEvent.PlaylistOrderChangedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.updatePlaylistOrder(event.getPlaylist());
                String message = "플레이리스트 순서 변경: " + event.getPlaylist().getName();
                UIUtils.showToast(mainWindow, message, 1500);
            }
        });
    }

    @EventListener
    public void onAllPlaylistsLoaded(PlaylistEvent.AllPlaylistsLoadedEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.loadAllPlaylists(event.getPlaylists());
                String message = "플레이리스트 " + event.getPlaylists().size() + "개 로드됨";
                UIUtils.showToast(mainWindow, message, 2000);
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
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText("창이 복원되었습니다");
            }
        });
    }

    // ========== Media Control Events (UI가 발행한 것들에 대한 피드백) ==========
    @EventListener
    public void onRequestPlay(MediaControlEvent.RequestPlayEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                if (event.getMusicToPlay() != null) {
                    mainWindow.setStatusText("재생 요청: " + event.getMusicToPlay().getTitle());
                } else {
                    mainWindow.setStatusText("재생 요청됨");
                }
            }
        });
    }

    @EventListener
    public void onRequestPause(MediaControlEvent.RequestPauseEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText("일시정지 요청됨");
            }
        });
    }

    @EventListener
    public void onRequestStop(MediaControlEvent.RequestStopEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText("정지 요청됨");
            }
        });
    }

    @EventListener
    public void onRequestNext(MediaControlEvent.RequestNextMusicEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText("다음 곡 요청됨");
            }
        });
    }

    @EventListener
    public void onRequestPrevious(MediaControlEvent.RequestPreviousMusicEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText("이전 곡 요청됨");
            }
        });
    }

    @EventListener
    public void onRequestSeek(MediaControlEvent.RequestSeekEvent event) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                String timeStr = UIUtils.formatTime(event.getPositionMillis());
                mainWindow.setStatusText("탐색 요청: " + timeStr);
            }
        });
    }

    // ========== 공개 API ==========
    public static UIModule getInstance() {
        return instance;
    }

    public boolean isUIReady() {
        return isJavaFXInitialized && mainWindow != null;
    }

    public void showMessage(String title, String message) {
        Platform.runLater(() -> {
            UIUtils.showInfo(title, message);
        });
    }

    public void showWarning(String title, String message) {
        Platform.runLater(() -> {
            UIUtils.showWarning(title, message);
        });
    }

    public void showError(String title, String message) {
        Platform.runLater(() -> {
            UIUtils.showError(title, message);
        });
    }

    public void updateStatusMessage(String message) {
        Platform.runLater(() -> {
            if (mainWindow != null) {
                mainWindow.setStatusText(message);
            }
        });
    }

    /**
     * 비동기 작업을 UI 스레드에서 안전하게 실행
     */
    public void runAsync(Runnable task) {
        Task<Void> asyncTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                task.run();
                return null;
            }
        };
        
        asyncTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                if (mainWindow != null) {
                    mainWindow.setStatusText("작업 완료");
                }
            });
        });
        
        asyncTask.setOnFailed(e -> {
            Throwable exception = asyncTask.getException();
            Platform.runLater(() -> {
                if (mainWindow != null) {
                    mainWindow.setStatusText("작업 실패: " + exception.getMessage());
                }
                UIUtils.showError("작업 오류", "비동기 작업 중 오류가 발생했습니다: " + exception.getMessage());
            });
        });
        
        Thread asyncThread = new Thread(asyncTask);
        asyncThread.setDaemon(true);
        asyncThread.setName("UIModule-AsyncTask");
        asyncThread.start();
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