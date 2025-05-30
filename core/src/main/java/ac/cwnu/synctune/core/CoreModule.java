package ac.cwnu.synctune.core;

import ac.cwnu.synctune.core.exception.ModuleInitializationException;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreModule extends SyncTuneModule {
    private static final Logger log = LogManager.getLogger(CoreModule.class);
    private static volatile CoreModule instance;
    private final EventBus eventBus;
    private final List<SyncTuneModule> registeredModules = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final String basePackageToScan;

    private CoreModule(String basePackage) {
        this.basePackageToScan = (basePackage != null && !basePackage.isEmpty()) ? basePackage : "ac.cwnu.synctune";
        this.eventBus = new EventBus(true);
        this.eventBus.register(this);
    }

    /**
     * CoreModule의 싱글톤 인스턴스를 반환합니다.
     * 이 메서드가 처음 호출될 때 CoreModule이 초기화됩니다.
     *
     * @param basePackageToScan 모듈을 스캔할 루트 패키지 이름
     * @return CoreModule 인스턴스
     */
    public static CoreModule initialize(String basePackageToScan) {
        if (instance == null) {
            synchronized (CoreModule.class) {
                if (instance == null) {
                    instance = new CoreModule(basePackageToScan);
                }
            }
        } else {
            log.warn("CoreModule already initialized. Base package cannot be changed.");
        }
        return instance;
    }

    /**
     * 이미 초기화된 CoreModule 인스턴스를 반환합니다.
     * 반드시 initialize()가 먼저 호출되어야 합니다.
     *
     * @return CoreModule 인스턴스
     * @throws IllegalStateException CoreModule이 아직 초기화되지 않은 경우
     */
    public static CoreModule getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CoreModule has not been initialized. Call CoreModule.initialize(basePackage) first.");
        }
        return instance;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("CoreModule is already running or starting.");
            return;
        }
        log.info("Starting SyncTune Core Module...");
        log.debug("Scanning for modules in package: {}", basePackageToScan);
        // Find all classes annotated with @ModuleStart in the specified package
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(basePackageToScan))
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
        );

        Set<Class<?>> moduleClasses = reflections.getTypesAnnotatedWith(ModuleStart.class);

        for (Class<?> moduleClass : moduleClasses) {
            if (SyncTuneModule.class.isAssignableFrom(moduleClass) && moduleClass != CoreModule.class) {
                try {
                    // Create an instance of the module using reflection
                    Constructor<?> constructor = moduleClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    SyncTuneModule moduleInstance = (SyncTuneModule) constructor.newInstance();

                    log.info("Initializing module: {}", moduleInstance.getModuleName());
                    eventBus.register(moduleInstance);
                    moduleInstance.start();
                    registeredModules.add(moduleInstance);
                    log.info("Initialized module: {}", moduleInstance.getModuleName());
                } catch (NoSuchMethodException e) {
                    publishEvent(new ErrorEvent("Module " + moduleClass.getName() + " must have a public no-arg constructor.", e, true));
                    throw new ModuleInitializationException("Module " + moduleClass.getName() + " must have a public no-arg constructor.", e);
                } catch (Exception e) {
                    publishEvent(new ErrorEvent("Failed to start module " + moduleClass.getName(), e, true));
                    throw new ModuleInitializationException("Failed to start module " + moduleClass.getName(), e);
                }
                // Ignore CoreModule itself, as it is already started
            } else if (moduleClass != CoreModule.class) {
                log.warn("Class {} is annotated with @ModuleStart but does not extend SyncTuneModule or is CoreModule itself. Skipping.", moduleClass.getName());
            }
        }

        // Publish an ApplicationReadyEvent to signal that the application is fully initialized
        publishEvent(new SystemEvent.ApplicationReadyEvent());
        log.info("All discovered modules started. Application is ready.");

        // Register a shutdown hook to ensure graceful shutdown
        if (!shuttingDown.get()) { // Prevent multiple shutdown hooks
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered. Initiating graceful shutdown...");
                if (instance != null && instance.isRunning()) {
                    instance.stop();
                }
            }, "SyncTune-ShutdownHook"));
        }
    }

    @Override
    public void stop() {
        if (!shuttingDown.compareAndSet(false, true)) {
            log.warn("Shutdown already in progress.");
            return;
        }

        if (!running.get()) {
            log.info("CoreModule is not running. No action taken.");
            shuttingDown.set(false);
            return;
        }

        log.info("Stopping SyncTune Core Module...");
        // Publish an ApplicationShutdownEvent to signal that the application is shutting down
        publishEvent(new SystemEvent.ApplicationShutdownEvent());

        // Stop all registered modules in reverse order of their registration
        List<SyncTuneModule> reversedModules = new ArrayList<>(registeredModules);
        Collections.reverse(reversedModules);

        for (SyncTuneModule module : reversedModules) {
            try {
                log.info("Stopping module: {}", module.getModuleName());
                module.stop();
                eventBus.unregister(module);
                log.info("Module stopped and unregistered: {}", module.getModuleName());
            } catch (Exception e) {
                log.error("Error stopping module {}: {}", module.getModuleName(), e.getMessage(), e);
            }
        }
        registeredModules.clear();
        log.info("Shutting down event bus...");
        eventBus.shutdown();
        running.set(false);
        log.info("SyncTune Core Module stopped successfully.");
        instance = null;
    }

    public void publishEvent(BaseEvent event) {
        if (event == null) {
            log.warn("Cannot publish a null event.");
            return;
        }
        log.debug("[EventLog] Publishing event: {}", event.getClass().getSimpleName());
        if (eventBus != null) {
            eventBus.post(event);
        } else {
            log.error("EventBus is not initialized. Cannot post event: {}", event);
        }
    }

    @EventListener
    public void onErrorOccurred(ErrorEvent event) {
        log.error("[CoreModule-ErrorListener] Received ErrorEvent: {}", event.getMessage());
        if (event.getException() != null) {
            log.error(event.getException().getMessage(), event.getException());
        }

        if (event.isFatal()) {
            log.error("Fatal error occurred. Initiating shutdown via System.exit()...");
            if (!shuttingDown.get()) {
                if (instance != null && instance.isRunning()) {
                    new Thread(() -> {
                        instance.stop();
                        log.info("Graceful shutdown attempted due to fatal error. Exiting.");
                        System.exit(1);
                    }, "FatalError-ShutdownThread").start();
                } else {
                    log.info("Core is not running or already shutting down. Exiting due to fatal error.");
                    System.exit(1);
                }
            } else {
                log.error("Shutdown already in progress. Fatal error occurred during shutdown.");
            }
        }
    }

    public boolean isRunning() {
        return running.get() && !shuttingDown.get();
    }
}