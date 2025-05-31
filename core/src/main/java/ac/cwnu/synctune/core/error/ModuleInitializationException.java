package ac.cwnu.synctune.core.error;

public class ModuleInitializationException extends RuntimeException {
    public ModuleInitializationException(String message) {
        super(message);
    }

    public ModuleInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}