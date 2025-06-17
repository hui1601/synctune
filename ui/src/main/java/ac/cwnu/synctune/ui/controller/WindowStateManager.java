package ac.cwnu.synctune.ui.controller;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;

public class WindowStateManager {
    private static final Logger log = LogManager.getLogger(WindowStateManager.class);
    
    private final Stage stage;
    private final EventPublisher publisher;
    private boolean isMinimized = false;
    private boolean isClosing = false;

    public WindowStateManager(Stage stage, EventPublisher publisher) {
        this.stage = stage;
        this.publisher = publisher;
        attachListeners();
        log.debug("WindowStateManager 초기화 완료");
    }

    private void attachListeners() {
        // 창 닫기 요청 처리
        stage.setOnCloseRequest(this::handleCloseRequest);

        // 최소화/복원 상태 감지
        stage.iconifiedProperty().addListener((obs, wasMinimized, isMinimized) -> {
            this.isMinimized = isMinimized;
            
            if (!isMinimized && wasMinimized) {
                log.debug("윈도우 복원됨");
                publisher.publish(new PlayerUIEvent.MainWindowRestoredEvent());
            }
        });

        // 윈도우 포커스 변경 감지
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                log.debug("윈도우 포커스 획득");
            }
        });

        // 윈도우 크기 변경 감지
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            log.trace("윈도우 너비 변경: {} -> {}", oldWidth, newWidth);
        });

        stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            log.trace("윈도우 높이 변경: {} -> {}", oldHeight, newHeight);
        });
    }

    private void handleCloseRequest(WindowEvent event) {
        if (isClosing) {
            return; // 이미 닫는 중이면 무시
        }

        log.debug("윈도우 닫기 요청됨");
        isClosing = true;

        // 창 닫힘 이벤트 발행
        publisher.publish(new PlayerUIEvent.MainWindowClosedEvent());
        
        // 애플리케이션 종료 요청
        publisher.publish(new SystemEvent.RequestApplicationShutdownEvent());
        
        // 이벤트 처리를 위해 잠시 대기 후 실제 종료
        Platform.runLater(() -> {
            try {
                Thread.sleep(100); // 이벤트 처리 시간 확보
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.exit();
        });
    }

    public void minimizeToSystemTray() {
        stage.setIconified(true);
        log.debug("윈도우를 시스템 트레이로 최소화");
    }

    public void restoreFromSystemTray() {
        stage.setIconified(false);
        stage.toFront();
        log.debug("시스템 트레이에서 윈도우 복원");
    }

    public void toggleFullScreen() {
        stage.setFullScreen(!stage.isFullScreen());
        log.debug("전체화면 모드 토글: {}", stage.isFullScreen());
    }

    public boolean isMinimized() {
        return isMinimized;
    }

    public boolean isClosing() {
        return isClosing;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        stage.setAlwaysOnTop(alwaysOnTop);
        log.debug("항상 위에 모드: {}", alwaysOnTop);
    }
}