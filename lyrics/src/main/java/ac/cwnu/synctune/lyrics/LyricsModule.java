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
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackProgressUpdateEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackStartedEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;

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

        log.info("=== 가사 파일 로딩 시작 ===");
        log.info("음악 파일: {}", currentMusic.getFilePath());
        log.info("음악 제목: {}", currentMusic.getTitle());
        
        File lrcFile = findLrcFile(currentMusic);
        if (lrcFile != null && lrcFile.exists()) {
            try {
                currentLyrics = LrcParser.parse(lrcFile);
                log.info("✅ 가사 파일 로드 성공: {} ({}줄)", lrcFile.getAbsolutePath(), currentLyrics.size());
                
                // 가사 발견 이벤트 발행
                publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), lrcFile.getAbsolutePath()));
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
                
                // 첫 번째 가사 라인이 있으면 미리 표시
                if (!currentLyrics.isEmpty()) {
                    LrcLine firstLine = currentLyrics.get(0);
                    if (firstLine.getTimeMillis() <= 1000) { // 1초 이내 시작하는 가사
                        publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                        lastPublishedLine = firstLine;
                        log.info("첫 번째 가사 라인 발행: {}", firstLine.getText());
                    }
                }
                
            } catch (IOException e) {
                log.error("❌ 가사 파일 파싱 실패: {}", e.getMessage(), e);
                currentLyrics = null;
                publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), false));
                handleNoLyrics();
            }
        } else {
            log.info("❌ 가사 파일을 찾을 수 없습니다. 샘플 가사 사용");
            
            // 가사 파일이 없으면 샘플 LRC 사용
            File sampleLrc = new File("sample/sample.lrc");
            if (sampleLrc.exists()) {
                try {
                    currentLyrics = LrcParser.parse(sampleLrc);
                    log.info("✅ 샘플 가사 파일 로드 성공 ({}줄)", currentLyrics.size());
                    
                    publish(new LyricsEvent.LyricsFoundEvent(currentMusic.getFilePath(), sampleLrc.getAbsolutePath()));
                    publish(new LyricsEvent.LyricsParseCompleteEvent(currentMusic.getFilePath(), true));
                    
                    // 첫 번째 가사 라인 표시
                    if (!currentLyrics.isEmpty()) {
                        LrcLine firstLine = currentLyrics.get(0);
                        publish(new LyricsEvent.NextLyricsEvent(firstLine.getText(), firstLine.getTimeMillis()));
                        lastPublishedLine = firstLine;
                        log.info("샘플 가사 첫 번째 라인 발행: {}", firstLine.getText());
                    }
                    
                } catch (IOException e) {
                    log.error("❌ 샘플 가사 파일 파싱 실패", e);
                    handleNoLyrics();
                }
            } else {
                handleNoLyrics();
            }
        }
        log.info("=== 가사 파일 로딩 완료 ===");
    }
    
    /**
     * 가사가 없을 때 처리
     */
    private void handleNoLyrics() {
        currentLyrics = null;
        publish(new LyricsEvent.LyricsNotFoundEvent(currentMusic.getFilePath()));
        // 가사 없음 메시지 발행
        publish(new LyricsEvent.NextLyricsEvent("가사를 찾을 수 없습니다", 0));
        log.info("가사 없음 메시지 발행");
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
     * 음악 파일에 해당하는 LRC 파일 찾기 - 강화된 버전
     */
    private File findLrcFile(MusicInfo music) {
        log.info("🔍 LRC 파일 검색 시작");
        
        // 1. MusicInfo에 LRC 경로가 설정되어 있는 경우
        if (music.getLrcPath() != null && !music.getLrcPath().isEmpty()) {
            log.info("1️⃣ MusicInfo에서 LRC 경로 확인: {}", music.getLrcPath());
            File lrcFile = new File(music.getLrcPath());
            if (lrcFile.exists()) {
                log.info("✅ MusicInfo에서 LRC 발견: {}", lrcFile.getAbsolutePath());
                return lrcFile;
            } else {
                log.info("❌ MusicInfo LRC 경로가 존재하지 않음: {}", music.getLrcPath());
            }
        } else {
            log.info("1️⃣ MusicInfo에 LRC 경로 없음");
        }

        // 2. 음악 파일과 같은 디렉토리에서 같은 이름의 .lrc 파일 찾기
        File musicFile = new File(music.getFilePath());
        log.info("2️⃣ 음악 파일 경로: {}", musicFile.getAbsolutePath());
        log.info("2️⃣ 음악 파일 존재 여부: {}", musicFile.exists());
        
        if (musicFile.exists()) {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            log.info("2️⃣ 기본 파일명 (확장자 제거): {}", baseName);
            
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            log.info("2️⃣ 찾는 LRC 파일 경로: {}", lrcFile.getAbsolutePath());
            log.info("2️⃣ LRC 파일 존재 여부: {}", lrcFile.exists());
            
            if (lrcFile.exists()) {
                log.info("✅ 같은 디렉토리에서 LRC 발견: {}", lrcFile.getAbsolutePath());
                return lrcFile;
            }
            
            // 다양한 파일명 패턴으로 시도
            String[] possibleNames = {
                baseName + ".lrc",
                baseName.toLowerCase() + ".lrc", 
                baseName.toUpperCase() + ".lrc",
                music.getTitle() + ".lrc",
                music.getTitle().toLowerCase() + ".lrc"
            };
            
            for (String name : possibleNames) {
                File tryFile = new File(musicFile.getParent(), name);
                log.info("2️⃣ 추가 시도: {}", tryFile.getAbsolutePath());
                if (tryFile.exists()) {
                    log.info("✅ 패턴 매칭으로 LRC 발견: {}", tryFile.getAbsolutePath());
                    return tryFile;
                }
            }
        } else {
            log.info("❌ 음악 파일이 존재하지 않음: {}", music.getFilePath());
        }

        // 3. lyrics 폴더에서 찾기
        log.info("3️⃣ lyrics 폴더에서 검색");
        File musicFile2 = new File(music.getFilePath());
        String baseName2 = getFileNameWithoutExtension(musicFile2.getName());
        File lrcInLyricsDir = new File("lyrics", baseName2 + ".lrc");
        log.info("3️⃣ lyrics 폴더 LRC 경로: {}", lrcInLyricsDir.getAbsolutePath());
        log.info("3️⃣ lyrics 폴더 LRC 존재: {}", lrcInLyricsDir.exists());
        
        if (lrcInLyricsDir.exists()) {
            log.info("✅ lyrics 폴더에서 LRC 발견: {}", lrcInLyricsDir.getAbsolutePath());
            return lrcInLyricsDir;
        }

        // 4. 음악 제목으로 찾기 (공백을 언더스코어로 변환)
        log.info("4️⃣ 제목 기반 검색");
        String titleBasedName = music.getTitle().replaceAll("[^a-zA-Z0-9가-힣]", "_");
        File titleBasedLrc = new File("lyrics", titleBasedName + ".lrc");
        log.info("4️⃣ 제목 기반 LRC 경로: {}", titleBasedLrc.getAbsolutePath());
        log.info("4️⃣ 제목 기반 LRC 존재: {}", titleBasedLrc.exists());
        
        if (titleBasedLrc.exists()) {
            log.info("✅ 제목 기반으로 LRC 발견: {}", titleBasedLrc.getAbsolutePath());
            return titleBasedLrc;
        }

        // 5. 현재 디렉토리의 모든 .lrc 파일 출력 (디버깅용)
        if (musicFile.exists()) {
            File parentDir = musicFile.getParentFile();
            log.info("5️⃣ 디버깅: {} 디렉토리의 모든 파일 확인", parentDir.getAbsolutePath());
            File[] allFiles = parentDir.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    if (file.getName().toLowerCase().endsWith(".lrc")) {
                        log.info("🎵 발견된 LRC 파일: {}", file.getName());
                    }
                }
            }
        }

        log.info("❌ LRC 파일을 찾을 수 없음: {}", music.getTitle());
        return null;
    }

    /**
     * 파일명에서 확장자 제거
     */
    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        String result = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        log.debug("파일명 '{}' -> 기본명 '{}'", fileName, result);
        return result;
    }

    /**
     * 샘플 LRC 파일 생성 (테스트용)
     */
    private void createSampleLrcFileIfNotExists() {
        File sampleDir = new File("sample");
        if (!sampleDir.exists()) {
            boolean created = sampleDir.mkdirs();
            log.debug("sample 디렉토리 생성: {}", created);
        }

        File sampleLrc = new File(sampleDir, "sample.lrc");
        if (!sampleLrc.exists()) {
            try {
                String sampleLyrics = """
                    [00:00.00]SyncTune 테스트 가사
                    [00:05.00]이것은 샘플 가사입니다
                    [00:10.00]LyricsModule이 정상적으로 작동하고 있습니다
                    [00:15.00]가사가 시간에 맞춰 표시됩니다
                    [00:20.00]멋진 음악을 들어보세요
                    [00:25.00]SyncTune으로 즐거운 시간 되세요
                    [00:30.00]가사 동기화가 완료되었습니다
                    [00:35.00]🎵 음악과 함께 즐기세요 🎵
                    [00:40.00]감사합니다!
                    [00:45.00]이 가사는 모든 곡에 공통으로 사용됩니다
                    [00:50.00]실제 음악에는 해당 LRC 파일을 추가하세요
                    """;
                java.nio.file.Files.write(sampleLrc.toPath(), sampleLyrics.getBytes());
                log.info("샘플 LRC 파일 생성: {}", sampleLrc.getAbsolutePath());
            } catch (IOException e) {
                log.error("샘플 LRC 파일 생성 실패", e);
            }
        } else {
            log.debug("샘플 LRC 파일이 이미 존재함: {}", sampleLrc.getAbsolutePath());
        }
        
        // lyrics 디렉토리도 생성
        File lyricsDir = new File("lyrics");
        if (!lyricsDir.exists()) {
            boolean created = lyricsDir.mkdirs();
            log.debug("lyrics 디렉토리 생성: {}", created);
        }
    }
}