package ac.cwnu.synctune;

import ac.cwnu.synctune.core.CoreModule;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        // Core 모듈이 자동으로 모든 모듈을 초기화하고 이벤트를 처리합니다.
        try {
            CoreModule core = CoreModule.initialize("ac.cwnu.synctune");
            core.start();
        } catch (Exception e) {
            log.error("SyncTune 애플리케이션 시작 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
