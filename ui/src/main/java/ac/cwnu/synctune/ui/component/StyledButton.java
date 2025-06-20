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
    private String baseStyle;

    public StyledButton(String text, ButtonStyle style) {
        super(text);
        this.currentStyle = style;
        
        try {
            System.out.println("StyledButton 생성: " + text + " (" + style.name() + ")");
            applyStyle(style);
            setupAnimations();
            System.out.println("StyledButton 초기화 완료: " + text);
        } catch (Exception e) {
            System.err.println("StyledButton 초기화 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 기본 버튼으로 사용
            applyFallbackStyle();
        }
    }

    private void applyStyle(ButtonStyle style) {
        try {
            baseStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-text-fill: %s; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 8 16; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 500; " +
                "-fx-cursor: hand;",
                style.normalColor, style.textColor
            );
            
            setStyle(baseStyle);
            System.out.println("버튼 스타일 적용 완료: " + style.name());
            
        } catch (Exception e) {
            System.err.println("버튼 스타일 적용 오류: " + e.getMessage());
            applyFallbackStyle();
        }
    }

    private void applyFallbackStyle() {
        try {
            // CSS 파싱 오류가 발생할 경우 매우 간단한 스타일만 적용
            setStyle(
                "-fx-background-color: #95a5a6; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 8 16;"
            );
            System.out.println("버튼 대체 스타일 적용 완료");
        } catch (Exception e) {
            System.err.println("대체 스타일 적용도 실패: " + e.getMessage());
            // 모든 스타일 적용 실패 시 기본 JavaFX 버튼 사용
        }
    }

    private void setupAnimations() {
        try {
            setOnMouseEntered(e -> {
                if (animationEnabled && currentStyle != null) {
                    try {
                        String hoverStyle = baseStyle.replace(currentStyle.normalColor, currentStyle.hoverColor);
                        setStyle(hoverStyle);
                        
                        ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                        scale.setToX(1.05);
                        scale.setToY(1.05);
                        scale.play();
                    } catch (Exception ex) {
                        System.err.println("호버 효과 적용 오류: " + ex.getMessage());
                    }
                }
            });
            
            setOnMouseExited(e -> {
                if (animationEnabled && currentStyle != null) {
                    try {
                        setStyle(baseStyle);
                        
                        ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                        scale.setToX(1.0);
                        scale.setToY(1.0);
                        scale.play();
                    } catch (Exception ex) {
                        System.err.println("호버 종료 효과 적용 오류: " + ex.getMessage());
                    }
                }
            });

            setOnMousePressed(e -> {
                if (animationEnabled) {
                    try {
                        ScaleTransition scale = new ScaleTransition(Duration.millis(50), this);
                        scale.setToX(0.95);
                        scale.setToY(0.95);
                        scale.play();
                    } catch (Exception ex) {
                        System.err.println("클릭 효과 적용 오류: " + ex.getMessage());
                    }
                }
            });

            setOnMouseReleased(e -> {
                if (animationEnabled) {
                    try {
                        ScaleTransition scale = new ScaleTransition(Duration.millis(50), this);
                        scale.setToX(1.05);
                        scale.setToY(1.05);
                        scale.play();
                    } catch (Exception ex) {
                        System.err.println("클릭 해제 효과 적용 오류: " + ex.getMessage());
                    }
                }
            });
            
            System.out.println("버튼 애니메이션 설정 완료");
            
        } catch (Exception e) {
            System.err.println("버튼 애니메이션 설정 오류: " + e.getMessage());
            // 애니메이션 설정 실패해도 버튼 기능은 유지
        }
    }
    
    public void setButtonStyle(ButtonStyle style) {
        try {
            this.currentStyle = style;
            applyStyle(style);
        } catch (Exception e) {
            System.err.println("버튼 스타일 변경 오류: " + e.getMessage());
        }
    }
    
    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }
    
    public void pulse() {
        if (animationEnabled) {
            try {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(300), this);
                pulse.setToX(1.2);
                pulse.setToY(1.2);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
            } catch (Exception e) {
                System.err.println("펄스 애니메이션 오류: " + e.getMessage());
            }
        }
    }
    
    public void setGlowEffect(boolean enabled) {
        try {
            if (enabled && currentStyle != null) {
                DropShadow glow = new DropShadow();
                glow.setColor(Color.web(currentStyle.normalColor));
                glow.setRadius(10);
                glow.setSpread(0.3);
                setEffect(glow);
            } else {
                setEffect(null);
            }
        } catch (Exception e) {
            System.err.println("글로우 효과 적용 오류: " + e.getMessage());
        }
    }
}