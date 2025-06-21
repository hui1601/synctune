package ac.cwnu.synctune.ui;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import ac.cwnu.synctune.ui.scanner.MusicFolderScanner;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

@Module(name = "UI", version = "1.0.0")
public class UIModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(UIModule.class);
    private static UIModule instance;
    private MainApplicationWindow mainWindow;
    private boolean isJavaFXInitialized = false;
    private volatile boolean isInitializing = false;
    
    // MusicFolderScanner 관련 필드
    private MusicFolderScanner musicFolderScanner;
    private ExecutorService scannerExecutorService;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        instance = this;
        log.info("UIModule이 시작되었습니다.");

        // MusicFolderScanner 초기화
        initializeMusicFolderScanner();

        // JavaFX 플랫폼 초기화를 별도 스레드에서 수행
        initializeJavaFX();
    }

    /**
     * MusicFolderScanner 및 관련 리소스 초기화
     */
    private void initializeMusicFolderScanner() {
        try {
            musicFolderScanner = new MusicFolderScanner();
            scannerExecutorService = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "MusicScanner-Thread");
                t.setDaemon(true);
                return t;
            });
            log.info("MusicFolderScanner가 초기화되었습니다.");
        } catch (Exception e) {
            log.error("MusicFolderScanner 초기화 실패", e);
        }
    }

    /**
     * 폴더를 비동기적으로 스캔하여 음악 파일을 찾습니다.
     * 
     * @param folder 스캔할 폴더
     * @param callback 스캔 진행 상황 콜백
     * @return CompletableFuture<MusicFolderScanner.ScanResult>
     */
    public CompletableFuture<MusicFolderScanner.ScanResult> scanMusicFolderAsync(
            File folder, MusicFolderScanner.ScanProgressCallback callback) {
        
        if (musicFolderScanner == null || scannerExecutorService == null) {
            log.error("MusicFolderScanner가 초기화되지 않았습니다.");
            return CompletableFuture.failedFuture(
                new IllegalStateException("MusicFolderScanner not initialized"));
        }

        MusicFolderScanner.ScanOptions options = MusicFolderScanner.createDefaultOptions();
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("음악 폴더 스캔 시작: {}", folder.getAbsolutePath());
            
            MusicFolderScanner.ScanResult result = musicFolderScanner.scanFolder(
                folder, options, callback);
            
            log.info("음악 폴더 스캔 완료: {}", result);
            return result;
        }, scannerExecutorService);
    }

    /**
     * JavaFX Task를 생성하여 UI와 연동된 폴더 스캔을 수행합니다.
     * 
     * @param folder 스캔할 폴더
     * @param callback 스캔 진행 상황 콜백
     * @return Task<MusicFolderScanner.ScanResult>
     */
    public Task<MusicFolderScanner.ScanResult> createMusicScanTask(
            File folder, MusicFolderScanner.ScanProgressCallback callback) {
        
        if (musicFolderScanner == null) {
            throw new IllegalStateException("MusicFolderScanner not initialized");
        }

        MusicFolderScanner.ScanOptions options = MusicFolderScanner.createDefaultOptions();
        return musicFolderScanner.createScanTask(folder, options, callback);
    }

    /**
     * 빠른 스캔을 위한 Task 생성 (얕은 깊이 스캔)
     * 
     * @param folder 스캔할 폴더
     * @param callback 스캔 진행 상황 콜백
     * @return Task<MusicFolderScanner.ScanResult>
     */
    public Task<MusicFolderScanner.ScanResult> createQuickMusicScanTask(
            File folder, MusicFolderScanner.ScanProgressCallback callback) {
        
        if (musicFolderScanner == null) {
            throw new IllegalStateException("MusicFolderScanner not initialized");
        }

        MusicFolderScanner.ScanOptions options = MusicFolderScanner.createQuickScanOptions();
        return musicFolderScanner.createScanTask(folder, options, callback);
    }

    /**
     * 커스텀 옵션으로 스캔 Task 생성
     * 
     * @param folder 스캔할 폴더
     * @param options 스캔 옵션
     * @param callback 스캔 진행 상황 콜백
     * @return Task<MusicFolderScanner.ScanResult>
     */
    public Task<MusicFolderScanner.ScanResult> createCustomMusicScanTask(
            File folder, 
            MusicFolderScanner.ScanOptions options,
            MusicFolderScanner.ScanProgressCallback callback) {
        
        if (musicFolderScanner == null) {
            throw new IllegalStateException("MusicFolderScanner not initialized");
        }

        return musicFolderScanner.createScanTask(folder, options, callback);
    }

    /**
     * 현재 진행 중인 스캔을 취소합니다.
     */
    public void cancelCurrentScan() {
        if (musicFolderScanner != null) {
            musicFolderScanner.cancelScan();
            log.info("음악 폴더 스캔 취소 요청됨");
        }
    }

    /**
     * 파일이 지원되는 음악 파일인지 확인
     * 
     * @param file 확인할 파일
     * @return 지원되는 음악 파일 여부
     */
    public boolean isSupportedMusicFile(File file) {
        MusicFolderScanner.ScanOptions options = MusicFolderScanner.createDefaultOptions();
        return MusicFolderScanner.isSupportedMusicFile(file, options);
    }

    /**
     * 기본 스캔 옵션을 반환합니다.
     * 
     * @return 기본 스캔 옵션
     */
    public MusicFolderScanner.ScanOptions getDefaultScanOptions() {
        return MusicFolderScanner.createDefaultOptions();
    }

    /**
     * 빠른 스캔 옵션을 반환합니다.
     * 
     * @return 빠른 스캔 옵션
     */
    public MusicFolderScanner.ScanOptions getQuickScanOptions() {
        return MusicFolderScanner.createQuickScanOptions();
    }

    /**
     * MainApplicationWindow에서 폴더 스캔 기능에 접근할 수 있도록 
     * UIModule 인스턴스를 반환합니다.
     * 
     * @return UIModule 인스턴스
     */
    public static UIModule getInstance() {
        return instance;
    }

    /**
     * MusicFolderScanner 인스턴스를 반환합니다.
     * 
     * @return MusicFolderScanner 인스턴스 또는 null
     */
    public MusicFolderScanner getMusicFolderScanner() {
        return musicFolderScanner;
    }

    private void initializeJavaFX() {
        if (isInitializing) {
            log.warn("JavaFX 초기화가 이미 진행 중입니다.");
            return;
        }
        
        isInitializing = true;
        
        // JavaFX가 이미 실행 중인지 확인
        try {
            if (Platform.isFxApplicationThread()) {
                log.info("이미 JavaFX 스레드에서 실행 중입니다. 즉시 UI 초기화를 진행합니다.");
                initializeUI();
                return;
            }
        } catch (IllegalStateException e) {
            // Platform이 아직 초기화되지 않은 상태
            log.debug("JavaFX Platform이 아직 초기화되지 않음");
        }

        // JavaFX 애플리케이션을 별도 스레드에서 시작
        Thread javafxThread = new Thread(() -> {
            try {
                log.info("JavaFX 애플리케이션을 시작합니다...");
                System.setProperty("javafx.platform.exitOnClose", "false");
                Application.launch(JavaFXApp.class);
            } catch (IllegalStateException e) {
                // JavaFX가 이미 시작된 경우
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
                isInitializing = false;
            }
        });
        javafxThread.setName("JavaFX-Init-Thread");
        javafxThread.setDaemon(false); // 메인 스레드가 종료되어도 UI가 유지되도록
        javafxThread.start();
    }

    private void initializeUI() {
        if (isJavaFXInitialized) {
            log.debug("UI가 이미 초기화되었습니다.");
            return;
        }
        
        try {
            log.info("UI 초기화를 시작합니다...");
            
            // Platform implicit exit를 false로 설정하여 창을 닫아도 즉시 종료되지 않도록 함
            Platform.setImplicitExit(false);
            
            mainWindow = new MainApplicationWindow(eventPublisher);
            mainWindow.show();
            isJavaFXInitialized = true;
            isInitializing = false;
            
            log.info("메인 윈도우가 성공적으로 표시되었습니다. (ImplicitExit=false)");
            
            // PlaybackController를 이벤트 시스템에 등록
            registerPlaybackController();
            
        } catch (Exception e) {
            log.error("UI 초기화 중 오류 발생", e);
            isInitializing = false;
        }
    }

    /**
     * PlaybackController를 이벤트 시스템에 등록합니다.
     * MainApplicationWindow가 초기화된 후에 호출되어야 합니다.
     */
    private void registerPlaybackController() {
        if (mainWindow != null && mainWindow.getPlaybackController() != null) {
            // PlaybackController에는 @EventListener 어노테이션이 있는 메서드들이 있으므로
            // CoreModule의 EventBus에 등록해야 합니다.
            // 하지만 직접 Core의 EventBus에 접근할 수 없으므로,
            // UIModule 자체가 이벤트를 받아서 PlaybackController에 전달하는 방식을 사용합니다.
            log.debug("PlaybackController 이벤트 등록 준비 완료");
        }
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료됩니다. Core로부터 종료 신호를 받았습니다.");
        
        // MusicFolderScanner 리소스 정리
        cleanupMusicFolderScanner();
        
        if (Platform.isFxApplicationThread()) {
            // UI 스레드에서 호출된 경우
            cleanupAndExit();
        } else {
            // 다른 스레드에서 호출된 경우 UI 스레드에서 실행
            Platform.runLater(this::cleanupAndExit);
        }
    }

    /**
     * MusicFolderScanner 관련 리소스 정리
     */
    private void cleanupMusicFolderScanner() {
        try {
            if (musicFolderScanner != null) {
                musicFolderScanner.cancelScan();
                musicFolderScanner = null;
                log.debug("MusicFolderScanner 정리 완료");
            }
            
            if (scannerExecutorService != null && !scannerExecutorService.isShutdown()) {
                scannerExecutorService.shutdown();
                // 강제 종료를 위해 일정 시간 대기 후 shutdownNow 호출
                try {
                    if (!scannerExecutorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        scannerExecutorService.shutdownNow();
                        log.debug("스캐너 스레드풀 강제 종료됨");
                    } else {
                        log.debug("스캐너 스레드풀 정상 종료됨");
                    }
                } catch (InterruptedException e) {
                    scannerExecutorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scannerExecutorService = null;
            }
        } catch (Exception e) {
            log.error("MusicFolderScanner 정리 중 오류", e);
        }
    }

    private void cleanupAndExit() {
        try {
            // 메인 윈도우 정리
            if (mainWindow != null) {
                log.debug("메인 윈도우를 닫습니다.");
                mainWindow.forceClose(); // 안전한 강제 종료 메서드 사용
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

    // 재생 상태 이벤트들을 PlaybackController에 전달
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.debug("UIModule이 PlaybackStartedEvent 수신");
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateCurrentMusic(event.getCurrentMusic());
                    // PlaybackController에도 이벤트 전달
                    if (mainWindow.getPlaybackController() != null) {
                        mainWindow.getPlaybackController().onPlaybackStarted(event);
                    }
                } catch (Exception e) {
                    log.error("PlaybackStartedEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.debug("UIModule이 PlaybackPausedEvent 수신");
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    // PlaybackController에 이벤트 전달
                    if (mainWindow.getPlaybackController() != null) {
                        mainWindow.getPlaybackController().onPlaybackPaused(event);
                    }
                } catch (Exception e) {
                    log.error("PlaybackPausedEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.debug("UIModule이 PlaybackStoppedEvent 수신");
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    // PlaybackController에 이벤트 전달
                    if (mainWindow.getPlaybackController() != null) {
                        mainWindow.getPlaybackController().onPlaybackStopped(event);
                    }
                } catch (Exception e) {
                    log.error("PlaybackStoppedEvent 처리 중 오류", e);
                }
            });
        }
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        log.debug("UIModule이 MusicChangedEvent 수신");
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateCurrentMusic(event.getNewMusic());
                    // PlaybackController에도 이벤트 전달
                    if (mainWindow.getPlaybackController() != null) {
                        mainWindow.getPlaybackController().onMusicChanged(event);
                    }
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
    }

    @EventListener
    public void onLyricsNotFound(LyricsEvent.LyricsNotFoundEvent event) {
        log.info("가사 파일을 찾을 수 없음: {}", event.getMusicFilePath());
        if (isJavaFXInitialized && mainWindow != null) {
            Platform.runLater(() -> {
                try {
                    mainWindow.updateLyrics("가사를 찾을 수 없습니다");
                } catch (Exception e) {
                    log.error("LyricsNotFoundEvent 처리 중 오류", e);
                }
            });
        }
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
            // 애플리케이션 초기화 시 추가 작업이 필요하면 여기에 구현
        }
        
        @Override
        public void stop() throws Exception {
            super.stop();
            // JavaFX 애플리케이션 종료 시 추가 정리 작업
        }
    }
}