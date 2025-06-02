package ac.cwnu.synctune.core.error;

import ac.cwnu.synctune.core.CoreModule;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

/**
 * 심각한(Fatal) 오류 발생 시 이를 처리하고 애플리케이션을 안전하게 종료하는 역할을 합니다.
 */
public class FatalErrorReporter {
    private static final Logger log = LogManager.getLogger(FatalErrorReporter.class);
    private static volatile boolean shutdownInProgress = false; // shutdown 중복 방지 플래그

    /**
     * 심각한 오류를 보고하고 애플리케이션 종료를 시도합니다.
     * 이 메서드는 동기화되어 여러 스레드에서 동시에 호출되어도 한 번만 종료 로직이 실행되도록 합니다.
     *
     * @param message 오류 메시지
     * @param cause   오류의 원인이 된 Throwable
     */
    public static synchronized void reportFatalError(String message, Throwable cause) {
        if (shutdownInProgress) {
            log.warn("Shutdown is already in progress due to a previous fatal error. New fatal error: '{}' - skipped further action.", message, cause);
            return;
        }
        shutdownInProgress = true; // 종료 절차 시작 표시

        log.error("FATAL ERROR OCCURRED: {}", message, cause);
        log.debug("Initiating application shutdown sequence due to fatal error...");

        try {
            // CoreModule 인스턴스를 가져와서 종료 시도
            CoreModule coreModule = CoreModule.getInstance();
            if (coreModule != null && coreModule.isRunning()) {
                log.debug("Attempting to gracefully stop CoreModule...");
                // 별도 스레드에서 CoreModule.stop()을 호출하여 Deadlock 방지
                Thread shutdownThread = new Thread(() -> {
                    try {
                        coreModule.stop();
                        log.debug("CoreModule stopped gracefully after fatal error.");
                    } catch (Exception e) {
                        log.error("Exception during graceful shutdown of CoreModule after fatal error.", e);
                    } finally {
                        log.error("Exiting application (System.exit(1)) after attempting graceful shutdown.");
                        // 강제 종료
                        System.exit(1);
                    }
                }, "FatalError-GracefulShutdownThread");
                shutdownThread.start();
                try {
                    log.debug("Waiting for graceful shutdown thread to complete...");
                    // 최대 5초 동안 graceful shutdown 스레드가 종료되기를 기다림
                    shutdownThread.join(5000);
                    if (shutdownThread.isAlive()) {
                        log.warn("Graceful shutdown thread is still alive after timeout. Forcing exit.");
                    }
                } catch (InterruptedException e) {
                    log.warn("Main thread interrupted while waiting for graceful shutdown. Forcing exit.", e);
                    // InterruptedException 발생 시 현재 스레드를 인터럽트 상태로 설정
                    Thread.currentThread().interrupt();
                }
            } else {
                log.warn("CoreModule is not running or not initialized. Proceeding to direct exit.");
            }
        } catch (IllegalStateException e) {
            log.error("CoreModule instance is not available (likely not initialized): {}. Cannot perform graceful shutdown.", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception during fatal error reporting preparation.", e);
        } finally {
            // 만약 graceful shutdown 스레드가 실행되지 못했거나, CoreModule이 없는 경우 등
            if (!Thread.currentThread().getName().equals("FatalError-GracefulShutdownThread")) {
                log.error("Forcing application exit (System.exit(1)) as a final measure in fatal error handler.");
                System.exit(1);
            }
        }
    }
}