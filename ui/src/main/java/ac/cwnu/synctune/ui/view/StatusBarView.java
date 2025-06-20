package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class StatusBarView extends HBox {
    private final Label statusLabel = new Label("준비됨");
    private final Label musicInfoLabel = new Label("재생 중인 곡 없음");
    private final Label timeLabel = new Label("00:00 / 00:00");
    private final Label playbackStateLabel = new Label("⏹");
    private final ProgressBar miniProgressBar = new ProgressBar(0);

    public StatusBarView() {
        initializeComponents();
        layoutComponents();
        setInitialState();
    }

    private void initializeComponents() {
        // 추가 설정만 수행 (객체는 이미 필드에서 생성됨)
        miniProgressBar.setPrefWidth(100);
        miniProgressBar.setPrefHeight(8);
        
        // 스타일 설정
        statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        musicInfoLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 11px;");
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-font-family: monospace;");
        playbackStateLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 14px;");
    }

    private void layoutComponents() {
        setSpacing(10);
        setPadding(new Insets(5, 10, 5, 10));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        // 왼쪽: 상태 메시지
        HBox leftBox = new HBox(5);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.getChildren().addAll(playbackStateLabel, statusLabel);

        // 중앙: 음악 정보 (확장 가능)
        HBox centerBox = new HBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(musicInfoLabel, miniProgressBar);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        // 오른쪽: 시간 정보
        HBox rightBox = new HBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.getChildren().add(timeLabel);

        // 구분선
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        getChildren().addAll(leftBox, sep1, centerBox, sep2, rightBox);
    }

    private void setInitialState() {
        updateStatus("SyncTune 시작됨");
        updatePlaybackState(false, false, true); // stopped
    }

    // ========== 업데이트 메서드들 ==========

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            String displayText = music.getTitle();
            if (music.getArtist() != null && !music.getArtist().equals("Unknown Artist")) {
                displayText += " - " + music.getArtist();
            }
            
            // 너무 긴 텍스트는 잘라내기
            if (displayText.length() > 50) {
                displayText = displayText.substring(0, 47) + "...";
            }
            
            musicInfoLabel.setText(displayText);
        } else {
            musicInfoLabel.setText("재생 중인 곡 없음");
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (totalMs > 0) {
            double progress = (double) currentMs / totalMs;
            miniProgressBar.setProgress(Math.max(0, Math.min(1, progress)));
            
            String currentTime = UIUtils.formatTime(currentMs);
            String totalTime = UIUtils.formatTime(totalMs);
            timeLabel.setText(currentTime + " / " + totalTime);
        } else {
            miniProgressBar.setProgress(0);
            timeLabel.setText("00:00 / 00:00");
        }
    }

    public void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        if (isPlaying) {
            playbackStateLabel.setText("▶");
            playbackStateLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 14px;");
            updateStatus("재생 중");
        } else if (isPaused) {
            playbackStateLabel.setText("⏸");
            playbackStateLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 14px;");
            updateStatus("일시정지");
        } else if (isStopped) {
            playbackStateLabel.setText("⏹");
            playbackStateLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");
            updateStatus("정지");
        }
    }

    public void showScanProgress(String message) {
        updateStatus("스캔: " + message);
    }

    public void showError(String message) {
        updateStatus("오류: " + message);
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
        
        // 3초 후 원래 스타일로 복원
        Timeline timeline = new Timeline(
            new KeyFrame(
                Duration.seconds(3),
                e -> statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;")
            )
        );
        timeline.play();
    }

    public void showSuccess(String message) {
        updateStatus(message);
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
        
        // 3초 후 원래 스타일로 복원
        Timeline timeline = new Timeline(
            new KeyFrame(
                Duration.seconds(3),
                e -> statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;")
            )
        );
        timeline.play();
    }
}