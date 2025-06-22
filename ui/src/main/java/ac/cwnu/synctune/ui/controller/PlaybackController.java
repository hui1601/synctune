package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.event.VolumeControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.application.Platform;
import javafx.scene.control.Slider;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isPaused = false;
    private boolean isUserSeeking = false;
    private boolean isUserChangingVolume = false; // 사용자가 볼륨을 조절 중인지 추적
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 80);

    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        log.debug("PlaybackController 초기화 완료");
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

        // 진행 바 드래그 시작/종료 감지
        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (wasChanging && !isChanging) {
                // 드래그 종료 시 탐색 이벤트 발행
                long seekPosition = (long) view.getProgressSlider().getValue();
                log.debug("진행 바 탐색: {}ms", seekPosition);
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                isUserSeeking = false;
            } else if (!wasChanging && isChanging) {
                // 드래그 시작
                isUserSeeking = true;
            }
        });

        // ========== 볼륨 제어 이벤트 핸들러들 ==========
        
        // 볼륨 슬라이더 - 값 변경 시 즉시 반영
        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUserChangingVolume) {
                // 프로그래매틱하게 변경된 경우는 무시 (무한 루프 방지)
                return;
            }
            
            float volume = newVal.floatValue() / 100.0f; // 0-100을 0.0-1.0으로 변환
            log.debug("볼륨 슬라이더 변경: {}% -> {}", newVal.intValue(), volume);
            publisher.publish(new VolumeControlEvent.RequestVolumeChangeEvent(volume));
        });
        
        // 볼륨 슬라이더 드래그 상태 추적
        view.getVolumeSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isUserChangingVolume = isChanging;
        });
        
        // 음소거 버튼
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
            // 곡이 변경되면 보통 재생 상태가 됨
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
            // 사용자가 조절 중이 아닐 때만 UI 업데이트 (무한 루프 방지)
            if (!isUserChangingVolume) {
                // 볼륨 슬라이더 업데이트 (0.0-1.0을 0-100으로 변환)
                view.getVolumeSlider().setValue(event.getVolume() * 100);
            }
            
            // 음소거 버튼 상태 업데이트
            view.getMuteButton().setSelected(event.isMuted());
            
            log.debug("볼륨 UI 업데이트 완료: {}%, 음소거: {}", 
                event.getVolume() * 100, event.isMuted());
        });
    }

    private void updateButtonStates() {
        log.debug("버튼 상태 업데이트 시작 - playing: {}, paused: {}", isPlaybackActive, isPaused);
    
        // 재생/일시정지 버튼 상태
        view.getPlayButton().setDisable(isPlaybackActive);
        view.getPauseButton().setDisable(!isPlaybackActive);
    
        // 정지 버튼은 재생 중이거나 일시정지 상태일 때 활성화
        view.getStopButton().setDisable(!isPlaybackActive && !isPaused);
    
        // 이전/다음 곡 버튼은 항상 활성화 (플레이리스트가 있는 경우)
        // TODO: 실제로는 플레이리스트 상태에 따라 결정해야 함
        view.getPrevButton().setDisable(false);
        view.getNextButton().setDisable(false);
    
        log.debug("버튼 상태 업데이트 완료 - 재생버튼: {}, 일시정지버튼: {}, 정지버튼: {}", 
             view.getPlayButton().isDisabled(), 
             view.getPauseButton().isDisabled(), 
             view.getStopButton().isDisabled());
    }

    public void resetToInitialState() {
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = false;
            updateButtonStates();
            view.getProgressSlider().setValue(0);
            
            // 볼륨을 PlayerModule과 동일한 기본값으로 리셋
            if (!isUserChangingVolume) {
                view.getVolumeSlider().setValue(80); // 80% 기본값
            }
            view.getMuteButton().setSelected(false);
            
            log.debug("PlaybackController 초기 상태로 리셋됨");
        });
    }

    private void initializeComponents() {
        // 슬라이더 설정
        progressSlider.setPrefWidth(400);
        progressSlider.setShowTickLabels(false);
        progressSlider.setShowTickMarks(false);
        
        volumeSlider.setPrefWidth(100);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setShowTickMarks(false);
        volumeSlider.setValue(80); // 명시적으로 80% 설정
        
        // 제목 라벨 스타일
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // 아티스트 라벨 스타일
        artistLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        artistLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        // 음소거 버튼 스타일
        setupMuteButtonStyle();
        
        // 초기 상태 설정
        pauseButton.setDisable(true);
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