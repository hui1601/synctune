package ac.cwnu.synctune.sdk.event;

/**
 * 예외 및 오류 발생 시 사용되는 이벤트입니다.
 */
public class ErrorEvent extends BaseEvent {
    private final Throwable exception;
    private final String message;
    private final boolean isFatal;

    public ErrorEvent(String message, Throwable exception, boolean isFatal) {
        this.message = message;
        this.exception = exception;
        this.isFatal = isFatal;
    }

    public ErrorEvent(String message, Throwable exception) {
        this(message, exception, false); // 기본적으로 non-fatal
    }

    public Throwable getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFatal() {
        return isFatal;
    }

    @Override
    public String toString() {
        return super.toString() + " {message=\"" + message + "\", isFatal=" + isFatal +
                (exception != null ? ", exception=" + exception.getClass().getSimpleName() : "") + "}";
    }
}