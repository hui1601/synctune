package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.component.AlbumArtDisplay;
import ac.cwnu.synctune.ui.component.StyledButton;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class PlayerControlsView extends VBox {
    // 필드 선언 시 즉시 초기화
    private final StyledButton playButton = new StyledButton("▶", StyledButton.ButtonStyle.PRIMARY);
    private final StyledButton pauseButton = new StyledButton("⏸", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton stopButton = new StyledButton("⏹", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton prevButton = new StyledButton("⏮", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton nextButton = new StyledButton("⏭", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton shuffleButton = new StyledButton("🔀", StyledButton.ButtonStyle.CONTROL);
    private final StyledButton repeatButton = new StyledButton("🔁", StyledButton.ButtonStyle.CONTROL);
    
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 50);
    
    private final Label currentTimeLabel = new Label("00:00");
    private final Label totalTimeLabel = new Label("00:00");
    private final Label musicTitleLabel = new Label("재생 중인 곡이 없습니다");
    private final Label musicArtistLabel = new Label("");
    private final Label musicAlbumLabel = new Label("");
    private final Label volumeIconLabel = new Label("🔊");
    
    private final AlbumArtDisplay albumArtDisplay = new AlbumArtDisplay(80);
    
    // 상태 관리
    private MusicInfo currentMusic = null;
    private boolean isUserSeeking = false;

    public PlayerControlsView() {
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // 버튼 크기 조정
        playButton.setPrefSize(50, 50);
        pauseButton.setPrefSize(50, 50);
        stopButton.setPrefSize(40, 40);
        prevButton.setPrefSize(40, 40);
        nextButton.setPrefSize(40, 40);
        shuffleButton.setPrefSize(35, 35);
        repeatButton.setPrefSize(35, 35);

        // 툴팁 추가
        playButton.setTooltip(new Tooltip("재생 (Space)"));
        pauseButton.setTooltip(new Tooltip("일시정지 (Space)"));
        stopButton.setTooltip(new Tooltip("정지 (Ctrl+S)"));
        prevButton.setTooltip(new Tooltip("이전 곡 (Ctrl+←)"));
        nextButton.setTooltip(new Tooltip("다음 곡 (Ctrl+→)"));
        shuffleButton.setTooltip(new Tooltip("셔플"));
        repeatButton.setTooltip(new Tooltip("반복"));

        // 슬라이더 설정
        progressSlider.setPrefWidth(500);
        progressSlider.setStyle("-fx-pref-height: 8;");
        progressSlider.setBlockIncrement(10000); // 10초 단위
        
        volumeSlider.setPrefWidth(120);
        volumeSlider.setTooltip(new Tooltip("볼륨 (Ctrl+↑/↓)"));

        // 음악 정보 라벨 스타일
        musicTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        musicTitleLabel.setStyle("-fx-text-fill: #2c3e50;");
        musicTitleLabel.setMaxWidth(400);
        musicTitleLabel.setWrapText(false);
        
        musicArtistLabel.setFont(Font.font("System", 14));
        musicArtistLabel.setStyle("-fx-text-fill: #7f8c8d;");
        musicArtistLabel.setMaxWidth(400);
        
        musicAlbumLabel.setFont(Font.font("System", 12));
        musicAlbumLabel.setStyle("-fx-text-fill: #95a5a6;");
        musicAlbumLabel.setMaxWidth(400);

        // 시간 라벨 스타일 (고정폭 폰트 사용)
        currentTimeLabel.setFont(Font.font("Consolas", 12));
        totalTimeLabel.setFont(Font.font("Consolas", 12));
        currentTimeLabel.setStyle("-fx-text-fill: #2c3e50;");
        totalTimeLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // 볼륨 아이콘 설정
        volumeIconLabel.setStyle("-fx-font-size: 16px;");
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");

        // 음악 정보 및 앨범 아트 영역
        HBox musicInfoBox = createMusicInfoBox();

        // 컨트롤 버튼 영역
        HBox buttonBox = createButtonBox();

        // 진행 바 영역
        HBox progressBox = createProgressBox();

        // 볼륨 및 추가 컨트롤 영역
        HBox volumeBox = createVolumeBox();

        getChildren().addAll(musicInfoBox, buttonBox, progressBox, volumeBox);
    }

    private HBox createMusicInfoBox() {
        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER);

        // 앨범 아트
        VBox albumArtBox = new VBox();
        albumArtBox.setAlignment(Pos.CENTER);
        albumArtBox.getChildren().add(albumArtDisplay);

        // 음악 정보 텍스트
        VBox textInfoBox = new VBox(3);
        textInfoBox.setAlignment(Pos.CENTER_LEFT);
        textInfoBox.setPrefWidth(400);
        textInfoBox.getChildren().addAll(musicTitleLabel, musicArtistLabel, musicAlbumLabel);

        infoBox.getChildren().addAll(albumArtBox, textInfoBox);
        return infoBox;
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        // 추가 기능 버튼들
        HBox leftButtons = new HBox(10);
        leftButtons.setAlignment(Pos.CENTER);
        leftButtons.getChildren().addAll(shuffleButton, repeatButton);

        // 메인 플레이어 컨트롤
        HBox mainControls = new HBox(10);
        mainControls.setAlignment(Pos.CENTER);
        mainControls.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton);

        // 빈 공간 (대칭을 위해)
        HBox rightButtons = new HBox(10);
        rightButtons.setPrefWidth(leftButtons.getPrefWidth());

        buttonBox.getChildren().addAll(leftButtons, mainControls, rightButtons);
        return buttonBox;
    }

    private HBox createProgressBox() {
        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER);

        // 시간 표시를 위한 고정 너비 설정
        currentTimeLabel.setPrefWidth(45);
        currentTimeLabel.setAlignment(Pos.CENTER_RIGHT);
        totalTimeLabel.setPrefWidth(45);
        totalTimeLabel.setAlignment(Pos.CENTER_LEFT);

        progressBox.getChildren().addAll(currentTimeLabel, progressSlider, totalTimeLabel);
        return progressBox;
    }

    private HBox createVolumeBox() {
        HBox volumeBox = new HBox(15);
        volumeBox.setAlignment(Pos.CENTER);

        Label volumeLabel = new Label("볼륨");
        volumeLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        HBox volumeControls = new HBox(8);
        volumeControls.setAlignment(Pos.CENTER);
        volumeControls.getChildren().addAll(volumeIconLabel, volumeSlider, volumeLabel);

        volumeBox.getChildren().add(volumeControls);
        return volumeBox;
    }

    private void setupEventHandlers() {
        // 진행 바 드래그 시 라벨 업데이트
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                isUserSeeking = true;
                updateTimeLabel(currentTimeLabel, newVal.longValue());
            } else {
                isUserSeeking = false;
            }
        });

        // 볼륨 슬라이더 변경 시 아이콘 업데이트
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVolumeIcon(newVal.doubleValue());
        });

        // 초기 상태 설정
        updatePlaybackState(false, false, true); // stopped
    }

    // ========== 업데이트 메서드들 ==========

    public void updateMusicInfo(MusicInfo music) {
        this.currentMusic = music;
        
        if (music != null) {
            musicTitleLabel.setText(music.getTitle());
            musicArtistLabel.setText(music.getArtist());
            musicAlbumLabel.setText(music.getAlbum());
            progressSlider.setMax(music.getDurationMillis());
            updateTimeLabel(totalTimeLabel, music.getDurationMillis());
            
            // 앨범 아트 로드 시도
            albumArtDisplay.clearAlbumArt();
            loadAlbumArtAsync(music);
        } else {
            musicTitleLabel.setText("재생 중인 곡이 없습니다");
            musicArtistLabel.setText("");
            musicAlbumLabel.setText("");
            albumArtDisplay.clearAlbumArt();
            progressSlider.setMax(100);
            progressSlider.setValue(0);
            updateTimeLabel(totalTimeLabel, 0);
        }
    }

    private void loadAlbumArtAsync(MusicInfo music) {
        // 백그라운드에서 앨범 아트 로드
        new Thread(() -> {
            try {
                String musicDir = new java.io.File(music.getFilePath()).getParent();
                java.io.File[] imageFiles = new java.io.File(musicDir).listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                           lower.endsWith(".png") || lower.endsWith(".gif");
                });
                
                if (imageFiles != null && imageFiles.length > 0) {
                    // 가장 적합한 이미지 파일 선택
                    java.io.File bestImage = selectBestAlbumArt(imageFiles);
                    
                    javafx.application.Platform.runLater(() -> {
                        albumArtDisplay.setAlbumArt(bestImage.getAbsolutePath());
                    });
                }
            } catch (Exception e) {
                // 앨범 아트 로드 실패는 조용히 무시
            }
        }).start();
    }

    private java.io.File selectBestAlbumArt(java.io.File[] imageFiles) {
        // 파일명 우선순위: cover > folder > album > front
        String[] preferredNames = {"cover", "folder", "album", "front"};
        
        for (String prefName : preferredNames) {
            for (java.io.File file : imageFiles) {
                String name = UIUtils.getFileNameWithoutExtension(file.getName()).toLowerCase();
                if (name.equals(prefName)) {
                    return file;
                }
            }
        }
        
        // 우선순위 이름이 없으면 가장 큰 파일 선택
        java.io.File largest = imageFiles[0];
        for (java.io.File file : imageFiles) {
            if (file.length() > largest.length()) {
                largest = file;
            }
        }
        return largest;
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (!isUserSeeking) {
            progressSlider.setValue(currentMs);
        }
        updateTimeLabel(currentTimeLabel, currentMs);
        
        if (totalMs > 0) {
            progressSlider.setMax(totalMs);
            updateTimeLabel(totalTimeLabel, totalMs);
        }
    }

    public void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        playButton.setDisable(isPlaying);
        pauseButton.setDisable(!isPlaying);
        
        if (isPlaying) {
            playButton.setStyle("-fx-opacity: 0.6;");
            pauseButton.setStyle("-fx-opacity: 1.0;");
        } else {
            playButton.setStyle("-fx-opacity: 1.0;");
            pauseButton.setStyle("-fx-opacity: 0.6;");
        }
        
        // 정지 시 진행바 초기화
        if (isStopped) {
            progressSlider.setValue(0);
            updateTimeLabel(currentTimeLabel, 0);
        }
    }

    private void updateTimeLabel(Label label, long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            label.setText(String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60));
        } else {
            label.setText(String.format("%d:%02d", minutes, seconds % 60));
        }
    }

    private void updateVolumeIcon(double volume) {
        if (volume == 0) {
            volumeIconLabel.setText("🔇");
        } else if (volume < 30) {
            volumeIconLabel.setText("🔈");
        } else if (volume < 70) {
            volumeIconLabel.setText("🔉");
        } else {
            volumeIconLabel.setText("🔊");
        }
    }

    // ========== 고급 UI 업데이트 메서드들 ==========

    public void showBuffering(boolean buffering) {
        if (buffering) {
            playButton.setText("⟲");
            playButton.setDisable(true);
        } else {
            playButton.setText("▶");
            updatePlaybackState(false, false, true);
        }
    }

    public void updateShuffleState(boolean enabled) {
        if (enabled) {
            shuffleButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
            shuffleButton.setTooltip(new Tooltip("셔플: 켜짐"));
        } else {
            shuffleButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
            shuffleButton.setTooltip(new Tooltip("셔플: 꺼짐"));
        }
    }

    public void updateRepeatState(String mode) {
        switch (mode.toLowerCase()) {
            case "none":
                repeatButton.setText("🔁");
                repeatButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                repeatButton.setTooltip(new Tooltip("반복: 끔"));
                break;
            case "all":
                repeatButton.setText("🔁");
                repeatButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                repeatButton.setTooltip(new Tooltip("반복: 전체"));
                break;
            case "one":
                repeatButton.setText("🔂");
                repeatButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                repeatButton.setTooltip(new Tooltip("반복: 한 곡"));
                break;
        }
    }

    public void setAlbumArt(String imagePath) {
        if (imagePath != null) {
            albumArtDisplay.setAlbumArt(imagePath);
        } else {
            albumArtDisplay.clearAlbumArt();
        }
    }

    public void showError(String message) {
        // 임시로 제목 라벨에 에러 표시
        String originalText = musicTitleLabel.getText();
        String originalStyle = musicTitleLabel.getStyle();
        
        musicTitleLabel.setText("오류: " + message);
        musicTitleLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        
        // 5초 후 원래 상태로 복원
        Timeline timeline = new Timeline(
            new javafx.animation.KeyFrame(
                Duration.seconds(5),
                e -> {
                    musicTitleLabel.setText(originalText);
                    musicTitleLabel.setStyle(originalStyle);
                }
            )
        );
        timeline.play();
    }

    public void enableControls(boolean enabled) {
        playButton.setDisable(!enabled);
        pauseButton.setDisable(!enabled);
        stopButton.setDisable(!enabled);
        prevButton.setDisable(!enabled);
        nextButton.setDisable(!enabled);
        progressSlider.setDisable(!enabled);
        volumeSlider.setDisable(!enabled);
        shuffleButton.setDisable(!enabled);
        repeatButton.setDisable(!enabled);
    }

    // ========== Getter 메서드들 ==========
    
    public Button getPlayButton() { return playButton; }
    public Button getPauseButton() { return pauseButton; }
    public Button getStopButton() { return stopButton; }
    public Button getPrevButton() { return prevButton; }
    public Button getNextButton() { return nextButton; }
    public Button getShuffleButton() { return shuffleButton; }
    public Button getRepeatButton() { return repeatButton; }
    public Slider getProgressSlider() { return progressSlider; }
    public Slider getVolumeSlider() { return volumeSlider; }
    public AlbumArtDisplay getAlbumArtDisplay() { return albumArtDisplay; }

    // ========== 애니메이션 효과 ==========

    public void pulsePlayButton() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), playButton);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    public void highlightProgressSlider() {
        String originalStyle = progressSlider.getStyle();
        progressSlider.setStyle(originalStyle + "; -fx-accent: #e74c3c;");
        
        Timeline timeline = new Timeline(
            new javafx.animation.KeyFrame(
                Duration.seconds(1),
                e -> progressSlider.setStyle(originalStyle)
            )
        );
        timeline.play();
    }
    
    // ========== 상태 조회 메서드들 ==========
    
    public boolean isUserSeeking() {
        return isUserSeeking;
    }
    
    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }
    
    public double getProgressValue() {
        return progressSlider.getValue();
    }
    
    public double getVolumeValue() {
        return volumeSlider.getValue();
    }
}