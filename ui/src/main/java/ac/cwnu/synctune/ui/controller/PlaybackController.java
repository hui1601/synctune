package ac.cwnu.synctune.ui.controller;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.ui.view.PlayerControlsView;

/**
 * PlayerControlsView의 버튼 및 슬라이더 이벤트를 처리하고
 * MediaControlEvent를 발행하는 컨트롤러 클래스
 */
public class PlaybackController {
    private final PlayerControlsView view;
    private final EventPublisher publisher;

    /**
     * 생성자
     * @param view PlayerControlsView 인스턴스
     * @param publisher 이벤트 발행을 위한 EventPublisher
     */
    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
    }

    /**
     * UI 컨트롤에 이벤트 핸들러를 등록하여
     * 각 버튼 클릭 시 적절한 MediaControlEvent를 발행
     */
    private void attachEventHandlers() {
        view.getPlayButton().setOnAction(e ->
            publisher.publish(new MediaControlEvent.RequestPlayEvent())
        );

        view.getPauseButton().setOnAction(e ->
            publisher.publish(new MediaControlEvent.RequestPauseEvent())
        );

        view.getStopButton().setOnAction(e ->
            publisher.publish(new MediaControlEvent.RequestStopEvent())
        );

        view.getNextButton().setOnAction(e ->
            publisher.publish(new MediaControlEvent.RequestNextMusicEvent())
        );

        view.getPrevButton().setOnAction(e ->
            publisher.publish(new MediaControlEvent.RequestPreviousMusicEvent())
        );

        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                long seekPosition = (long) view.getProgressSlider().getValue();
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
            }
        });
    }
}
