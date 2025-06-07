// File: ac/cwnu/synctune/ui/view/LyricsView.java
package ac.cwnu.synctune.ui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 동기화된 가사를 표시하는 뷰 컴포넌트
 */
public class LyricsView extends VBox {
    private final Label lyricsLabel;

    public LyricsView() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);

        lyricsLabel = new Label("가사가 없습니다.");
        lyricsLabel.setWrapText(true);
        getChildren().add(lyricsLabel);
    }

    /**
     * 현재 재생 시간에 맞는 가사를 업데이트합니다.
     * @param text 표시할 가사 텍스트
     */
    public void updateLyrics(String text) {
        lyricsLabel.setText(text);
    }
}
