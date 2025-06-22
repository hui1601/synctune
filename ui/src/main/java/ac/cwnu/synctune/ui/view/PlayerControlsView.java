package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.StyledButton;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PlayerControlsView extends VBox {
    // 기본 컨트롤 버튼들
    private final StyledButton playButton = new StyledButton("▶", StyledButton.ButtonStyle.PRIMARY);
    private final StyledButton pauseButton = new StyledButton("⏸", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton stopButton = new StyledButton("⏹", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton prevButton = new StyledButton("⏮", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton nextButton = new StyledButton("⏭", StyledButton.ButtonStyle.CONTROL);
    
    // 볼륨 컨트롤
    private final ToggleButton muteButton = new ToggleButton("🔊");
    
    // 슬라이더들
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 50);
    
    // 라벨들
    private final Label currentTimeLabel = new Label("00:00");
    private final Label totalTimeLabel = new Label("00:00");
    private final Label titleLabel = new Label("재생 중인 곡이 없습니다");
    private final Label artistLabel = new Label("");

    public PlayerControlsView() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupTooltips();
    }

    private void initializeComponents() {
        // 슬라이더 설정
        progressSlider.setPrefWidth(400);
        progressSlider.setShowTickLabels(false);
        progressSlider.setShowTickMarks(false);
        
        volumeSlider.setPrefWidth(100);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setShowTickMarks(false);
        
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

    private void setupMuteButtonStyle() {
        String toggleStyle = "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; " +
                           "-fx-border-radius: 5; -fx-background-radius: 5; " +
                           "-fx-padding: 5 10; -fx-cursor: hand;";
        String toggleSelectedStyle = "-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-border-radius: 5; -fx-background-radius: 5; " +
                                   "-fx-padding: 5 10; -fx-cursor: hand;";
        
        muteButton.setStyle(toggleStyle);
        
        muteButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            muteButton.setStyle(newVal ? toggleSelectedStyle : toggleStyle);
            muteButton.setText(newVal ? "🔇" : "🔊");
        });
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");

        // 상단: 곡 정보 영역
        HBox infoBox = createMusicInfoBox();
        
        // 중앙: 주요 컨트롤 버튼들
        HBox mainControlBox = createMainControlBox();
        
        // 진행 바 영역
        HBox progressBox = createProgressBox();
        
        // 하단: 볼륨 컨트롤
        HBox volumeBox = createVolumeBox();

        getChildren().addAll(infoBox, mainControlBox, progressBox, volumeBox);
    }

    private HBox createMusicInfoBox() {
        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER);
        
        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER);
        textInfo.getChildren().addAll(titleLabel, artistLabel);
        
        infoBox.getChildren().add(textInfo);
        return infoBox;
    }

    private HBox createMainControlBox() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        // 버튼 크기 설정
        setButtonSize(prevButton, 50, 35);
        setButtonSize(playButton, 60, 40);
        setButtonSize(pauseButton, 60, 40);
        setButtonSize(stopButton, 50, 35);
        setButtonSize(nextButton, 50, 35);
        
        buttonBox.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton);
        return buttonBox;
    }

    private HBox createProgressBox() {
        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER);
        
        // 시간 라벨 스타일
        currentTimeLabel.setMinWidth(50);
        totalTimeLabel.setMinWidth(50);
        currentTimeLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-family: 'Courier New';");
        totalTimeLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-family: 'Courier New';");
        
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressBox.getChildren().addAll(currentTimeLabel, progressSlider, totalTimeLabel);
        
        return progressBox;
    }

    private HBox createVolumeBox() {
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        volumeBox.getChildren().addAll(muteButton, volumeSlider);
        return volumeBox;
    }

    private void setButtonSize(StyledButton button, double width, double height) {
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
    }

    private void setupEventHandlers() {
        // 진행 바 드래그 시 라벨 업데이트
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                updateTimeLabel(currentTimeLabel, newVal.longValue());
            }
        });
        
        // 볼륨 슬라이더와 음소거 버튼 연동
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (muteButton.isSelected() && newVal.doubleValue() > 0) {
                muteButton.setSelected(false);
            }
        });
        
        muteButton.setOnAction(e -> {
            if (muteButton.isSelected()) {
                volumeSlider.setValue(0);
            } else {
                volumeSlider.setValue(50); // 기본 볼륨으로 복원
            }
        });
        
        // 키보드 단축키 지원
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE:
                    if (!playButton.isDisabled()) {
                        playButton.fire();
                    } else if (!pauseButton.isDisabled()) {
                        pauseButton.fire();
                    }
                    event.consume();
                    break;
                case LEFT:
                    if (event.isControlDown()) {
                        prevButton.fire();
                        event.consume();
                    }
                    break;
                case RIGHT:
                    if (event.isControlDown()) {
                        nextButton.fire();
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void setupTooltips() {
        playButton.setTooltip(new Tooltip("재생 (Space)"));
        pauseButton.setTooltip(new Tooltip("일시정지 (Space)"));
        stopButton.setTooltip(new Tooltip("정지"));
        prevButton.setTooltip(new Tooltip("이전 곡 (Ctrl+←)"));
        nextButton.setTooltip(new Tooltip("다음 곡 (Ctrl+→)"));
        muteButton.setTooltip(new Tooltip("음소거"));
        progressSlider.setTooltip(new Tooltip("재생 위치 조절"));
        volumeSlider.setTooltip(new Tooltip("볼륨 조절"));
    }

    public void updateMusicInfo(MusicInfo music) {
        if (music != null) {
            titleLabel.setText(music.getTitle());
            artistLabel.setText(music.getArtist());
            progressSlider.setMax(music.getDurationMillis());
            updateTimeLabel(totalTimeLabel, music.getDurationMillis());
        } else {
            titleLabel.setText("재생 중인 곡이 없습니다");
            artistLabel.setText("");
            progressSlider.setMax(100);
            updateTimeLabel(totalTimeLabel, 0);
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (!progressSlider.isValueChanging()) {
            progressSlider.setValue(currentMs);
        }
        updateTimeLabel(currentTimeLabel, currentMs);
        
        // 총 시간이 변경된 경우 업데이트
        if (totalMs != progressSlider.getMax()) {
            progressSlider.setMax(totalMs);
            updateTimeLabel(totalTimeLabel, totalMs);
        }
    }

    private void updateTimeLabel(Label label, long timeMs) {
        if (label != null) {
            label.setText(UIUtils.formatTime(timeMs));
        }
    }

    public void setPlaybackState(boolean isPlaying, boolean isPaused) {
        playButton.setDisable(isPlaying);
        pauseButton.setDisable(!isPlaying);
        stopButton.setDisable(!isPlaying && !isPaused);
    }

    // Getter 메서드들
    public StyledButton getPlayButton() { return playButton; }
    public StyledButton getPauseButton() { return pauseButton; }
    public StyledButton getStopButton() { return stopButton; }
    public StyledButton getPrevButton() { return prevButton; }
    public StyledButton getNextButton() { return nextButton; }
    public ToggleButton getMuteButton() { return muteButton; }
    public Slider getProgressSlider() { return progressSlider; }
    public Slider getVolumeSlider() { return volumeSlider; }
}