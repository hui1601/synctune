package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.StyledButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlayerControlsView extends VBox {
    // 필드 선언 시 즉시 초기화
    private final StyledButton playButton = new StyledButton("▶", StyledButton.ButtonStyle.PRIMARY);
    private final StyledButton pauseButton = new StyledButton("⏸", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton stopButton = new StyledButton("⏹", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton prevButton = new StyledButton("⏮", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton nextButton = new StyledButton("⏭", StyledButton.ButtonStyle.CONTROL);
    
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 50);
    
    private final Label currentTimeLabel = new Label("00:00");
    private final Label totalTimeLabel = new Label("00:00");
    private final Label musicInfoLabel = new Label("재생 중인 곡이 없습니다");

    public PlayerControlsView() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // 슬라이더 추가 설정
        progressSlider.setPrefWidth(400);
        volumeSlider.setPrefWidth(100);
    }

    private void layoutComponents() {
        setSpacing(10);
        setPadding(new Insets(15));
        setAlignment(Pos.CENTER);

        // 곡 정보 영역
        HBox infoBox = new HBox();
        infoBox.setAlignment(Pos.CENTER);
        infoBox.getChildren().add(musicInfoLabel);

        // 컨트롤 버튼 영역
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton);

        // 진행 바 영역
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.getChildren().addAll(currentTimeLabel, progressSlider, totalTimeLabel);

        // 볼륨 컨트롤 영역
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        Label volumeLabel = new Label("🔊");
        volumeBox.getChildren().addAll(volumeLabel, volumeSlider);

        getChildren().addAll(infoBox, buttonBox, progressBox, volumeBox);
    }

    private void setupEventHandlers() {
        // 진행 바 드래그 시 라벨 업데이트
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                updateTimeLabel(currentTimeLabel, newVal.longValue());
            }
        });

        // 초기 상태 설정
        pauseButton.setDisable(true);
    }

    public void updateMusicInfo(MusicInfo music) {
        if (music != null) {
            musicInfoLabel.setText(music.getTitle() + " - " + music.getArtist());
            progressSlider.setMax(music.getDurationMillis());
            updateTimeLabel(totalTimeLabel, music.getDurationMillis());
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (!progressSlider.isValueChanging()) {
            progressSlider.setValue(currentMs);
        }
        updateTimeLabel(currentTimeLabel, currentMs);
    }

    private void updateTimeLabel(Label label, long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        label.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // Getter 메서드들
    public Button getPlayButton() { return playButton; }
    public Button getPauseButton() { return pauseButton; }
    public Button getStopButton() { return stopButton; }
    public Button getPrevButton() { return prevButton; }
    public Button getNextButton() { return nextButton; }
    public Slider getProgressSlider() { return progressSlider; }
    public Slider getVolumeSlider() { return volumeSlider; }
}