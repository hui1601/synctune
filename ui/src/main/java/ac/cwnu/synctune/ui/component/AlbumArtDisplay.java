package ac.cwnu.synctune.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AlbumArtDisplay extends StackPane {
    private final ImageView imageView;
    private final Label placeholderLabel;
    private final double size;

    public AlbumArtDisplay(double size) {
        this.size = size;
        
        // 이미지 뷰 설정
        imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // 플레이스홀더 라벨
        placeholderLabel = new Label("♪");
        placeholderLabel.setStyle(
            "-fx-font-size: " + (size / 3) + "px; " +
            "-fx-text-fill: #95a5a6; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1;"
        );

        setAlignment(Pos.CENTER);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMinSize(size, size);

        showPlaceholder();
    }

    public void setAlbumArt(Image image) {
        if (image != null) {
            imageView.setImage(image);
            getChildren().clear();
            getChildren().add(imageView);
        } else {
            showPlaceholder();
        }
    }

    public void setAlbumArt(String imagePath) {
        try {
            Image image = new Image(imagePath);
            setAlbumArt(image);
        } catch (Exception e) {
            showPlaceholder();
        }
    }

    private void showPlaceholder() {
        getChildren().clear();
        getChildren().add(placeholderLabel);
    }

    public void clearAlbumArt() {
        showPlaceholder();
    }
}