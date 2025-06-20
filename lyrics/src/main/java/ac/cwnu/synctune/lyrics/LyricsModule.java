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

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Module(name = "Lyrics", version = "1.0.0")
public class LyricsModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(LyricsModule.class);

    // 현재 재생 중인 음악과 가사 정보
    private MusicInfo currentMusic;
    private List<LrcLine> currentLyrics;
    private LrcLine lastPublishedLine; // 중복 발행 방지

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("LyricsModule이 시작되었습니다.");

        // 샘플 LRC 파일 생성 (존재하지 않는 경우)
        createSampleLrcFileIfNotExists();

        log.info("LyricsModule 초기화 완료. 이벤트 리스너 등록됨.");
    }

    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
    }

    @Override
    public String getModuleName() {
        return "LyricsModule";
    }

    /**
     * 재생 시작 이벤트 리스너 - 새 곡에 대한 가사 로드
     */
    @EventListener
    public void onPlaybackStarted(PlaybackStartedEvent event) {
        log.info("PlaybackStartedEvent 수신: {}", event.getCurrentMusic().getTitle());
        currentMusic = event.getCurrentMusic();
        lastPublishedLine = null; // 새 곡이므로 초기화
        loadLyricsForCurrentMusic();
    }

    /**
     * 재생 진행 이벤트 리스너 - 현재 시간에 맞는 가사 찾기 및 발행
     */
    @EventListener
    public void onPlaybackProgress(PlaybackProgressUpdateEvent event) {
        // 현재 가사가 로드되어 있고, 재생 중인 음악이 있을 때만 처리
        if (currentLyrics != null && !currentLyrics.isEmpty() && currentMusic != null) {
            updateCurrentLyrics(event.getCurrentTimeMillis());
        }
    }

    /**
     * 현재 음악에 대한 가사 파일을 찾아서 로드
     */
    private void loadLyricsForCurrentMusic() {
        if (currentMusic == null) {
            log.warn("현재 음악 정보가 없습니다.");
            return;
        }

        File lrcFile = findLrcFile(currentMusic);
        if (lrcFile != null && lrcFile.exists()) {
            try {
                currentLyrics = LrcParser.parse(lrcFile);
                log.info("가사 파일 로드 성공: {} ({}줄)", lrcFile.getName(), currentLyrics.size());
                
                // 가사 발견 이벤트 발행
                publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), lrcFile.getAbsolutePath()));
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
                
                // 첫 번째 가사 라인이 있으면 미리 표시
                if (!currentLyrics.isEmpty()) {
                    LrcLine firstLine = currentLyrics.get(0);
                    if (firstLine.getTimeMillis() <= 1000) { // 1초 이내 시작하는 가사
                        publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                        lastPublishedLine = firstLine;
                    }
                }
                
            } catch (IOException e) {
                log.error("가사 파일 파싱 실패: {}", e.getMessage(), e);
                currentLyrics = null;
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), false));
                publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
            }
        } else {
            log.info("가사 파일을 찾을 수 없습니다: {}", currentMusic.getTitle());
            currentLyrics = null;
            publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
        }
    }

    /**
     * 현재 재생 시간에 맞는 가사를 찾아서 이벤트 발행
     */
    private void updateCurrentLyrics(long currentTimeMillis) {
        if (currentLyrics == null || currentLyrics.isEmpty()) {
            return;
        }

        LrcLine currentLine = LyricsTimelineMatcher.findCurrentLine(currentLyrics, currentTimeMillis);
        
        // 새로운 가사 라인이 있고, 이전에 발행한 라인과 다를 때만 발행 (중복 방지)
        if (currentLine != null && !currentLine.equals(lastPublishedLine)) {
            log.debug("현재 가사 업데이트: {} ({}ms)", currentLine.getText(), currentLine.getTimeMillis());
            publish(new LyricsEvent.NextLyricsEvent(currentLine.getText(), currentLine.getTimeMillis()));
            lastPublishedLine = currentLine;
        }
    }

    /**
     * 음악 파일에 해당하는 LRC 파일 찾기
     */
    private File findLrcFile(MusicInfo music) {
        // 1. MusicInfo에 LRC 경로가 설정되어 있는 경우
        if (music.getLrcPath() != null && !music.getLrcPath().isEmpty()) {
            File lrcFile = new File(music.getLrcPath());
            if (lrcFile.exists()) {
                log.debug("MusicInfo에서 LRC 경로 사용: {}", lrcFile.getAbsolutePath());
                return lrcFile;
            }
        }

        // 2. 음악 파일과 같은 디렉토리에서 같은 이름의 .lrc 파일 찾기
        File musicFile = new File(music.getFilePath());
        if (musicFile.exists()) {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                log.debug("음악 파일과 같은 디렉토리에서 LRC 발견: {}", lrcFile.getAbsolutePath());
                return lrcFile;
            }
        }

        // 3. lyrics 폴더에서 찾기
        File musicFile2 = new File(music.getFilePath());
        String baseName2 = getFileNameWithoutExtension(musicFile2.getName());
        File lrcInLyricsDir = new File("lyrics", baseName2 + ".lrc");
        if (lrcInLyricsDir.exists()) {
            log.debug("lyrics 폴더에서 LRC 발견: {}", lrcInLyricsDir.getAbsolutePath());
            return lrcInLyricsDir;
        }

        // 4. 샘플 LRC 파일 사용 (테스트용)
        File sampleLrc = new File("sample/sample.lrc");
        if (sampleLrc.exists()) {
            log.debug("샘플 LRC 파일 사용: {}", sampleLrc.getAbsolutePath());
            return sampleLrc;
        }

        log.debug("LRC 파일을 찾을 수 없음: {}", music.getTitle());
        return null;
    }

    /**
     * 파일명에서 확장자 제거
     */
    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * 샘플 LRC 파일 생성 (테스트용)
     */
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
                    [00:35.00]🎵 음악과 함께 즐기세요 🎵
                    [00:40.00]감사합니다!
                    """.getBytes());
                log.info("샘플 LRC 파일 생성: {}", sampleLrc.getAbsolutePath());
            } catch (IOException e) {
                log.error("샘플 LRC 파일 생성 실패", e);
            }
        }
    }
}