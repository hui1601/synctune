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
    // 필드를 선언과 동시에 초기화
    private final MarqueeLabel currentLyricsLabel = new MarqueeLabel("가사가 없습니다");
    private final VBox lyricsContainer = new VBox(5);
    private final ScrollPane lyricsScrollPane;
    private final Label statusLabel = new Label("가사 상태: 대기 중");
    
    private boolean lyricsAvailable = false;
    private String[] fullLyricsLines = null;

    public LyricsView() {
        // ScrollPane은 lyricsContainer가 필요하므로 생성자에서 초기화
        lyricsScrollPane = new ScrollPane(lyricsContainer);
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // 현재 가사 표시 설정
        currentLyricsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // 상태 라벨 설정
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setStyle("-fx-text-fill: #666666;");

        // 전체 가사 표시용 컨테이너 설정
        lyricsContainer.setAlignment(Pos.TOP_CENTER);
        lyricsContainer.setPadding(new Insets(10));

        // ScrollPane 설정
        lyricsScrollPane.setFitToWidth(true);
        lyricsScrollPane.setPrefHeight(400);
        lyricsScrollPane.setStyle("-fx-background-color: #fafafa;");
    }

    private void layoutComponents() {
        setSpacing(15);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("가사");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        getChildren().addAll(titleLabel, statusLabel, currentLyricsLabel, lyricsScrollPane);
    }

    public void updateLyrics(String lyrics) {
        if (lyrics == null || lyrics.trim().isEmpty()) {
            currentLyricsLabel.setText("가사가 없습니다");
            return;
        }
        
        currentLyricsLabel.setText(lyrics);
        
        // 전체 가사에서 현재 라인 하이라이트
        if (fullLyricsLines != null) {
            highlightCurrentLine(lyrics);
            scrollToCurrentLine(lyrics);
        }
    }

    public void setFullLyrics(String[] lyricsLines) {
        this.fullLyricsLines = lyricsLines;
        lyricsContainer.getChildren().clear();
        
        if (lyricsLines == null || lyricsLines.length == 0) {
            Label noLyricsLabel = new Label("가사를 찾을 수 없습니다");
            noLyricsLabel.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
            lyricsContainer.getChildren().add(noLyricsLabel);
            return;
        }
        
        for (String line : lyricsLines) {
            Label lineLabel = new Label(line.trim());
            lineLabel.setWrapText(true);
            lineLabel.setPadding(new Insets(2, 5, 2, 5));
            lineLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");
            lyricsContainer.getChildren().add(lineLabel);
        }
    }

    public void showLyricsAvailable(boolean available) {
        this.lyricsAvailable = available;
        if (available) {
            statusLabel.setText("가사 상태: 가사 파일 발견됨");
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
        } else {
            statusLabel.setText("가사 상태: 가사 파일 없음");
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            clearLyrics();
        }
    }

    public void showLyricsLoading() {
        statusLabel.setText("가사 상태: 로딩 중...");
        statusLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 12px;");
        currentLyricsLabel.setText("가사를 불러오는 중...");
    }

    public void showLyricsParseError() {
        statusLabel.setText("가사 상태: 파싱 오류");
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        currentLyricsLabel.setText("가사 파일을 읽을 수 없습니다");
        clearLyricsContainer();
    }

    public void showLyricsReady() {
        statusLabel.setText("가사 상태: 동기화 준비됨");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
    }

    private void highlightCurrentLine(String currentLine) {
        lyricsContainer.getChildren().forEach(node -> {
            if (node instanceof Label label) {
                if (label.getText().trim().equals(currentLine.trim())) {
                    // 현재 라인 하이라이트
                    label.setStyle(
                        "-fx-text-fill: #ffffff; " +
                        "-fx-background-color: #007bff; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px;"
                    );
                } else {
                    // 다른 라인들은 기본 스타일
                    label.setStyle(
                        "-fx-text-fill: #333333; " +
                        "-fx-background-color: transparent; " +
                        "-fx-padding: 2 5 2 5; " +
                        "-fx-font-weight: normal; " +
                        "-fx-font-size: 13px;"
                    );
                }
            }
        });
    }

    private void scrollToCurrentLine(String currentLine) {
        // 현재 하이라이트된 라인으로 자동 스크롤
        for (int i = 0; i < lyricsContainer.getChildren().size(); i++) {
            if (lyricsContainer.getChildren().get(i) instanceof Label label) {
                if (label.getText().trim().equals(currentLine.trim())) {
                    // 스크롤 위치 계산 (가운데 정렬)
                    double targetPosition = (double) i / lyricsContainer.getChildren().size();
                    double centeredPosition = Math.max(0, targetPosition - 0.3); // 약간 위쪽에 위치
                    
                    lyricsScrollPane.setVvalue(centeredPosition);
                    break;
                }
            }
        }
    }

    public void clearLyrics() {
        currentLyricsLabel.setText("가사가 없습니다");
        clearLyricsContainer();
    }

    private void clearLyricsContainer() {
        lyricsContainer.getChildren().clear();
        Label noLyricsLabel = new Label("가사를 찾을 수 없습니다");
        noLyricsLabel.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
        lyricsContainer.getChildren().add(noLyricsLabel);
        fullLyricsLines = null;
    }

    // ========== 가사 검색 및 표시 기능 ==========
    
    public void searchLyricsOnline(String artist, String title) {
        showLyricsLoading();
        statusLabel.setText("가사 상태: 온라인 검색 중...");
        
        // TODO: 온라인 가사 검색 API 연동
        // 현재는 시뮬레이션
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(2),
                e -> {
                    statusLabel.setText("가사 상태: 온라인에서 가사를 찾을 수 없음");
                    statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
                }
            )
        );
        timeline.play();
    }

    public void showLyricsFromFile(String lrcFilePath) {
        showLyricsLoading();
        statusLabel.setText("가사 상태: 파일에서 로딩 중... " + getShortPath(lrcFilePath));
    }

    private String getShortPath(String fullPath) {
        if (fullPath == null || fullPath.length() <= 30) {
            return fullPath;
        }
        return "..." + fullPath.substring(fullPath.length() - 27);
    }

    // ========== 가사 표시 옵션 ==========
    
    public void setLyricsFontSize(int size) {
        currentLyricsLabel.setFont(Font.font("System", FontWeight.BOLD, size));
        
        // 전체 가사 컨테이너의 폰트 크기도 조정
        lyricsContainer.getChildren().forEach(node -> {
            if (node instanceof Label label) {
                String currentStyle = label.getStyle();
                String newStyle = currentStyle.replaceAll("-fx-font-size: \\d+px", "-fx-font-size: " + (size - 2) + "px");
                label.setStyle(newStyle);
            }
        });
    }

    public void setLyricsColor(String color) {
        String style = "-fx-text-fill: " + color + ";";
        currentLyricsLabel.setStyle(style);
    }

    public void enableKaraokeMode(boolean enabled) {
        if (enabled) {
            statusLabel.setText("가사 상태: 노래방 모드 활성화");
            // 노래방 모드에서는 가사가 더 크게 표시
            setLyricsFontSize(24);
        } else {
            statusLabel.setText("가사 상태: 일반 모드");
            setLyricsFontSize(18);
        }
    }

    public boolean isLyricsAvailable() {
        return lyricsAvailable;
    }
}