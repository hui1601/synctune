package ac.cwnu.synctune.ui.controller;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * 메인 윈도우의 상태 변화(닫힘, 복원 등)를 감지하여
 * PlayerUIEvent를 발행하는 매니저 클래스
 */
public class WindowStateManager {
    private final Stage stage;
    private final EventPublisher publisher;

    /**
     * 생성자
     * @param stage 메인 애플리케이션 윈도우
     * @param publisher 이벤트 발행을 위한 EventPublisher
     */
    public WindowStateManager(Stage stage, EventPublisher publisher) {
        this.stage = stage;
        this.publisher = publisher;
        attachListeners();
    }

    private void attachListeners() {
        // 창 닫힘 이벤트
        stage.setOnCloseRequest((WindowEvent event) ->
            publisher.publish(new PlayerUIEvent.MainWindowClosedEvent())
        );

        // 최소화 해제(복원) 이벤트
        stage.iconifiedProperty().addListener((obs, wasMinimized, isMinimized) -> {
            if (!isMinimized) {
                publisher.publish(new PlayerUIEvent.MainWindowRestoredEvent());
            }
        });
    }
}
