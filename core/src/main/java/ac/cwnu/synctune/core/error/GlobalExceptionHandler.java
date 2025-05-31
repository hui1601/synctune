package ac.cwnu.synctune.core.error;

import ac.cwnu.synctune.core.CoreModule;
import ac.cwnu.synctune.sdk.event.ErrorEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

/**
 * 애플리케이션 내에서 처리되지 않은 예외(Uncaught Exception)를 중앙에서 처리합니다.
 * 모든 스레드에서 발생하는 예외를 로깅하고, 필요한 경우 추가적인 조치를 취할 수 있습니다.
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler previousDefaultHandler;

    public GlobalExceptionHandler() {
        this.previousDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * 이 핸들러를 기본 UncaughtExceptionHandler로 등록합니다.
     * 기존 핸들러가 있다면 이 핸들러 다음에 호출될 수 있도록 체이닝합니다.
     */
    public static void register() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        log.info("GlobalExceptionHandler registered as default uncaught exception handler.");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("UNCAUGHT EXCEPTION in thread \"{}\" [ID:{}]: {}", t.getName(), t.threadId(), e.getMessage(), e);

        // 특정 유형의 예외는 치명적인 오류로 간주하여 애플리케이션 종료
        if (isFatal(e)) {
            FatalErrorReporter.reportFatalError("Uncaught fatal exception in thread " + t.getName(), e);
        } else {
            // ErrorEvent를 생성하여 CoreModule에 게시
            CoreModule.getInstance().publish(new ErrorEvent("Uncaught non-fatal exception", e, false));
        }

        if (previousDefaultHandler != null && previousDefaultHandler != this) {
            log.debug("Passing uncaught exception to previous default handler: {}", previousDefaultHandler.getClass().getName());
            previousDefaultHandler.uncaughtException(t, e);
        }
    }

    /**
     * 특정 예외가 애플리케이션을 종료해야 하는 치명적인 예외인지 판단합니다.
     *
     * @param e 검사할 예외
     * @return 치명적이면 true, 아니면 false
     */
    private boolean isFatal(Throwable e) {
        // 예: OutOfMemoryError, StackOverflowError 등은 치명적인 예외로 간주
        return e instanceof VirtualMachineError;
    }
}