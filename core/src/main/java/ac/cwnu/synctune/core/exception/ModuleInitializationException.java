package ac.cwnu.synctune.core.exception;

public class ModuleInitializationException extends RuntimeException {
    public ModuleInitializationException(String message) {
        super(message);
    }

    public ModuleInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}