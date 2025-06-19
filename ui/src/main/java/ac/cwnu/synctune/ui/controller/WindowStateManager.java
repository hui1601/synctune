package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.event.SystemEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WindowStateManager {
    private static final Logger log = LogManager.getLogger(WindowStateManager.class);
    
    private final Stage stage;
    private final EventPublisher publisher;
    private boolean isMinimized = false;
    private boolean isCloseRequested = false;

    public WindowStateManager(Stage stage, EventPublisher publisher) {
        this.stage = stage;
        this.publisher = publisher;
        attachListeners();
        log.debug("WindowStateManager 초기화 완료");
    }

    private void attachListeners() {
        // 창 닫기 요청 처리 - 즉시 닫지 않고 이벤트만 발행
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

    public void handleCloseRequest(WindowEvent event) {
        if (isCloseRequested) {
            log.debug("이미 종료 요청이 처리 중입니다. 이벤트를 무시합니다.");
            return;
        }

        log.info("사용자가 윈도우 닫기를 요청했습니다. 안전한 종료 절차를 시작합니다.");
        isCloseRequested = true;

        // 창이 즉시 닫히지 않도록 이벤트를 소비(consume)
        event.consume();

        // 1. UI 창 닫힘 이벤트 발행
        log.debug("PlayerUIEvent.MainWindowClosedEvent 발행");
        publisher.publish(new PlayerUIEvent.MainWindowClosedEvent());
        
        // 2. 애플리케이션 종료 요청 이벤트 발행
        log.debug("SystemEvent.RequestApplicationShutdownEvent 발행");
        publisher.publish(new SystemEvent.RequestApplicationShutdownEvent());
        
        // 실제 창 닫기는 UIModule.stop()에서 Core의 지시를 받아 수행
        log.info("종료 이벤트를 발행했습니다. Core 모듈이 안전한 종료를 진행할 것입니다.");
    }

    /**
     * Core에서 지시한 강제 종료 (UIModule.stop()에서 호출)
     */
    public void forceClose() {
        log.debug("Core로부터 강제 종료 지시를 받았습니다.");
        Platform.runLater(() -> {
            try {
                // 이벤트 핸들러를 우회하여 직접 닫기
                stage.setOnCloseRequest(null);
                stage.close();
                log.debug("윈도우가 강제로 닫혔습니다.");
            } catch (Exception e) {
                log.error("윈도우 강제 닫기 중 오류", e);
            }
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

    public boolean isCloseRequested() {
        return isCloseRequested;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        stage.setAlwaysOnTop(alwaysOnTop);
        log.debug("항상 위에 모드: {}", alwaysOnTop);
    }
}