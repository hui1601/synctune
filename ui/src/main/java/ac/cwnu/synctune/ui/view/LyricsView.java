package ac.cwnu.synctune.ui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class LyricsView extends VBox {
    // 현재 가사 표시용 라벨 (MarqueeLabel 대신 일반 Label 사용)
    private final Label currentLyricsLabel = new Label("가사가 로드되지 않았습니다");
    private final VBox lyricsContainer = new VBox(5);
    private final ScrollPane lyricsScrollPane;
    private final Label statusLabel = new Label("가사 상태: 대기 중");
    private String[] allLyricsLines = null;

    public LyricsView() {
        // ScrollPane은 lyricsContainer가 필요하므로 생성자에서 초기화
        lyricsScrollPane = new ScrollPane(lyricsContainer);
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // 현재 가사 표시 설정 (MarqueeLabel을 일반 Label로 변경)
        currentLyricsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        currentLyricsLabel.setStyle("-fx-text-fill: #2c3e50; -fx-background-color: #ecf0f1; -fx-padding: 10; -fx-background-radius: 5;");
        currentLyricsLabel.setWrapText(true); // 긴 텍스트 줄바꿈 처리
        currentLyricsLabel.setTextAlignment(TextAlignment.CENTER);
        currentLyricsLabel.setAlignment(Pos.CENTER);
        currentLyricsLabel.setPrefWidth(350); // 적절한 너비 설정

        // 상태 라벨 설정
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d;");

        // 전체 가사 표시용 컨테이너 설정
        lyricsContainer.setAlignment(Pos.TOP_CENTER);
        lyricsContainer.setPadding(new Insets(10));
        lyricsContainer.setStyle("-fx-background-color: white;");

        // ScrollPane 설정
        lyricsScrollPane.setFitToWidth(true);
        lyricsScrollPane.setPrefHeight(400);
        lyricsScrollPane.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5;");
        lyricsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        lyricsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // 기본 메시지 표시
        updateDefaultMessage();
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("가사");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #34495e;");

        getChildren().addAll(titleLabel, statusLabel, currentLyricsLabel, lyricsScrollPane);
    }

    public void updateLyrics(String lyrics) {
        if (lyrics == null || lyrics.trim().isEmpty()) {
            lyrics = "가사가 없습니다";
        }
        
        // 긴 가사의 경우 줄임표 처리 (MarqueeLabel 기능 대체)
        String displayLyrics = lyrics;
        if (lyrics.length() > 50) {
            displayLyrics = lyrics.substring(0, 47) + "...";
            currentLyricsLabel.setTooltip(new javafx.scene.control.Tooltip(lyrics)); // 전체 텍스트는 툴팁으로
        } else {
            currentLyricsLabel.setTooltip(null);
        }
        
        currentLyricsLabel.setText(displayLyrics);
        statusLabel.setText("가사 상태: 표시 중");
        
        // 전체 가사에서 현재 라인 하이라이트
        highlightCurrentLine(lyrics);
    }

    public void updateLyrics(String lyric, int index) {
        currentLyricsLabel.setText(lyric);

        // 전체 가사 중 index번째만 형광펜 스타일
        for (int i = 0; i < lyricsContainer.getChildren().size(); i++) {
            if (lyricsContainer.getChildren().get(i) instanceof Label label) {
                if (i == index) {
                    label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffeaa7; -fx-padding: 8; -fx-background-radius: 3;");
                    scrollToLabel(label);
                } else {
                    label.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: normal; -fx-padding: 5;");
                }
            }
        }
    }


    public void setFullLyrics(String[] lyricsLines) {
        this.allLyricsLines = lyricsLines;
        lyricsContainer.getChildren().clear();
        
        if (lyricsLines == null || lyricsLines.length == 0) {
            updateDefaultMessage();
            return;
        }
        
        for (int i = 0; i < lyricsLines.length; i++) {
            String line = lyricsLines[i];
            if (line == null) line = "";
            
            Label lineLabel = new Label((i + 1) + ". " + line);
            lineLabel.setWrapText(true);
            lineLabel.setStyle("-fx-text-fill: #2c3e50; -fx-padding: 5;");
            lineLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            lineLabel.setPrefWidth(330); // ScrollPane에 맞는 너비
            lyricsContainer.getChildren().add(lineLabel);
        }
        
        statusLabel.setText("가사 상태: " + lyricsLines.length + "줄 로드됨");
    }

    public String[] getFullLyricsLines() { return allLyricsLines; }

    private void updateDefaultMessage() {
        lyricsContainer.getChildren().clear();
        Label defaultLabel = new Label("🎵 음악을 재생하면 가사가 여기에 표시됩니다 🎵");
        defaultLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
        defaultLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        defaultLabel.setWrapText(true);
        defaultLabel.setTextAlignment(TextAlignment.CENTER);
        defaultLabel.setAlignment(Pos.CENTER);
        defaultLabel.setPrefWidth(330);
        lyricsContainer.getChildren().add(defaultLabel);
    }

    private void highlightCurrentLine(String currentLine) {
        if (currentLine == null) return;
        
        lyricsContainer.getChildren().forEach(node -> {
            if (node instanceof Label label) {
                String labelText = label.getText();
                // 라인 번호 제거 후 비교
                String cleanText = labelText.replaceFirst("^\\d+\\. ", "");
                
                if (cleanText.equals(currentLine)) {
                    // 현재 라인 하이라이트
                    label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffeaa7; -fx-padding: 8; -fx-background-radius: 3;");
                    
                    // 현재 라인이 보이도록 스크롤
                    scrollToLabel(label);
                } else {
                    // 일반 라인 스타일
                    label.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: normal; -fx-padding: 5;");
                }
            }
        });
    }
    
    private void scrollToLabel(Label targetLabel) {
        try {
            double nodeY = targetLabel.getBoundsInParent().getMinY();
            double scrollPaneHeight = lyricsScrollPane.getViewportBounds().getHeight();
            double contentHeight = lyricsContainer.getBoundsInLocal().getHeight();
            
            if (contentHeight > scrollPaneHeight) {
                double vValue = nodeY / (contentHeight - scrollPaneHeight);
                lyricsScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
            }
        } catch (Exception e) {
            // 스크롤 오류는 무시 (가사 표시에는 영향 없음)
        }
    }
    
    public void showLyricsNotFound() {
        currentLyricsLabel.setText("가사 파일을 찾을 수 없습니다");
        currentLyricsLabel.setTooltip(null);
        statusLabel.setText("가사 상태: 파일 없음");
        lyricsContainer.getChildren().clear();
        
        Label notFoundLabel = new Label("❌ 이 곡에 대한 가사 파일을 찾을 수 없습니다");
        notFoundLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
        notFoundLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        notFoundLabel.setWrapText(true);
        notFoundLabel.setTextAlignment(TextAlignment.CENTER);
        notFoundLabel.setAlignment(Pos.CENTER);
        notFoundLabel.setPrefWidth(330);
        lyricsContainer.getChildren().add(notFoundLabel);
    }
    
    public void showLyricsLoading() {
        currentLyricsLabel.setText("가사를 로드하는 중...");
        currentLyricsLabel.setTooltip(null);
        statusLabel.setText("가사 상태: 로드 중");
        lyricsContainer.getChildren().clear();
        
        Label loadingLabel = new Label("⏳ 가사를 로드하는 중입니다...");
        loadingLabel.setStyle("-fx-text-fill: #3498db; -fx-font-style: italic;");
        loadingLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        loadingLabel.setWrapText(true);
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        loadingLabel.setAlignment(Pos.CENTER);
        loadingLabel.setPrefWidth(330);
        lyricsContainer.getChildren().add(loadingLabel);
    }
    
    public void showLyricsError(String errorMessage) {
        currentLyricsLabel.setText("가사 로드 중 오류 발생");
        currentLyricsLabel.setTooltip(errorMessage != null ? new javafx.scene.control.Tooltip(errorMessage) : null);
        statusLabel.setText("가사 상태: 오류");
        lyricsContainer.getChildren().clear();
        
        Label errorLabel = new Label("⚠️ 가사를 로드하는 중 오류가 발생했습니다");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
        errorLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setPrefWidth(330);
        
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            errorLabel.setTooltip(new javafx.scene.control.Tooltip("오류: " + errorMessage));
        }
        
        lyricsContainer.getChildren().add(errorLabel);
    }
}