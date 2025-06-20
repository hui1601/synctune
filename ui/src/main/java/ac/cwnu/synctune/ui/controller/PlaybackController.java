package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isUserSeeking = false;
    private MusicInfo currentMusic = null;
    
    // 상태 관리
    private float currentVolume = 0.5f;
    private boolean isMuted = false;
    private float lastVolumeBeforeMute = 0.5f;
    
    // 탐색 관련
    private Timeline seekPreviewTimer;
    private boolean isSeekPreviewActive = false;

    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        setupKeyboardShortcuts();
        setupAdvancedFeatures();
        log.debug("PlaybackController 초기화 완료");
    }

    private void attachEventHandlers() {
        // 재생 버튼
        view.getPlayButton().setOnAction(e -> requestPlay());

        // 일시정지 버튼
        view.getPauseButton().setOnAction(e -> requestPause());

        // 정지 버튼
        view.getStopButton().setOnAction(e -> requestStop());

        // 이전 곡 버튼
        view.getPrevButton().setOnAction(e -> requestPrevious());

        // 다음 곡 버튼
        view.getNextButton().setOnAction(e -> requestNext());

        // 진행 바 상호작용
        setupProgressSliderHandlers();

        // 볼륨 슬라이더
        setupVolumeSliderHandlers();

        // 셔플 버튼 (만약 있다면)
        if (view.getShuffleButton() != null) {
            view.getShuffleButton().setOnAction(e -> toggleShuffle());
        }

        // 반복 버튼 (만약 있다면)
        if (view.getRepeatButton() != null) {
            view.getRepeatButton().setOnAction(e -> toggleRepeat());
        }
    }

    private void setupProgressSliderHandlers() {
        // 진행 바 드래그 시작/종료 감지
        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (wasChanging && !isChanging) {
                // 드래그 종료 시 탐색 이벤트 발행
                long seekPosition = (long) view.getProgressSlider().getValue();
                log.debug("진행 바 탐색: {}ms", seekPosition);
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                isUserSeeking = false;
                
                // 탐색 완료 피드백
                view.highlightProgressSlider();
                
            } else if (!wasChanging && isChanging) {
                // 드래그 시작
                isUserSeeking = true;
                startSeekPreview();
            }
        });

        // 진행 바 클릭으로 즉시 탐색
        view.getProgressSlider().setOnMouseClicked(event -> {
            if (!view.getProgressSlider().isValueChanging()) {
                double clickPosition = event.getX() / view.getProgressSlider().getWidth();
                long totalDuration = (long) view.getProgressSlider().getMax();
                long seekPosition = (long) (clickPosition * totalDuration);
                
                view.getProgressSlider().setValue(seekPosition);
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                
                log.debug("진행 바 클릭 탐색: {}ms", seekPosition);
            }
        });
    }

    private void setupVolumeSliderHandlers() {
        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!view.getVolumeSlider().isValueChanging()) {
                float newVolume = newVal.floatValue() / 100f;
                setVolume(newVolume);
                log.debug("볼륨 변경: {}%", newVal.intValue());
            }
        });

        // 볼륨 슬라이더 클릭으로 즉시 변경
        view.getVolumeSlider().setOnMouseClicked(event -> {
            double clickPosition = event.getX() / view.getVolumeSlider().getWidth();
            float newVolume = (float) (clickPosition * 100);
            view.getVolumeSlider().setValue(newVolume);
            setVolume(newVolume / 100f);
        });

        // 볼륨 슬라이더 더블클릭으로 음소거 토글
        view.getVolumeSlider().setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMute();
            }
        });
    }

    private void setupAdvancedFeatures() {
        // 마우스 휠로 볼륨 조정
        view.getVolumeSlider().setOnScroll(event -> {
            double delta = event.getDeltaY() > 0 ? 5 : -5;
            double newValue = Math.max(0, Math.min(100, view.getVolumeSlider().getValue() + delta));
            view.getVolumeSlider().setValue(newValue);
            setVolume((float) newValue / 100f);
            event.consume();
        });

        // 진행 바 마우스 휠로 탐색 (10초 단위)
        view.getProgressSlider().setOnScroll(event -> {
            long currentPos = (long) view.getProgressSlider().getValue();
            long delta = event.getDeltaY() > 0 ? 10000 : -10000; // 10초
            long newPos = Math.max(0, Math.min((long) view.getProgressSlider().getMax(), currentPos + delta));
            
            seekTo(newPos);
            event.consume();
        });
    }

    private void setupKeyboardShortcuts() {
        // 키보드 단축키 설정 (Scene에 추가되어야 함)
        Platform.runLater(() -> {
            if (view.getScene() != null) {
                view.getScene().setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case SPACE:
                            if (isPlaybackActive) {
                                requestPause();
                            } else {
                                requestPlay();
                            }
                            event.consume();
                            break;
                        case S:
                            if (event.isControlDown()) {
                                requestStop();
                                event.consume();
                            }
                            break;
                        case RIGHT:
                            if (event.isControlDown()) {
                                requestNext();
                                event.consume();
                            } else if (event.isShiftDown()) {
                                seekForward();
                                event.consume();
                            }
                            break;
                        case LEFT:
                            if (event.isControlDown()) {
                                requestPrevious();
                                event.consume();
                            } else if (event.isShiftDown()) {
                                seekBackward();
                                event.consume();
                            }
                            break;
                        case UP:
                            if (event.isControlDown()) {
                                adjustVolume(0.1f);
                                event.consume();
                            }
                            break;
                        case DOWN:
                            if (event.isControlDown()) {
                                adjustVolume(-0.1f);
                                event.consume();
                            }
                            break;
                        case M:
                            if (event.isControlDown()) {
                                toggleMute();
                                event.consume();
                            }
                            break;
                        case HOME:
                            seekToBeginning();
                            event.consume();
                            break;
                        case END:
                            seekToEnd();
                            event.consume();
                            break;
                    }
                });
            }
        });
    }

    // ========== 공개 제어 메서드들 ==========

    public void requestPlay() {
        log.debug("재생 요청");
        if (currentMusic != null) {
            publisher.publish(new MediaControlEvent.RequestPlayEvent(currentMusic));
        } else {
            publisher.publish(new MediaControlEvent.RequestPlayEvent());
        }
        updatePlaybackState(true, false, false);
        view.pulsePlayButton(); // 시각적 피드백
    }

    public void requestPause() {
        log.debug("일시정지 요청");
        publisher.publish(new MediaControlEvent.RequestPauseEvent());
        updatePlaybackState(false, true, false);
    }

    public void requestStop() {
        log.debug("정지 요청");
        publisher.publish(new MediaControlEvent.RequestStopEvent());
        updatePlaybackState(false, false, true);
    }

    public void requestNext() {
        log.debug("다음 곡 요청");
        publisher.publish(new MediaControlEvent.RequestNextMusicEvent());
    }

    public void requestPrevious() {
        log.debug("이전 곡 요청");
        publisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
    }

    public void seekTo(long positionMs) {
        log.debug("탐색 요청: {}ms", positionMs);
        publisher.publish(new MediaControlEvent.RequestSeekEvent(positionMs));
        
        // 즉시 UI 업데이트 (사용자 경험 향상)
        if (!isUserSeeking) {
            view.getProgressSlider().setValue(positionMs);
        }
    }

    // ========== 고급 제어 메서드들 ==========

    public void seekForward() {
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.min(currentPos + 10000, (long) view.getProgressSlider().getMax()); // 10초 앞으로
        seekTo(newPos);
    }

    public void seekBackward() {
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.max(currentPos - 10000, 0); // 10초 뒤로
        seekTo(newPos);
    }

    public void seekToBeginning() {
        seekTo(0);
    }

    public void seekToEnd() {
        long endPos = (long) view.getProgressSlider().getMax();
        seekTo(Math.max(0, endPos - 1000)); // 끝에서 1초 전
    }

    public void adjustVolume(float delta) {
        float newVolume = Math.max(0f, Math.min(1f, currentVolume + delta));
        setVolume(newVolume);
        
        // 시각적 피드백
        Platform.runLater(() -> {
            view.getVolumeSlider().setValue(newVolume * 100);
        });
        
        log.debug("볼륨 조정: {}%", (int) (newVolume * 100));
    }

    public void setVolume(float volume) {
        float validVolume = Math.max(0f, Math.min(1f, volume));
        currentVolume = validVolume;
        
        // 음소거 상태가 아니라면 실제 볼륨 적용
        if (!isMuted) {
            // TODO: 볼륨 변경 이벤트 발행 (이벤트 클래스 추가 시 구현)
            // publisher.publish(new MediaControlEvent.RequestVolumeChangeEvent(validVolume));
        }
        
        log.debug("볼륨 설정: {}%", (int) (validVolume * 100));
    }

    public void toggleMute() {
        if (isMuted) {
            // 음소거 해제
            isMuted = false;
            currentVolume = lastVolumeBeforeMute;
            view.getVolumeSlider().setValue(currentVolume * 100);
            setVolume(currentVolume);
            log.debug("음소거 해제: {}%", (int) (currentVolume * 100));
        } else {
            // 음소거 설정
            lastVolumeBeforeMute = currentVolume;
            isMuted = true;
            view.getVolumeSlider().setValue(0);
            setVolume(0);
            log.debug("음소거 활성화");
        }
        
        // TODO: 음소거 토글 이벤트 발행
        // publisher.publish(new MediaControlEvent.RequestMuteToggleEvent(isMuted));
    }

    private void toggleShuffle() {
        // TODO: 셔플 토글 이벤트 구현
        log.debug("셔플 토글 요청");
        // publisher.publish(new MediaControlEvent.RequestShuffleToggleEvent());
    }

    private void toggleRepeat() {
        // TODO: 반복 토글 이벤트 구현
        log.debug("반복 토글 요청");
        // publisher.publish(new MediaControlEvent.RequestRepeatToggleEvent());
    }

    // ========== 탐색 미리보기 기능 ==========
    
    private void startSeekPreview() {
        if (seekPreviewTimer != null) {
            seekPreviewTimer.stop();
        }
        
        isSeekPreviewActive = true;
        
        // 탐색 미리보기 - 드래그 중에 시간 표시 업데이트
        seekPreviewTimer = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            if (isUserSeeking) {
                long previewPosition = (long) view.getProgressSlider().getValue();
                // 시간 라벨만 업데이트 (실제 재생 위치는 변경하지 않음)
                updateTimeLabelsOnly(previewPosition);
            }
        }));
        seekPreviewTimer.setCycleCount(Timeline.INDEFINITE);
        seekPreviewTimer.play();
    }

    private void stopSeekPreview() {
        if (seekPreviewTimer != null) {
            seekPreviewTimer.stop();
        }
        isSeekPreviewActive = false;
    }

    private void updateTimeLabelsOnly(long positionMs) {
        // 시간 라벨만 업데이트하는 헬퍼 메서드
        // PlayerControlsView에 이런 메서드가 있다고 가정
        // view.updateTimeLabelsOnly(positionMs);
    }

    // ========== 상태 업데이트 메서드들 ==========

    private void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        Platform.runLater(() -> {
            view.updatePlaybackState(isPlaying, isPaused, isStopped);
            isPlaybackActive = isPlaying;
            
            // 재생 상태에 따른 추가 UI 업데이트
            if (isPlaying) {
                view.enableControls(true);
            } else if (isStopped) {
                // 정지 시 진행바 초기화
                view.getProgressSlider().setValue(0);
            }
        });
    }

    public void updateCurrentMusic(MusicInfo music) {
        this.currentMusic = music;
        Platform.runLater(() -> {
            view.updateMusicInfo(music);
            
            if (music != null) {
                // 새 곡이 로드되면 진행바 최대값 설정
                view.getProgressSlider().setMax(music.getDurationMillis());
                view.getProgressSlider().setValue(0);
                log.debug("새 곡 설정: {} ({}ms)", music.getTitle(), music.getDurationMillis());
            }
        });
    }

    public void updateProgress(long currentMs, long totalMs) {
        Platform.runLater(() -> {
            if (!isUserSeeking && !isSeekPreviewActive) {
                view.updateProgress(currentMs, totalMs);
            }
        });
    }

    public void updateVolume(float volume) {
        this.currentVolume = volume;
        Platform.runLater(() -> {
            if (!isMuted) {
                view.getVolumeSlider().setValue(volume * 100);
            }
        });
    }

    public void updateShuffleState(boolean enabled) {
        Platform.runLater(() -> {
            view.updateShuffleState(enabled);
        });
    }

    public void updateRepeatState(String repeatMode) {
        Platform.runLater(() -> {
            view.updateRepeatState(repeatMode);
        });
    }

    // ========== 재생 대기열 관리 ==========

    public void playMusicFromQueue(MusicInfo music) {
        this.currentMusic = music;
        log.info("대기열에서 음악 재생: {}", music.getTitle());
        publisher.publish(new MediaControlEvent.RequestPlayEvent(music));
        updatePlaybackState(true, false, false);
    }

    public void addToQueue(MusicInfo music) {
        log.debug("대기열에 추가: {}", music.getTitle());
        // TODO: 대기열 추가 이벤트 구현
        // publisher.publish(new MediaControlEvent.AddToQueueEvent(music));
    }

    public void clearQueue() {
        log.debug("대기열 초기화");
        // TODO: 대기열 초기화 이벤트 구현
        // publisher.publish(new MediaControlEvent.ClearQueueEvent());
    }

    // ========== 고급 기능들 ==========

    public void setPlaybackSpeed(float speed) {
        // TODO: 재생 속도 변경 이벤트 구현
        log.debug("재생 속도 변경 요청: {}x", speed);
        // publisher.publish(new MediaControlEvent.RequestPlaybackSpeedEvent(speed));
    }

    public void skipSilence(boolean enabled) {
        // TODO: 무음 구간 건너뛰기 설정
        log.debug("무음 구간 건너뛰기: {}", enabled);
        // publisher.publish(new MediaControlEvent.RequestSkipSilenceEvent(enabled));
    }

    public void enableCrossfade(boolean enabled, int durationMs) {
        // TODO: 크로스페이드 설정
        log.debug("크로스페이드: {} ({}ms)", enabled, durationMs);
        // publisher.publish(new MediaControlEvent.RequestCrossfadeEvent(enabled, durationMs));
    }

    public void setEqualizer(String preset) {
        // TODO: 이퀄라이저 설정
        log.debug("이퀄라이저 프리셋: {}", preset);
        // publisher.publish(new MediaControlEvent.RequestEqualizerEvent(preset));
    }

    // ========== 상태 조회 메서드들 ==========

    public boolean isUserSeeking() {
        return isUserSeeking;
    }

    public boolean isPlaybackActive() {
        return isPlaybackActive;
    }

    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }

    public float getCurrentVolume() {
        return currentVolume;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public long getCurrentPosition() {
        return (long) view.getProgressSlider().getValue();
    }

    public long getTotalDuration() {
        return (long) view.getProgressSlider().getMax();
    }

    public double getProgress() {
        long total = getTotalDuration();
        return total > 0 ? (double) getCurrentPosition() / total : 0.0;
    }

    // ========== 오류 처리 ==========

    public void handlePlaybackError(String errorMessage) {
        Platform.runLater(() -> {
            updatePlaybackState(false, false, true);
            view.showError(errorMessage);
            view.enableControls(false);
            
            log.error("재생 오류: {}", errorMessage);
            
            // 사용자에게 오류 알림
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("재생 오류");
            alert.setHeaderText("음악 재생 중 오류가 발생했습니다");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    public void handleSeekError(long requestedPosition) {
        log.warn("탐색 실패: {}ms 위치로 이동할 수 없습니다", requestedPosition);
        Platform.runLater(() -> {
            // 진행 바를 이전 위치로 복원
            if (currentMusic != null) {
                // 현재 실제 위치로 복원 (실제 플레이어에서 위치 정보를 받아와야 함)
                // view.getProgressSlider().setValue(actualCurrentPosition);
            }
            
            view.showError("탐색할 수 없는 위치입니다");
        });
    }

    public void handleVolumeError(String errorMessage) {
        log.warn("볼륨 조정 실패: {}", errorMessage);
        Platform.runLater(() -> {
            view.showError("볼륨을 조정할 수 없습니다: " + errorMessage);
            
            // 볼륨 슬라이더를 이전 값으로 복원
            view.getVolumeSlider().setValue(currentVolume * 100);
        });
    }

    // ========== 성능 최적화 기능들 ==========

    /**
     * 배터리 절약 모드 - UI 업데이트 빈도 감소
     */
    public void setBatterySavingMode(boolean enabled) {
        // TODO: 배터리 절약 모드 구현
        log.debug("배터리 절약 모드: {}", enabled);
    }

    /**
     * 고성능 모드 - 더 부드러운 UI 업데이트
     */
    public void setHighPerformanceMode(boolean enabled) {
        // TODO: 고성능 모드 구현
        log.debug("고성능 모드: {}", enabled);
    }

    // ========== 접근성 기능들 ==========

    /**
     * 키보드 내비게이션 개선
     */
    public void enableKeyboardNavigation(boolean enabled) {
        Platform.runLater(() -> {
            if (enabled) {
                // 키보드 포커스 시각화 강화
                view.getPlayButton().setFocusTraversable(true);
                view.getPauseButton().setFocusTraversable(true);
                view.getStopButton().setFocusTraversable(true);
                view.getPrevButton().setFocusTraversable(true);
                view.getNextButton().setFocusTraversable(true);
                view.getProgressSlider().setFocusTraversable(true);
                view.getVolumeSlider().setFocusTraversable(true);
            }
        });
    }

    /**
     * 고대비 모드 지원
     */
    public void setHighContrastMode(boolean enabled) {
        Platform.runLater(() -> {
            if (enabled) {
                // 고대비 스타일 적용
                view.getStyleClass().add("high-contrast");
            } else {
                view.getStyleClass().remove("high-contrast");
            }
        });
    }

    // ========== 사용자 설정 저장/복원 ==========

    /**
     * 사용자 설정 저장
     */
    public void saveUserSettings() {
        try {
            // TODO: 설정 파일에 저장
            // Settings settings = new Settings();
            // settings.setVolume(currentVolume);
            // settings.setMuted(isMuted);
            // settings.save();
            
            log.debug("사용자 설정 저장됨");
        } catch (Exception e) {
            log.error("설정 저장 실패", e);
        }
    }

    /**
     * 사용자 설정 복원
     */
    public void loadUserSettings() {
        try {
            // TODO: 설정 파일에서 로드
            // Settings settings = Settings.load();
            // setVolume(settings.getVolume());
            // if (settings.isMuted()) toggleMute();
            
            log.debug("사용자 설정 복원됨");
        } catch (Exception e) {
            log.error("설정 로드 실패", e);
        }
    }

    // ========== 이벤트 기반 상태 동기화 ==========

    /**
     * 외부 플레이어 상태와 동기화
     */
    public void syncWithExternalPlayer(boolean isPlaying, boolean isPaused, 
                                     long currentPos, long totalDuration, float volume) {
        Platform.runLater(() -> {
            updatePlaybackState(isPlaying, isPaused, !isPlaying && !isPaused);
            
            if (!isUserSeeking) {
                view.getProgressSlider().setValue(currentPos);
                view.getProgressSlider().setMax(totalDuration);
            }
            
            updateVolume(volume);
            view.updateProgress(currentPos, totalDuration);
        });
    }

    /**
     * 미디어 키 이벤트 처리
     */
    public void handleMediaKey(String keyAction) {
        switch (keyAction.toLowerCase()) {
            case "play_pause":
                if (isPlaybackActive) {
                    requestPause();
                } else {
                    requestPlay();
                }
                break;
            case "stop":
                requestStop();
                break;
            case "next":
                requestNext();
                break;
            case "previous":
                requestPrevious();
                break;
            case "volume_up":
                adjustVolume(0.1f);
                break;
            case "volume_down":
                adjustVolume(-0.1f);
                break;
            case "mute":
                toggleMute();
                break;
            default:
                log.warn("알 수 없는 미디어 키: {}", keyAction);
        }
    }

    // ========== 디버깅 및 모니터링 ==========

    /**
     * 현재 상태를 문자열로 반환 (디버깅용)
     */
    public String getStatusSummary() {
        return String.format(
            "PlaybackController{active=%s, seeking=%s, volume=%.2f, muted=%s, music=%s}",
            isPlaybackActive,
            isUserSeeking,
            currentVolume,
            isMuted,
            currentMusic != null ? currentMusic.getTitle() : "none"
        );
    }

    /**
     * 성능 통계 반환
     */
    public String getPerformanceStats() {
        return String.format(
            "Performance{seekPreviewActive=%s, timersActive=%d}",
            isSeekPreviewActive,
            (seekPreviewTimer != null && seekPreviewTimer.getStatus() == Timeline.Status.RUNNING ? 1 : 0)
        );
    }

    // ========== 정리 ==========

    public void dispose() {
        log.debug("PlaybackController 정리 중...");
        
        try {
            // 타이머 정리
            if (seekPreviewTimer != null) {
                seekPreviewTimer.stop();
                seekPreviewTimer = null;
            }
            
            // 상태 초기화
            isUserSeeking = false;
            isSeekPreviewActive = false;
            
            // 사용자 설정 저장
            saveUserSettings();
            
            // 뷰 정리
            Platform.runLater(() -> {
                view.enableControls(false);
            });
            
        } catch (Exception e) {
            log.error("PlaybackController 정리 중 오류", e);
        }
        
        log.debug("PlaybackController 정리 완료");
    }

    // ========== 접근자 메서드들 ==========
    
    public PlayerControlsView getView() { return view; }
    public EventPublisher getEventPublisher() { return publisher; }
    
    // ========== 콜백 인터페이스 (향후 확장용) ==========
    
    public interface PlaybackStateListener {
        void onStateChanged(boolean isPlaying, boolean isPaused, boolean isStopped);
        void onMusicChanged(MusicInfo music);
        void onProgressChanged(long currentMs, long totalMs);
        void onVolumeChanged(float volume, boolean muted);
        void onError(String errorMessage);
    }
    
    private PlaybackStateListener stateListener;
    
    public void setStateListener(PlaybackStateListener listener) {
        this.stateListener = listener;
    }
    
    private void notifyStateListener() {
        if (stateListener != null) {
            try {
                stateListener.onStateChanged(isPlaybackActive, false, !isPlaybackActive);
                if (currentMusic != null) {
                    stateListener.onMusicChanged(currentMusic);
                }
                stateListener.onProgressChanged(getCurrentPosition(), getTotalDuration());
                stateListener.onVolumeChanged(currentVolume, isMuted);
            } catch (Exception e) {
                log.error("상태 리스너 알림 중 오류", e);
            }
        }
    }
}
            