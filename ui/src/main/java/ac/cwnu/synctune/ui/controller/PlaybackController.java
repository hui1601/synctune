package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.application.Platform;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
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
            updatePlaybackState(true);
        });

        // 일시정지 버튼
        view.getPauseButton().setOnAction(e -> {
            log.debug("일시정지 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestPauseEvent());
            updatePlaybackState(false);
        });

        // 정지 버튼
        view.getStopButton().setOnAction(e -> {
            log.debug("정지 버튼 클릭됨");
            publisher.publish(new MediaControlEvent.RequestStopEvent());
            updatePlaybackState(false);
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

    private void updatePlaybackState(boolean isPlaying) {
        Platform.runLater(() -> {
            view.getPlayButton().setDisable(isPlaying);
            view.getPauseButton().setDisable(!isPlaying);
            isPlaybackActive = isPlaying;
        });
    }

    public boolean isUserSeeking() {
        return isUserSeeking;
    }

    public boolean isPlaybackActive() {
        return isPlaybackActive;
    }
}