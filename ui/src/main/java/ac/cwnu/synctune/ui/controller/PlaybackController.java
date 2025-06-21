package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.application.Platform;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isPaused = false;
    private boolean isUserSeeking = false;

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

        // 볼륨 슬라이더
        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!view.getVolumeSlider().isValueChanging()) {
                // TODO: 볼륨 변경 이벤트 추가 시 구현
                log.debug("볼륨 변경: {}%", newVal.intValue());
            }
        });
    }

    // 재생 상태 이벤트 리스너들
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.debug("PlaybackStartedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = true;
            isPaused = false;
            updateButtonStates();
        });
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.debug("PlaybackPausedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = true;
            updateButtonStates();
        });
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.debug("PlaybackStoppedEvent 수신");
        Platform.runLater(() -> {
            isPlaybackActive = false;
            isPaused = false;
            updateButtonStates();
        });
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        log.debug("MusicChangedEvent 수신");
        // 곡이 변경되면 재생 상태로 설정
        Platform.runLater(() -> {
            isPlaybackActive = true;
            isPaused = false;
            updateButtonStates();
        });
    }

    private void updateButtonStates() {
        // 재생/일시정지 버튼 상태
        view.getPlayButton().setDisable(isPlaybackActive);
        view.getPauseButton().setDisable(!isPlaybackActive);
        
        // 정지 버튼은 재생 중이거나 일시정지 상태일 때 활성화
        view.getStopButton().setDisable(!isPlaybackActive && !isPaused);
        
        // 이전/다음 곡 버튼은 항상 활성화 (플레이리스트가 있는 경우)
        // TODO: 실제로는 플레이리스트 상태에 따라 결정해야 함
        view.getPrevButton().setDisable(false);
        view.getNextButton().setDisable(false);
        
        log.debug("버튼 상태 업데이트: playing={}, paused={}", isPlaybackActive, isPaused);
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