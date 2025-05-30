package ac.cwnu.synctune.core;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.core.exception.ModuleInitializationException;

import org.reflections.Reflections; // Reflections 라이브러리 임포트
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreModule extends SyncTuneModule {

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
     * 스캔할 기본 패키지를 지정해야 합니다.
     * @param basePackageToScan 모듈을 스캔할 루트 패키지 이름 (예: "ac.cwnu.synctune")
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
            System.err.println("[CoreModule] Warning: CoreModule already initialized. Base package cannot be changed.");
        }
        return instance;
    }

    /**
     * 이미 초기화된 CoreModule 인스턴스를 반환합니다.
     * 반드시 initialize()가 먼저 호출되어야 합니다.
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
            System.out.println("[CoreModule] CoreModule is already running or starting.");
            return;
        }
        System.out.println("[CoreModule] Starting SyncTune Core Module...");
        System.out.println("[CoreModule] Scanning for modules in package: " + basePackageToScan);

        // 클래스패스 스캐닝으로 @ModuleStart 어노테이션이 붙은 SyncTuneModule 구현체 찾기
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(basePackageToScan))
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
        );

        Set<Class<?>> moduleClasses = reflections.getTypesAnnotatedWith(ModuleStart.class);

        for (Class<?> moduleClass : moduleClasses) {
            if (SyncTuneModule.class.isAssignableFrom(moduleClass) && moduleClass != CoreModule.class) {
                try {
                    // 기본 생성자로 인스턴스 생성
                    Constructor<?> constructor = moduleClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    SyncTuneModule moduleInstance = (SyncTuneModule) constructor.newInstance();

                    System.out.println("[CoreModule] Initializing module: " + moduleInstance.getModuleName());
                    // 각 모듈의 start() 호출 전에 이벤트 버스에 등록하여, start() 내부에서 발생하는 이벤트를 다른 모듈이 받을 수 있도록 함 (순서 중요)
                    eventBus.register(moduleInstance);
                    moduleInstance.start(); // 각 모듈의 start() 호출
                    registeredModules.add(moduleInstance);
                    System.out.println("[CoreModule] Module started and registered: " + moduleInstance.getModuleName());

                } catch (NoSuchMethodException e) {
                    publishEvent(new ErrorEvent("Module " + moduleClass.getName() + " must have a public no-arg constructor.", e, true));
                    throw new ModuleInitializationException("Module " + moduleClass.getName() + " must have a public no-arg constructor.", e);
                } catch (Exception e) {
                    publishEvent(new ErrorEvent("Failed to start module " + moduleClass.getName(), e, true));
                    throw new ModuleInitializationException("Failed to start module " + moduleClass.getName(), e);
                }
                // CoreModule 자체는 이미 시작되었으므로 무시
            } else if (moduleClass != CoreModule.class) {

                System.err.println("[CoreModule] Warning: Class " + moduleClass.getName() +
                        " is annotated with @ModuleStart but does not extend SyncTuneModule or is CoreModule itself. Skipping.");
            }
        }

        // 모든 모듈 시작 후 ApplicationReadyEvent 발행
        publishEvent(new SystemEvent.ApplicationReadyEvent());
        System.out.println("[CoreModule] All discovered modules started. Application is ready.");

        // 종료 훅(Shutdown Hook) 등록: JVM 종료 시 stop() 메서드 호출 보장
        // Shutdown Hook은 이미 등록되어 있다면 중복 등록되지 않도록 주의해야 함 (여기서는 CoreModule 싱글톤이므로 한번만 등록됨)
        if (!shuttingDown.get()) { // 이미 종료 중이면 추가 등록 방지
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[CoreModule] Shutdown hook triggered. Initiating graceful shutdown...");
                // 이 시점에서 System.exit()을 직접 호출하면 무한 루프나 데드락 발생 가능성 있음
                // stop() 메서드가 완료되기를 기다리거나, stop() 내에서 System.exit()을 호출하지 않도록 주의
                if (instance != null && instance.isRunning()) {
                    instance.stop();
                }
            }, "SyncTune-ShutdownHook"));
        }
    }

    @Override
    public void stop() {
        // shuttingDown 플래그를 먼저 설정하여 중복 호출 및 종료 중 작업 방지
        if (!shuttingDown.compareAndSet(false, true)) {
            System.out.println("[CoreModule] Shutdown already in progress.");
            return;
        }

        if (!running.get()) { // 실행 중이 아니면 종료할 필요 없음
            System.out.println("[CoreModule] CoreModule is not running.");
            shuttingDown.set(false); // 종료 완료 또는 실패 시 플래그 리셋
            return;
        }

        System.out.println("[CoreModule] Stopping SyncTune Core Module...");
        // 종료 시작 이벤트 발행 (다른 모듈이 종료 준비를 할 수 있도록)
        publishEvent(new SystemEvent.ApplicationShutdownEvent());

        // 등록된 모듈들을 역순으로 중지
        List<SyncTuneModule> reversedModules = new ArrayList<>(registeredModules);
        Collections.reverse(reversedModules);

        for (SyncTuneModule module : reversedModules) {
            try {
                System.out.println("[CoreModule] Stopping module: " + module.getModuleName());
                module.stop();
                eventBus.unregister(module);
                System.out.println("[CoreModule] Module stopped and unregistered: " + module.getModuleName());
            } catch (Exception e) {
                System.err.println("[CoreModule] Error stopping module " + module.getModuleName() + ": " + e.getMessage());
                // 여기서 ErrorEvent를 발행할 수 있지만, 종료 과정이므로 주의해야 함
                // e.printStackTrace(System.err);
            }
        }
        registeredModules.clear();

        // EventBus의 스레드 풀을 정상적으로 종료
        System.out.println("[CoreModule] Shutting down event bus...");
        eventBus.shutdown(); // EventBus의 ExecutorService 종료

        // 모든 정리 작업 후 실행 상태 변경
        running.set(false);
        System.out.println("[CoreModule] SyncTune Core Module stopped successfully.");
         instance = null; // 애플리케이션 완전 종료 시점에 인스턴스 null 처리. 재시작을 고려한다면 유지.
    }

    public void publishEvent(BaseEvent event) {
        if (event == null) {
            System.err.println("[CoreModule] Warning: Cannot publish a null event.");
            return;
        }
        // 이벤트 로깅
        System.out.println("[EventLog] " + event);
        if (eventBus != null) {
            eventBus.post(event);
        } else {
            System.err.println("[CoreModule] EventBus is not initialized. Cannot post event: " + event);
        }
    }

    @EventListener
    public void onErrorOccurred(ErrorEvent event) {
        System.err.println("[CoreModule-ErrorListener] Received ErrorEvent: " + event.getMessage() +
                (event.getException() != null ? " - Exception: " + event.getException().getClass().getSimpleName() : ""));
        if (event.getException() != null) {
            event.getException().printStackTrace(System.err);
        }

        if (event.isFatal()) {
            System.err.println("[CoreModule-ErrorListener] Fatal error occurred. Initiating shutdown via System.exit()...");
            if (!shuttingDown.get()) {
                if (instance != null && instance.isRunning()) {
                    new Thread(() -> {
                        instance.stop();
                        System.out.println("[CoreModule-ErrorListener] Graceful shutdown attempted due to fatal error. Exiting.");
                        System.exit(1);
                    }, "FatalError-ShutdownThread").start();
                } else {
                    System.out.println("[CoreModule-ErrorListener] Core is not running or already shutting down. Exiting due to fatal error.");
                    System.exit(1);
                }
            } else {
                System.err.println("[CoreModule-ErrorListener] Shutdown already in progress. Fatal error occurred during shutdown.");
            }
        }
    }

    public boolean isRunning() {
        return running.get() && !shuttingDown.get();
    }
}