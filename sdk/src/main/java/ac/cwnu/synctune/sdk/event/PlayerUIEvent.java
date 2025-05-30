package ac.cwnu.synctune.sdk.event;

public class PlayerUIEvent {
    private PlayerUIEvent() {}
/**
 * PlayerModule의 메인 UI 창이 닫혔을 때
 */
    public static class MainWindowClosedEvent extends BaseEvent {}
/**
 * PlayerModule의 메인 UI 창이 다시 열렸거나 최소화에서 복구되었을 때
 */
     public static class MainWindowRestoredEvent extends BaseEvent {}
}