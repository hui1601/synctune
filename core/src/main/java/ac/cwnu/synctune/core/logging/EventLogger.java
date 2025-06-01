package ac.cwnu.synctune.core.logging;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

/**
 * 모든 이벤트 로깅을 위한 클래스입니다.
 */
public class EventLogger {
    private static final Logger log = LogManager.getLogger(EventLogger.class);

    /**
     * 이벤트를 로깅합니다.
     *
     * @param event 로깅할 이벤트 객체
     */
    @EventListener
    public void baseEventListener(BaseEvent event) {
        if (event == null) {
            log.warn("Received null event in EventLogger");
            return;
        }
        log.debug(event.toString());
    }
}
