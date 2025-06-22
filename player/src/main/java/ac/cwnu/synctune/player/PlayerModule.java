package ac.cwnu.synctune.player;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;

@Module(name = "Player", version = "1.0.0")
public class PlayerModule extends SyncTuneModule {
    private static final Logger log = LogManager.getLogger(PlayerModule.class);
    
    // 간소화된 상태 관리
    private MusicInfo currentMusic;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong currentPosition = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    
    // 진행 상황 업데이트용 스케줄러
    private ScheduledExecutorService scheduler;

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] 시작되었습니다.", getModuleName());
        
        // 스케줄러 초기화
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerModule-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        // 샘플 음악 준비
        prepareSampleMusic();
        
        log.info("[{}] 모듈 초기화 완료.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        
        stopPlayback();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        log.info("[{}] 모듈 종료 완료.", getModuleName());
    }

    @EventListener
    public void onPlayRequest(MediaControlEvent.RequestPlayEvent event) {
        log.info("[{}] 재생 요청 수신", getModuleName());
        
        MusicInfo musicToPlay = event.getMusicToPlay();
        if (musicToPlay != null) {
            playMusic(musicToPlay);
        } else if (currentMusic != null) {
            if (isPaused.get()) {
                resumePlayback();
            } else {
                playMusic(currentMusic);
            }
        } else {
            // 기본 샘플 음악 재생
            playMusic(createSampleMusic());
        }
    }

    @EventListener
    public void onPauseRequest(MediaControlEvent.RequestPauseEvent event) {
        log.info("[{}] 일시정지 요청 수신", getModuleName());
        pausePlayback();
    }

    @EventListener
    public void onStopRequest(MediaControlEvent.RequestStopEvent event) {
        log.info("[{}] 정지 요청 수신", getModuleName());
        stopPlayback();
    }

    @EventListener
    public void onSeekRequest(MediaControlEvent.RequestSeekEvent event) {
        log.info("[{}] 탐색 요청 수신: {}ms", getModuleName(), event.getPositionMillis());
        long seekPos = Math.max(0, Math.min(event.getPositionMillis(), totalDuration.get()));
        currentPosition.set(seekPos);
    }

    /**
     * 음악 재생 (시뮬레이션)
     */
    private void playMusic(MusicInfo music) {
        if (music == null) return;
        
        log.info("[{}] 음악 재생 시작: {}", getModuleName(), music.getTitle());
        
        currentMusic = music;
        totalDuration.set(music.getDurationMillis());
        currentPosition.set(0);
        isPlaying.set(true);
        isPaused.set(false);
        
        // 재생 시작 이벤트 발행
        publish(new PlaybackStatusEvent.PlaybackStartedEvent(music));
        
        // 진행 상황 업데이트 시작
        startProgressUpdates();
    }

    private void pausePlayback() {
        if (isPlaying.get()) {
            isPlaying.set(false);
            isPaused.set(true);
            publish(new PlaybackStatusEvent.PlaybackPausedEvent());
            log.info("[{}] 일시정지됨", getModuleName());
        }
    }

    private void resumePlayback() {
        if (isPaused.get()) {
            isPlaying.set(true);
            isPaused.set(false);
            startProgressUpdates();
            log.info("[{}] 재생 재개됨", getModuleName());
        }
    }

    private void stopPlayback() {
        isPlaying.set(false);
        isPaused.set(false);
        currentPosition.set(0);
        publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
        log.info("[{}] 정지됨", getModuleName());
    }

    /**
     * 진행 상황 업데이트 시작
     */
    private void startProgressUpdates() {
        if (scheduler == null || scheduler.isShutdown()) return;
        
        scheduler.scheduleAtFixedRate(() -> {
            if (isPlaying.get()) {
                long current = currentPosition.addAndGet(500); // 0.5초씩 증가
                long total = totalDuration.get();
                
                // 진행 상황 이벤트 발행
                publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(current, total));
                
                // 재생 완료 체크
                if (current >= total) {
                    stopPlayback();
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * 샘플 음악 준비
     */
    private void prepareSampleMusic() {
        MusicInfo sample = createSampleMusic();
        log.info("[{}] 샘플 음악 준비됨: {}", getModuleName(), sample.getTitle());
    }

    private MusicInfo createSampleMusic() {
        return new MusicInfo(
            "샘플 테스트 음악",
            "SyncTune System",
            "Test Album",
            findFirstMusicFile(), // 실제 파일이 있으면 사용, 없으면 샘플 경로
            180000L, // 3분
            "sample/sample.lrc"
        );
    }

    /**
     * 실제 음악 파일 찾기 (간소화)
     */
    private String findFirstMusicFile() {
        File musicDir = new File("music");
        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp3") || lower.endsWith(".wav") || 
                       lower.endsWith(".flac") || lower.endsWith(".m4a");
            });
            
            if (files != null && files.length > 0) {
                log.info("[{}] 실제 음악 파일 발견: {}", getModuleName(), files[0].getName());
                return files[0].getAbsolutePath();
            }
        }
        
        // 실제 파일이 없으면 샘플 경로 반환
        return "sample/test.mp3";
    }

    // 상태 조회 메서드들
    public boolean isCurrentlyPlaying() {
        return isPlaying.get();
    }

    public boolean isCurrentlyPaused() {
        return isPaused.get();
    }

    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }

    public long getCurrentPosition() {
        return currentPosition.get();
    }

    public long getTotalDuration() {
        return totalDuration.get();
    }
}