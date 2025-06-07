package ac.cwnu.synctune.sdk.event;

public interface EventPublisher {
    void publish(BaseEvent event);
    void register(Object lister);
    void unregister(Object lister);
}