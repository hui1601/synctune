package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.VolumeControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isPaused = false;
    private boolean isUserSeeking = false;
    private boolean isUserChangingVolume = false;
    
    // 볼륨 변경 throttling을 위한 필드들
    private Timeline volumeThrottle;
    private double pendingVolumeValue = -1;

    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        initializeVolumeThrottle();
        attachEventHandlers();
        log.debug("PlaybackController 초기화 완료");
    }

    private void initializeVolumeThrottle() {
        // 100ms마다 한 번씩만 볼륨 이벤트 발행 (throttling)
        volumeThrottle = new Timeline(new javafx.animation.KeyFrame(
            Duration.millis(100),
            e -> {
                if (pendingVolumeValue >= 0) {
                    float volume = (float) (pendingVolumeValue / 100.0);
                    publisher.publish(new VolumeControlEvent.RequestVolumeChangeEvent(volume));
                    log.trace("Throttled 볼륨 변경 이벤트 발행: {}%", pendingVolumeValue);
                    pendingVolumeValue = -1;
                }
            }
        ));
        volumeThrottle.setCycleCount(Timeline.INDEFINITE);
    }

    private void attachEventHandlers() {
        // 재생 버튼
        view.getPlayButton().setOnAction(e -> {
            log.debug("재생 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestPlayEvent());
        });

        // 일시정지 버튼
        view.getPauseButton().setOnAction(e -> {
            log.debug("일시정지 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestPauseEvent());
        });

        // 정지 버튼
        view.getStopButton().setOnAction(e -> {
            log.debug("정지 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestStopEvent());
        });

        // 이전 곡 버튼
        view.getPrevButton().setOnAction(e -> {
            log.debug("이전 곡 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
        });

        // 다음 곡 버튼
        view.getNextButton().setOnAction(e -> {
            log.debug("다음 곡 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestNextMusicEvent());
        });

        // 진행 바 드래그 감지
        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (wasChanging && !isChanging) {
                long seekPosition = (long) view.getProgressSlider().getValue();
                log.debug("진행 바 탐색: {}ms", seekPosition);
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                isUserSeeking = false;
            } else if (!wasChanging && isChanging) {
                isUserSeeking = true;
            }
        });

        // ========== 개선된 볼륨 제어 ==========
        
        // 볼륨 슬라이더 - 실시간 반응 (throttling 적용)
        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUserChangingVolume) {
                // 프로그래매틱 변경인 경우는 무시
                return;
            }
            
            // throttling을 통한 실시간 볼륨 변경
            pendingVolumeValue = newVal.doubleValue();
            
            if (!volumeThrottle.getStatus().equals(Timeline.Status.RUNNING)) {
                volumeThrottle.play();
            }
            
            log.trace("볼륨 슬라이더 실시간 변경: {}%", newVal.intValue());
        });
        
        // 볼륨 슬라이더 드래그 상태 추적 개선
        view.getVolumeSlider().setOnMousePressed(e -> {
            isUserChangingVolume = true;
            log.trace("볼륨 슬라이더 드래그 시작");
        });
        
        view.getVolumeSlider().setOnMouseReleased(e -> {
            isUserChangingVolume = false;
            volumeThrottle.stop();
            
            // 최종 볼륨 값을 즉시 발행
            if (pendingVolumeValue >= 0) {
                float volume = (float) (pendingVolumeValue / 100.0);
                publisher.publish(new VolumeControlEvent.RequestVolumeChangeEvent(volume));
                log.debug("볼륨 슬라이더 최종 값: {}%", pendingVolumeValue);
                pendingVolumeValue = -1;
            }
            
            log.trace("볼륨 슬라이더 드래그 종료");
        });
        
        // 키보드로 볼륨 조절 시에도 즉시 반응
        view.getVolumeSlider().setOnKeyPressed(e -> {
            Platform.runLater(() -> {
                if (!isUserChangingVolume) {
                    float volume = (float) (view.getVolumeSlider().getValue() / 100.0);
                    publisher.publish(new VolumeControlEvent.RequestVolumeChangeEvent(volume));
                    log.debug("키보드 볼륨 조절: {}%", view.getVolumeSlider().getValue());
                }
            });
        });
        
        // 음소거 버튼 - 즉시 반응
        view.getMuteButton().setOnAction(e -> {
            boolean muted = view.getMuteButton().isSelected();
            log.debug("음소거 버튼 클릭: {}", muted);
            publisher.publish(new VolumeControlEvent.RequestMuteEvent(muted));
        });
    }

    // 재생 상태 이벤트 리스너들
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.debug("PlaybackController: PlaybackStartedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = true;
            isPaused = false;
            updateButtonStates();
            log.debug("재생 시작 상태로 버튼 업데이트됨");
        });
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.debug("PlaybackController: PlaybackPausedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = true;
            updateButtonStates();
            log.debug("일시정지 상태로 버튼 업데이트됨");
        });
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.debug("PlaybackController: PlaybackStoppedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = false;
            updateButtonStates();
        
            // 정지 시 진행바도 초기화
            if (!isUserSeeking()) {
                view.getProgressSlider().setValue(0);
            }
        
            log.debug("정지 상태로 버튼 업데이트됨");
        });
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        log.debug("PlaybackController: MusicChangedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = true;
            isPaused = false;
            updateButtonStates();
            log.debug("음악 변경으로 재생 상태로 버튼 업데이트됨");
        });
    }
    
    // ========== 볼륨 상태 이벤트 리스너 ==========
    
    @EventListener
    public void onVolumeChanged(VolumeControlEvent.VolumeChangedEvent event) {
        log.debug("PlaybackController: VolumeChangedEvent 수신 - 볼륨: {}%, 음소거: {}", 
            event.getVolume() * 100, event.isMuted());
        
        Platform.runLater(() -> {
            // 사용자가 조작 중이 아닐 때만 UI 업데이트
            if (!isUserChangingVolume) {
                double newSliderValue = event.getVolume() * 100;
                
                // 현재 값과 다를 때만 업데이트 (무한 루프 방지)
                if (Math.abs(view.getVolumeSlider().getValue() - newSliderValue) > 0.1) {
                    view.getVolumeSlider().setValue(newSliderValue);
                    log.trace("볼륨 슬라이더 UI 업데이트: {}%", newSliderValue);
                }
            }
            
            // 음소거 버튼 상태는 항상 업데이트
            if (view.getMuteButton().isSelected() != event.isMuted()) {
                view.getMuteButton().setSelected(event.isMuted());
            }
        });
    }

    private void updateButtonStates() {
        view.getPlayButton().setDisable(isPlaybackActive);
        view.getPauseButton().setDisable(!isPlaybackActive);
        view.getStopButton().setDisable(!isPlaybackActive && !isPaused);
        view.getPrevButton().setDisable(false);
        view.getNextButton().setDisable(false);
    }

    public void resetToInitialState() {
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = false;
            updateButtonStates();
            view.getProgressSlider().setValue(0);
            
            if (!isUserChangingVolume) {
                view.getVolumeSlider().setValue(80);
            }
            view.getMuteButton().setSelected(false);
            
            log.debug("PlaybackController 초기 상태로 리셋됨");
        });
    }

    public boolean isUserSeeking() {
        return isUserSeeking;
    }

    public boolean isPlaybackActive() {
        return isPlaybackActive;
    }

    public boolean isPaused() {
        return isPaused;
    }
}