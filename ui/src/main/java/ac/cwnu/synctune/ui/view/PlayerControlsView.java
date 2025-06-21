package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.AlbumArtDisplay;
import ac.cwnu.synctune.ui.component.MarqueeLabel;
import ac.cwnu.synctune.ui.component.StyledButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
    
    // 추가 컨트롤 버튼들 (미구현 부분)
    private final ToggleButton shuffleButton = new ToggleButton("🔀");
    private final ToggleButton repeatButton = new ToggleButton("🔁");
    private final ToggleButton muteButton = new ToggleButton("🔊");
    
    // 슬라이더들
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 50);
    
    // 라벨들
    private final Label currentTimeLabel = new Label("00:00");
    private final Label totalTimeLabel = new Label("00:00");
    private final MarqueeLabel titleLabel = new MarqueeLabel("재생 중인 곡이 없습니다");
    private final Label artistLabel = new Label("");
    private final Label albumLabel = new Label("");
    
    // 앨범 아트 디스플레이
    private final AlbumArtDisplay albumArt = new AlbumArtDisplay(80);
    
    // 재생 모드 상태
    private RepeatMode currentRepeatMode = RepeatMode.NONE;
    
    public enum RepeatMode {
        NONE("🔁", "반복 없음"),
        ONE("🔂", "한 곡 반복"),
        ALL("🔁", "전체 반복");
        
        private final String icon;
        private final String description;
        
        RepeatMode(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }
        
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

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
        
        // 아티스트/앨범 라벨 스타일
        artistLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        artistLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        albumLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        albumLabel.setStyle("-fx-text-fill: #95a5a6;");
        
        // 토글 버튼 스타일
        setupToggleButtonStyles();
        
        // 초기 상태 설정
        pauseButton.setDisable(true);
        // updateRepeatButton() 호출을 setupTooltips() 이후로 이동
    }

    private void setupToggleButtonStyles() {
        String toggleStyle = "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; " +
                           "-fx-border-radius: 5; -fx-background-radius: 5; " +
                           "-fx-padding: 5 10; -fx-cursor: hand;";
        String toggleSelectedStyle = "-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-border-radius: 5; -fx-background-radius: 5; " +
                                   "-fx-padding: 5 10; -fx-cursor: hand;";
        
        shuffleButton.setStyle(toggleStyle);
        repeatButton.setStyle(toggleStyle);
        muteButton.setStyle(toggleStyle);
        
        // 선택 상태 스타일 변경
        shuffleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            shuffleButton.setStyle(newVal ? toggleSelectedStyle : toggleStyle);
        });
        
        repeatButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateRepeatButton();
        });
        
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
        
        // 하단: 부가 컨트롤들 (볼륨, 셔플, 반복)
        HBox bottomControlBox = createBottomControlBox();

        getChildren().addAll(infoBox, mainControlBox, progressBox, bottomControlBox);
    }

    private HBox createMusicInfoBox() {
        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPrefHeight(100);
        
        // 곡 정보 텍스트 영역
        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER_LEFT);
        textInfo.setPrefWidth(300);
        
        textInfo.getChildren().addAll(titleLabel, artistLabel, albumLabel);
        
        infoBox.getChildren().addAll(albumArt, textInfo);
        
        return infoBox;
    }

    private HBox createMainControlBox() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        // 버튼 크기 조정
        setButtonSize(prevButton, 40);
        setButtonSize(playButton, 50);
        setButtonSize(pauseButton, 50);
        setButtonSize(stopButton, 40);
        setButtonSize(nextButton, 40);
        
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

    private HBox createBottomControlBox() {
        HBox bottomBox = new HBox(20);
        bottomBox.setAlignment(Pos.CENTER);
        
        // 왼쪽: 재생 모드 컨트롤
        HBox modeControls = new HBox(10);
        modeControls.setAlignment(Pos.CENTER_LEFT);
        modeControls.getChildren().addAll(shuffleButton, repeatButton);
        
        // 오른쪽: 볼륨 컨트롤
        HBox volumeControls = new HBox(10);
        volumeControls.setAlignment(Pos.CENTER_RIGHT);
        volumeControls.getChildren().addAll(muteButton, volumeSlider);
        
        // 공간 분배
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        bottomBox.getChildren().addAll(modeControls, spacer, volumeControls);
        return bottomBox;
    }

    private void setButtonSize(Button button, double size) {
        button.setPrefSize(size, size);
        button.setMinSize(size, size);
        button.setMaxSize(size, size);
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
        
        // 반복 모드 버튼 처리
        repeatButton.setOnAction(e -> cycleRepeatMode());
        
        // 키보드 단축키 지원 (향후 확장)
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
        
        shuffleButton.setTooltip(new Tooltip("셔플 재생"));
        repeatButton.setTooltip(new Tooltip("반복 모드"));
        muteButton.setTooltip(new Tooltip("음소거"));
        
        progressSlider.setTooltip(new Tooltip("재생 위치 조절"));
        volumeSlider.setTooltip(new Tooltip("볼륨 조절"));
        
        // Tooltip 설정 후 repeatButton 업데이트
        updateRepeatButton();
    }

    private void cycleRepeatMode() {
        switch (currentRepeatMode) {
            case NONE:
                currentRepeatMode = RepeatMode.ALL;
                break;
            case ALL:
                currentRepeatMode = RepeatMode.ONE;
                break;
            case ONE:
                currentRepeatMode = RepeatMode.NONE;
                break;
        }
        updateRepeatButton();
    }

    private void updateRepeatButton() {
        repeatButton.setText(currentRepeatMode.getIcon());
        
        // Tooltip이 존재하는지 확인하고 안전하게 처리
        Tooltip tooltip = repeatButton.getTooltip();
        if (tooltip != null) {
            tooltip.setText(currentRepeatMode.getDescription());
        } else {
            // Tooltip이 없으면 새로 생성
            repeatButton.setTooltip(new Tooltip(currentRepeatMode.getDescription()));
        }
        
        String style = currentRepeatMode == RepeatMode.NONE ? 
            "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d;" :
            "-fx-background-color: #3498db; -fx-text-fill: white;";
        style += " -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10; -fx-cursor: hand;";
        
        repeatButton.setStyle(style);
    }

    public void updateMusicInfo(MusicInfo music) {
        if (music != null) {
            titleLabel.setText(music.getTitle());
            artistLabel.setText(music.getArtist());
            albumLabel.setText(music.getAlbum());
            progressSlider.setMax(music.getDurationMillis());
            updateTimeLabel(totalTimeLabel, music.getDurationMillis());
            
            // 앨범 아트 업데이트 (구현 예정)
            albumArt.clearAlbumArt();
        } else {
            titleLabel.setText("재생 중인 곡이 없습니다");
            artistLabel.setText("");
            albumLabel.setText("");
            progressSlider.setMax(100);
            updateTimeLabel(totalTimeLabel, 0);
            albumArt.clearAlbumArt();
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
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            label.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            label.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    public void setPlaybackState(boolean isPlaying, boolean isPaused) {
        playButton.setDisable(isPlaying);
        pauseButton.setDisable(!isPlaying);
        stopButton.setDisable(!isPlaying && !isPaused);
    }

    // Getter 메서드들
    public Button getPlayButton() { return playButton; }
    public Button getPauseButton() { return pauseButton; }
    public Button getStopButton() { return stopButton; }
    public Button getPrevButton() { return prevButton; }
    public Button getNextButton() { return nextButton; }
    public ToggleButton getShuffleButton() { return shuffleButton; }
    public ToggleButton getRepeatButton() { return repeatButton; }
    public ToggleButton getMuteButton() { return muteButton; }
    public Slider getProgressSlider() { return progressSlider; }
    public Slider getVolumeSlider() { return volumeSlider; }
    public RepeatMode getRepeatMode() { return currentRepeatMode; }
    public AlbumArtDisplay getAlbumArt() { return albumArt; }
}