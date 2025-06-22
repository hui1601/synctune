package ac.cwnu.synctune.ui.view;

import java.util.ArrayList;
import java.util.List;

import ac.cwnu.synctune.sdk.model.LrcLine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class LyricsView extends VBox {
    // 현재 가사 표시용 라벨 (상단 고정)
    private final Label currentLyricsLabel = new Label("가사가 로드되지 않았습니다");
    private final VBox lyricsContainer = new VBox(8);
    private final ScrollPane lyricsScrollPane;
    private final Label statusLabel = new Label("가사 상태: 대기 중");

    // 전체 가사 저장
    private List<LrcLine> fullLyrics = new ArrayList<>();
    private String currentHighlightedText = null;
    private Label currentHighlightedLabel = null;

    public LyricsView() {
        // ScrollPane은 lyricsContainer가 필요하므로 생성자에서 초기화
        lyricsScrollPane = new ScrollPane(lyricsContainer);
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // 현재 가사 표시 설정 (상단 고정 표시용)
        currentLyricsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        currentLyricsLabel.setStyle("-fx-text-fill: #2c3e50; -fx-background-color: #ecf0f1; -fx-padding: 10; -fx-background-radius: 5;");
        currentLyricsLabel.setWrapText(true);
        currentLyricsLabel.setTextAlignment(TextAlignment.CENTER);
        currentLyricsLabel.setAlignment(Pos.CENTER);
        currentLyricsLabel.setPrefWidth(350);

        // 상태 라벨 설정
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d;");

        // 전체 가사 표시용 컨테이너 설정
        lyricsContainer.setAlignment(Pos.TOP_CENTER);
        lyricsContainer.setPadding(new Insets(15));
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

    /**
     * 전체 가사를 한 번에 설정합니다 (처음 로드 시)
     */
    public void setFullLyrics(List<LrcLine> lrcLines) {
        this.fullLyrics = new ArrayList<>(lrcLines != null ? lrcLines : new ArrayList<>());
        updateFullLyricsDisplay();
        
        if (fullLyrics.isEmpty()) {
            statusLabel.setText("가사 상태: 가사 없음");
            showLyricsNotFound();
        } else {
            statusLabel.setText("가사 상태: " + fullLyrics.size() + "줄 로드됨");
        }
    }

    /**
     * 현재 재생 시간에 맞는 가사를 하이라이트합니다 (실시간 호출)
     */
    public void highlightLyricsByTime(long currentTimeMillis) {
        if (fullLyrics.isEmpty()) {
            return;
        }
        
        // 현재 시간에 맞는 가사 라인 찾기
        LrcLine currentLrcLine = null;
        for (LrcLine lrcLine : fullLyrics) {
            if (lrcLine.getTimeMillis() <= currentTimeMillis) {
                currentLrcLine = lrcLine;
            } else {
                break; // 현재 시간보다 큰 시간이 나오면 중단
            }
        }
        
        if (currentLrcLine != null) {
            updateLyrics(currentLrcLine.getText());
        }
    }

    /**
     * 현재 재생 중인 가사를 하이라이트합니다
     */
    public void updateLyrics(String currentLyrics) {
        if (currentLyrics == null || currentLyrics.trim().isEmpty()) {
            currentLyrics = "가사가 없습니다";
        }
        
        // 상단 현재 가사 라벨 업데이트
        String displayLyrics = currentLyrics;
        if (currentLyrics.length() > 50) {
            displayLyrics = currentLyrics.substring(0, 47) + "...";
            currentLyricsLabel.setTooltip(new javafx.scene.control.Tooltip(currentLyrics));
        } else {
            currentLyricsLabel.setTooltip(null);
        }
        
        currentLyricsLabel.setText(displayLyrics);
        currentHighlightedText = currentLyrics;
        
        // 전체 가사에서 현재 라인 하이라이트 및 스크롤
        highlightCurrentLine(currentLyrics);
        
        statusLabel.setText("가사 상태: 표시 중");
    }

    /**
     * 전체 가사를 표시합니다 (한 번에 모든 가사 표시)
     */
    private void updateFullLyricsDisplay() {
        lyricsContainer.getChildren().clear();
        currentHighlightedLabel = null;
        
        if (fullLyrics.isEmpty()) {
            updateDefaultMessage();
            return;
        }
        
        for (LrcLine lrcLine : fullLyrics) {
            String text = lrcLine.getText();
            if (text == null || text.trim().isEmpty()) {
                text = " "; // 빈 줄 처리
            }
            
            Label lineLabel = new Label(text);
            lineLabel.setWrapText(true);
            lineLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            lineLabel.setPrefWidth(320);
            lineLabel.setMaxWidth(320);
            lineLabel.setAlignment(Pos.CENTER);
            lineLabel.setTextAlignment(TextAlignment.CENTER);
            
            // 기본 스타일 적용
            applyDefaultLineStyle(lineLabel);
            
            // 사용자 데이터에 LrcLine 저장 (나중에 하이라이트에 사용)
            lineLabel.setUserData(lrcLine);
            
            lyricsContainer.getChildren().add(lineLabel);
        }
    }

    private void applyDefaultLineStyle(Label label) {
        label.setStyle(
            "-fx-text-fill: #7f8c8d; " +
            "-fx-padding: 8 12; " +
            "-fx-background-color: transparent; " +
            "-fx-background-radius: 5; " +
            "-fx-font-size: 14px; " +
            "-fx-opacity: 0.7;"
        );
    }

    private void applyHighlightStyle(Label label) {
        label.setStyle(
            "-fx-text-fill: #ffffff; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
            "-fx-padding: 12 16; " +
            "-fx-background-radius: 8; " +
            "-fx-font-size: 16px; " +
            "-fx-opacity: 1.0; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.4), 8, 0, 0, 3);"
        );
    }

    private void updateDefaultMessage() {
        lyricsContainer.getChildren().clear();
        Label defaultLabel = new Label("🎵 음악을 재생하면 가사가 여기에 표시됩니다 🎵");
        defaultLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-font-size: 16px;");
        defaultLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        defaultLabel.setWrapText(true);
        defaultLabel.setTextAlignment(TextAlignment.CENTER);
        defaultLabel.setAlignment(Pos.CENTER);
        defaultLabel.setPrefWidth(320);
        lyricsContainer.getChildren().add(defaultLabel);
    }

    /**
     * 현재 가사 라인을 하이라이트하고 스크롤합니다
     */
    private void highlightCurrentLine(String currentLine) {
        if (currentLine == null || fullLyrics.isEmpty()) return;
        
        // 이전 하이라이트 제거
        if (currentHighlightedLabel != null) {
            applyDefaultLineStyle(currentHighlightedLabel);
        }
        
        Label newHighlightedLabel = null;
        
        // 현재 가사와 일치하는 라벨 찾기
        for (javafx.scene.Node node : lyricsContainer.getChildren()) {
            if (node instanceof Label label && label.getUserData() instanceof LrcLine lrcLine) {
                if (lrcLine.getText().equals(currentLine)) {
                    applyHighlightStyle(label);
                    newHighlightedLabel = label;
                    break;
                }
            }
        }
        
        currentHighlightedLabel = newHighlightedLabel;
        
        // 하이라이트된 라인으로 스크롤
        if (currentHighlightedLabel != null) {
            scrollToLabel(currentHighlightedLabel);
        }
    }
    
    /**
     * 특정 라벨이 보이도록 스크롤합니다
     */
    private void scrollToLabel(Label targetLabel) {
        try {
            // 잠시 기다린 후 스크롤 (레이아웃이 완료된 후)
            javafx.application.Platform.runLater(() -> {
                try {
                    double nodeY = targetLabel.getBoundsInParent().getMinY();
                    double scrollPaneHeight = lyricsScrollPane.getViewportBounds().getHeight();
                    double contentHeight = lyricsContainer.getBoundsInLocal().getHeight();
                    
                    if (contentHeight > scrollPaneHeight) {
                        // 하이라이트된 라인이 화면 중앙에 오도록 계산
                        double targetY = nodeY - (scrollPaneHeight / 2) + (targetLabel.getHeight() / 2);
                        double vValue = targetY / (contentHeight - scrollPaneHeight);
                        
                        // 범위를 0~1로 제한
                        vValue = Math.max(0, Math.min(1, vValue));
                        
                        lyricsScrollPane.setVvalue(vValue);
                    }
                } catch (Exception e) {
                    // 스크롤 오류는 무시 (가사 표시에는 영향 없음)
                }
            });
        } catch (Exception e) {
            // 스크롤 오류는 무시
        }
    }
    
    public void showLyricsNotFound() {
        currentLyricsLabel.setText("가사 파일을 찾을 수 없습니다");
        currentLyricsLabel.setTooltip(null);
        statusLabel.setText("가사 상태: 파일 없음");
        fullLyrics.clear();
        currentHighlightedText = null;
        currentHighlightedLabel = null;
        lyricsContainer.getChildren().clear();
        
        Label notFoundLabel = new Label("❌ 이 곡에 대한 가사 파일을 찾을 수 없습니다");
        notFoundLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-size: 16px;");
        notFoundLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        notFoundLabel.setWrapText(true);
        notFoundLabel.setTextAlignment(TextAlignment.CENTER);
        notFoundLabel.setAlignment(Pos.CENTER);
        notFoundLabel.setPrefWidth(320);
        lyricsContainer.getChildren().add(notFoundLabel);
    }
    
    public void showLyricsLoading() {
        currentLyricsLabel.setText("가사를 로드하는 중...");
        currentLyricsLabel.setTooltip(null);
        statusLabel.setText("가사 상태: 로드 중");
        lyricsContainer.getChildren().clear();
        
        Label loadingLabel = new Label("⏳ 가사를 로드하는 중입니다...");
        loadingLabel.setStyle("-fx-text-fill: #3498db; -fx-font-style: italic; -fx-font-size: 16px;");
        loadingLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        loadingLabel.setWrapText(true);
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        loadingLabel.setAlignment(Pos.CENTER);
        loadingLabel.setPrefWidth(320);
        lyricsContainer.getChildren().add(loadingLabel);
    }
    
    public void showLyricsError(String errorMessage) {
        currentLyricsLabel.setText("가사 로드 중 오류 발생");
        currentLyricsLabel.setTooltip(errorMessage != null ? new javafx.scene.control.Tooltip(errorMessage) : null);
        statusLabel.setText("가사 상태: 오류");
        fullLyrics.clear();
        currentHighlightedText = null;
        currentHighlightedLabel = null;
        lyricsContainer.getChildren().clear();
        
        Label errorLabel = new Label("⚠️ 가사를 로드하는 중 오류가 발생했습니다");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-size: 16px;");
        errorLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setPrefWidth(320);
        
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            errorLabel.setTooltip(new javafx.scene.control.Tooltip("오류: " + errorMessage));
        }
        
        lyricsContainer.getChildren().add(errorLabel);
    }

    /**
     * 문자열 배열로 전체 가사를 설정 (기존 호환성 유지)
     */
    public void setFullLyrics(String[] lyricsLines) {
        if (lyricsLines == null || lyricsLines.length == 0) {
            setFullLyrics((List<LrcLine>) null);
            return;
        }
        
        List<LrcLine> lrcLinesList = new ArrayList<>();
        for (int i = 0; i < lyricsLines.length; i++) {
            String line = lyricsLines[i] != null ? lyricsLines[i] : "";
            // 시간 정보가 없으므로 인덱스 기반으로 임시 시간 생성
            lrcLinesList.add(new LrcLine(i * 1000L, line));
        }
        
        setFullLyrics(lrcLinesList);
    }

    /**
     * 현재 하이라이트된 가사 텍스트를 반환합니다
     */
    public String getCurrentHighlightedText() {
        return currentHighlightedText;
    }

    /**
     * 전체 가사 목록을 반환합니다
     */
    public List<LrcLine> getFullLyrics() {
        return new ArrayList<>(fullLyrics);
    }
}