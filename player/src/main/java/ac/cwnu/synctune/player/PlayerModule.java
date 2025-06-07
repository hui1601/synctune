package ac.cwnu.synctune.player;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * PlayerModule은 SyncTune의 플레이어 기능을 구현합니다.
 * 최소한의 import만 사용하여 빌드 오류를 방지합니다.
 */
@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // TODO: 실제 오디오 재생 기능은 필요한 이벤트 클래스들이 확인된 후 구현
        log.info("[{}] 모듈 초기화 완료.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        log.info("[{}] 모듈 종료 완료.", getModuleName());
    }

    // TODO: 이벤트 리스너들은 실제 이벤트 클래스가 확인된 후 추가
    // @EventListener
    // public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) { ... }
}