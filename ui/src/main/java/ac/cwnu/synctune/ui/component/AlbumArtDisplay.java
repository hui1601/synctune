package ac.cwnu.synctune.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AlbumArtDisplay extends StackPane {
    private final ImageView imageView;
    private final Label placeholderLabel;
    private final ProgressIndicator loadingIndicator;
    private final Rectangle clipRect;
    private final double size;
    private boolean isRotating = false;
    private RotateTransition rotateAnimation;

    public AlbumArtDisplay(double size) {
        this.size = size;
        
        // 클리핑용 사각형
        clipRect = new Rectangle(size, size);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        
        // 이미지 뷰 설정
        imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setClip(clipRect);

        // 플레이스홀더 라벨
        placeholderLabel = new Label("♪");
        placeholderLabel.setStyle(
            "-fx-font-size: " + (size / 3) + "px; " +
            "-fx-text-fill: #95a5a6; " +
            "-fx-background-color: linear-gradient(to bottom, #ecf0f1, #bdc3c7); " +
            "-fx-border-color: #7f8c8d; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );

        // 로딩 인디케이터
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(size / 3, size / 3);
        loadingIndicator.setVisible(false);

        setAlignment(Pos.CENTER);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMinSize(size, size);

        // 그림자 효과
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(8);
        shadow.setOffsetY(2);
        setEffect(shadow);

        showPlaceholder();
    }

    public void setAlbumArt(Image image) {
        if (image != null) {
            showLoading();
            
            // 이미지 로드 완료 후 표시
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0) {
                    hideLoading();
                    imageView.setImage(image);
                    showImageView();
                }
            });
            
            // 이미지가 이미 로드된 경우
            if (image.getProgress() >= 1.0) {
                hideLoading();
                imageView.setImage(image);
                showImageView();
            }
        } else {
            showPlaceholder();
        }
    }

    public void setAlbumArt(String imagePath) {
        try {
            showLoading();
            Image image = new Image(imagePath, true); // 백그라운드 로딩
            setAlbumArt(image);
        } catch (Exception e) {
            hideLoading();
            showPlaceholder();
        }
    }

    private void showImageView() {
        getChildren().clear();
        getChildren().add(imageView);
        
        // 페이드 인 효과
        FadeTransition fade = new FadeTransition(Duration.millis(300), imageView);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showPlaceholder() {
        hideLoading();
        getChildren().clear();
        getChildren().add(placeholderLabel);
        
        FadeTransition fade = new FadeTransition(Duration.millis(200), placeholderLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showLoading() {
        if (!getChildren().contains(loadingIndicator)) {
            getChildren().add(loadingIndicator);
        }
        loadingIndicator.setVisible(true);
    }

    private void hideLoading() {
        loadingIndicator.setVisible(false);
    }

    public void clearAlbumArt() {
        stopRotation();
        showPlaceholder();
    }

    // 회전 애니메이션 (재생 중일 때 사용)
    public void startRotation() {
        if (!isRotating && imageView.getImage() != null) {
            isRotating = true;
            rotateAnimation = new RotateTransition(Duration.seconds(10), imageView);
            rotateAnimation.setByAngle(360);
            rotateAnimation.setCycleCount(RotateTransition.INDEFINITE);
            rotateAnimation.play();
        }
    }

    public void stopRotation() {
        if (rotateAnimation != null) {
            rotateAnimation.stop();
            imageView.setRotate(0);
        }
        isRotating = false;
    }

    public void setCircular(boolean circular) {
        if (circular) {
            // 원형 클리핑
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(size / 2);
            imageView.setClip(circle);
            placeholderLabel.setStyle(placeholderLabel.getStyle() + 
                "; -fx-background-radius: " + (size / 2) + "; -fx-border-radius: " + (size / 2) + ";");
        } else {
            // 둥근 사각형 클리핑
            imageView.setClip(clipRect);
        }
    }

    public void setBorderEnabled(boolean enabled) {
        if (enabled) {
            setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-border-radius: 6;");
        } else {
            setStyle("");
        }
    }
}