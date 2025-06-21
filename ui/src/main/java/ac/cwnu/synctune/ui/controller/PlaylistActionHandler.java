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
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
        FileChooser.ExtensionFilter musicFilter = new FileChooser.ExtensionFilter(
            "음악 파일", 
            SUPPORTED_EXTENSIONS.stream().map(ext -> "*." + ext).toArray(String[]::new)
        );
        fileChooser.getExtensionFilters().add(musicFilter);
        fileChooser.getExtensionFilters().add(
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
        
        // 백그라운드에서 처리
        Platform.runLater(() -> {
            int addedCount = 0;
            
            for (File file : files) {
                try {
                    if (isSupportedMusicFile(file)) {
                        MusicInfo musicInfo = createMusicInfoFromFile(file);
                        view.addMusicToCurrentPlaylist(musicInfo);
                        publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, musicInfo));
                        addedCount++;
                    }
                } catch (Exception e) {
                    log.warn("음악 파일 처리 중 오류: {} - {}", file.getName(), e.getMessage());
                }
            }
            
            view.updateStatusLabel(String.format("%d개의 음악 파일이 추가되었습니다.", addedCount), false);
            
            if (addedCount < files.size()) {
                int failedCount = files.size() - addedCount;
                showAlert("경고", String.format("%d개 파일이 추가되지 못했습니다.", failedCount), 
                         Alert.AlertType.WARNING);
            }
        });
    }
    
    private boolean isSupportedMusicFile(File file) {
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(ext -> fileName.endsWith("." + ext));
    }
    
    private MusicInfo createMusicInfoFromFile(File file) {
        try {
            // 파일명에서 기본 정보 추출
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // 파일명 파싱 시도
            String title = baseName;
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            
            if (baseName.contains(" - ")) {
                String[] parts = baseName.split(" - ", 2);
                if (parts.length == 2) {
                    artist = parts[0].trim();
                    title = parts[1].trim();
                }
            } else if (baseName.matches("^\\d+\\.\\s*.*")) {
                String withoutTrackNumber = baseName.replaceFirst("^\\d+\\.\\s*", "");
                if (withoutTrackNumber.contains(" - ")) {
                    String[] parts = withoutTrackNumber.split(" - ", 2);
                    if (parts.length == 2) {
                        artist = parts[0].trim();
                        title = parts[1].trim();
                    }
                } else {
                    title = withoutTrackNumber;
                }
            }
            
            // 파일 크기 기반 대략적인 재생 시간 계산
            long estimatedDuration = estimateDuration(file.length(), fileName);
            
            // LRC 파일 찾기
            String lrcPath = findLrcFile(file);
            
            log.debug("MusicInfo 생성: {} - {} ({})", artist, title, UIUtils.formatTime(estimatedDuration));
            
            return new MusicInfo(title, artist, album, file.getAbsolutePath(), estimatedDuration, lrcPath);
            
        } catch (Exception e) {
            log.error("MusicInfo 생성 중 오류: {}", file.getName(), e);
            return new MusicInfo(file.getName(), "Unknown Artist", "Unknown Album", 
                               file.getAbsolutePath(), 180000L, null);
        }
    }
    
    private long estimateDuration(long fileSize, String fileName) {
        String ext = fileName.toLowerCase();
        long duration;
        
        if (ext.endsWith(".mp3")) {
            duration = (fileSize * 1000) / (16 * 1024);
        } else if (ext.endsWith(".wav")) {
            duration = (fileSize * 1000) / (176 * 1024);
        } else if (ext.endsWith(".flac")) {
            duration = (fileSize * 1000) / (100 * 1024);
        } else {
            duration = 180000L; // 기본 3분
        }
        
        return Math.max(10000L, Math.min(duration, 7200000L));
    }
    
    private String findLrcFile(File musicFile) {
        try {
            String baseName = getFileNameWithoutExtension(musicFile.getName());
            
            // 같은 디렉토리에서 찾기
            File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
            if (lrcFile.exists()) {
                return lrcFile.getAbsolutePath();
            }
            
            // lyrics 폴더에서 찾기
            File lyricsDir = new File("lyrics");
            if (lyricsDir.exists()) {
                File lrcInRootLyrics = new File(lyricsDir, baseName + ".lrc");
                if (lrcInRootLyrics.exists()) {
                    return lrcInRootLyrics.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            log.debug("LRC 파일 찾기 실패: {}", musicFile.getName(), e);
        }
        
        return null;
    }

    private String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
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
}