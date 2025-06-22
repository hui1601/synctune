package ac.cwnu.synctune.player;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
import ac.cwnu.synctune.sdk.event.PlaylistQueryEvent;
import ac.cwnu.synctune.sdk.event.VolumeControlEvent;
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
    
    
    // 볼륨 관리
    private final AtomicReference<Float> currentVolume = new AtomicReference<>(0.8f); // 기본 80%
    private final AtomicBoolean isMuted = new AtomicBoolean(false);
    
    // 자동 재생 관련
    private final AtomicBoolean autoPlayNextEnabled = new AtomicBoolean(true);
    private final AtomicBoolean isWaitingForNextMusic = new AtomicBoolean(false);
    
    // 진행 상황 업데이트용 스케줄러
    private ScheduledExecutorService scheduler;
    private boolean isSimulationMode = false;


    // PlayerModule 생성자 또는 start 메서드에서 초기 볼륨 설정
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
        
        // 초기 볼륨 상태 발행 (UI와 동기화)
        log.debug("[{}] 초기 볼륨 상태 발행: {}%, 음소거: {}", 
            getModuleName(), currentVolume.get() * 100, isMuted.get());
        publishVolumeChangedEvent();
        
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
        requestNextMusic();
    }

    @EventListener  
    public void onPreviousMusicRequest(MediaControlEvent.RequestPreviousMusicEvent event) {
        log.info("[{}] 이전 곡 요청 수신", getModuleName());
        requestPreviousMusic();
    }

    // ========== 볼륨 제어 이벤트 리스너들 ==========
    
    @EventListener
    public void onVolumeChangeRequest(VolumeControlEvent.RequestVolumeChangeEvent event) {
        log.debug("[{}] 볼륨 변경 요청: {}%", getModuleName(), event.getVolume() * 100);
        setVolume(event.getVolume());
    }
    
    @EventListener
    public void onMuteRequest(VolumeControlEvent.RequestMuteEvent event) {
        log.debug("[{}] 음소거 요청: {}", getModuleName(), event.isMuted());
        setMuted(event.isMuted());
    }

    @EventListener
    public void onNextMusicFound(PlaylistQueryEvent.NextMusicFoundEvent event) {
        log.info("[{}] 다음 곡 찾음: {}", getModuleName(), 
            event.getNextMusic() != null ? event.getNextMusic().getTitle() : "없음");
        
        if (event.getNextMusic() != null) {
            isWaitingForNextMusic.set(false);
            playMusic(event.getNextMusic());
        } else {
            log.info("[{}] 더 이상 재생할 곡이 없습니다.", getModuleName());
            isWaitingForNextMusic.set(false);
        }
    }

    @EventListener
    public void onPreviousMusicFound(PlaylistQueryEvent.PreviousMusicFoundEvent event) {
        log.info("[{}] 이전 곡 찾음: {}", getModuleName(), 
            event.getPreviousMusic() != null ? event.getPreviousMusic().getTitle() : "없음");
        
        if (event.getPreviousMusic() != null) {
            playMusic(event.getPreviousMusic());
        } else {
            log.info("[{}] 이전 곡이 없습니다.", getModuleName());
        }
    }

    @EventListener
    public void onCurrentMusicRemovedFromPlaylist(PlaylistQueryEvent.CurrentMusicRemovedFromPlaylistEvent event) {
        log.info("[{}] 현재 재생 중인 곡이 플레이리스트에서 제거됨: {}", 
            getModuleName(), event.getRemovedMusic().getTitle());
        
        // 현재 재생 중인 곡이 제거된 곡과 같은지 확인 (더 안전한 비교)
        if (currentMusic != null && isSameMusic(currentMusic, event.getRemovedMusic())) {
            log.info("[{}] 재생 중인 곡이 제거되어 재생을 정지합니다.", getModuleName());
            stopPlayback();
            currentMusic = null;
        }
    }
    
    /**
     * 두 MusicInfo가 같은 곡인지 안전하게 비교
     */
    private boolean isSameMusic(MusicInfo music1, MusicInfo music2) {
        if (music1 == music2) return true;
        if (music1 == null || music2 == null) return false;
        
        // 파일 경로로 비교 (MusicInfo.equals와 동일)
        return music1.getFilePath().equals(music2.getFilePath());
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
     * 다음 곡 요청
     */
    private void requestNextMusic() {
        if (isWaitingForNextMusic.get()) {
            log.debug("이미 다음 곡을 요청 중입니다.");
            return;
        }
        
        isWaitingForNextMusic.set(true);
        publish(new PlaylistQueryEvent.RequestNextMusicInPlaylistEvent(currentMusic));
        
        // 타임아웃 설정 (5초 후에도 응답이 없으면 취소)
        scheduler.schedule(() -> {
            if (isWaitingForNextMusic.get()) {
                log.warn("[{}] 다음 곡 요청 타임아웃", getModuleName());
                isWaitingForNextMusic.set(false);
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 이전 곡 요청
     */
    private void requestPreviousMusic() {
        publish(new PlaylistQueryEvent.RequestPreviousMusicInPlaylistEvent(currentMusic));
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
                        
                        // 볼륨 적용
                        applyVolumeSettings();
                        
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

    // ========== 볼륨 제어 메서드들 ==========
    
    /**
 * 볼륨 설정 (0.0 ~ 1.0)
    */
    private void setVolume(float volume) {
        float validVolume = Math.max(0.0f, Math.min(1.0f, volume));
        float oldVolume = currentVolume.getAndSet(validVolume);
    
        log.debug("[{}] 볼륨 변경 요청: {}% -> {}%", getModuleName(), oldVolume * 100, validVolume * 100);
    
        // 볼륨이 0보다 크면 음소거 해제
        if (validVolume > 0.0f && isMuted.get()) {
            log.debug("[{}] 볼륨 설정으로 인한 음소거 해제", getModuleName());
            isMuted.set(false);
        }
    
        applyVolumeSettings();
        publishVolumeChangedEvent();
    }

    
    /**
     * 음소거 설정
     */
    private void setMuted(boolean muted) {
        boolean oldMuted = isMuted.getAndSet(muted);
        if (oldMuted != muted) {
            log.debug("[{}] 음소거 상태 변경: {} -> {}", getModuleName(), oldMuted, muted);
            applyVolumeSettings();
            publishVolumeChangedEvent();
        }
    }
    
    /**
     * 실제 오디오 클립에 볼륨 적용
     */
    private void applyVolumeSettings() {
        if (volumeControl == null) {
            log.trace("[{}] 볼륨 컨트롤이 없어 볼륨 적용 생략", getModuleName());
            return;
        }
        
        try {
            float effectiveVolume = isMuted.get() ? 0.0f : currentVolume.get();
            
            // 볼륨을 데시벨로 변환 (올바른 로그 스케일 변환)
            float gainDB;
            if (effectiveVolume <= 0.0f) {
                gainDB = volumeControl.getMinimum(); // 완전 음소거
                log.trace("[{}] 음소거 상태: 최소 데시벨 사용 ({}dB)", getModuleName(), gainDB);
            } else {
                // 올바른 데시벨 변환: 20 * log10(volume)
                // 1.0 (100%) = 0 dB (기본 볼륨)
                // 0.5 (50%) = -6 dB
                // 0.1 (10%) = -20 dB
                gainDB = 20.0f * (float) Math.log10(effectiveVolume);
                
                // 볼륨 컨트롤의 범위 내로 제한
                float originalGainDB = gainDB;
                gainDB = Math.max(volumeControl.getMinimum(), 
                         Math.min(volumeControl.getMaximum(), gainDB));
                
                if (Math.abs(originalGainDB - gainDB) > 0.1f) {
                    log.debug("[{}] 데시벨 값이 볼륨 컨트롤 범위로 제한됨: {}dB -> {}dB", 
                        getModuleName(), originalGainDB, gainDB);
                }
            }
            
            volumeControl.setValue(gainDB);
            log.debug("[{}] 오디오 클립 볼륨 적용 성공: {}% -> {}dB (범위: {}~{}dB)", 
                getModuleName(), effectiveVolume * 100, gainDB, 
                volumeControl.getMinimum(), volumeControl.getMaximum());
            
        } catch (Exception e) {
            log.error("[{}] 볼륨 적용 중 오류", getModuleName(), e);
        }
    }
    
    /**
     * 볼륨 변경 이벤트 발행
     */
    private void publishVolumeChangedEvent() {
        float currentVol = currentVolume.get();
        boolean muted = isMuted.get();
        
        log.trace("[{}] 볼륨 변경 이벤트 발행: {}%, 음소거: {}", getModuleName(), currentVol * 100, muted);
        publish(new VolumeControlEvent.VolumeChangedEvent(currentVol, muted));
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
                        handlePlaybackCompleted();
                    } else if (!isSimulationMode && audioClip != null && !audioClip.isRunning() && !isPaused.get()) {
                        log.info("[{}] 오디오 클립이 정지됨", getModuleName());
                        handlePlaybackCompleted();
                    }
                }
            } catch (Exception e) {
                log.error("진행 상황 업데이트 중 오류", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        
        log.debug("진행 상황 업데이트 시작됨 (500ms 간격)");
    }

    /**
     * 재생 완료 처리 (자동 다음 곡 재생 포함)
     */
    private void handlePlaybackCompleted() {
        log.info("[{}] 재생 완료 처리 시작", getModuleName());
        
        // 재생 상태 초기화
        isPlaying.set(false);
        isPaused.set(false);
        currentPosition.set(0);
        pausePosition.set(0);
        
        // 재생 완료 이벤트 발행
        publish(new PlaybackStatusEvent.PlaybackStoppedEvent());
        
        // 자동 다음 곡 재생
        if (autoPlayNextEnabled.get() && !isWaitingForNextMusic.get()) {
            log.info("[{}] 자동 다음 곡 재생 시도", getModuleName());
            requestNextMusic();
        } else {
            log.info("[{}] 재생 완료 (자동 재생 비활성화 또는 이미 요청 중)", getModuleName());
        }
    }
    private void setupVolumeControl() {
        try {
            if (audioClip != null && audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                
                log.debug("[{}] 볼륨 컨트롤 설정 완료 - 범위: {}dB ~ {}dB", 
                    getModuleName(), volumeControl.getMinimum(), volumeControl.getMaximum());
                
                // 현재 볼륨 설정 적용
                applyVolumeSettings();
            } else {
                log.warn("[{}] MASTER_GAIN 볼륨 컨트롤이 지원되지 않습니다.", getModuleName());
                volumeControl = null;
            }
        } catch (Exception e) {
            log.error("[{}] 볼륨 컨트롤 설정 중 오류", getModuleName(), e);
            volumeControl = null;
        }
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

    public boolean isAutoPlayNextEnabled() {
        return autoPlayNextEnabled.get();
    }

    public void setAutoPlayNextEnabled(boolean enabled) {
        autoPlayNextEnabled.set(enabled);
        log.info("[{}] 자동 다음 곡 재생: {}", getModuleName(), enabled ? "활성화" : "비활성화");
    }
    
    public float getCurrentVolume() {
        return currentVolume.get();
    }
    
    public boolean isMuted() {
        return isMuted.get();
    }
}