package ac.cwnu.synctune.core;

import ac.cwnu.synctune.core.error.FatalErrorReporter;
import ac.cwnu.synctune.core.error.GlobalExceptionHandler;
import ac.cwnu.synctune.core.error.ModuleInitializationException;
import ac.cwnu.synctune.core.initializer.ModuleLoader;
import ac.cwnu.synctune.core.initializer.ModuleScanner;
import ac.cwnu.synctune.core.logging.EventLogger;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Module(name = "Core", version = "1.0.0")
public class CoreModule extends SyncTuneModule implements ModuleLifecycleListener, EventPublisher {
    private static final Logger log = LogManager.getLogger(CoreModule.class);
    private static volatile CoreModule instance;
    private final EventBus eventBus;
    private final List<SyncTuneModule> registeredModules = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final String basePackageToScan;
    private final ModuleScanner moduleScanner;
    private final ModuleLoader moduleLoader;
    private Thread shutdownHookThread; // Shutdown hook 참조

    private CoreModule(String basePackage) {
        this.basePackageToScan = (basePackage != null && !basePackage.isEmpty()) ? basePackage : "ac.cwnu.synctune";
        this.eventBus = new EventBus(true); // 비동기 이벤트 처리
        this.eventBus.register(this); // CoreModule 자체 이벤트 리스너 등록

        this.moduleScanner = new ModuleScanner(this.basePackageToScan);
        this.moduleLoader = new ModuleLoader(this.eventBus);
        this.moduleLoader.addLifecycleListener(this); // CoreModule이 다른 모듈의 생명주기 로깅
    }

    /**
     * CoreModule의 싱글톤 인스턴스를 초기화하고 반환합니다.
     * 애플리케이션 시작 시 한 번만 호출되어야 합니다.
     *
     * @param basePackageToScan 모듈을 스캔할 루트 패키지
     * @return CoreModule 인스턴스
     */
    public static CoreModule initialize(String basePackageToScan) {
        if (instance == null) {
            synchronized (CoreModule.class) {
                if (instance == null) {
                    instance = new CoreModule(basePackageToScan);
                    log.info("CoreModule initialized. Base package for module scan: {}", instance.basePackageToScan);
                }
            }
        } else {
            log.warn("CoreModule already initialized. Subsequent calls to initialize() are ignored. Current base package: {}", instance.basePackageToScan);
        }
        return instance;
    }

    /**
     * 이미 초기화된 CoreModule 인스턴스를 반환합니다.
     * core 모듈의 외부에서 호출하는 것은 금지되어 있습니다.
     *
     * @return CoreModule 인스턴스
     * @throws IllegalStateException CoreModule이 아직 초기화되지 않은 경우
     * @throws IllegalStateException CoreModule의 외부에서 호출된 경우
     */
    public static CoreModule getInternalInstance() {
        if (instance == null) {
            log.warn("CoreModule.getInternalInstance() called. Consider using injected EventPublisher for event publishing.");
            throw new IllegalStateException("CoreModule has not been initialized. Call CoreModule.initialize() first.");
        }
        return instance;
    }

    /**
     * 애플리케이션을 부트스트랩합니다. GlobalExceptionHandler를 등록하고 CoreModule을 시작합니다.
     *
     * @param basePackage 모듈 스캔을 위한 기본 패키지
     */
    public static void bootstrap(String basePackage) {
        GlobalExceptionHandler.register();
        try {
            CoreModule core = CoreModule.initialize(basePackage);
            core.start(core);
        } catch (Exception e) {
            // CoreModule.initialize 또는 start() 에서 발생한 예외
            log.error("Critical failure during CoreModule bootstrap: {}. Application will exit.", e.getMessage(), e);
            // GlobalExceptionHandler가 등록되었더라도, bootstrap 단계의 심각한 오류는 직접 처리 후 종료
            FatalErrorReporter.reportFatalError("CoreModule bootstrap failed", e);
        }
    }


    @Override
    public void start(EventPublisher publisher) {
        // 절대 일어나지 않아야 하는 상황. 발생하면 심각한 오류로 간주.
        if (publisher == null) {
            log.error("CoreModule received null EventPublisher on start. This should not happen.");
            return;
        }
        this.eventPublisher = publisher;
        if (!running.compareAndSet(false, true)) {
            log.info("CoreModule is already running or starting.");
            return;
        }
        shuttingDown.set(false); // 시작 시에는 종료 상태 초기화
        log.debug("Starting SyncTune Core Module...");

        try {
            Set<Class<? extends SyncTuneModule>> moduleClasses = moduleScanner.scanForModules();
            List<SyncTuneModule> startedModules = moduleLoader.loadAndStartModules(moduleClasses, this);
            this.eventBus.register(new EventLogger());
            log.debug("EventBus registered with EventLogger for detailed event logging.");
            registeredModules.addAll(startedModules);

            publish(new SystemEvent.ApplicationReadyEvent());
            log.debug("All discovered modules started. SyncTune application is ready.");

            registerShutdownHook();

        } catch (ModuleInitializationException e) {
            String errMsg = "Fatal error during module initialization process. CoreModule startup aborted.";
            log.error(errMsg, e);
            publish(new ErrorEvent(errMsg, e.getCause() != null ? e.getCause() : e, true));
        } catch (Exception e) {
            String errMsg = "Unexpected error during CoreModule startup. CoreModule startup aborted.";
            log.error(errMsg, e);
            publish(new ErrorEvent(errMsg, e, true));
        }
    }

