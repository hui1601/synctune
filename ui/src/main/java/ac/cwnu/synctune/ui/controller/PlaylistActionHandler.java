package ac.cwnu.synctune.ui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.event.PlaylistQueryEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.util.MusicInfoHelper;
import ac.cwnu.synctune.ui.view.PlaylistView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;
    private static final String PLAYLIST_NAME = "재생목록"; // 단일 플레이리스트 이름
    
    // 현재 재생 중인 곡 추적
    private MusicInfo currentPlayingMusic;

    public PlaylistActionHandler(PlaylistView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        log.debug("PlaylistActionHandler 초기화 완료 (단일 플레이리스트 모드)");
    }

    private void attachEventHandlers() {
        // 파일 추가 버튼
        view.getAddButton().setOnAction(e -> {
            log.debug("파일 추가 버튼 클릭됨");
            addMusicFiles();
        });

        // 폴더 추가 버튼 (새로 추가)
        view.getAddFolderButton().setOnAction(e -> {
            log.debug("폴더 추가 버튼 클릭됨");
            addMusicFolderWithDialog();
        });

        // 곡 제거 버튼
        view.getRemoveButton().setOnAction(e -> {
            log.debug("곡 제거 버튼 클릭됨");
            removeSelectedMusic();
        });

        // 전체 삭제 버튼
        view.getClearButton().setOnAction(e -> {
            log.debug("전체 삭제 버튼 클릭됨");
            clearCurrentPlaylist();
        });
    }

    private void addMusicFiles() {
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
            addFilesToPlaylist(selectedFiles);
        }
    }

    /**
     * 폴더 추가 다이얼로그 표시 (새로 추가)
     */
    private void addMusicFolderWithDialog() {
        // 폴더 선택 다이얼로그
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 폴더 선택");
        
        // 기본 폴더를 사용자 홈 디렉토리로 설정
        try {
            String userHome = System.getProperty("user.home");
            File homeDir = new File(userHome);
            if (homeDir.exists()) {
                directoryChooser.setInitialDirectory(homeDir);
            }
        } catch (Exception e) {
            log.debug("기본 폴더 설정 실패", e);
        }
        
        File selectedDirectory = directoryChooser.showDialog(view.getScene().getWindow());
        
        if (selectedDirectory != null) {
            // 하위 폴더 포함 여부 확인 다이얼로그
            Alert recursiveAlert = new Alert(Alert.AlertType.CONFIRMATION);
            recursiveAlert.setTitle("폴더 추가 방식 선택");
            recursiveAlert.setHeaderText("폴더 내 음악 파일 검색 방식을 선택하세요");
            recursiveAlert.setContentText("하위 폴더까지 포함하여 검색하시겠습니까?");
            
            ButtonType yesButton = new ButtonType("하위 폴더 포함");
            ButtonType noButton = new ButtonType("현재 폴더만");
            ButtonType cancelButton = new ButtonType("취소", ButtonType.CANCEL.getButtonData());
            
            recursiveAlert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            
            Optional<ButtonType> result = recursiveAlert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == yesButton) {
                    addMusicFromDirectory(selectedDirectory, true);  // 재귀적 검색
                } else if (result.get() == noButton) {
                    addMusicFromDirectory(selectedDirectory, false); // 현재 폴더만
                }
                // 취소인 경우 아무것도 하지 않음
            }
        }
    }
    
    private void addFilesToPlaylist(List<File> files) {
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
                        
                        // MusicInfoHelper를 사용하여 정확한 메타데이터 추출 (개선된 LRC 검색 포함)
                        MusicInfo musicInfo = MusicInfoHelper.createFromFile(file);
                        
                        Platform.runLater(() -> {
                            view.addMusicToCurrentPlaylist(musicInfo);
                            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(PLAYLIST_NAME, musicInfo));
                        });
                        
                        addedCount++;
                        log.debug("음악 파일 추가 완료: {} - {} ({}ms) [LRC: {}]", 
                            musicInfo.getArtist(), musicInfo.getTitle(), musicInfo.getDurationMillis(),
                            musicInfo.getLrcPath() != null ? "있음" : "없음");
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
        List<MusicInfo> selectedMusic = view.getSelectedMusicList();
        
        if (selectedMusic.isEmpty()) {
            showAlert("오류", "제거할 곡을 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 현재 재생 중인 곡이 선택된 곡에 포함되어 있는지 확인
        boolean containsCurrentPlaying = false;
        if (currentPlayingMusic != null) {
            for (MusicInfo music : selectedMusic) {
                if (music.equals(currentPlayingMusic)) {
                    containsCurrentPlaying = true;
                    break;
                }
            }
        }
        
        // 확인 다이얼로그
        String message;
        if (containsCurrentPlaying) {
            if (selectedMusic.size() == 1) {
                message = "현재 재생 중인 곡을 재생목록에서 제거하시겠습니까?\n재생이 정지됩니다.";
            } else {
                message = String.format("선택한 %d개의 곡을 재생목록에서 제거하시겠습니까?\n현재 재생 중인 곡이 포함되어 있어 재생이 정지됩니다.", 
                    selectedMusic.size());
            }
        } else {
            message = selectedMusic.size() == 1 ? 
                "선택한 곡을 재생목록에서 제거하시겠습니까?" :
                String.format("선택한 %d개의 곡을 재생목록에서 제거하시겠습니까?", selectedMusic.size());
        }
            
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("곡 제거");
        confirmAlert.setHeaderText("곡 제거 확인");
        confirmAlert.setContentText(message);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 현재 재생 중인 곡이 제거되는 경우 이벤트 발행
            if (containsCurrentPlaying) {
                log.info("현재 재생 중인 곡이 제거됩니다: {}", currentPlayingMusic.getTitle());
                publisher.publish(new PlaylistQueryEvent.CurrentMusicRemovedFromPlaylistEvent(currentPlayingMusic));
            }
            
            // 선택된 곡들 제거
            selectedMusic.forEach(music -> {
                log.debug("곡 제거 요청 - 재생목록: {}, 곡: {}", PLAYLIST_NAME, music.getTitle());
                publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(PLAYLIST_NAME, music));
                
                // UI에서 즉시 제거
                Platform.runLater(() -> view.removeMusicFromCurrentPlaylist(music));
            });
        }
    }
    
    private void clearCurrentPlaylist() {
        List<MusicInfo> allMusic = view.getAllMusicInCurrentPlaylist();
        if (allMusic.isEmpty()) {
            showAlert("정보", "재생목록이 이미 비어있습니다.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // 현재 재생 중인 곡이 포함되어 있는지 확인
        boolean containsCurrentPlaying = false;
        if (currentPlayingMusic != null) {
            for (MusicInfo music : allMusic) {
                if (music.equals(currentPlayingMusic)) {
                    containsCurrentPlaying = true;
                    break;
                }
            }
        }
        
        // 확인 다이얼로그
        String message;
        if (containsCurrentPlaying) {
            message = String.format("재생목록의 모든 곡(%d개)을 제거하시겠습니까?\n현재 재생 중인 곡이 포함되어 있어 재생이 정지됩니다.", 
                allMusic.size());
        } else {
            message = String.format("재생목록의 모든 곡(%d개)을 제거하시겠습니까?", allMusic.size());
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("재생목록 비우기");
        confirmAlert.setHeaderText("재생목록의 모든 곡을 제거하시겠습니까?");
        confirmAlert.setContentText(message);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 현재 재생 중인 곡이 포함된 경우 이벤트 발행
            if (containsCurrentPlaying) {
                log.info("재생목록 전체 삭제로 현재 재생 중인 곡도 제거됩니다: {}", currentPlayingMusic.getTitle());
                publisher.publish(new PlaylistQueryEvent.CurrentMusicRemovedFromPlaylistEvent(currentPlayingMusic));
            }
            
            allMusic.forEach(music -> {
                publisher.publish(new PlaylistEvent.MusicRemovedFromPlaylistEvent(PLAYLIST_NAME, music));
            });
            
            // UI에서 즉시 제거
            Platform.runLater(() -> {
                view.clearCurrentPlaylistItems();
                view.updateStatusLabel("재생목록이 비워졌습니다.", false);
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

    // ========== 현재 재생 중인 곡 관리 ==========
    
    /**
     * 현재 재생 중인 곡 설정 (UIModule에서 호출)
     */
    public void setCurrentPlayingMusic(MusicInfo music) {
        this.currentPlayingMusic = music;
        log.debug("현재 재생 중인 곡 설정: {}", music != null ? music.getTitle() : "없음");
    }
    
    /**
     * 현재 재생 중인 곡 반환
     */
    public MusicInfo getCurrentPlayingMusic() {
        return currentPlayingMusic;
    }

    // 외부에서 호출 가능한 메서드들
    public void addMusicToCurrentPlaylist(MusicInfo music) {
        if (music != null) {
            Platform.runLater(() -> {
                view.addMusicToCurrentPlaylist(music);
            });
            publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(PLAYLIST_NAME, music));
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
        if (files != null && !files.isEmpty()) {
            addFilesToPlaylist(files);
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
     * 폴더에서 음악 파일들을 추가 (개선된 버전)
     */
    public void addMusicFromDirectory(File directory, boolean recursive) {
        if (directory == null || !directory.isDirectory()) {
            showAlert("오류", "유효한 폴더를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        log.info("폴더에서 음악 파일 추가 시작: {} (재귀: {})", directory.getAbsolutePath(), recursive);
        
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
                
                log.info("폴더 스캔 완료: {}개 파일 발견", musicFiles.size());
                
                // 파일들을 재생목록에 추가
                addFilesToPlaylist(musicFiles);
                
            } catch (Exception e) {
                log.error("폴더 스캔 중 오류", e);
                Platform.runLater(() -> {
                    view.updateStatusLabel("폴더 스캔 중 오류가 발생했습니다.", true);
                    showAlert("오류", "폴더 스캔 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
        
        scanThread.setName("DirectoryScanner-" + directory.getName());
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    /**
     * 개선된 폴더 내 음악 파일 검색 (재귀 옵션 포함)
     */
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
                log.debug("음악 파일 발견: {}", file.getAbsolutePath());
            } else if (file.isDirectory() && recursive && !isExcludedDirectory(file)) {
                // 재귀적으로 하위 디렉토리 스캔 (제외 디렉토리가 아닌 경우)
                List<File> subDirFiles = findMusicFilesInDirectory(file, true);
                musicFiles.addAll(subDirFiles);
                log.debug("하위 디렉토리 스캔: {} ({}개 파일)", file.getName(), subDirFiles.size());
            }
        }
        
        // 파일명으로 정렬 (자연스러운 순서)
        musicFiles.sort((f1, f2) -> {
            String name1 = f1.getName().toLowerCase();
            String name2 = f2.getName().toLowerCase();
            return name1.compareToIgnoreCase(name2);
        });
        
        return musicFiles;
    }
    
    /**
     * 제외해야 할 디렉토리인지 확인
     */
    private boolean isExcludedDirectory(File directory) {
        String dirName = directory.getName().toLowerCase();
        
        // 숨김 폴더나 시스템 폴더 제외
        return dirName.startsWith(".") || 
               dirName.equals("system") ||
               dirName.equals("windows") ||
               dirName.equals("program files") ||
               dirName.equals("program files (x86)") ||
               dirName.equals("$recycle.bin") ||
               dirName.equals("recycler") ||
               dirName.equals("temp") ||
               dirName.equals("tmp");
    }
}