package ac.cwnu.synctune.core;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {
    private static final Logger log = LogManager.getLogger(EventBus.class);
    private final Map<Class<? extends BaseEvent>, List<EventListenerMethod>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor;

    public EventBus(boolean asyncEventDispatch) {
        if (asyncEventDispatch) {
            this.eventExecutor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "synctune-event-dispatcher");
                thread.setDaemon(true); // 애플리케이션 종료 시 함께 종료되도록 데몬 스레드로 설정
                return thread;
            });
        } else {
            this.eventExecutor = null; // 동기 처리 시 null
        }
    }

    public EventBus() {
        this(false); // 기본값: 동기 이벤트 처리
    }

    /**
     * 리스너 객체를 등록합니다.
     * 객체 내 @EventListener 어노테이션이 붙은 메서드를 찾아 리스너로 등록합니다.
     *
     * @param listenerInstance 등록할 리스너 객체
     */
    public void register(Object listenerInstance) {
        for (Method method : listenerInstance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListener.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                // @EventListener 메서드는 반드시 BaseEvent를 상속받는 파라미터 하나만 가져야 함
                if (parameterTypes.length == 1 && BaseEvent.class.isAssignableFrom(parameterTypes[0])) {
                    @SuppressWarnings("unchecked")
                    Class<? extends BaseEvent> eventType = (Class<? extends BaseEvent>) parameterTypes[0];
                    method.setAccessible(true); // private 메서드도 접근 가능하도록 설정
                    // 해당 이벤트 타입의 리스너 리스트를 가져오거나 새로 생성
                    listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                            .add(new EventListenerMethod(listenerInstance, method));
                    log.debug("[EventBus] Registered listener: {}.{}({})",
                            listenerInstance.getClass().getSimpleName(), method.getName(), eventType.getSimpleName());
                } else {
                    log.debug("[EventBus] Warning: @EventListener method {}.{} must have exactly one parameter that extends BaseEvent.",
                            listenerInstance.getClass().getSimpleName(), method.getName());
                }
            }
        }
    }

    /**
     * 리스너 객체의 등록을 해제합니다.
     *
     * @param listenerInstance 등록 해제할 리스너 객체
     */
    public void unregister(Object listenerInstance) {
        listeners.values().forEach(list ->
                list.removeIf(listenerMethod -> listenerMethod.getTargetInstance() == listenerInstance)
        );
        log.info("[EventBus] Unregistered all listeners for: {}", listenerInstance.getClass().getSimpleName());
    }

    /**
     * 이벤트를 발행합니다. 등록된 리스너 중 해당 이벤트 타입을 처리하는 메서드를 호출합니다.
     *
     * @param event 발행할 이벤트 객체
     */
    public void post(BaseEvent event) {
        if (event == null) {
            log.warn("Cannot post a null event.");
            return;
        }
        // 동일한 이벤트 타입의 리스너를 중복 호출하지 않도록 Set을 사용
        Set<EventListenerMethod> uniqueListenersToInvoke = new HashSet<>();

        listeners.forEach((registeredType, listenerMethods) -> {
            // registeredType이 event.getClass()의 슈퍼클래스이거나 같은 클래스인 경우
            if (registeredType.isAssignableFrom(event.getClass())) {
                uniqueListenersToInvoke.addAll(listenerMethods);
            }
        });

        if (!uniqueListenersToInvoke.isEmpty()) {
            for (EventListenerMethod listenerMethod : uniqueListenersToInvoke) {
                if (eventExecutor != null) { // Asynchronous event dispatch
                    eventExecutor.submit(() -> invokeListener(listenerMethod, event));
                } else { // Synchronous event dispatch
                    invokeListener(listenerMethod, event);
                }
            }
        }
    }

    private void invokeListener(EventListenerMethod listenerMethod, BaseEvent event) {
        try {
            listenerMethod.invoke(event);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            log.error("[EventBus] Error in event listener {}.{}: {}",
                    listenerMethod.getTargetInstance().getClass().getSimpleName(),
                    listenerMethod.getMethod().getName(), targetException.getMessage());
            // 리스너 실행 중 발생한 예외를 ErrorEvent로 다시 발행 (무한 루프 방지를 위해 ErrorEvent는 제외)
            if (!(event instanceof ErrorEvent)) { // ErrorEvent 처리 중 발생한 오류는 다시 ErrorEvent로 발행하지 않음
                post(new ErrorEvent("Error in listener " + listenerMethod.getMethod().getName(), targetException, false));
            }
        } catch (IllegalAccessException e) {
            log.error("[EventBus] Illegal access trying to invoke event listener {}.{}: {}",
                    listenerMethod.getTargetInstance().getClass().getSimpleName(),
                    listenerMethod.getMethod().getName(), e.getMessage());
        }
    }

    /**
     * EventBus 종료 시 ExecutorService를 종료
     */
    public void shutdown() {
        if (eventExecutor != null) {
            eventExecutor.shutdown();
            log.info("[EventBus] Event executor shutdown.");
        }
    }

    /**
     * 리스너 메서드와 해당 메서드를 가진 객체를 캡슐화하는 내부 클래스
     */
    private static class EventListenerMethod {
        private final Object targetInstance;
        private final Method method;

        public EventListenerMethod(Object targetInstance, Method method) {
            this.targetInstance = targetInstance;
            this.method = method;
        }

        public Object getTargetInstance() {
            return targetInstance;
        }

        public Method getMethod() {
            return method;
        }

        public void invoke(BaseEvent event) throws InvocationTargetException, IllegalAccessException {
            method.invoke(targetInstance, event);
        }
    }
}