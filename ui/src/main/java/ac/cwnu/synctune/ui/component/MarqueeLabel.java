package ac.cwnu.synctune.ui.component;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class MarqueeLabel extends Pane {
    private final Label label;
    private Timeline animation;
    private boolean isScrolling = false;
    private double scrollSpeed = 50.0; // 픽셀/초
    private boolean isPaused = false;
    private Duration pauseDuration = Duration.seconds(2);

    public MarqueeLabel(String text) {
        label = new Label(text);
        getChildren().add(label);
        setupScrolling();
        
        // 기본 스타일 설정
        setStyle("-fx-background-color: transparent;");
        label.setStyle("-fx-text-fill: inherit;");
    }

    private void setupScrolling() {
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            checkIfScrollingNeeded();
        });

        label.textProperty().addListener((obs, oldText, newText) -> {
            checkIfScrollingNeeded();
        });
        
        // 마우스 호버 시 일시정지
        setOnMouseEntered(e -> pauseScrolling());
        setOnMouseExited(e -> resumeScrolling());
    }

    private void checkIfScrollingNeeded() {
        if (animation != null) {
            animation.stop();
        }

        Bounds textBounds = label.getBoundsInLocal();
        double textWidth = textBounds.getWidth();
        double paneWidth = getWidth();

        if (textWidth > paneWidth && paneWidth > 0) {
            startScrolling(textWidth, paneWidth);
        } else {
            label.setTranslateX(0);
            isScrolling = false;
        }
    }

    private void startScrolling(double textWidth, double paneWidth) {
        if (isPaused) return;
        
        isScrolling = true;
        double scrollDistance = textWidth + 50; // 여유 공간
        double totalDistance = paneWidth + scrollDistance;
        double duration = totalDistance / scrollSpeed;

        animation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                e -> label.setTranslateX(paneWidth)),
            new KeyFrame(Duration.seconds(duration), 
                e -> label.setTranslateX(-scrollDistance))
        );
        
        animation.setCycleCount(Timeline.INDEFINITE);
        
        // 시작 전 잠시 대기
        Timeline delayedStart = new Timeline(
            new KeyFrame(pauseDuration, e -> {
                if (!isPaused) {
                    animation.play();
                }
            })
        );
        delayedStart.play();
    }
    
    private void pauseScrolling() {
        isPaused = true;
        if (animation != null) {
            animation.pause();
        }
    }
    
    private void resumeScrolling() {
        isPaused = false;
        if (animation != null && animation.getStatus() == Timeline.Status.PAUSED) {
            animation.play();
        }
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    public void setFont(javafx.scene.text.Font font) {
        label.setFont(font);
    }
    
    public void setScrollSpeed(double pixelsPerSecond) {
        this.scrollSpeed = pixelsPerSecond;
        checkIfScrollingNeeded(); // 다시 계산
    }
    
    public void setPauseDuration(Duration duration) {
        this.pauseDuration = duration;
    }
    
    public void stopScrolling() {
        if (animation != null) {
            animation.stop();
        }
        label.setTranslateX(0);
        isScrolling = false;
    }
}