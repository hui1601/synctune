package ac.cwnu.synctune.sdk.event;

import java.time.LocalDateTime;

/**
 * 모든 이벤트의 기본이 되는 추상 클래스입니다.
 * 모든 이벤트는 발생 시간을 가집니다.
 */
public abstract class BaseEvent {
    private final LocalDateTime timestamp;

    public BaseEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [timestamp=" + timestamp + "]";
    }
}