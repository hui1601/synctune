package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.ui.component.MarqueeLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LyricsView extends VBox {
    private final MarqueeLabel currentLyricsLabel;
    private final ScrollPane lyricsScrollPane;
    private final VBox lyricsContainer;

    public LyricsView() {
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // 현재 가사 표시 (스크롤 효과)
        currentLyricsLabel = new MarqueeLabel("가사가 없습니다");
        currentLyricsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // 전체 가사 표시용 컨테이너
        lyricsContainer = new VBox(5);
        lyricsContainer.setAlignment(Pos.TOP_CENTER);
        lyricsContainer.setPadding(new Insets(10));

        lyricsScrollPane = new ScrollPane(lyricsContainer);
        lyricsScrollPane.setFitToWidth(true);
        lyricsScrollPane.setPrefHeight(400);
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("가사");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        getChildren().addAll(titleLabel, currentLyricsLabel, lyricsScrollPane);
    }

    public void updateLyrics(String lyrics) {
        currentLyricsLabel.setText(lyrics);
        
        // 전체 가사에서 현재 라인 하이라이트
        highlightCurrentLine(lyrics);
    }

    public void setFullLyrics(String[] lyricsLines) {
        lyricsContainer.getChildren().clear();
        for (String line : lyricsLines) {
            Label lineLabel = new Label(line);
            lineLabel.setWrapText(true);
            lyricsContainer.getChildren().add(lineLabel);
        }
    }

    private void highlightCurrentLine(String currentLine) {
        lyricsContainer.getChildren().forEach(node -> {
            if (node instanceof Label label) {
                if (label.getText().equals(currentLine)) {
                    label.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                } else {
                    label.setStyle("-fx-text-fill: #333333; -fx-font-weight: normal;");
                }
            }
        });
    }
}