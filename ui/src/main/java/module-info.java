module synctune.ui.main {
    // JavaFX 모듈들
    requires javafx.controls;
    requires javafx.fxml;
    
    // SyncTune SDK
    requires transitive synctune.sdk.main;
    
    // 로깅
    requires org.slf4j;
    
    // UI 패키지 내보내기
    exports ac.cwnu.synctune.ui;
    exports ac.cwnu.synctune.ui.view;
    exports ac.cwnu.synctune.ui.controller;
    exports ac.cwnu.synctune.ui.component;
}