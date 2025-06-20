package ac.cwnu.synctune.lyrics;

import ac.cwnu.synctune.lyrics.parser.LrcParser;
import ac.cwnu.synctune.lyrics.provider.CurrentLyricsProvider;
import ac.cwnu.synctune.lyrics.synchronizer.LyricsTimelineMatcher;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackProgressUpdateEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import ac.cwnu.synctune.lyrics.synchronizer.PlaybackTimeReceiver;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Module(name = "Lyrics", version = "1.0.0")
public class LyricsModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(LyricsModule.class);

    private long currentTimeMillis = 0;
    private CurrentLyricsProvider provider;
    private PlaybackTimeReceiver receiver;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("LyricsModule이 시작되었습니다.");

        // ★ 예시: 실제 사용할 LRC 경로
        File lrcFile = new File("sample/sample.lrc");
        try {
            List<LrcLine> parsed = LrcParser.parse(lrcFile);
        if (parsed == null || parsed.isEmpty()) {
            log.warn("가사 파일 파싱 실패 또는 비어 있음: {}", lrcFile.getPath());
            return;
        }

        provider = new CurrentLyricsProvider(parsed, publisher);
        receiver = new PlaybackTimeReceiver(provider);

        } catch (IOException e){
            log.error("LRC 파일 파싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
    }

    @Override
    public String getModuleName() {
        return "LyricsModule";
    }
}
