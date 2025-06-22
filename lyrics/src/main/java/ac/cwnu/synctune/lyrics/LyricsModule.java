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
     * 재생 시작 이벤트 리스너 - 개선된 버전
     */
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.info("재생 시작: {}", event.getCurrentMusic().getTitle());
        currentMusic = event.getCurrentMusic();
        lastPublishedLine = null;
        
        // 가사 로딩을 별도 스레드에서 처리
        loadLyricsAsync();
    }

    /**
     * 비동기 가사 로딩 (UI 블로킹 방지)
     */
    private void loadLyricsAsync() {
        Thread lyricsLoadThread = new Thread(() -> {
            try {
                loadLyricsForCurrentMusic();
            } catch (Exception e) {
                log.error("가사 로딩 중 오류 발생", e);
                publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
                publish(new LyricsEvent.NextLyricsEvent("가사 로딩 중 오류가 발생했습니다", 0));
            }
        });
        lyricsLoadThread.setName("LyricsLoader-" + currentMusic.getTitle().replaceAll("[^a-zA-Z0-9]", ""));
        lyricsLoadThread.setDaemon(true);
        lyricsLoadThread.start();
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
     * 현재 음악에 대한 가사 파일 로드 (개선된 버전)
     */
    private void loadLyricsForCurrentMusic() {
        if (currentMusic == null) {
            log.warn("현재 음악 정보가 없습니다.");
            return;
        }

        log.info("가사 로딩 시작: {}", currentMusic.getTitle());
        
        // 1. MusicInfo에 이미 LRC 경로가 설정되어 있는 경우
        if (currentMusic.getLrcPath() != null && !currentMusic.getLrcPath().isEmpty()) {
            File lrcFile = new File(currentMusic.getLrcPath());
            if (lrcFile.exists()) {
                log.info("MusicInfo에서 LRC 경로 사용: {}", lrcFile.getAbsolutePath());
                loadLrcFile(lrcFile);
                return;
            } else {
                log.warn("MusicInfo의 LRC 경로가 유효하지 않음: {}", currentMusic.getLrcPath());
            }
        }
        
        // 2. 음악 파일과 동일한 이름의 LRC 파일 찾기
        File musicFile = new File(currentMusic.getFilePath());
        File lrcFile = findLrcFileForMusic(musicFile);
        
        if (lrcFile != null && lrcFile.exists()) {
            log.info("음악 파일과 연결된 LRC 파일 발견: {}", lrcFile.getAbsolutePath());
            loadLrcFile(lrcFile);
        } else {
            log.info("LRC 파일을 찾을 수 없음. 샘플 가사 시도");
            loadSampleLyrics();
        }
    }

    /**
     * 음악 파일에 대응하는 LRC 파일을 찾는 개선된 메서드
     */
    private File findLrcFileForMusic(File musicFile) {
        if (!musicFile.exists()) {
            log.warn("음악 파일이 존재하지 않음: {}", musicFile.getAbsolutePath());
            return null;
        }
        
        String baseName = getFileNameWithoutExtension(musicFile.getName());
        String parentDir = musicFile.getParent();
        
        // 검색할 경로들의 우선순위
        String[] searchPaths = {
            // 1. 음악 파일과 같은 디렉토리
            parentDir + File.separator + baseName + ".lrc",
            
            // 2. 음악 파일 디렉토리의 lyrics 서브폴더
            parentDir + File.separator + "lyrics" + File.separator + baseName + ".lrc",
            
            // 3. 프로젝트 루트의 lyrics 폴더
            "lyrics" + File.separator + baseName + ".lrc",
            
            // 4. 대소문자 다른 확장자들 시도
            parentDir + File.separator + baseName + ".LRC",
            parentDir + File.separator + baseName + ".Lrc"
        };
        
        for (String path : searchPaths) {
            File lrcFile = new File(path);
            if (lrcFile.exists() && lrcFile.isFile()) {
                log.debug("LRC 파일 발견: {} -> {}", musicFile.getName(), lrcFile.getAbsolutePath());
                return lrcFile;
            }
        }
        
        // 5. 유사한 이름의 LRC 파일 찾기 (부분 일치)
        try {
            File[] filesInDir = new File(parentDir).listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".lrc"));
            
            if (filesInDir != null) {
                String musicBaseName = baseName.toLowerCase();
                
                for (File file : filesInDir) {
                    String lrcBaseName = getFileNameWithoutExtension(file.getName()).toLowerCase();
                    
                    // 부분 일치 체크 (음악 파일명의 70% 이상 일치)
                    if (calculateSimilarity(musicBaseName, lrcBaseName) > 0.7) {
                        log.info("유사한 이름의 LRC 파일 발견: {} -> {}", musicFile.getName(), file.getName());
                        return file;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("유사한 LRC 파일 검색 중 오류", e);
        }
        
        return null;
    }

    /**
     * 두 문자열의 유사도 계산 (단순한 Levenshtein 거리 기반)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * LRC 파일을 로드하는 통합 메서드
     */
    private void loadLrcFile(File lrcFile) {
        try {
            currentLyrics = LrcParser.parse(lrcFile);
            log.info("가사 파일 로드 성공: {} ({}줄)", lrcFile.getName(), currentLyrics.size());
            
            publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), lrcFile.getAbsolutePath()));
            publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
            
            if (!currentLyrics.isEmpty()) {
                // 전체 가사 이벤트 발행
                publish(new LyricsEvent.LyricsFullTextEvent(
                    currentMusic.getFilePath(),
                    currentLyrics
                ));
                log.info("LyricsFullTextEvent 발행 ({}줄)", currentLyrics.size());
                
                // 첫 번째 가사 라인 발행
                LrcLine firstLine = currentLyrics.get(0);
                publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                lastPublishedLine = firstLine;
                log.info("첫 번째 가사 라인 발행: {}", firstLine.getText());
            } else {
                handleNoLyrics("가사 파일이 비어있습니다");
            }
            
        } catch (IOException e) {
            log.error("가사 파일 파싱 실패: {} - {}", lrcFile.getName(), e.getMessage());
            publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), false));
            handleNoLyrics("가사 파일 파싱에 실패했습니다");
        }
    }

    /**
     * 샘플 가사 로드
     */
    private void loadSampleLyrics() {
        File sampleLrc = new File("sample/sample.lrc");
        if (sampleLrc.exists()) {
            log.info("샘플 LRC 파일 사용");
            loadLrcFile(sampleLrc);
        } else {
            handleNoLyrics("가사 파일을 찾을 수 없습니다");
        }
    }

    /**
     * 가사가 없을 때 처리 (개선된 버전)
     */
    private void handleNoLyrics(String reason) {
        currentLyrics = null;
        log.info("가사 없음: {}", reason);
        
        publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
        publish(new LyricsEvent.NextLyricsEvent(reason, 0));
        
        // 빈 가사 목록으로 UI 초기화
        publish(new LyricsEvent.LyricsFullTextEvent(currentMusic.getFilePath(), List.of()));
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

    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
}