package ac.cwnu.synctune.ui.view;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.scene.control.TableView;
import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

// MusicLibraryView에 추가할 고급 기능들

public class MusicLibraryViewExtensions {
    
    /**
     * 음악 파일 드래그 앤 드롭 지원
     */
    private void setupDragAndDrop(TableView<MusicInfo> musicTable) {
        musicTable.setOnDragOver(event -> {
            if (event.getGestureSource() != musicTable && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        musicTable.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                List<File> droppedFiles = db.getFiles();
                List<File> musicFiles = droppedFiles.stream()
                    .filter(UIUtils::hasAudioExtension)
                    .collect(Collectors.toList());
                
                if (!musicFiles.isEmpty()) {
                    // 음악 파일들을 라이브러리에 추가
                    addFilesToLibrary(musicFiles);
                    success = true;
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private void addFilesToLibrary(List<File> musicFiles) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (File file : musicFiles) {
                    // 메타데이터 추출 및 라이브러리에 추가
                    MusicInfo musicInfo = extractMusicInfo(file);
                    if (musicInfo != null) {
                        Platform.runLater(() -> addMusic(musicInfo));
                    }
                }
                return null;
            }
        };
        
        task.setOnSucceeded(e -> {
            UIUtils.showSuccess("파일 추가", musicFiles.size() + "개 음악 파일이 추가되었습니다.");
        });
        
        task.setOnFailed(e -> {
            UIUtils.showError("파일 추가 실패", "일부 파일을 추가할 수 없습니다.");
        });
        
        new Thread(task).start();
    }
    
    private MusicInfo extractMusicInfo(File file) {
        try {
            String fileName = file.getName();
            String title = UIUtils.getFileNameWithoutExtension(fileName);
            String artist = "Unknown Artist";
            
            // 기본 메타데이터 생성 (실제로는 ID3 태그 파싱)
            return new MusicInfo(title, artist, "Unknown Album", 
                file.getAbsolutePath(), 0L, null);
        } catch (Exception e) {
            return null;
        }
    }
    
    // 이 메서드들은 실제 MusicLibraryView에서 구현되어야 합니다
    private void addMusic(MusicInfo musicInfo) {
        // MusicLibraryView의 addMusic 메서드 호출
    }
}