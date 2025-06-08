package ac.cwnu.synctune.sdk.event;

/**
 * 시스템 레벨의 이벤트 (예: 초기화, 종료 등)를 정의합니다.
 */
public class SystemEvent {

    private SystemEvent() {
    } // 인스턴스화 방지

    public static class ApplicationReadyEvent extends BaseEvent {
    } // Core 모듈 초기화 완료 등

    public static class ApplicationShutdownEvent extends BaseEvent {
    } // 종료 요청 또는 진행
    public static class RequestApplicationShutdownEvent extends BaseEvent {
    } // 종료 요청 이벤트, 예: 사용자가 앱을 종료하려고 할 때
}