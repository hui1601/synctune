package ac.cwnu.synctune.ui.component;

import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class StyledButton extends Button {
    public enum ButtonStyle {
        PRIMARY("#3498db", "#2980b9", "#ffffff"),
        SUCCESS("#2ecc71", "#27ae60", "#ffffff"),
        WARNING("#f39c12", "#e67e22", "#ffffff"),
        DANGER("#e74c3c", "#c0392b", "#ffffff"),
        CONTROL("#95a5a6", "#7f8c8d", "#ffffff"),
        SECONDARY("#6c757d", "#5a6268", "#ffffff"),
        INFO("#17a2b8", "#138496", "#ffffff"),
        LIGHT("#f8f9fa", "#e2e6ea", "#212529"),
        DARK("#343a40", "#23272b", "#ffffff");

        private final String normalColor;
        private final String hoverColor;
        private final String textColor;

        ButtonStyle(String normalColor, String hoverColor, String textColor) {
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            this.textColor = textColor;
        }
        
        public String getNormalColor() { return normalColor; }
        public String getHoverColor() { return hoverColor; }
        public String getTextColor() { return textColor; }
    }

    private ButtonStyle currentStyle;
    private boolean animationEnabled = true;

    public StyledButton(String text, ButtonStyle style) {
        super(text);
        this.currentStyle = style;
        applyStyle(style);
        setupAnimations();
    }

    private void applyStyle(ButtonStyle style) {
        setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 16; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 500; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);",
            style.normalColor, style.textColor
        ));
    }

    private void setupAnimations() {
        setOnMouseEntered(e -> {
            if (animationEnabled) {
                setStyle(getStyle().replace(currentStyle.normalColor, currentStyle.hoverColor));
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                scale.setToX(1.05);
                scale.setToY(1.05);
                scale.play();
            }
        });
        
        setOnMouseExited(e -> {
            if (animationEnabled) {
                setStyle(getStyle().replace(currentStyle.hoverColor, currentStyle.normalColor));
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
            }
        });

        setOnMousePressed(e -> {
            if (animationEnabled) {
                ScaleTransition scale = new ScaleTransition(Duration.millis(50), this);
                scale.setToX(0.95);
                scale.setToY(0.95);
                scale.play();
            }
        });

        setOnMouseReleased(e -> {
            if (animationEnabled) {
                ScaleTransition scale = new ScaleTransition(Duration.millis(50), this);
                scale.setToX(1.05);
                scale.setToY(1.05);
                scale.play();
            }
        });
    }
    
    public void setButtonStyle(ButtonStyle style) {
        this.currentStyle = style;
        applyStyle(style);
    }
    
    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }
    
    public void pulse() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(300), this);
        pulse.setToX(1.2);
        pulse.setToY(1.2);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }
    
    public void setGlowEffect(boolean enabled) {
        if (enabled) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(currentStyle.normalColor));
            glow.setRadius(10);
            glow.setSpread(0.3);
            setEffect(glow);
        } else {
            setEffect(null);
        }
    }
}