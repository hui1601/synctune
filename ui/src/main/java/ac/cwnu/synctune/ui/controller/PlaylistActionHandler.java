package ac.cwnu.synctune.ui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.view.PlaylistView;
import ac.cwnu.synctune.ui.util.MusicInfoHelper;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;

    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        log.debug("PlaylistActionHandler 초기화 완료");
    }

    private void attachEventHandlers() {
        // 플레이리스트 생성
        view.getCreateButton().setOnAction(e -> createNewPlaylist());

        // 플레이리스트 삭제
        view.getDeleteButton().setOnAction(e -> deleteSelectedPlaylist());

        // 곡 추가 - 파일 선택
        view.getAddButton().setOnAction(e -> addMusicFiles());

        // 곡 제거
        view.getRemoveButton().setOnAction(e -> removeSelectedMusic());
        
        // 플레이리스트 전체 삭제
        view.getClearButton().setOnAction(e -> clearCurrentPlaylist());
    }

    private void createNewPlaylist() {
        String name = view.getPlaylistNameInput();
        if (name == null || name.trim().isEmpty()) {
            showAlert("오류", "플레이리스트 이름을 입력해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 중복 이름 확인
        if (view.getPlaylists().contains(name)) {
            showAlert("오류", "이미 존재하는 플레이리스트 이름입니다.", Alert.AlertType.WARNING);
            return;
        }
        
        log.debug("새 플레이리스트 생성 요청: {}", name);
        Playlist playlist = new Playlist(name);
        publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(playlist));
        
        // UI 업데이트
        Platform.runLater(() -> {
            view.addPlaylist(name);
            view.clearPlaylistNameInput();
        });
    }

    private void deleteSelectedPlaylist() {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "삭제할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 기본 플레이리스트는 삭제 불가
        if (view.isDefaultPlaylist(selectedPlaylist)) {
            showAlert("오류", "기본 플레이리스트는 삭제할 수 없습니다.", Alert.AlertType.WARNING);
            return;
        }
        
        // 삭제 확인 다이얼로그
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("플레이리스트 삭제");
        confirmAlert.setHeaderText("플레이리스트를 삭제하시겠습니까?");
        confirmAlert.setContentText("플레이리스트 '" + selectedPlaylist + "'를 삭제하시겠습니까?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.debug("플레이리스트 삭제 요청: {}", selectedPlaylist);
            publisher.publish(new PlaylistEvent.PlaylistDeletedEvent(selectedPlaylist));
            
            // UI에서 즉시 제거
            Platform.runLater(() -> view.removePlaylist(selectedPlaylist));
        }
    }

    private void addMusicFiles() {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 파일 선택 다이얼로그
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 선택");
        
        // 확장자 필터 설정
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("모든 음악 파일", "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg"),
            new FileChooser.ExtensionFilter("MP3 파일", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV 파일", "*.wav"),
            new FileChooser.ExtensionFilter("FLAC 파일", "*.flac"),
            new FileChooser.ExtensionFilter("M4A 파일", "*.m4a"),
            new FileChooser.ExtensionFilter("AAC 파일", "*.aac"),
            new FileChooser.ExtensionFilter("OGG 파일", "*.ogg"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        // 여러 파일 선택
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(view.getScene().getWindow());
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            addFilesToPlaylist(selectedFiles, selectedPlaylist);
        }
    }
    
    private void addFilesToPlaylist(List<File> files, String playlistName) {
        if (files.isEmpty()) return;
        
        view.updateStatusLabel("음악 파일을 추가하는 중...", false);
        
        // 백그라운드에서 처리 (UI 블로킹 방지)
        Thread processThread = new Thread(() -> {
            int addedCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (File file : files) {
                try {
                    if (MusicInfoHelper.isSupportedAudioFile(file)) {
                        log.debug("음악 파일 처리 시작: {}", file.getName());
                        
                        // MusicInfoHelper를 사용하여 정확한 메타데이터 추출
                        MusicInfo musicInfo = MusicInfoHelper.createFromFile(file);
                        
                        Platform.runLater(() -> {
                            view.addMusicToCurrentPlaylist(musicInfo);
                            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, musicInfo));
                        });
                        
                        addedCount++;
                        log.debug("음악 파일 추가 완료: {} - {} ({}ms)", 
                            musicInfo.getArtist(), musicInfo.getTitle(), musicInfo.getDurationMillis());
                    } else {
                        errorCount++;
                        errorMessages.add(file.getName() + " (지원되지 않는 형식)");
                        log.warn("지원되지 않는 파일 형식: {}", file.getName());
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add(file.getName() + " (" + e.getMessage() + ")");
                    log.error("음악 파일 처리 중 오류: {} - {}", file.getName(), e.getMessage());
                }
            }
            
            // UI 업데이트 (메인 스레드에서)
            final int finalAddedCount = addedCount;
            final int finalErrorCount = errorCount;
            final List<String> finalErrorMessages = new ArrayList<>(errorMessages);
            
            Platform.runLater(() -> {
                if (finalAddedCount > 0) {
                    view.updateStatusLabel(String.format("%d개의 음악 파일이 추가되었습니다.", finalAddedCount), false);
                }
                
                if (finalErrorCount > 0) {
                    StringBuilder errorMsg = new StringBuilder();
                    errorMsg.append(String.format("%d개 파일이 추가되지 못했습니다:\n", finalErrorCount));
                    
                    // 최대 5개까지만 표시
                    int showCount = Math.min(finalErrorMessages.size(), 5);
                    for (int i = 0; i < showCount; i++) {
                        errorMsg.append("• ").append(finalErrorMessages.get(i)).append("\n");
                    }
                    
                    if (finalErrorMessages.size() > 5) {
                        errorMsg.append(String.format("... 및 %d개 더", finalErrorMessages.size() - 5));
                    }
                    
                    showAlert("경고", errorMsg.toString(), Alert.AlertType.WARNING);
                }
                
                if (finalAddedCount == 0 && finalErrorCount > 0) {
                    view.updateStatusLabel("선택한 파일 중 추가할 수 있는 음악 파일이 없습니다.", true);
                }
            });
        });
        
        processThread.setName("MusicFileProcessor");
        processThread.setDaemon(true);
        processThread.start();
    }
    
    private void removeSelectedMusic() {
        String selectedPlaylist = view.getSelectedPlaylist();
        List<MusicInfo> selectedMusic = view.getSelectedMusicList();
        
        if (selectedPlaylist == null || selectedMusic.isEmpty()) {
            showAlert("오류", "제거할 곡을 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 확인 다이얼로그
        String message = selectedMusic.size() == 1 ? 
            "선택한 곡을 플레이리스트에서 제거하시겠습니까?" :
            String.format("선택한 %d개의 곡을 플레이리스트에서 제거하시겠습니까?", selectedMusic.size());
            
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("곡 제거");
        confirmAlert.setHeaderText("곡 제거 확인");
        confirmAlert.setContentText(message);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedMusic.forEach(music -> {
                log.debug("곡 제거 요청 - 플레이리스트: {}, 곡: {}", selectedPlaylist, music.getTitle());
                publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(selectedPlaylist, music));
                
                // UI에서 즉시 제거
                Platform.runLater(() -> view.removeMusicFromCurrentPlaylist(music));
            });
        }
    }
    
    private void clearCurrentPlaylist() {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "비울 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        List<MusicInfo> allMusic = view.getAllMusicInCurrentPlaylist();
        if (allMusic.isEmpty()) {
            showAlert("정보", "플레이리스트가 이미 비어있습니다.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // 확인 다이얼로그
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("플레이리스트 비우기");
        confirmAlert.setHeaderText("플레이리스트의 모든 곡을 제거하시겠습니까?");
        confirmAlert.setContentText(String.format("플레이리스트 '%s'의 모든 곡(%d개)을 제거하시겠습니까?", 
                                                 selectedPlaylist, allMusic.size()));
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            allMusic.forEach(music -> {
                publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(selectedPlaylist, music));
            });
            
            // UI에서 즉시 제거
            Platform.runLater(() -> {
                view.clearCurrentPlaylistItems();
                view.updateStatusLabel("플레이리스트가 비워졌습니다.", false);
            });
        }
    }
    
    public void playMusic(MusicInfo music) {
        if (music != null && publisher != null) {
            log.info("곡 재생 요청: {} - {}", music.getArtist(), music.getTitle());
            publisher.publish(new MediaControlEvent.RequestPlayEvent(music));
            
            Platform.runLater(() -> {
                view.updateStatusLabel("재생 중: " + music.getTitle(), false);
            });
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // 외부에서 호출 가능한 메서드들
    public void addMusicToCurrentPlaylist(MusicInfo music) {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist != null && music != null) {
            Platform.runLater(() -> {
                view.addMusicToCurrentPlaylist(music);
            });
            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(selectedPlaylist, music));
        }
    }

    public void updatePlaylistItems(List<MusicInfo> musicList) {
        Platform.runLater(() -> {
            view.updatePlaylistItems(musicList);
        });
    }
    
    /**
     * 여러 파일을 한 번에 추가 (외부에서 호출)
     */
    public void addMusicFiles(List<File> files) {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        if (files != null && !files.isEmpty()) {
            addFilesToPlaylist(files, selectedPlaylist);
        }
    }
    
    /**
     * 단일 파일 추가 (외부에서 호출)
     */
    public void addMusicFile(File file) {
        if (file != null) {
            addMusicFiles(List.of(file));
        }
    }
    
    /**
     * 폴더에서 음악 파일들을 추가
     */
    public void addMusicFromDirectory(File directory, boolean recursive) {
        if (directory == null || !directory.isDirectory()) {
            showAlert("오류", "유효한 폴더를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 백그라운드에서 폴더 스캔
        Thread scanThread = new Thread(() -> {
            try {
                Platform.runLater(() -> view.updateStatusLabel("폴더를 스캔하는 중...", false));
                
                List<File> musicFiles = findMusicFilesInDirectory(directory, recursive);
                
                if (musicFiles.isEmpty()) {
                    Platform.runLater(() -> {
                        view.updateStatusLabel("폴더에서 음악 파일을 찾을 수 없습니다.", true);
                        showAlert("알림", "선택한 폴더에서 지원되는 음악 파일을 찾을 수 없습니다.", Alert.AlertType.INFORMATION);
                    });
                    return;
                }
                
                Platform.runLater(() -> {
                    view.updateStatusLabel(String.format("폴더에서 %d개의 음악 파일을 발견했습니다.", musicFiles.size()), false);
                });
                
                // 파일들을 플레이리스트에 추가
                addFilesToPlaylist(musicFiles, selectedPlaylist);
                
            } catch (Exception e) {
                log.error("폴더 스캔 중 오류", e);
                Platform.runLater(() -> {
                    view.updateStatusLabel("폴더 스캔 중 오류가 발생했습니다.", true);
                    showAlert("오류", "폴더 스캔 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
        
        scanThread.setName("DirectoryScanner");
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    private List<File> findMusicFilesInDirectory(File directory, boolean recursive) {
        List<File> musicFiles = new ArrayList<>();
        
        if (!directory.isDirectory()) {
            return musicFiles;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return musicFiles;
        }
        
        for (File file : files) {
            if (file.isFile() && MusicInfoHelper.isSupportedAudioFile(file)) {
                musicFiles.add(file);
            } else if (file.isDirectory() && recursive && !file.getName().startsWith(".")) {
                // 숨김 폴더는 스캔하지 않음
                musicFiles.addAll(findMusicFilesInDirectory(file, true));
            }
        }
        
        // 파일명으로 정렬
        musicFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        
        return musicFiles;
    }
}