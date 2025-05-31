package ac.cwnu.synctune.core.initializer;

import ac.cwnu.synctune.core.EventBus;
import ac.cwnu.synctune.core.error.ModuleInitializationException;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.ModuleInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 스캔된 모듈 클래스들을 인스턴스화하고 초기화(start)하며, 생명주기 이벤트를 관리합니다.
 */
public class ModuleLoader {
    private static final Logger log = LogManager.getLogger(ModuleLoader.class);
    private final EventBus eventBus;
    private final List<ModuleLifecycleListener> lifecycleListeners = new ArrayList<>();

    public ModuleLoader(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void addLifecycleListener(ModuleLifecycleListener listener) {
        if (listener != null && !this.lifecycleListeners.contains(listener)) {
            this.lifecycleListeners.add(listener);
            log.debug("Added lifecycle listener: {}", listener.getClass().getName());
        }
    }

    public void removeLifecycleListener(ModuleLifecycleListener listener) {
        if (listener != null) {
            boolean removed = this.lifecycleListeners.remove(listener);
            if (removed) {
                log.debug("Removed lifecycle listener: {}", listener.getClass().getName());
            }
        }
    }

    /**
     * 제공된 모듈 클래스들을 로드하고 시작합니다.
     *
     * @param moduleClasses 시작할 모듈 클래스들의 집합
     * @return 성공적으로 시작된 모듈 인스턴스들의 리스트
     * @throws ModuleInitializationException 모듈 초기화 중 치명적인 오류 발생 시
     */

    public List<SyncTuneModule> loadAndStartModules(Set<Class<? extends SyncTuneModule>> moduleClasses, EventPublisher eventPublisher) {
        List<SyncTuneModule> startedModules = new ArrayList<>();
        if (moduleClasses == null || moduleClasses.isEmpty()) {
            log.info("No external modules found to load and start.");
            return startedModules;
        }

        log.info("Attempting to load and start {} module(s)...", moduleClasses.size());
        for (Class<? extends SyncTuneModule> moduleClass : moduleClasses) {
            Module moduleAnnotation = moduleClass.getAnnotation(Module.class);
            String moduleNameFromAnnotation = (moduleAnnotation != null && !moduleAnnotation.name().isEmpty())
                    ? moduleAnnotation.name()
                    : moduleClass.getSimpleName(); // 어노테이션 없거나 이름 비어있으면 클래스명 사용
            String moduleVersionFromAnnotation = (moduleAnnotation != null && !moduleAnnotation.version().isEmpty())
                    ? moduleAnnotation.version()
                    : "N/A"; // 어노테이션 없거나 버전 비어있으면 "N/A"

            ModuleInfo moduleInfo = new ModuleInfo(moduleNameFromAnnotation, moduleVersionFromAnnotation, moduleClass);

            invokeBeforeModuleLoadListeners(moduleInfo);
            SyncTuneModule moduleInstance = null;
            try {
                log.debug("Instantiating module: {}", moduleClass.getName());
                Constructor<? extends SyncTuneModule> constructor = moduleClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                moduleInstance = constructor.newInstance();
                log.info("Module instantiated: {} (Version: {}, Class: {})",
                        moduleInstance.getModuleName(), moduleInfo.getVersion(), moduleClass.getName());


                invokeAfterModuleLoadListeners(moduleInfo, moduleInstance);
                invokeBeforeModuleStartListeners(moduleInstance);

                log.info("Initializing and starting module: {}", moduleInstance.getModuleName());
                eventBus.register(moduleInstance);
                moduleInstance.start(eventPublisher);
                startedModules.add(moduleInstance);
                log.info("Successfully started and registered module: {}", moduleInstance.getModuleName());

                invokeAfterModuleStartListeners(moduleInstance);

            } catch (NoSuchMethodException e) {
                handleModuleError(moduleClass, "must have a public no-arg constructor", e, true, moduleNameFromAnnotation);
            } catch (InvocationTargetException e) {
                handleModuleError(moduleClass, "error during instantiation or start method invocation", e.getTargetException(), true, moduleNameFromAnnotation);
            } catch (InstantiationException | IllegalAccessException e) {
                handleModuleError(moduleClass, "failed to instantiate (check constructor visibility or abstract class)", e, true, moduleNameFromAnnotation);
            } catch (Exception e) {
                handleModuleError(moduleClass, "an unexpected error occurred during loading or starting", e, true, moduleNameFromAnnotation);
            }
        }
        return startedModules;
    }

    private void handleModuleError(Class<?> moduleClass, String errorMessageFragment, Throwable cause, boolean isFatal, String moduleName) {
        String fullErrorMessage = String.format("Module %s (Class: %s) %s.", moduleName, moduleClass.getName(), errorMessageFragment);
        log.error(fullErrorMessage, cause);
        eventBus.post(new ErrorEvent(fullErrorMessage, cause, isFatal));
        if (isFatal) {
            throw new ModuleInitializationException(fullErrorMessage, cause);
        }
    }

    /**
     * 등록된 모듈들을 중지시키고 이벤트 버스에서 등록 해제합니다.
     *
     * @param modules 중지할 모듈 인스턴스들의 리스트
     */
    public void stopAndUnloadModules(List<SyncTuneModule> modules) {
        if (modules == null || modules.isEmpty()) {
            log.info("No modules to stop and unload.");
            return;
        }
        log.info("Attempting to stop and unload {} module(s)...", modules.size());

        for (SyncTuneModule module : modules) {
            Class<?> moduleClass = module.getClass();
            Module moduleAnnotation = moduleClass.getAnnotation(Module.class);
            String moduleNameFromAnnotation = (moduleAnnotation != null && !moduleAnnotation.name().isEmpty())
                    ? moduleAnnotation.name()
                    : module.getModuleName();
            String moduleVersionFromAnnotation = (moduleAnnotation != null && !moduleAnnotation.version().isEmpty())
                    ? moduleAnnotation.version()
                    : "N/A";

            ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo =
                    new ac.cwnu.synctune.sdk.model.ModuleInfo(
                            moduleNameFromAnnotation, moduleVersionFromAnnotation, moduleClass);
            try {
                log.info("Stopping module: {}", module.getModuleName());
                invokeBeforeModuleStopListeners(module);
                module.stop();
                invokeAfterModuleStopListeners(module);

                invokeBeforeModuleUnloadListeners(module);
                eventBus.unregister(module);
                invokeAfterModuleUnloadListeners(moduleInfo);
                log.info("Module stopped and unregistered: {}", module.getModuleName());
            } catch (Exception e) {
                String errorMsg = String.format("Error stopping module %s", module.getModuleName());
                log.error(errorMsg, e);
                eventBus.post(new ErrorEvent(errorMsg, e, false));
            }
        }
    }

    // Lifecycle listener invocation helpers
    private void invokeBeforeModuleLoadListeners(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.beforeModuleLoad(moduleInfo);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (beforeModuleLoad)", e);
            }
        });
    }

    private void invokeAfterModuleLoadListeners(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo, SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.afterModuleLoad(moduleInfo, instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (afterModuleLoad)", e);
            }
        });
    }

    private void invokeBeforeModuleStartListeners(SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.beforeModuleStart(instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (beforeModuleStart)", e);
            }
        });
    }

    private void invokeAfterModuleStartListeners(SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.afterModuleStart(instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (afterModuleStart)", e);
            }
        });
    }

    private void invokeBeforeModuleStopListeners(SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.beforeModuleStop(instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (beforeModuleStop)", e);
            }
        });
    }

    private void invokeAfterModuleStopListeners(SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.afterModuleStop(instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (afterModuleStop)", e);
            }
        });
    }

    private void invokeBeforeModuleUnloadListeners(SyncTuneModule instance) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.beforeModuleUnload(instance);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (beforeModuleUnload)", e);
            }
        });
    }

    private void invokeAfterModuleUnloadListeners(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo) {
        lifecycleListeners.forEach(listener -> {
            try {
                listener.afterModuleUnload(moduleInfo);
            } catch (Exception e) {
                log.warn("Lifecycle listener error (afterModuleUnload)", e);
            }
        });
    }
}