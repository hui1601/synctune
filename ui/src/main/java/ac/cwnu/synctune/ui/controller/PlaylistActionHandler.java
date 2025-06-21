package ac.cwnu.synctune.ui.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.view.PlaylistView;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class PlaylistActionHandler {
    private static final Logger log = LogManager.getLogger(PlaylistActionHandler.class);
    
    private final PlaylistView view;
    private final EventPublisher publisher;
    
    // 지원되는 음악 파일 확장자
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
        "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma"
    );

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
        
        // 폴더에서 곡 추가
        view.getAddFolderButton().setOnAction(e -> addMusicFromFolder());

        // 곡 제거
        view.getRemoveButton().setOnAction(e -> removeSelectedMusic());
        
        // 플레이리스트 전체 삭제
        view.getClearButton().setOnAction(e -> clearCurrentPlaylist());
        
        // 플레이리스트 가져오기
        view.getImportButton().setOnAction(e -> importPlaylist());
        
        // 플레이리스트 내보내기
        view.getExportButton().setOnAction(e -> exportPlaylist());
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
        confirmAlert.setContentText("플레이리스트 '" + selectedPlaylist + "'를 삭제하시겠습니까?\n" +
                                   "이 작업은 되돌릴 수 없습니다.");
        
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
        FileChooser.ExtensionFilter musicFilter = new FileChooser.ExtensionFilter(
            "음악 파일", 
            SUPPORTED_EXTENSIONS.stream().map(ext -> "*." + ext).toArray(String[]::new)
        );
        fileChooser.getExtensionFilters().add(musicFilter);
        
        // 개별 확장자 필터들
        SUPPORTED_EXTENSIONS.forEach(ext -> {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(ext.toUpperCase() + " 파일", "*." + ext)
            );
        });
        
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        // 여러 파일 선택
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(view.getScene().getWindow());
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            addFilesToPlaylist(selectedFiles, selectedPlaylist);
        }
    }
    
    private void addMusicFromFolder() {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "곡을 추가할 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        // 폴더 선택 다이얼로그
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 폴더 선택");
        
        File selectedDirectory = directoryChooser.showDialog(view.getScene().getWindow());
        
        if (selectedDirectory != null) {
            // 백그라운드에서 폴더 스캔
            scanFolderForMusic(selectedDirectory, selectedPlaylist);
        }
    }
    
    private void scanFolderForMusic(File directory, String playlistName) {
        // 진행 상황 다이얼로그
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("폴더 스캔");
        progressAlert.setHeaderText("음악 파일을 검색하고 있습니다...");
        progressAlert.setContentText("폴더: " + directory.getName());
        
        // 취소 버튼 추가
        ButtonType cancelButton = new ButtonType("취소");
        progressAlert.getButtonTypes().setAll(cancelButton);
        
        // 백그라운드 태스크
        Task<List<File>> scanTask = new Task<List<File>>() {
            @Override
            protected List<File> call() throws Exception {
                List<File> musicFiles = new ArrayList<>();
                scanDirectoryRecursive(directory, musicFiles);
                return musicFiles;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    progressAlert.close();
                    List<File> musicFiles = getValue();
                    
                    if (musicFiles.isEmpty()) {
                        showAlert("정보", "선택한 폴더에서 음악 파일을 찾을 수 없습니다.", Alert.AlertType.INFORMATION);
                    } else {
                        String message = String.format("총 %d개의 음악 파일을 찾았습니다.\n플레이리스트에 추가하시겠습니까?", 
                                                      musicFiles.size());
                        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmAlert.setTitle("파일 추가 확인");
                        confirmAlert.setHeaderText("음악 파일 발견");
                        confirmAlert.setContentText(message);
                        
                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            addFilesToPlaylist(musicFiles, playlistName);
                        }
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert("오류", "폴더 스캔 중 오류가 발생했습니다: " + getException().getMessage(), 
                            Alert.AlertType.ERROR);
                });
            }
        };
        
        // 진행 상황 다이얼로그 표시
        progressAlert.showAndWait().ifPresent(result -> {
            if (result == cancelButton) {
                scanTask.cancel();
            }
        });
        
        // 백그라운드에서 실행
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }
    
    private void scanDirectoryRecursive(File directory, List<File> musicFiles) throws IOException {
        if (!directory.isDirectory()) return;
        
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 숨김 폴더와 시스템 폴더 제외
                if (!file.getName().startsWith(".") && 
                    !file.getName().equalsIgnoreCase("System Volume Information")) {
                    scanDirectoryRecursive(file, musicFiles);
                }
            } else if (file.isFile() && isSupportedMusicFile(file)) {
                musicFiles.add(file);
            }
        }
    }
    
    private boolean isSupportedMusicFile(File file) {
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(ext -> fileName.endsWith("." + ext));
    }
    
    private void addFilesToPlaylist(List<File> files, String playlistName) {
        if (files.isEmpty()) return;
        
        // 진행 상황 표시
        view.showProgress(true);
        view.updateStatusLabel("음악 파일을 추가하는 중... (0/" + files.size() + ")", false);
        
        // 백그라운드에서 처리
        CompletableFuture.runAsync(() -> {
            int addedCount = 0;
            
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                try {
                    MusicInfo musicInfo = createMusicInfoFromFile(file);
                    
                    Platform.runLater(() -> {
                        view.addMusicToCurrentPlaylist(musicInfo);
                        publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, musicInfo));
                    });
                    
                    addedCount++;
                    
                    // 진행 상황 업데이트
                    final int currentIndex = i + 1;
                    final int finalAddedCount = addedCount;
                    Platform.runLater(() -> {
                        view.updateStatusLabel(String.format("음악 파일을 추가하는 중... (%d/%d)", 
                                                            currentIndex, files.size()), false);
                    });
                    
                } catch (Exception e) {
                    log.warn("음악 파일 처리 중 오류: {} - {}", file.getName(), e.getMessage());
                }
            }
            
            // 완료 처리
            final int finalTotalAdded = addedCount;
            Platform.runLater(() -> {
                view.showProgress(false);
                view.updateStatusLabel(String.format("%d개의 음악 파일이 추가되었습니다.", finalTotalAdded), false);
                
                if (finalTotalAdded < files.size()) {
                    int failedCount = files.size() - finalTotalAdded;
                    showAlert("경고", String.format("%d개 파일이 추가되지 못했습니다.\n" +
                                                   "지원되지 않는 형식이거나 파일이 손상되었을 수 있습니다.", 
                                                 failedCount), Alert.AlertType.WARNING);
                }
            });
        });
    }
    
    private MusicInfo createMusicInfoFromFile(File file) {
        try {
            // 파일명에서 기본 정보 추출
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // 파일명 파싱 시도 (예: "Artist - Title.mp3")
            String title = baseName;
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            
            if (baseName.contains(" - ")) {
                String[] parts = baseName.split(" - ", 2);
                if (parts.length == 2) {
                    artist = parts[0].trim();
                    title = parts[1].trim();
                }
            }
            
            // 파일 크기 기반 대략적인 재생 시간 계산 (MP3 기준)
            long fileSize = file.length();
            long estimatedDuration = estimateDuration(fileSize, fileName);
            
            // LRC 파일 찾기
            String lrcPath = findLrcFile(file);
            
            return new MusicInfo(title, artist, album, file.getAbsolutePath(), estimatedDuration, lrcPath);
            
        } catch (Exception e) {
            log.error("MusicInfo 생성 중 오류: {}", file.getName(), e);
            // 기본값으로 반환
            return new MusicInfo(file.getName(), "Unknown Artist", "Unknown Album", 
                               file.getAbsolutePath(), 180000L, null);
        }
    }
    
    private long estimateDuration(long fileSize, String fileName) {
        // 간단한 추정 공식 (실제로는 메타데이터를 읽어야 정확함)
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".mp3")) {
            // MP3: 대략 1MB당 1분으로 추정
            return (fileSize / (1024 * 1024)) * 60 * 1000;
        } else if (ext.endsWith(".wav")) {
            // WAV: 무압축이므로 더 짧게 추정
            return (fileSize / (10 * 1024 * 1024)) * 60 * 1000;
        } else {
            // 기본값: 3분
            return 180000L;
        }
    }
    
    private String findLrcFile(File musicFile) {
        try {
            String baseName = musicFile.getName().substring(0, musicFile.getName().lastIndexOf('.'));
            
            // 같은 디렉토리에서 찾기
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
            // lyrics 폴더에서 찾기
            File lyricsDir = new File(musicFile.getParent(), "lyrics");
            if (lyricsDir.exists()) {
                File lrcInLyricsDir = new File(lyricsDir, baseName + ".lrc");
                if (lrcInLyricsDir.exists()) {
                    return lrcInLyricsDir.getAbsolutePath();
                }
            }
            
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 실패: {}", musicFile.getName());
        }
        
        return null;
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
        confirmAlert.setContentText(String.format("플레이리스트 '%s'의 모든 곡(%d개)을 제거하시겠습니까?\n" +
                                                 "이 작업은 되돌릴 수 없습니다.", 
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
    
    private void importPlaylist() {
        // M3U 플레이리스트 파일 가져오기
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("플레이리스트 가져오기");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("M3U 플레이리스트", "*.m3u", "*.m3u8"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        File playlistFile = fileChooser.showOpenDialog(view.getScene().getWindow());
        
        if (playlistFile != null) {
            importM3UPlaylist(playlistFile);
        }
    }
    
    private void importM3UPlaylist(File playlistFile) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(playlistFile.getAbsolutePath()));
            List<File> musicFiles = new ArrayList<>();
            
            String playlistName = playlistFile.getName().replaceFirst("\\.[^.]+$", "");
            
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    File musicFile = new File(line);
                    if (musicFile.exists() && isSupportedMusicFile(musicFile)) {
                        musicFiles.add(musicFile);
                    } else {
                        // 상대 경로인 경우 플레이리스트 파일 기준으로 시도
                        File relativeMusicFile = new File(playlistFile.getParent(), line);
                        if (relativeMusicFile.exists() && isSupportedMusicFile(relativeMusicFile)) {
                            musicFiles.add(relativeMusicFile);
                        }
                    }
                }
            }
            
            if (musicFiles.isEmpty()) {
                showAlert("경고", "플레이리스트에서 유효한 음악 파일을 찾을 수 없습니다.", Alert.AlertType.WARNING);
                return;
            }
            
            // 플레이리스트 생성 확인
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("플레이리스트 가져오기");
            confirmAlert.setHeaderText("플레이리스트를 가져오시겠습니까?");
            confirmAlert.setContentText(String.format("플레이리스트: %s\n곡 수: %d개", playlistName, musicFiles.size()));
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 플레이리스트 생성
                Playlist playlist = new Playlist(playlistName);
                publisher.publish(new PlaylistEvent.PlaylistCreatedEvent(playlist));
                
                Platform.runLater(() -> {
                    view.addPlaylist(playlistName);
                    view.selectPlaylist(playlistName);
                });
                
                // 곡들 추가
                addFilesToPlaylist(musicFiles, playlistName);
            }
            
        } catch (Exception e) {
            log.error("플레이리스트 가져오기 실패: {}", playlistFile.getName(), e);
            showAlert("오류", "플레이리스트 가져오기 중 오류가 발생했습니다: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
        }
    }
    
    private void exportPlaylist() {
        String selectedPlaylist = view.getSelectedPlaylist();
        if (selectedPlaylist == null) {
            showAlert("오류", "내보낼 플레이리스트를 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        List<MusicInfo> musicList = view.getAllMusicInCurrentPlaylist();
        if (musicList.isEmpty()) {
            showAlert("정보", "플레이리스트가 비어있습니다.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // 저장 위치 선택
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("플레이리스트 내보내기");
        fileChooser.setInitialFileName(selectedPlaylist + ".m3u");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("M3U 플레이리스트", "*.m3u")
        );
        
        File saveFile = fileChooser.showSaveDialog(view.getScene().getWindow());
        
        if (saveFile != null) {
            exportM3UPlaylist(saveFile, selectedPlaylist, musicList);
        }
    }
    
    private void exportM3UPlaylist(File saveFile, String playlistName, List<MusicInfo> musicList) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("#EXTM3U");
            lines.add("#PLAYLIST:" + playlistName);
            
            for (MusicInfo music : musicList) {
                long durationSeconds = music.getDurationMillis() / 1000;
                lines.add(String.format("#EXTINF:%d,%s - %s", durationSeconds, music.getArtist(), music.getTitle()));
                lines.add(music.getFilePath());
            }
            
            Files.write(saveFile.toPath(), lines);
            
            showAlert("성공", String.format("플레이리스트가 성공적으로 내보내졌습니다.\n" +
                                          "파일: %s\n곡 수: %d개", 
                                          saveFile.getName(), musicList.size()), 
                    Alert.AlertType.INFORMATION);
            
        } catch (Exception e) {
            log.error("플레이리스트 내보내기 실패: {}", saveFile.getName(), e);
            showAlert("오류", "플레이리스트 내보내기 중 오류가 발생했습니다: " + e.getMessage(), 
                    Alert.AlertType.ERROR);
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
}