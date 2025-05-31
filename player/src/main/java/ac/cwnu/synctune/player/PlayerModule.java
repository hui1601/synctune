package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlayerUIEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * PlayerModule은 SyncTune의 플레이어 관련 기능을 구현하는 모듈입니다.
 * 샘플 코드입니다.
 */
@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("PlayerModule이 시작되었습니다.");
    }

    @Override
    public void stop() {
        log.info("PlayerModule이 종료되었습니다.");
    }

    @EventListener
    public void onPlayerEvent(PlayerUIEvent event) {
        log.debug("Received player event: {}", event);
    }
}
