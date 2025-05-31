package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * PlayerModule은 SyncTune의 플레이어 관련 기능을 구현하는 모듈입니다.
 * 샘플 코드입니다.
 */
@ModuleStart
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger logger = LogManager.getLogger(PlayerModule.class);

    @Override
    public void start() {
        logger.info("PlayerModule이 시작되었습니다.");
    }

    @Override
    public void stop() {
        logger.info("PlayerModule이 종료되었습니다.");
    }

    @EventListener
    public void onPlayerEvent(PlayerUIEvent event) {
        logger.debug("Received player event: {}", event);
    }

    @Override
    public String getModuleName() {
        return "PlayerModule";
    }
}
