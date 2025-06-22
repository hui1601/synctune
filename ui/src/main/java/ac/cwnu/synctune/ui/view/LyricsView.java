package ac.cwnu.synctune.ui.view;

import java.util.List;

import ac.cwnu.synctune.sdk.model.LrcLine;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class LyricsView extends VBox {
    // 현재 가사 표시용 라벨
    private final Label currentLyricsLabel = new Label("가사가 로드되지 않았습니다");
    private final VBox lyricsContainer = new VBox(5);
    private final ScrollPane lyricsScrollPane;
    private final Label statusLabel = new Label("가사 상태: 대기 중");
    
    // 가사 데이터와 현재 상태
    private List<LrcLine> allLyricsLines;
    private long currentPlaybackTime = 0;
    private int lastHighlightedIndex = -1;

    public LyricsView() {
        lyricsScrollPane = new ScrollPane(lyricsContainer);
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // 현재 가사 표시 설정
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
        lyricsContainer.setPadding(new Insets(10));
        lyricsContainer.setStyle("-fx-background-color: white;");

        // ScrollPane 설정
        lyricsScrollPane.setFitToWidth(true);
        lyricsScrollPane.setPrefHeight(400);
        lyricsScrollPane.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5;");
        lyricsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        lyricsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
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
     * 타임스탬프 기반으로 가사를 업데이트하는 주요 메서드
     */
    public void updateLyricsByTimestamp(long currentTimeMillis) {
        Platform.runLater(() -> {
            if (allLyricsLines == null || allLyricsLines.isEmpty()) {
                return;
            }

            currentPlaybackTime = currentTimeMillis;
            
            // 현재 시간에 맞는 가사 라인 찾기 (약간의 선행 시간 포함)
            int currentLineIndex = findCurrentLineIndex(currentTimeMillis);
            
            if (currentLineIndex >= 0) {
                LrcLine currentLine = allLyricsLines.get(currentLineIndex);
                
                // 현재 가사 라벨 업데이트
                updateCurrentLyricsLabel(currentLine.getText());
                
                // 전체 가사에서 하이라이트 업데이트 (인덱스가 달라질 때만)
                if (currentLineIndex != lastHighlightedIndex) {
                    highlightLineByIndex(currentLineIndex);
                    lastHighlightedIndex = currentLineIndex;
                    
                    System.out.println(String.format("타임스탬프 기반 가사 업데이트: %dms -> 인덱스 %d, 가사: %s", 
                        currentTimeMillis, currentLineIndex, currentLine.getText()));
                }
            } else {
                // 아직 첫 번째 가사 시간에 도달하지 않았을 때
                if (currentTimeMillis < allLyricsLines.get(0).getTimeMillis()) {
                    updateCurrentLyricsLabel("🎵 곧 가사가 시작됩니다...");
                    clearAllHighlights();
                }
            }
        });
    }

    /**
     * 현재 재생 시간에 맞는 가사 라인 인덱스 찾기 (개선된 로직)
     */
    private int findCurrentLineIndex(long currentTimeMillis) {
        if (allLyricsLines == null || allLyricsLines.isEmpty()) {
            return -1;
        }
        
        // 가사 표시 선행 시간 (300ms 일찍 표시하여 자연스럽게)
        final long ADVANCE_TIME_MS = 300;
        long adjustedTime = currentTimeMillis + ADVANCE_TIME_MS;
        
        int resultIndex = -1;
        
        // 조정된 시간을 기준으로 현재 가사 라인 찾기
        for (int i = 0; i < allLyricsLines.size(); i++) {
            LrcLine currentLine = allLyricsLines.get(i);
            
            // 다음 라인이 있는지 확인
            boolean hasNextLine = (i + 1) < allLyricsLines.size();
            LrcLine nextLine = hasNextLine ? allLyricsLines.get(i + 1) : null;
            
            // 현재 시간이 이 라인의 시작 시간 이후이고
            // 다음 라인의 시작 시간 이전이면 이 라인을 선택
            if (adjustedTime >= currentLine.getTimeMillis()) {
                if (nextLine == null || adjustedTime < nextLine.getTimeMillis()) {
                    resultIndex = i;
                    break;
                }
                resultIndex = i; // 마지막 후보로 설정
            } else {
                // 아직 이 라인의 시간에 도달하지 않음
                break;
            }
        }
        
        return resultIndex;
    }

    /**
     * 모든 하이라이트 제거
     */
    private void clearAllHighlights() {
        for (int i = 0; i < lyricsContainer.getChildren().size(); i++) {
            if (lyricsContainer.getChildren().get(i) instanceof Label label) {
                label.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: normal; -fx-padding: 5;");
            }
        }
        lastHighlightedIndex = -1;
    }

    /**
     * 특정 인덱스의 가사 라인을 하이라이트
     */
    private void highlightLineByIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= lyricsContainer.getChildren().size()) {
            return;
        }
        
        // 모든 라인의 스타일 초기화 후 해당 라인만 하이라이트
        for (int i = 0; i < lyricsContainer.getChildren().size(); i++) {
            if (lyricsContainer.getChildren().get(i) instanceof Label label) {
                if (i == targetIndex) {
                    // 현재 라인 하이라이트
                    label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; " +
                                 "-fx-background-color: #ffeaa7; -fx-padding: 8; " +
                                 "-fx-background-radius: 3; -fx-border-color: #f39c12; " +
                                 "-fx-border-width: 1; -fx-border-radius: 3;");
                    
                    // 현재 라인이 보이도록 스크롤
                    scrollToLabel(label);
                } else {
                    // 일반 라인 스타일
                    label.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: normal; -fx-padding: 5;");
                }
            }
        }
    }

    /**
     * 현재 가사 라벨 업데이트
     */
    private void updateCurrentLyricsLabel(String lyrics) {
        if (lyrics == null || lyrics.trim().isEmpty()) {
            lyrics = "가사가 없습니다";
        }
        
        String displayLyrics = lyrics;
        if (lyrics.length() > 50) {
            displayLyrics = lyrics.substring(0, 47) + "...";
            currentLyricsLabel.setTooltip(new javafx.scene.control.Tooltip(lyrics));
        } else {
            currentLyricsLabel.setTooltip(null);
        }
        
        currentLyricsLabel.setText(displayLyrics);
        statusLabel.setText("가사 상태: 표시 중");
    }

    /**
     * 전체 가사 설정
     */
    public void setFullLyrics(List<LrcLine> lines) {
        Platform.runLater(() -> {
            this.allLyricsLines = lines;
            this.lastHighlightedIndex = -1;
            lyricsContainer.getChildren().clear();

            System.out.println("setFullLyrics 호출: lines.size() = " + (lines == null ? 0 : lines.size()));
            
            if (lines == null || lines.isEmpty()) {
                updateDefaultMessage();
                return;
            }
            
            // 가사 라인들을 VBox에 추가
            for (int i = 0; i < lines.size(); i++) {
                LrcLine line = lines.get(i);
                String text = line.getText();
                
                Label lineLabel = new Label((i + 1) + ". " + text);
                lineLabel.setWrapText(true);
                lineLabel.setStyle("-fx-text-fill: #2c3e50; -fx-padding: 5;");
                lineLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                lineLabel.setPrefWidth(330);
                
                // 타임스탬프 정보를 툴팁으로 표시
                lineLabel.setTooltip(new javafx.scene.control.Tooltip(
                    String.format("시간: %s", formatTime(line.getTimeMillis()))));
                
                lyricsContainer.getChildren().add(lineLabel);
            }

            System.out.println("lyricsContainer children count: " + lyricsContainer.getChildren().size());
            
            statusLabel.setText("가사 상태: " + lines.size() + "줄 로드됨");
            
            // 현재 재생 시간이 있으면 즉시 업데이트
            if (currentPlaybackTime > 0) {
                updateLyricsByTimestamp(currentPlaybackTime);
            }
        });
    }

    /**
     * 시간을 MM:SS 형식으로 포맷
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 특정 라벨로 스크롤 (개선된 버전)
     */
    private void scrollToLabel(Label targetLabel) {
        try {
            // 스크롤 애니메이션을 위한 타이머 사용 (부드러운 스크롤)
            Platform.runLater(() -> {
                try {
                    double nodeY = targetLabel.getBoundsInParent().getMinY();
                    double scrollPaneHeight = lyricsScrollPane.getViewportBounds().getHeight();
                    double contentHeight = lyricsContainer.getBoundsInLocal().getHeight();
                    
                    if (contentHeight > scrollPaneHeight) {
                        // 라벨이 뷰포트 상단 1/3 지점에 오도록 계산 (더 자연스러운 위치)
                        double targetY = nodeY - (scrollPaneHeight / 3);
                        double vValue = targetY / (contentHeight - scrollPaneHeight);
                        
                        // 0.0 ~ 1.0 범위로 제한
                        vValue = Math.max(0, Math.min(1, vValue));
                        
                        // 부드러운 스크롤을 위한 애니메이션
                        animateScrollTo(vValue);
                    }
                } catch (Exception e) {
                    // 스크롤 오류는 무시 (가사 표시에는 영향 없음)
                    System.out.println("스크롤 계산 오류 (무시됨): " + e.getMessage());
                }
            });
        } catch (Exception e) {
            // 메인 스크롤 오류도 무시
            System.out.println("스크롤 오류 (무시됨): " + e.getMessage());
        }
    }
    
    /**
     * 부드러운 스크롤 애니메이션
     */
    private void animateScrollTo(double targetVValue) {
        try {
            double currentVValue = lyricsScrollPane.getVvalue();
            double distance = Math.abs(targetVValue - currentVValue);
            
            // 거리가 너무 작으면 애니메이션 없이 즉시 이동
            if (distance < 0.01) {
                lyricsScrollPane.setVvalue(targetVValue);
                return;
            }
            
            // 간단한 선형 애니메이션 (JavaFX Timeline 없이)
            Thread animationThread = new Thread(() -> {
                try {
                    int steps = 10;
                    double stepSize = (targetVValue - currentVValue) / steps;
                    
                    for (int i = 1; i <= steps; i++) {
                        final double newValue = currentVValue + (stepSize * i);
                        
                        Platform.runLater(() -> {
                            try {
                                lyricsScrollPane.setVvalue(newValue);
                            } catch (Exception e) {
                                // 애니메이션 중 오류 무시
                            }
                        });
                        
                        Thread.sleep(20); // 20ms씩 대기 (총 200ms 애니메이션)
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // 애니메이션 오류 무시
                }
            });
            
            animationThread.setName("LyricsScrollAnimation");
            animationThread.setDaemon(true);
            animationThread.start();
            
        } catch (Exception e) {
            // 애니메이션 실패 시 즉시 이동
            lyricsScrollPane.setVvalue(targetVValue);
        }
    }

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

    // 레거시 메서드들 (하위 호환성을 위해 유지)
    
    /**
     * @deprecated 타임스탬프 기반 업데이트를 사용하세요: updateLyricsByTimestamp()
     */
    @Deprecated
    public void updateLyrics(String lyrics) {
        updateCurrentLyricsLabel(lyrics);
    }

    /**
     * @deprecated 타임스탬프 기반 업데이트를 사용하세요: updateLyricsByTimestamp()
     */
    @Deprecated
    public void updateLyrics(String lyric, int index) {
        Platform.runLater(() -> {
            updateCurrentLyricsLabel(lyric);
            
            if (index >= 0 && index < lyricsContainer.getChildren().size()) {
                highlightLineByIndex(index);
                lastHighlightedIndex = index;
            }
        });
    }

    /**
     * @deprecated 타임스탬프 기반 업데이트를 사용하세요: updateLyricsByTimestamp()
     */
    @Deprecated
    public void updateLyrics(String lyric, long timestamp) {
        updateLyricsByTimestamp(timestamp);
    }

    // 공개 메서드들
    public List<LrcLine> getFullLyricsLines() { 
        return allLyricsLines; 
    }
    
    public long getCurrentPlaybackTime() {
        return currentPlaybackTime;
    }
    
    public int getLastHighlightedIndex() {
        return lastHighlightedIndex;
    }
    
    public void showLyricsNotFound() {
        Platform.runLater(() -> {
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
        });
    }
    
    public void showLyricsLoading() {
        Platform.runLater(() -> {
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
        });
    }
    
    public void showLyricsError(String errorMessage) {
        Platform.runLater(() -> {
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
        });
    }
}