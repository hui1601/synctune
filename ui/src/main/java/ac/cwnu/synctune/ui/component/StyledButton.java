package ac.cwnu.synctune.ui.component;

import javafx.scene.control.Button;

public class StyledButton extends Button {
    public enum ButtonStyle {
        PRIMARY("#3498db", "#2980b9"),
        SUCCESS("#2ecc71", "#27ae60"),
        WARNING("#f39c12", "#e67e22"),
        DANGER("#e74c3c", "#c0392b"),
        CONTROL("#95a5a6", "#7f8c8d");

        private final String normalColor;
        private final String hoverColor;

        ButtonStyle(String normalColor, String hoverColor) {
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
        }
    }

    public StyledButton(String text, ButtonStyle style) {
        super(text);
        applyStyle(style);
    }

    private void applyStyle(ButtonStyle style) {
        setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-padding: 8 16; " +
            "-fx-font-size: 12px; " +
            "-fx-cursor: hand;",
            style.normalColor
        ));

        setOnMouseEntered(e -> setStyle(getStyle().replace(style.normalColor, style.hoverColor)));
        setOnMouseExited(e -> setStyle(getStyle().replace(style.hoverColor, style.normalColor)));
    }
}