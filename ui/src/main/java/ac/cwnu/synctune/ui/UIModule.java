package ac.cwnu.synctune.ui;

import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * UI 모듈은 SyncTune의 사용자 인터페이스를 담당합니다.
 * 샘플 코드입니다.
 */
@ModuleStart
public class UIModule extends SyncTuneModule implements ModuleLifecycleListener {
    private final static Logger log = LogManager.getLogger(UIModule.class);

    @Override
    public void start() {
        log.info("UIModule이 시작되었습니다.");
        // 여기에 UI 모듈 초기화 코드를 추가합니다.
    }

    @Override
    public void stop() {
        log.info("UIModule이 종료되었습니다.");
        // 여기에 UI 모듈 정리 코드를 추가합니다.
    }

    @Override
    public String getModuleName() {
        return "UIModule";
    }
}
