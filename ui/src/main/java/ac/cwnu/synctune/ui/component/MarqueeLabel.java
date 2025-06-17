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

    public MarqueeLabel(String text) {
        label = new Label(text);
        getChildren().add(label);
        setupScrolling();
    }

    private void setupScrolling() {
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            checkIfScrollingNeeded();
        });

        label.textProperty().addListener((obs, oldText, newText) -> {
            checkIfScrollingNeeded();
        });
    }

    private void checkIfScrollingNeeded() {
        if (animation != null) {
            animation.stop();
        }

        Bounds textBounds = label.getBoundsInLocal();
        double textWidth = textBounds.getWidth();
        double paneWidth = getWidth();

        if (textWidth > paneWidth) {
            startScrolling(textWidth, paneWidth);
        } else {
            label.setTranslateX(0);
            isScrolling = false;
        }
    }

    private void startScrolling(double textWidth, double paneWidth) {
        isScrolling = true;
        double scrollDistance = textWidth + 50; // 여유 공간

        animation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                e -> label.setTranslateX(paneWidth)),
            new KeyFrame(Duration.seconds(scrollDistance / 50), 
                e -> label.setTranslateX(-textWidth))
        );
        
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
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
}