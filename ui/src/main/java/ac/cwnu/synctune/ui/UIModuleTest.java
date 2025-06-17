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

// UIModuleTest.java - UI 모듈 단위 테스트
// ui/src/test/java/ac/cwnu/synctune/ui/UIModuleTest.java

package ac.cwnu.synctune.ui;

import ac.cwnu.synctune.sdk.event.BaseEvent;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UIModuleTest {
    
    private UIModule uiModule;
    private TestEventPublisher testPublisher;
    
    @BeforeAll
    public void setupJavaFX() throws InterruptedException {
        // JavaFX 툴킷 초기화
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> latch.countDown());
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX 초기화 실패");
    }
    
    @BeforeEach
    public void setUp() {
        testPublisher = new TestEventPublisher();
        uiModule = new UIModule();
    }
    
    @Test
    public void testModuleStartup() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                uiModule.start(testPublisher);
                latch.countDown();
            } catch (Exception e) {
                fail("UI 모듈 시작 중 예외 발생: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "UI 모듈 시작 시간 초과");
        assertEquals("UI", uiModule.getModuleName());
    }
    
    @Test
    public void testEventHandling() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            uiModule.start(testPublisher);
            startLatch.countDown();
            
            // 테스트 이벤트 발행
            MusicInfo testMusic = new MusicInfo("Test Song", "Test Artist", "Test Album", "/test/path