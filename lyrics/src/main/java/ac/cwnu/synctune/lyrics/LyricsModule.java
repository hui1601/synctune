package ac.cwnu.synctune.lyrics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;

import ac.cwnu.synctune.lyrics.parser.LrcParser;
import ac.cwnu.synctune.lyrics.synchronizer.LyricsTimelineMatcher;
import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;

@Module(name = "Lyrics", version = "1.0.0")
public class LyricsModule extends SyncTuneModule {
    private static final Logger log = LogManager.getLogger(LyricsModule.class);

    // 현재 재생 중인 음악과 가사 정보
    private MusicInfo currentMusic;
    private List<LrcLine> currentLyrics;
    private LrcLine lastPublishedLine;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("LyricsModule이 시작되었습니다.");

        log.info("LyricsModule 초기화 완료.");
    }

    @Override
    public void stop() {
        log.info("LyricsModule이 종료되었습니다.");
    }

    /**
     * 재생 시작 이벤트 리스너
     */
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.info("재생 시작: {}", event.getCurrentMusic().getTitle());
        currentMusic = event.getCurrentMusic();
        lastPublishedLine = null;
        loadLyricsForCurrentMusic();
    }

    /**
     * 재생 진행 이벤트 리스너
     */
    @EventListener
    public void onPlaybackProgress(PlaybackStatusEvent.PlaybackProgressUpdateEvent event) {
        if (currentLyrics != null && !currentLyrics.isEmpty()) {
            updateCurrentLyrics(event.getCurrentTimeMillis());
        }
    }

    /**
     * 현재 음악에 대한 가사 파일 로드 (간소화)
     */
    private void loadLyricsForCurrentMusic() {
        if (currentMusic == null) {
            log.warn("현재 음악 정보가 없습니다.");
            return;
        }

        log.info("가사 로딩 시작: {}", currentMusic.getTitle());
        
        File lrcFile = findLrcFile(currentMusic);
        
        if (lrcFile != null && lrcFile.exists()) {
            try {
                currentLyrics = LrcParser.parse(lrcFile);
                log.info("가사 파일 로드 성공: {} ({}줄)", lrcFile.getName(), currentLyrics.size());
                
                publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), lrcFile.getAbsolutePath()));
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
                
                // 첫 번째 가사 라인 즉시 발행
                if (!currentLyrics.isEmpty()) {
                    publish(new LyricsEvent.LyricsFullTextEvent(
                    currentMusic.getFilePath(),
                    currentLyrics.stream().map(LrcLine::getText).toArray(String[]::new)
                    ));
                    log.info("LyricsFullTextEvent 발행 ({}줄)", currentLyrics.size());
                    LrcLine firstLine = currentLyrics.get(0);
                    publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                    lastPublishedLine = firstLine;
                    log.info("첫 번째 가사 라인 발행: {}", firstLine.getText());
                }
                
            } catch (IOException e) {
                log.error("가사 파일 파싱 실패: {}", e.getMessage());
                handleNoLyrics();
            }
        } else {
            log.info("가사 파일을 찾을 수 없습니다. 샘플 가사 사용");
            loadSampleLyrics();
        }
    }

    /**
     * 샘플 가사 로드
     */
    private void loadSampleLyrics() {
        File sampleLrc = new File("sample/sample.lrc");
        if (sampleLrc.exists()) {
            try {
                currentLyrics = LrcParser.parse(sampleLrc);
                log.info("샘플 가사 로드 성공 ({}줄)", currentLyrics.size());
                
                publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), sampleLrc.getAbsolutePath()));
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
                
                if (!currentLyrics.isEmpty()) {
                    publish(new LyricsEvent.LyricsFullTextEvent(
                    currentMusic.getFilePath(),
                    currentLyrics.stream().map(LrcLine::getText).toArray(String[]::new)
                    ));
                    LrcLine firstLine = currentLyrics.get(0);
                    publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                    lastPublishedLine = firstLine;
                    log.info("샘플 첫 번째 가사 라인 발행: {}", firstLine.getText());
                }
                
            } catch (IOException e) {
                log.error("샘플 가사 파일 파싱 실패", e);
                handleNoLyrics();
            }
        } else {
            handleNoLyrics();
        }
    }

    /**
     * 가사가 없을 때 처리
     */
    private void handleNoLyrics() {
        currentLyrics = null;
        publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
        publish(new LyricsEvent.NextLyricsEvent("가사를 찾을 수 없습니다", 0));
    }

    /**
     * 현재 재생 시간에 맞는 가사 업데이트
     */
    private void updateCurrentLyrics(long currentTimeMillis) {
        if (currentLyrics == null || currentLyrics.isEmpty()) {
            return;
        }

        LrcLine currentLine = LyricsTimelineMatcher.findCurrentLine(currentLyrics, currentTimeMillis);
        
        if (currentLine != null && !currentLine.equals(lastPublishedLine)) {
            log.debug("가사 업데이트: {} ({}ms)", currentLine.getText(), currentLine.getTimeMillis());
            publish(new LyricsEvent.NextLyricsEvent(currentLine.getText(), currentLine.getTimeMillis()));
            lastPublishedLine = currentLine;
        }
    }

    /**
     * 가사 파일 찾기 (간소화)
     */
    private File findLrcFile(MusicInfo music) {
        // 1. MusicInfo에 LRC 경로가 있는 경우
        if (music.getLrcPath() != null && !music.getLrcPath().isEmpty()) {
            File lrcFile = new File(music.getLrcPath());
            if (lrcFile.exists()) {
                return lrcFile;
            }
        }

        // 2. 음악 파일과 같은 디렉토리에서 찾기
        File musicFile = new File(music.getFilePath());
        if (musicFile.exists()) {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile;
            }
        }

        // 3. lyrics 폴더에서 찾기
        File musicFile2 = new File(music.getFilePath());
        String baseName2 = getFileNameWithoutExtension(musicFile2.getName());
        File lrcInLyricsDir = new File("lyrics", baseName2 + ".lrc");
        if (lrcInLyricsDir.exists()) {
            return lrcInLyricsDir;
        }

        return null;
    }

    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * 샘플 LRC 파일 생성 (한글 인코딩 문제 해결)
     */
    
}