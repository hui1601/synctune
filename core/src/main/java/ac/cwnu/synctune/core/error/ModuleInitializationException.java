package ac.cwnu.synctune.core.error;

/**
 * 모듈 초기화 중 발생하는 예외를 나타내는 클래스입니다.
 * 모듈이 로드되거나 시작될 때 필요한 초기화 작업에서 문제가 발생할 경우 이 예외가 발생합니다.
 */
public class ModuleInitializationException extends RuntimeException {
    public ModuleInitializationException(String message) {
        super(message);
    }

    public ModuleInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}