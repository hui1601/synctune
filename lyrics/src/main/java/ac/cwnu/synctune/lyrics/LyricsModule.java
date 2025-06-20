package ac.cwnu.synctune.lyrics;

import ac.cwnu.synctune.lyrics.parser.LrcParser;
import ac.cwnu.synctune.lyrics.provider.CurrentLyricsProvider;
import ac.cwnu.synctune.lyrics.synchronizer.LyricsTimelineMatcher;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackProgressUpdateEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackStartedEvent;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.model.MusicInfo;
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

    private CurrentLyricsProvider provider;
    private PlaybackTimeReceiver receiver;
    private MusicInfo currentMusic;
    private List<LrcLine> currentLyrics;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("LyricsModule이 시작되었습니다.");

        // 샘플 LRC 파일 생성 (존재하지 않는 경우)
        createSampleLrcFileIfNotExists();

        log.info("LyricsModule 초기화 완료.");
    }

    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
    }

    @Override
    public String getModuleName() {
        return "LyricsModule";
    }

    @EventListener
    public void onPlaybackStarted(PlaybackStartedEvent event) {
        log.info("PlaybackStartedEvent 수신: {}", event.getCurrentMusic().getTitle());
        currentMusic = event.getCurrentMusic();
        loadLyricsForCurrentMusic();
    }

    @EventListener
    public void onPlaybackProgress(PlaybackProgressUpdateEvent event) {
        if (currentLyrics != null && !currentLyrics.isEmpty()) {
            updateCurrentLyrics(event.getCurrentTimeMillis());
        }
    }

    private void loadLyricsForCurrentMusic() {
        if (currentMusic == null) {
            return;
        }

        File lrcFile = findLrcFile(currentMusic);
        if (lrcFile != null && lrcFile.exists()) {
            try {
                currentLyrics = LrcParser.parse(lrcFile);
                log.info("가사 파일 로드 성공: {} ({}줄)", lrcFile.getName(), currentLyrics.size());
                publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), lrcFile.getAbsolutePath()));
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
            } catch (IOException e) {
                log.error("가사 파일 파싱 실패: {}", e.getMessage(), e);
                currentLyrics = null;
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), false));
            }
        } else {
            log.info("가사 파일을 찾을 수 없습니다: {}", currentMusic.getTitle());
            currentLyrics = null;
            publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
        }
    }

    private void updateCurrentLyrics(long currentTimeMillis) {
        if (currentLyrics == null || currentLyrics.isEmpty()) {
            return;
        }

        LrcLine currentLine = LyricsTimelineMatcher.findCurrentLine(currentLyrics, currentTimeMillis);
        if (currentLine != null) {
            log.debug("현재 가사: {} ({}ms)", currentLine.getText(), currentLine.getTimeMillis());
            publish(new LyricsEvent.NextLyricsEvent(currentLine.getText(), currentLine.getTimeMillis()));
        }
    }

    private File findLrcFile(MusicInfo music) {
        // 1. MusicInfo에 LRC 경로가 설정되어 있는 경우
        if (music.getLrcPath() != null && !music.getLrcPath().isEmpty()) {
            File lrcFile = new File(music.getLrcPath());
            if (lrcFile.exists()) {
                return lrcFile;
            }
        }

        // 2. 음악 파일과 같은 디렉토리에서 같은 이름의 .lrc 파일 찾기
        File musicFile = new File(music.getFilePath());
        String baseName = getFileNameWithoutExtension(musicFile.getName());
        File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
        if (lrcFile.exists()) {
            return lrcFile;
        }

        // 3. 샘플 LRC 파일 사용 (테스트용)
        File sampleLrc = new File("sample/sample.lrc");
        if (sampleLrc.exists()) {
            log.debug("샘플 LRC 파일 사용: {}", sampleLrc.getAbsolutePath());
            return sampleLrc;
        }

        return null;
    }

    private String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private void createSampleLrcFileIfNotExists() {
        File sampleDir = new File("sample");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }

        File sampleLrc = new File(sampleDir, "sample.lrc");
        if (!sampleLrc.exists()) {
            try {
                java.nio.file.Files.write(sampleLrc.toPath(), 
                    """
                    [00:00.00]SyncTune 테스트 가사
                    [00:05.00]이것은 샘플 가사입니다
                    [00:10.00]LyricsModule이 정상적으로 작동하고 있습니다
                    [00:15.00]가사가 시간에 맞춰 표시됩니다
                    [00:20.00]멋진 음악을 들어보세요
                    [00:25.00]SyncTune으로 즐거운 시간 되세요
                    [00:30.00]가사 동기화가 완료되었습니다
                    """.getBytes());
                log.info("샘플 LRC 파일 생성: {}", sampleLrc.getAbsolutePath());
            } catch (IOException e) {
                log.error("샘플 LRC 파일 생성 실패", e);
            }
        }
    }
}