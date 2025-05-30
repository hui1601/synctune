package ac.cwnu.synctune.core;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.ErrorEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {
    // 이벤트 타입별 리스너 메서드 목록을 저장하는 맵
    // Key: 이벤트 클래스, Value: 해당 이벤트를 처리하는 리스너 메서드 정보 리스트
    private final Map<Class<? extends BaseEvent>, List<EventListenerMethod>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor; // 비동기 이벤트 처리를 위한 스레드 풀

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
     * @param listenerInstance 등록할 리스너 객체
     */
    public void register(Object listenerInstance) {
        for (Method method : listenerInstance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListener.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                // @EventListener 메서드는 반드시 BaseEvent를 상속받는 파라미터 하나만 가져야 함
                if (parameterTypes.length == 1 && BaseEvent.class.isAssignableFrom(parameterTypes[0])) {
                    @SuppressWarnings("unchecked") // 타입 검사 완료
                    Class<? extends BaseEvent> eventType = (Class<? extends BaseEvent>) parameterTypes[0];
                    method.setAccessible(true); // private 메서드도 접근 가능하도록 설정
                    // 해당 이벤트 타입의 리스너 리스트를 가져오거나 새로 생성
                    listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                            .add(new EventListenerMethod(listenerInstance, method));
                    System.out.println("[EventBus] Registered listener: " + listenerInstance.getClass().getSimpleName() +
                            "." + method.getName() + "(" + eventType.getSimpleName() + ")");
                } else {
                    System.err.println("[EventBus] Warning: @EventListener method " +
                            listenerInstance.getClass().getSimpleName() + "." + method.getName() +
                            " must have exactly one parameter that extends BaseEvent.");
                }
            }
        }
    }

    /**
     * 리스너 객체의 등록을 해제합니다. (구현 간소화를 위해 모든 메서드 제거)
     * @param listenerInstance 등록 해제할 리스너 객체
     */
    public void unregister(Object listenerInstance) {
        listeners.values().forEach(list ->
                list.removeIf(listenerMethod -> listenerMethod.getTargetInstance() == listenerInstance)
        );
        System.out.println("[EventBus] Unregistered all listeners for: " + listenerInstance.getClass().getSimpleName());
    }

    /**
     * 이벤트를 발행합니다. 등록된 리스너 중 해당 이벤트 타입을 처리하는 메서드를 호출합니다.
     * @param event 발행할 이벤트 객체
     */
    public void post(BaseEvent event) {
        if (event == null) {
            System.err.println("[EventBus] Warning: Cannot post a null event.");
            return;
        }
        // System.out.println("[EventBus] Posting event: " + event.toString()); // 이벤트 발행 시 로그 (CoreModule에서 로깅 담당)

        List<EventListenerMethod> eventListenerMethods = listeners.get(event.getClass());
        if (eventListenerMethods != null) {
            for (EventListenerMethod listenerMethod : eventListenerMethods) {
                if (eventExecutor != null) { // 비동기 처리
                    eventExecutor.submit(() -> invokeListener(listenerMethod, event));
                } else { // 동기 처리
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
            System.err.println("[EventBus] Error in event listener " +
                    listenerMethod.getTargetInstance().getClass().getSimpleName() + "." +
                    listenerMethod.getMethod().getName() + ": " + targetException.getMessage());
            targetException.printStackTrace(System.err);
            // 리스너 실행 중 발생한 예외를 ErrorEvent로 다시 발행 (무한 루프 방지 필요)
            if (!(event instanceof ErrorEvent)) { // ErrorEvent 처리 중 발생한 오류는 다시 ErrorEvent로 발행하지 않음
                post(new ErrorEvent("Error in listener " + listenerMethod.getMethod().getName(), targetException, false));
            }
        } catch (IllegalAccessException e) {
            System.err.println("[EventBus] Illegal access trying to invoke event listener " +
                    listenerMethod.getTargetInstance().getClass().getSimpleName() + "." +
                    listenerMethod.getMethod().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * EventBus 종료 시 ExecutorService를 종료합니다.
     */
    public void shutdown() {
        if (eventExecutor != null) {
            eventExecutor.shutdown();
            System.out.println("[EventBus] Event executor shutdown.");
        }
    }

    /**
     * 리스너 메서드와 해당 메서드를 가진 객체를 캡슐화하는 내부 클래스입니다.
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