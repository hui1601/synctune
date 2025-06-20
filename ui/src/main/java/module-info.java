module synctune.ui.main {
    // JavaFX 모듈들
    requires javafx.controls;
    requires javafx.fxml;
    
    // SyncTune SDK
    requires transitive synctune.sdk.main;
    
    // 로깅
    requires org.slf4j;
    requires java.prefs;
    
    // JavaFX Application 클래스를 위한 exports
    exports ac.cwnu.synctune.ui to javafx.graphics;
    exports ac.cwnu.synctune.ui.view to javafx.fxml;
    exports ac.cwnu.synctune.ui.controller to javafx.fxml;
    
    // JavaFX가 리플렉션으로 접근할 수 있도록 opens
    opens ac.cwnu.synctune.ui to javafx.fxml, javafx.graphics;
    opens ac.cwnu.synctune.ui.view to javafx.fxml;
    opens ac.cwnu.synctune.ui.controller to javafx.fxml;
    opens ac.cwnu.synctune.ui.util to javafx.fxml;
}