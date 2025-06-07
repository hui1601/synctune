package ac.cwnu.synctune.lyrics;

import ac.cwnu.synctune.lyrics.synchronizer.PlaybackTimeReceiver;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * LyricsModule은 SyncTune의 가사 관련 기능을 구현하는 모듈입니다.
 * 샘플 코드입니다.
 */
@Module(name = "Lyrics", version = "1.0.0")
public class LyricsModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(LyricsModule.class);

    private PlaybackTimeReceiver playbackTimeReceiver;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("LyricsModule이 시작되었습니다.");

        playbackTimeReceiver = new PlaybackTimeReceiver();

        publisher.register(playbackTimeReceiver);
    }

    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
        
        if(playbackTimeReceiver != null){
            eventPublisher.unregister(playbackTimeReceiver);
        }
    }

    @Override
    public String getModuleName() {
        return "LyricsModule";
    }
}