    private synchronized void registerShutdownHook() {
        if (shutdownHookThread == null) {
            shutdownHookThread = new Thread(() -> {
                log.debug("Shutdown hook triggered by JVM. Initiating graceful shutdown...");
                CoreModule currentInstance = CoreModule.instance; // 종료 시점의 instance
                if (currentInstance != null && currentInstance.isRunning() && !currentInstance.shuttingDown.get()) {
                    currentInstance.stop(); // 아직 실행 중이고, 정상 종료 절차가 시작되지 않았다면 시작
                } else if (currentInstance != null && currentInstance.shuttingDown.get()) {
                    log.warn("Shutdown hook: Shutdown was already in progress (likely via explicit stop() call).");
                } else {
                    log.warn("Shutdown hook: CoreModule not running or instance is null. No action taken by hook.");
                }
            }, "SyncTune-JVM-ShutdownHook");
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            log.debug("JVM ShutdownHook registered.");
        }
    }

    private synchronized void unregisterShutdownHook() {
        if (shutdownHookThread != null) {
            if (Thread.currentThread() == shutdownHookThread) {
                log.debug("Skipping explicit unregistration of JVM ShutdownHook as it is currently executing or called from it.");
            } else {
                try {
                    if (Runtime.getRuntime().removeShutdownHook(shutdownHookThread)) {
                        log.debug("JVM ShutdownHook unregistered successfully (likely due to explicit stop() call).");
                    } else {
                        log.warn("Failed to unregister JVM ShutdownHook. It might have already run, not been registered, or been previously unregistered.");
                    }
                } catch (IllegalStateException e) {
                    log.warn("Cannot unregister JVM ShutdownHook, JVM shutdown is already in progress: {}", e.getMessage());
                }
            }
            this.shutdownHookThread = null;
        }
    }


    @Override
    public void stop() {
        if (!running.get()) {
            log.info("CoreModule is not running. Stop action ignored.");
            // 만약 shuttingDown이 true인데 running이 false인 이상한 상태라면, 로깅 후 정리.
            if (shuttingDown.get()) {
                log.warn("CoreModule was not running but shuttingDown was true. Resetting shuttingDown flag.");
                shuttingDown.set(false);
            }
            return;
        }

        if (!shuttingDown.compareAndSet(false, true)) {
            log.warn("Shutdown is already in progress. Duplicate stop() call ignored.");
            return;
        }

        log.info("Stopping SyncTune Core Module...");
        publish(new SystemEvent.ApplicationShutdownEvent());

        List<SyncTuneModule> reversedModules = new ArrayList<>(registeredModules);
        Collections.reverse(reversedModules);
        moduleLoader.stopAndUnloadModules(reversedModules);

        registeredModules.clear();

        log.debug("Shutting down event bus...");
        if (eventBus != null) {
            eventBus.shutdown(); // EventBus의 스레드 풀 종료 등
        }
        unregisterShutdownHook(); // 더 이상 필요 없으므로 해제

        running.set(false);
        shuttingDown.set(false);
        super.stop();
        log.debug("SyncTune Core Module stopped successfully.");
    }

    @Override
    public void publish(BaseEvent event) {
        if (event == null) {
            log.warn("Cannot publish a null event.");
            return;
        }
        if (eventBus != null && (running.get() || shuttingDown.get())) {
            // 실행 중이거나 정상 종료 중일 때만 이벤트 발행
            log.trace("[EventPublish] Publishing event: {}", event); // 상세 로깅은 TRACE 레벨로
            eventBus.post(event);
        } else {
            log.error("EventBus is not available or CoreModule is not in a state to post events (running={}, shuttingDown={}). Event not posted: {}",
                    running.get(), shuttingDown.get(), event);
        }
    }

    @EventListener
    public void onErrorOccurred(ErrorEvent event) {
        // 이 리스너는 CoreModule이 EventBus에 등록되어 있을 때만 호출됨
        log.error("[CoreModule-ErrorListener] ErrorEvent received: \"{}\" (Fatal: {})", event.getMessage(), event.isFatal());
        if (event.getException() != null) {
            log.error("Underlying exception for ErrorEvent:", event.getException());
        }

        if (event.isFatal()) {
            log.error("Fatal error processing initiated by ErrorEvent: \"{}\"", event.getMessage());
            FatalErrorReporter.reportFatalError(event.getMessage(), event.getException());
        }
    }

    public boolean isRunning() {
        return running.get() && !shuttingDown.get();
    }

    @Override
    public void afterModuleLoad(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo, SyncTuneModule moduleInstance) {
        log.debug("[Lifecycle] Module loaded by Core: {} ({})", moduleInstance.getModuleName(), moduleInfo.getModuleClass().getName());
    }

    @Override
    public void afterModuleStart(SyncTuneModule moduleInstance) {
        log.info("[Lifecycle] Module started by Core: {}", moduleInstance.getModuleName());
    }

    @Override
    public void beforeModuleStop(SyncTuneModule moduleInstance) {
        log.debug("[Lifecycle] Module stopping by Core: {}", moduleInstance.getModuleName());
    }

    @Override
    public void afterModuleStop(SyncTuneModule moduleInstance) {
        log.info("[Lifecycle] Module stopped by Core: {}", moduleInstance.getModuleName());
    }

    @Override
    public void afterModuleUnload(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo) {
        log.debug("[Lifecycle] Module unloaded by Core: {} ({})", moduleInfo.getName(), moduleInfo.getModuleClass().getName());
    }
}