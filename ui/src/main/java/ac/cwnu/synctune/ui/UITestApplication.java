package ac.cwnu.synctune.ui;

import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.MainApplicationWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;

/**
 * UI 모듈을 단독으로 테스트하기 위한 애플리케이션
 */
public class UITestApplication extends Application {
    private static final Logger log = LogManager.getLogger(UITestApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("UI 테스트 애플리케이션 시작");
        
        // 테스트용 EventPublisher
        EventPublisher testPublisher = new TestEventPublisher();
        
        // 메인 윈도우 생성 및 표시
        MainApplicationWindow mainWindow = new MainApplicationWindow(testPublisher);
        mainWindow.show();
        
        log.info("UI 테스트 애플리케이션 준비 완료");
    }
    
    /**
     * 테스트용 EventPublisher 구현
     */
    private static class TestEventPublisher implements EventPublisher {
        @Override
        public void publish(BaseEvent event) {
            log.info("테스트 이벤트 발행: {}", event);
        }
    }
    
    public static void main(String[] args) {
        // 로깅 설정
        System.setProperty("logback.configurationFile", "logback-test.xml");
        
        // JavaFX 애플리케이션 실행
        launch(args);
    }
}