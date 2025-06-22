package ac.cwnu.synctune.player;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

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
    
    // 실제 오디오 재생을 위한 컴포넌트들
    private AudioInputStream audioInputStream;
    private AudioInputStream decodedAudioInputStream;
    private Clip audioClip;
    private FloatControl volumeControl;
    
    // 현재 상태 관리
    private MusicInfo currentMusic;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong currentPosition = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong pausePosition = new AtomicLong(0);
    
    // 진행 상황 업데이트용 스케줄러
    private ScheduledExecutorService scheduler;
    private boolean isSimulationMode = false;

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
        
        // 지원 가능한 오디오 포맷 로깅
        logSupportedFormats();
        
        log.info("[{}] 모듈 초기화 완료.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] 종료됩니다.", getModuleName());
        
        stopPlayback();
        releaseResources();
        
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
            log.warn("재생할 음악이 지정되지 않았습니다.");
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
        seekTo(event.getPositionMillis());
    }
    @EventListener
    public void onNextMusicRequest(MediaControlEvent.RequestNextMusicEvent event) {
        log.info("[{}] 다음 곡 요청 수신", getModuleName());
        // 다음 곡 재생 로직 구현 필요
        // 예: playNextTrack();
    }

    @EventListener  
    public void onPreviousMusicRequest(MediaControlEvent.RequestPreviousMusicEvent event) {
        log.info("[{}] 이전 곡 요청 수신", getModuleName());
        // 이전 곡 재생 로직 구현 필요
        // 예: playPreviousTrack();
        }
    

    /**
     * 지원 가능한 오디오 포맷 로깅
     */
    private void logSupportedFormats() {
        try {
            AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
            log.info("지원되는 오디오 파일 형식:");
            for (AudioFileFormat.Type type : types) {
                log.info("  - {}", type.toString());
            }
        } catch (Exception e) {
            log.debug("지원 형식 조회 실패: {}", e.getMessage());
        }
    }

    /**
     * 음악 재생 시작
     */
    private void playMusic(MusicInfo music) {
        if (music == null) return;
        
        log.info("[{}] 음악 재생 시작: {}", getModuleName(), music.getTitle());
        
        // 기존 재생 정지
        stopPlayback();
        
        currentMusic = music;
        File musicFile = new File(music.getFilePath());
        
        if (!musicFile.exists()) {
            log.warn("음악 파일을 찾을 수 없습니다: {} (시뮬레이션 모드로 진행)", music.getFilePath());
            startSimulationMode(music);
        } else {
            try {
                // 실제 오디오 파일 로드
                if (loadAudioFile(musicFile)) {
                    // 실제 재생 시작
                    if (audioClip != null) {
                        audioClip.setFramePosition(0);
                        audioClip.start();
                        isPlaying.set(true);
                        isPaused.set(false);
                        isSimulationMode = false;
                        
                        // 실제 재생 시간 계산
                        calculateActualDuration();
                        
                        log.info("실제 오디오 재생 시작: {} ({}ms)", music.getTitle(), totalDuration.get());
                    } else {
                        log.warn("audioClip이 null입니다. 시뮬레이션 모드로 전환합니다.");
                        startSimulationMode(music);
                    }
                } else {
                    // 로드 실패 시 시뮬레이션 모드
                    log.info("오디오 로드 실패, 시뮬레이션 모드로 전환");
                    startSimulationMode(music);
                }
            } catch (Exception e) {
                log.error("음악 재생 중 오류 발생: {}", e.getMessage(), e);
                startSimulationMode(music);
            }
        }
        
        // 재생 시작 이벤트 발행
        publish(new PlaybackStatusEvent.PlaybackStartedEvent(music));
        
        // 진행 상황 업데이트 시작
        startProgressUpdates();
    }
    
    /**
     * 실제 오디오 파일 로드 (MP3 지원 포함)
     */
    private boolean loadAudioFile(File musicFile) {
        try {
            releaseResources();
            
            log.debug("오디오 파일 로드 시도: {}", musicFile.getName());
            
            // 1단계: 원본 오디오 스트림 획득
            try {
                audioInputStream = AudioSystem.getAudioInputStream(musicFile);
                log.debug("원본 오디오 스트림 생성 성공");
            } catch (UnsupportedAudioFileException e) {
                log.error("지원되지 않는 오디오 파일 형식: {} - {}", musicFile.getName(), e.getMessage());
                return false;
            }
            
            AudioFormat sourceFormat = audioInputStream.getFormat();
            log.debug("원본 포맷: {}", formatToString(sourceFormat));
            
            // 2단계: PCM 포맷으로 변환이 필요한지 확인
            AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;
            AudioFormat targetFormat;
            
            if (sourceFormat.getEncoding().equals(targetEncoding)) {
                // 이미 PCM 형식
                decodedAudioInputStream = audioInputStream;
                targetFormat = sourceFormat;
                log.debug("이미 PCM 형식입니다.");
            } else {
                // PCM으로 변환 필요 (MP3 등)
                log.debug("PCM 형식으로 변환 시도...");
                
                // 대상 포맷 생성
                targetFormat = new AudioFormat(
                    targetEncoding,
                    sourceFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED ? 44100.0f : sourceFormat.getSampleRate(),
                    sourceFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED ? 16 : sourceFormat.getSampleSizeInBits(),
                    sourceFormat.getChannels() == AudioSystem.NOT_SPECIFIED ? 2 : sourceFormat.getChannels(),
                    sourceFormat.getChannels() == AudioSystem.NOT_SPECIFIED ? 4 : (sourceFormat.getChannels() * 2),
                    sourceFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED ? 44100.0f : sourceFormat.getSampleRate(),
                    false
                );
                
                log.debug("대상 포맷: {}", formatToString(targetFormat));
                
                // 변환 가능한지 확인
                if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                    log.error("오디오 형식 변환이 지원되지 않습니다: {} -> {}", 
                             sourceFormat.getEncoding(), targetFormat.getEncoding());
                    return false;
                }
                
                // 변환된 스트림 생성
                decodedAudioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                log.debug("PCM 변환 성공");
            }
            
            // 3단계: Clip 생성 및 데이터 로드
            DataLine.Info clipInfo = new DataLine.Info(Clip.class, targetFormat);
            
            if (!AudioSystem.isLineSupported(clipInfo)) {
                log.error("오디오 라인이 지원되지 않습니다: {}", formatToString(targetFormat));
                return false;
            }
            
            audioClip = (Clip) AudioSystem.getLine(clipInfo);
            audioClip.open(decodedAudioInputStream);
            
            // 4단계: 볼륨 컨트롤 설정
            if (audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                log.debug("볼륨 컨트롤 사용 가능");
            } else {
                log.debug("볼륨 컨트롤 지원되지 않음");
            }
            
            log.info("오디오 파일 로드 성공: {} (포맷: {})", musicFile.getName(), formatToString(targetFormat));
            return true;
            
        } catch (LineUnavailableException e) {
            log.error("오디오 라인을 사용할 수 없습니다: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("오디오 파일 로드 실패: {} - {}", musicFile.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * AudioFormat을 읽기 쉬운 문자열로 변환
     */
    private String formatToString(AudioFormat format) {
        return String.format("%s, %.1f Hz, %d bit, %d channels, %s, %s",
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            format.getChannels(),
            format.isBigEndian() ? "big endian" : "little endian",
            format.getFrameSize() + " bytes/frame"
        );
    }
    
    /**
     * 실제 재생 시간 계산
     */
    private void calculateActualDuration() {
        if (audioClip != null) {
            try {
                long frameLength = audioClip.getFrameLength();
                float frameRate = audioClip.getFormat().getFrameRate();
                
                if (frameLength != AudioSystem.NOT_SPECIFIED && frameRate != AudioSystem.NOT_SPECIFIED) {
                    long durationMs = (long) (frameLength / frameRate * 1000);
                    totalDuration.set(durationMs);
                    
                    // MusicInfo 업데이트 (실제 길이로)
                    if (currentMusic != null && currentMusic.getDurationMillis() != durationMs) {
                        currentMusic = new MusicInfo(
                            currentMusic.getTitle(),
                            currentMusic.getArtist(),
                            currentMusic.getAlbum(),
                            currentMusic.getFilePath(),
                            durationMs,
                            currentMusic.getLrcPath()
                        );
                    }
                    
                    log.debug("실제 재생 시간 계산됨: {}ms", durationMs);
                } else {
                    // 계산할 수 없으면 기본값 사용
                    totalDuration.set(currentMusic.getDurationMillis());
                    log.debug("재생 시간 계산 불가, 기본값 사용: {}ms", currentMusic.getDurationMillis());
                }
            } catch (Exception e) {
                log.warn("재생 시간 계산 실패, 기본값 사용: {}", e.getMessage());
                totalDuration.set(currentMusic.getDurationMillis());
            }
        }
    }
    
    /**
     * 시뮬레이션 모드로 재생 (파일이 없거나 지원되지 않는 형식)
     */
    private void startSimulationMode(MusicInfo music) {
        isSimulationMode = true;
        totalDuration.set(music.getDurationMillis() > 0 ? music.getDurationMillis() : 180000L); // 기본 3분
        currentPosition.set(0);
        isPlaying.set(true);
        isPaused.set(false);
        
        log.info("시뮬레이션 모드로 재생: {} ({}ms)", music.getTitle(), totalDuration.get());
    }

    private void pausePlayback() {
        if (!isPlaying.get()) {
            log.debug("재생 중이 아니므로 일시정지할 수 없습니다.");
            return;
        }
        
        if (isSimulationMode) {
            pausePosition.set(currentPosition.get());
            isPlaying.set(false);
            isPaused.set(true);
        } else if (audioClip != null && audioClip.isRunning()) {
            pausePosition.set(getCurrentPositionFromClip());
            audioClip.stop();
            isPlaying.set(false);
            isPaused.set(true);
        }
        
        publish(new PlaybackStatusEvent.PlaybackPausedEvent());
        log.info("[{}] 일시정지됨 (위치: {}ms)", getModuleName(), pausePosition.get());
    }

    private void resumePlayback() {
        if (!isPaused.get()) {
            log.debug("일시정지 상태가 아니므로 재개할 수 없습니다.");
            return;
        }
        
        if (isSimulationMode) {
            currentPosition.set(pausePosition.get());
            isPlaying.set(true);
            isPaused.set(false);
        } else if (audioClip != null) {
            try {
                // 일시정지된 위치로 이동
                long framePosition = (long) (pausePosition.get() * audioClip.getFormat().getFrameRate() / 1000);
                audioClip.setFramePosition((int) Math.min(framePosition, audioClip.getFrameLength() - 1));
                
                audioClip.start();
                isPlaying.set(true);
                isPaused.set(false);
            } catch (Exception e) {
                log.error("재생 재개 중 오류: {}", e.getMessage());
                return;
            }
        }
        
        publish(new PlaybackStatusEvent.PlaybackStartedEvent(currentMusic));
        log.info("[{}] 재생 재개됨 (위치: {}ms)", getModuleName(), pausePosition.get());
    }

    private void stopPlayback() {
        if (audioClip != null) {
            audioClip.stop();
        }
        
        isPlaying.set(false);
        isPaused.set(false);
        currentPosition.set(0);
        pausePosition.set(0);
        
        publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
        log.info("[{}] 재생 정지됨", getModuleName());
    }
    
    private void seekTo(long positionMs) {
        long validPosition = Math.max(0, Math.min(positionMs, totalDuration.get()));
        
        if (isSimulationMode) {
            currentPosition.set(validPosition);
            if (isPaused.get()) {
                pausePosition.set(validPosition);
            }
        } else if (audioClip != null) {
            try {
                long framePosition = (long) (validPosition * audioClip.getFormat().getFrameRate() / 1000);
                audioClip.setFramePosition((int) Math.min(framePosition, audioClip.getFrameLength() - 1));
                currentPosition.set(validPosition);
                
                if (isPaused.get()) {
                    pausePosition.set(validPosition);
                }
            } catch (Exception e) {
                log.error("탐색 중 오류: {}", e.getMessage());
            }
        }
        
        log.debug("탐색 완료: {}ms", validPosition);
    }

    /**
     * 진행 상황 업데이트 시작
     */
    private void startProgressUpdates() {
        if (scheduler == null || scheduler.isShutdown()) {
            log.warn("스케줄러가 사용할 수 없습니다.");
            return;
        }
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isPlaying.get()) {
                    long current;
                    
                    if (isSimulationMode) {
                        // 시뮬레이션 모드: 시간 증가
                        current = currentPosition.addAndGet(500);
                    } else {
                        // 실제 재생: Clip에서 현재 위치 가져오기
                        current = getCurrentPositionFromClip();
                        currentPosition.set(current);
                    }
                    
                    long total = totalDuration.get();
                    
                    // 진행 상황 이벤트 발행
                    publish(new PlaybackStatusEvent.PlaybackProgressUpdateEvent(current, total));
                    
                    // 재생 완료 체크 - 더 관대한 조건으로 수정
                    if (current >= total - 1000) { // 1초 남았을 때부터 완료로 간주
                        log.info("[{}] 재생 완료됨 ({}ms / {}ms)", getModuleName(), current, total);
                        stopPlayback();
                    } else if (!isSimulationMode && audioClip != null && !audioClip.isRunning() && !isPaused.get()) {
                        log.info("[{}] 오디오 클립이 정지됨", getModuleName());
                        stopPlayback();
                    }
                }
            } catch (Exception e) {
                log.error("진행 상황 업데이트 중 오류", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        
        log.debug("진행 상황 업데이트 시작됨 (500ms 간격)");
    }
    
    /**
     * Clip에서 현재 재생 위치 가져오기
     */
    private long getCurrentPositionFromClip() {
        if (audioClip == null) return currentPosition.get();
        
        try {
            long framePosition = audioClip.getFramePosition();
            float frameRate = audioClip.getFormat().getFrameRate();
            return (long) (framePosition * 1000.0 / frameRate);
        } catch (Exception e) {
            log.debug("재생 위치 가져오기 실패: {}", e.getMessage());
            return currentPosition.get();
        }
    }
    
    /**
     * 리소스 해제
     */
    private void releaseResources() {
        try {
            if (audioClip != null) {
                audioClip.close();
                audioClip = null;
            }
            
            if (decodedAudioInputStream != null) {
                decodedAudioInputStream.close();
                decodedAudioInputStream = null;
            }
            
            if (audioInputStream != null) {
                audioInputStream.close();
                audioInputStream = null;
            }
            
            volumeControl = null;
            
        } catch (Exception e) {
            log.error("리소스 해제 중 오류", e);
        }
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
    
    public boolean isSimulationMode() {
        return isSimulationMode;
    }
}