package ac.cwnu.synctune.ui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

/**
 * 플레이어 컨트롤(재생, 일시정지, 정지, 이전, 다음 버튼 및 진행 바)을 제공하는 뷰 컴포넌트
 */
public class PlayerControlsView extends HBox {

    private final Button playButton;
    private final Button pauseButton;
    private final Button stopButton;
    private final Button prevButton;
    private final Button nextButton;
    private final Slider progressSlider;

    public PlayerControlsView() {
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.CENTER);

        prevButton = new Button("⏮");
        playButton = new Button("▶");
        pauseButton = new Button("⏸");
        stopButton = new Button("⏹");
        nextButton = new Button("⏭");

        progressSlider = new Slider();
        progressSlider.setPrefWidth(300);
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setValue(0);

        this.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton, progressSlider);
    }

    public Button getPlayButton() {
        return playButton;
    }

    public Button getPauseButton() {
        return pauseButton;
    }

    public Button getStopButton() {
        return stopButton;
    }

    public Button getPrevButton() {
        return prevButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Slider getProgressSlider() {
        return progressSlider;
    }
}
