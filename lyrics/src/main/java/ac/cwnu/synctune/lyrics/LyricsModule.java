package ac.cwnu.synctune.lyrics;

import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

/**
 * LyricsModule은 SyncTune의 가사 관련 기능을 구현하는 모듈입니다.
 * 샘플 코드입니다.
 */
@ModuleStart
public class LyricsModule extends SyncTuneModule {
    private static final Logger log = LogManager.getLogger(LyricsModule.class);
    @Override
    public void start() {
        log.info("LyricsModule이 시작되었습니다.");
        // 여기에 가사 모듈 초기화 코드를 추가합니다.
    }
    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
        // 여기에 가사 모듈 정리 코드를 추가합니다.
    }
    @Override
    public String getModuleName() {
        return "LyricsModule";
    }
}
