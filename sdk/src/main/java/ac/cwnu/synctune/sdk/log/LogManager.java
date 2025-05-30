package ac.cwnu.synctune.sdk.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogManager는 SLF4J Logger를 관리하는 유틸리티 클래스입니다.
 * 이 클래스는 SLF4J Logger를 생성하고 반환하는 메서드를 제공합니다.
 *
 * <p>이 클래스는 인스턴스화할 수 없으며, 정적 메서드만을 제공합니다.</p>
 */
public final class LogManager {

    private LogManager() {
    }

    /**
     * 지정된 클래스에 대한 Logger를 반환합니다.
     * ```java
     * Logger logger = LogManager.getLogger(MyClass.class);
     * ```
     *
     * @param clazz Logger를 생성할 클래스
     * @return 해당 클래스에 대한 Logger 인스턴스
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 지정된 이름에 대한 Logger를 반환합니다.
     * ```java
     * Logger logger = LogManager.getLogger("MyLogger");
     * ```
     *
     * @param name Logger를 생성할 이름
     * @return 해당 이름에 대한 Logger 인스턴스
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
