package ac.cwnu.synctune.ui.util;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class UIUtils {
    
    // 진행 다이얼로그 참조 (전역적으로 관리)
    private static Alert currentProgressDialog = null;
    
    /**
     * 메인 스레드에서 안전하게 UI 업데이트 실행
     */
    public static void runOnUIThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
    
    /**
     * 에러 다이얼로그 표시
     */
    public static void showError(String title, String message) {
        runOnUIThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            // 창 스타일 설정
            alert.getDialogPane().setStyle("-fx-font-size: 12px;");
            alert.showAndWait();
        });
    }
    
    /**
     * 경고 다이얼로그 표시
     */
    public static void showWarning(String title, String message) {
        runOnUIThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 정보 다이얼로그 표시
     */
    public static void showInfo(String title, String message) {
        runOnUIThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 확인 다이얼로그 표시
     */
    public static boolean showConfirmation(String title, String message) {
        final boolean[] result = {false};
        
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            Optional<ButtonType> response = alert.showAndWait();
            result[0] = response.isPresent() && response.get() == ButtonType.OK;
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                
                Optional<ButtonType> response = alert.showAndWait();
                result[0] = response.isPresent() && response.get() == ButtonType.OK;
            });
        }
        
        return result[0];
    }
    
    /**
     * 텍스트 입력 다이얼로그 표시
     */
    public static String showTextInput(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * 진행 다이얼로그 표시
     */
    public static void showProgressDialog(String title, String message) {
        runOnUIThread(() -> {
            // 기존 다이얼로그가 있다면 닫기
            hideProgressDialog();
            
            currentProgressDialog = new Alert(Alert.AlertType.NONE);
            currentProgressDialog.setTitle(title);
            currentProgressDialog.setHeaderText(message);
            
            // 진행 표시기 추가
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefSize(50, 50);
            
            VBox content = new VBox(10);
            content.getChildren().addAll(progressIndicator);
            content.setStyle("-fx-alignment: center; -fx-padding: 20;");
            
            currentProgressDialog.getDialogPane().setContent(content);
            currentProgressDialog.getButtonTypes().clear(); // 버튼 제거
            currentProgressDialog.show();
        });
    }
    
    /**
     * 진행 다이얼로그 업데이트
     */
    public static void updateProgressDialog(String message) {
        runOnUIThread(() -> {
            if (currentProgressDialog != null) {
                currentProgressDialog.setHeaderText(message);
            }
        });
    }
    
    /**
     * 진행 다이얼로그 숨김
     */
    public static void hideProgressDialog() {
        runOnUIThread(() -> {
            if (currentProgressDialog != null) {
                currentProgressDialog.close();
                currentProgressDialog = null;
            }
        });
    }
    
    /**
     * 음악 파일 선택 다이얼로그
     */
    public static List<File> showMusicFileChooser(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 선택");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("모든 음악 파일", 
                "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("MP3 파일", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV 파일", "*.wav"),
            new FileChooser.ExtensionFilter("FLAC 파일", "*.flac"),
            new FileChooser.ExtensionFilter("M4A 파일", "*.m4a"),
            new FileChooser.ExtensionFilter("AAC 파일", "*.aac"),
            new FileChooser.ExtensionFilter("OGG 파일", "*.ogg"),
            new FileChooser.ExtensionFilter("WMA 파일", "*.wma"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        // 기본 디렉토리 설정 (사용자 음악 폴더)
        String userHome = System.getProperty("user.home");
        File musicDir = new File(userHome, "Music");
        if (musicDir.exists() && musicDir.isDirectory()) {
            fileChooser.setInitialDirectory(musicDir);
        }
        
        return fileChooser.showOpenMultipleDialog(owner);
    }
    
    /**
     * 단일 음악 파일 선택 다이얼로그
     */
    public static File showSingleMusicFileChooser(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 선택");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("모든 음악 파일", 
                "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        return fileChooser.showOpenDialog(owner);
    }
    
    /**
     * 가사 파일 선택 다이얼로그
     */
    public static File showLyricsFileChooser(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("가사 파일 선택");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("LRC 가사 파일", "*.lrc"),
            new FileChooser.ExtensionFilter("텍스트 파일", "*.txt"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        return fileChooser.showOpenDialog(owner);
    }
    
    /**
     * 파일 저장 다이얼로그
     */
    public static File showSaveFileDialog(Stage owner, String title, String defaultFileName, 
                                         String description, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        if (defaultFileName != null) {
            fileChooser.setInitialFileName(defaultFileName);
        }
        
        if (extensions != null && extensions.length > 0) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, extensions)
            );
        }
        
        return fileChooser.showSaveDialog(owner);
    }
    
    /**
     * 폴더 선택 다이얼로그
     */
    public static File showDirectoryChooser(Stage owner, String title) {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle(title);
        
        // 기본 디렉토리 설정
        String userHome = System.getProperty("user.home");
        File homeDir = new File(userHome);
        if (homeDir.exists() && homeDir.isDirectory()) {
            directoryChooser.setInitialDirectory(homeDir);
        }
        
        return directoryChooser.showDialog(owner);
    }
    
    /**
     * 시간을 MM:SS 형식으로 포맷
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * 시간을 HH:MM:SS 형식으로 포맷 (긴 곡용)
     */
    public static String formatLongTime(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00:00";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * 파일 크기를 사람이 읽기 쉬운 형식으로 포맷
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 파일 확장자 확인
     */
    public static boolean hasAudioExtension(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") ||
               name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ogg") ||
               name.endsWith(".wma");
    }
    
    /**
     * 가사 파일 확장자 확인
     */
    public static boolean hasLyricsExtension(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        return name.endsWith(".lrc") || name.endsWith(".txt");
    }
    
    /**
     * 경로를 짧게 표시 (긴 경로용)
     */
    public static String shortenPath(String fullPath, int maxLength) {
        if (fullPath == null || fullPath.length() <= maxLength) {
            return fullPath;
        }
        
        return "..." + fullPath.substring(fullPath.length() - (maxLength - 3));
    }
    
    /**
     * 파일명에서 확장자 제거
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * 파일의 확장자 가져오기
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
    
    /**
     * 안전한 파일명 생성 (특수문자 제거)
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        
        // 윈도우와 유닉스에서 사용할 수 없는 문자들 제거
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_").trim();
    }
    
    /**
     * 토스트 메시지 표시 (간단한 알림)
     */
    public static void showToast(Stage owner, String message, int durationMs) {
        runOnUIThread(() -> {
            // 간단한 토스트 구현
            Alert toast = new Alert(Alert.AlertType.INFORMATION);
            toast.setTitle("알림");
            toast.setHeaderText(null);
            toast.setContentText(message);
            toast.initOwner(owner);
            
            // 자동으로 닫히도록 타이머 설정
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(durationMs),
                    e -> toast.close()
                )
            );
            
            toast.show();
            timeline.play();
        });
    }
    
    /**
     * 성공 메시지 표시
     */
    public static void showSuccess(String title, String message) {
        runOnUIThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText("✅ 성공");
            alert.setContentText(message);
            alert.getDialogPane().setStyle("-fx-font-size: 12px;");
            alert.showAndWait();
        });
    }
    
    /**
     * 작업 완료 알림 (간단한 형태)
     */
    public static void showTaskCompleted(String taskName, int itemCount) {
        String message = String.format("%s 완료: %d개 항목 처리됨", taskName, itemCount);
        showInfo("작업 완료", message);
    }
    
    /**
     * 오류와 함께 상세 정보 표시
     */
    public static void showDetailedError(String title, String message, Exception exception) {
        runOnUIThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("오류가 발생했습니다");
            alert.setContentText(message);
            
            if (exception != null) {
                // 상세 정보 영역에 스택 트레이스 추가
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                exception.printStackTrace(pw);
                
                javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(sw.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                
                alert.getDialogPane().setExpandableContent(textArea);
            }
            
            alert.showAndWait();
        });
    }
    
    /**
     * 커스텀 선택 다이얼로그
     */
    public static Optional<String> showChoiceDialog(String title, String message, 
                                                   String defaultChoice, String... choices) {
        javafx.scene.control.ChoiceDialog<String> dialog = 
            new javafx.scene.control.ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        return dialog.showAndWait();
    }
    
    /**
     * 다중 선택 리스트 다이얼로그
     */
    public static List<String> showMultiSelectDialog(String title, String message, List<String> options) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(message);
        
        VBox content = new VBox(10);
        List<javafx.scene.control.CheckBox> checkBoxes = new java.util.ArrayList<>();
        
        options.forEach(option -> {
            javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox(option);
            checkBoxes.add(checkBox);
            content.getChildren().add(checkBox);
        });
        
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setPrefHeight(200);
        scrollPane.setFitToWidth(true);
        
        dialog.getDialogPane().setContent(scrollPane);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return checkBoxes.stream()
                .filter(javafx.scene.control.CheckBox::isSelected)
                .map(javafx.scene.control.CheckBox::getText)
                .toList();
        }
        
        return new java.util.ArrayList<>();
    }
    
    /**
     * 시간 입력 다이얼로그 (MM:SS 형식)
     */
    public static Optional<Long> showTimeInputDialog(String title, String message) {
        TextInputDialog dialog = new TextInputDialog("00:00");
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message + " (MM:SS 형식):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                String[] parts = result.get().split(":");
                if (parts.length == 2) {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    return Optional.of((long) (minutes * 60 + seconds) * 1000);
                }
            } catch (NumberFormatException e) {
                showError("입력 오류", "올바른 시간 형식(MM:SS)으로 입력해주세요.");
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 플레이리스트 내보내기 다이얼로그
     */
    public static File showPlaylistExportDialog(Stage owner) {
        return showSaveFileDialog(owner, "플레이리스트 내보내기", "playlist.m3u", 
                                 "M3U 플레이리스트", "*.m3u");
    }
    
    /**
     * 플레이리스트 가져오기 다이얼로그
     */
    public static File showPlaylistImportDialog(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("플레이리스트 가져오기");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("플레이리스트 파일", "*.m3u", "*.m3u8", "*.pls"),
            new FileChooser.ExtensionFilter("M3U 플레이리스트", "*.m3u", "*.m3u8"),
            new FileChooser.ExtensionFilter("PLS 플레이리스트", "*.pls"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        return fileChooser.showOpenDialog(owner);
    }
    
    /**
     * 설정 백업 저장 다이얼로그
     */
    public static File showSettingsBackupDialog(Stage owner) {
        return showSaveFileDialog(owner, "설정 백업", "synctune-settings.json", 
                                 "JSON 설정 파일", "*.json");
    }
    
    /**
     * 오디오 품질 정보 포맷팅
     */
    public static String formatAudioQuality(int sampleRate, int bitDepth, int channels) {
        String channelDesc = switch (channels) {
            case 1 -> "Mono";
            case 2 -> "Stereo";
            case 6 -> "5.1";
            case 8 -> "7.1";
            default -> channels + "ch";
        };
        
        return String.format("%d Hz / %d-bit / %s", sampleRate, bitDepth, channelDesc);
    }
    
    /**
     * 비트레이트 포맷팅
     */
    public static String formatBitrate(int bitrate) {
        if (bitrate >= 1000) {
            return String.format("%.1f Mbps", bitrate / 1000.0);
        } else {
            return bitrate + " kbps";
        }
    }
    
    /**
     * 진행률 포맷팅 (백분율)
     */
    public static String formatProgress(long current, long total) {
        if (total <= 0) {
            return "0%";
        }
        double percentage = (double) current / total * 100;
        return String.format("%.1f%%", percentage);
    }
    
    /**
     * CSS 스타일 적용
     */
    public static void applyStyle(javafx.scene.Node node, String style) {
        if (node != null) {
            node.setStyle(style);
        }
    }
    
    /**
     * CSS 클래스 추가
     */
    public static void addStyleClass(javafx.scene.Node node, String styleClass) {
        if (node != null && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }
    
    /**
     * CSS 클래스 제거
     */
    public static void removeStyleClass(javafx.scene.Node node, String styleClass) {
        if (node != null) {
            node.getStyleClass().remove(styleClass);
        }
    }
    
    /**
     * 디버그 정보 표시 (개발용)
     */
    public static void showDebugInfo(String title, Object... debugData) {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < debugData.length; i += 2) {
            if (i + 1 < debugData.length) {
                content.append(debugData[i]).append(": ").append(debugData[i + 1]).append("\n");
            }
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Debug: " + title);
        alert.setHeaderText("디버그 정보");
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
}