package ac.cwnu.synctune.ui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class UIUtils {
    
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
     * 음악 파일 선택 다이얼로그
     */
    public static List<File> showMusicFileChooser(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 선택");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("모든 음악 파일", "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg"),
            new FileChooser.ExtensionFilter("MP3 파일", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV 파일", "*.wav"),
            new FileChooser.ExtensionFilter("FLAC 파일", "*.flac"),
            new FileChooser.ExtensionFilter("M4A 파일", "*.m4a"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        return fileChooser.showOpenMultipleDialog(owner);
    }
    
    /**
     * 폴더 선택 다이얼로그
     */
    public static File showDirectoryChooser(Stage owner, String title) {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(owner);
    }
    
    /**
     * 시간을 MM:SS 형식으로 포맷
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * 시간을 HH:MM:SS 형식으로 포맷 (긴 곡용)
     */
    public static String formatLongTime(long milliseconds) {
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
}