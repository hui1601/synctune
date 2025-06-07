package ac.cwnu.synctune.sdk.event;

public interface EventPublisher {
    void publish(BaseEvent event);
}