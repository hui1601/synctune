package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.util.MusicInfoHelper;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainApplicationWindow extends Stage {
    private static final Logger log = LoggerFactory.getLogger(MainApplicationWindow.class);
    private final EventPublisher eventPublisher;
    
    // 뷰 컴포넌트들
    private PlayerControlsView controlsView;
    private PlaylistView playlistView;
    private LyricsView lyricsView;
    
    // 컨트롤러들
    private PlaybackController playbackController;
    private PlaylistActionHandler playlistActionHandler;
    private WindowStateManager windowStateManager;

    private int lastIndex = -1;

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(1200);
        setHeight(700);
        setMinWidth(1000);
        setMinHeight(600);
        
        initUI();
        initControllers();
        setupGlobalKeyboardShortcuts();
    }

    private void initUI() {
        BorderPane root = new BorderPane();
        
        // 메뉴바 생성
        MenuBar menuBar = createMenuBar();
        
        // 뷰 컴포넌트 생성
        controlsView = new PlayerControlsView();
        playlistView = new PlaylistView(eventPublisher);
        lyricsView = new LyricsView();
        
        // 레이아웃 구성
        VBox topContainer = new VBox();
        topContainer.getChildren().add(menuBar);
        
        root.setTop(topContainer);
        root.setBottom(controlsView);
        root.setLeft(playlistView);
        root.setCenter(lyricsView);
        
        // 여백 설정
        BorderPane.setMargin(controlsView, new Insets(10));
        BorderPane.setMargin(playlistView, new Insets(10));
        BorderPane.setMargin(lyricsView, new Insets(10));
        
        Scene scene = new Scene(root);
        setScene(scene);
        
        // 드래그 앤 드롭 설정 (Scene이 생성된 후)
        Platform.runLater(this::setupDragAndDrop);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
        Menu fileMenu = createFileMenu();
        
        // 재생 메뉴
        Menu playMenu = createPlayMenu();
        
        // 도움말 메뉴
        Menu helpMenu = createHelpMenu();
        
        menuBar.getMenus().addAll(fileMenu, playMenu, helpMenu);
        return menuBar;
    }
    
    private Menu createFileMenu() {
        Menu fileMenu = new Menu("파일");
        
        MenuItem openFileMenuItem = new MenuItem("파일 열기...");
        openFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openFileMenuItem.setOnAction(e -> openMusicFiles());
        
        MenuItem openFolderMenuItem = new MenuItem("폴더 열기...");
        openFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, 
            KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openFolderMenuItem.setOnAction(e -> openMusicFolder());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem exitMenuItem = new MenuItem("종료");
        exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitMenuItem.setOnAction(e -> requestClose());
        
        fileMenu.getItems().addAll(
            openFileMenuItem, openFolderMenuItem, separator,
            exitMenuItem
        );
        
        return fileMenu;
    }
    
    private Menu createPlayMenu() {
        Menu playMenu = new Menu("재생");
        
        MenuItem playMenuItem = new MenuItem("재생/일시정지");
        playMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
        playMenuItem.setOnAction(e -> togglePlayPause());
        
        MenuItem stopMenuItem = new MenuItem("정지");
        stopMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        stopMenuItem.setOnAction(e -> stopPlayback());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem previousMenuItem = new MenuItem("이전 곡");
        previousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN));
        previousMenuItem.setOnAction(e -> playPrevious());
        
        MenuItem nextMenuItem = new MenuItem("다음 곡");
        nextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN));
        nextMenuItem.setOnAction(e -> playNext());
        
        playMenu.getItems().addAll(
            playMenuItem, stopMenuItem, separator,
            previousMenuItem, nextMenuItem
        );
        
        return playMenu;
    }
    
    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("도움말");
        
        MenuItem keyboardShortcutsMenuItem = new MenuItem("키보드 단축키...");
        keyboardShortcutsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        keyboardShortcutsMenuItem.setOnAction(e -> showKeyboardShortcuts());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem aboutMenuItem = new MenuItem("SyncTune 정보...");
        aboutMenuItem.setOnAction(e -> showAbout());
        
        helpMenu.getItems().addAll(
            keyboardShortcutsMenuItem, separator,
            aboutMenuItem
        );
        
        return helpMenu;
    }

    private void initControllers() {
        playbackController = new PlaybackController(controlsView, eventPublisher);
        playlistActionHandler = new PlaylistActionHandler(playlistView, eventPublisher);
        windowStateManager = new WindowStateManager(this, eventPublisher);
        
        log.debug("컨트롤러들 초기화 완료");
    }
    
    private void setupGlobalKeyboardShortcuts() {
        getScene().setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case SPACE:
                        togglePlayPause();
                        event.consume();
                        break;
                    case ENTER:
                        playSelectedMusic();
                        event.consume();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    // ========== 드래그 앤 드롭 지원 ==========
    
    private void setupDragAndDrop() {
        getScene().setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        getScene().setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            boolean success = false;
            
            if (dragboard.hasFiles()) {
                List<File> files = dragboard.getFiles();
                
                // 폴더와 파일 분리
                List<File> musicFiles = new java.util.ArrayList<>();
                List<File> directories = new java.util.ArrayList<>();
                
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories.add(file);
                    } else if (MusicInfoHelper.isSupportedAudioFile(file)) {
                        musicFiles.add(file);
                    }
                }
                
                // 폴더가 있으면 폴더 내 음악 파일 검색
                for (File dir : directories) {
                    musicFiles.addAll(findMusicFilesInDirectory(dir));
                }
                
                if (!musicFiles.isEmpty()) {
                    processSelectedMusicFiles(musicFiles);
                    success = true;
                } else {
                    UIUtils.showError("오류", "지원되는 음악 파일이 없습니다.");
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
        
        log.info("드래그 앤 드롭 기능이 활성화되었습니다.");
    }

    // ========== 메뉴 액션 구현들 - 개선된 버전 ==========
    
    private void openMusicFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("음악 파일 열기");
        
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("음악 파일", "*.mp3", "*.wav", "*.flac", "*.m4a", "*.aac", "*.ogg"),
            new FileChooser.ExtensionFilter("MP3 파일", "*.mp3"),
            new FileChooser.ExtensionFilter("WAV 파일", "*.wav"),
            new FileChooser.ExtensionFilter("FLAC 파일", "*.flac"),
            new FileChooser.ExtensionFilter("모든 파일", "*.*")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(this);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            processSelectedMusicFiles(selectedFiles);
        }
    }
    
    private void processSelectedMusicFiles(List<File> files) {
        try {
            // 지원되는 음악 파일만 필터링
            List<File> validMusicFiles = files.stream()
                .filter(MusicInfoHelper::isSupportedAudioFile)
                .toList();
            
            if (validMusicFiles.isEmpty()) {
                UIUtils.showError("오류", "지원되는 음악 파일이 없습니다.\n지원 형식: MP3, WAV, FLAC, M4A, AAC, OGG");
                return;
            }
            
            if (validMusicFiles.size() < files.size()) {
                int unsupportedCount = files.size() - validMusicFiles.size();
                UIUtils.showInfo("알림", String.format("%d개의 지원되지 않는 파일이 제외되었습니다.", unsupportedCount));
            }
            
            // 상태 표시
            playlistView.updateStatusLabel("파일을 처리하는 중...", false);
            
            // 백그라운드에서 파일 처리
            Thread processThread = new Thread(() -> {
                try {
                    // 모든 파일을 MusicInfo로 변환
                    List<MusicInfo> musicInfoList = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < validMusicFiles.size(); i++) {
                        File file = validMusicFiles.get(i);
                        final int currentIndex = i + 1;
                        final int totalFiles = validMusicFiles.size();
                        
                        try {
                            MusicInfo musicInfo = MusicInfoHelper.createFromFile(file);
                            musicInfoList.add(musicInfo);
                            log.debug("파일 처리 완료: {} - {}", musicInfo.getArtist(), musicInfo.getTitle());
                            
                            // 진행 상황 업데이트
                            Platform.runLater(() -> {
                                String status = String.format("처리 중... (%d/%d) %s", 
                                    currentIndex, totalFiles, file.getName());
                                playlistView.updateStatusLabel(status, false);
                            });
                            
                        } catch (Exception e) {
                            log.error("파일 처리 실패: {}", file.getName(), e);
                            Platform.runLater(() -> {
                                UIUtils.showError("경고", "파일 처리 실패: " + file.getName() + "\n" + e.getMessage());
                            });
                        }
                    }
                    
                    if (musicInfoList.isEmpty()) {
                        Platform.runLater(() -> {
                            UIUtils.showError("오류", "처리할 수 있는 음악 파일이 없습니다.");
                            playlistView.updateStatusLabel("파일 처리 실패", true);
                        });
                        return;
                    }
                    
                    Platform.runLater(() -> {
                        // 1. 모든 파일을 재생목록에 추가
                        log.info("재생목록에 {}개 파일 추가 시작", musicInfoList.size());
                        for (MusicInfo musicInfo : musicInfoList) {
                            playlistActionHandler.addMusicToCurrentPlaylist(musicInfo);
                        }
                        
                        // 2. 첫 번째 파일 재생
                        MusicInfo firstMusic = musicInfoList.get(0);
                        log.info("첫 번째 파일 재생 요청: {} - {}", firstMusic.getArtist(), firstMusic.getTitle());
                        eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(firstMusic));
                        
                        // 3. 완료 메시지
                        String message = musicInfoList.size() == 1 ? 
                            "음악 파일이 재생목록에 추가되고 재생이 시작되었습니다." :
                            String.format("%d개의 음악 파일이 재생목록에 추가되었습니다.", musicInfoList.size());
                        
                        playlistView.updateStatusLabel("파일 추가 완료", false);
                        UIUtils.showInfo("완료", message);
                    });
                    
                } catch (Exception e) {
                    log.error("파일 처리 중 전체 오류", e);
                    Platform.runLater(() -> {
                        UIUtils.showError("오류", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
                        playlistView.updateStatusLabel("파일 처리 오류", true);
                    });
                }
            });
            
            processThread.setName("MusicFileProcessor");
            processThread.setDaemon(true);
            processThread.start();
            
        } catch (Exception e) {
            log.error("음악 파일 처리 중 오류", e);
            UIUtils.showError("오류", "음악 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private void openMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 폴더 선택");
        
        File selectedDirectory = directoryChooser.showDialog(this);
        if (selectedDirectory != null) {
            processMusicFolder(selectedDirectory);
        }
    }
    
    private void processMusicFolder(File directory) {
        try {
            // 상태 표시
            playlistView.updateStatusLabel("폴더를 스캔하는 중...", false);
            
            // 백그라운드에서 폴더 스캔
            Thread scanThread = new Thread(() -> {
                try {
                    // 폴더 내 음악 파일 검색
                    List<File> musicFiles = findMusicFilesInDirectory(directory);
                    
                    Platform.runLater(() -> {
                        if (musicFiles.isEmpty()) {
                            UIUtils.showInfo("알림", "선택한 폴더에서 지원되는 음악 파일을 찾을 수 없습니다.");
                            playlistView.updateStatusLabel("음악 파일 없음", true);
                            return;
                        }
                        
                        // 확인 다이얼로그
                        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmAlert.setTitle("폴더 로드");
                        confirmAlert.setHeaderText("음악 파일 로드");
                        confirmAlert.setContentText(String.format("%d개의 음악 파일을 발견했습니다.\n모두 재생목록에 추가하시겠습니까?", musicFiles.size()));
                        
                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            processSelectedMusicFiles(musicFiles);
                        } else {
                            playlistView.updateStatusLabel("폴더 로드 취소됨", false);
                        }
                    });
                    
                } catch (Exception e) {
                    log.error("폴더 스캔 중 오류", e);
                    Platform.runLater(() -> {
                        UIUtils.showError("오류", "폴더 스캔 중 오류가 발생했습니다: " + e.getMessage());
                        playlistView.updateStatusLabel("폴더 스캔 오류", true);
                    });
                }
            });
            
            scanThread.setName("FolderScanner");
            scanThread.setDaemon(true);
            scanThread.start();
            
        } catch (Exception e) {
            log.error("폴더 처리 중 오류", e);
            UIUtils.showError("오류", "폴더 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private List<File> findMusicFilesInDirectory(File directory) {
        return findMusicFilesRecursive(directory, false); // 하위 폴더 검색 안함
    }
    
    private List<File> findMusicFilesRecursive(File directory, boolean recursive) {
        List<File> musicFiles = new java.util.ArrayList<>();
        
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
                musicFiles.addAll(findMusicFilesRecursive(file, true));
            }
        }
        
        // 파일명으로 정렬
        musicFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        
        return musicFiles;
    }
    
    // ========== 기타 메뉴 액션들 ==========
    
    private void requestClose() {
        if (windowStateManager != null) {
            windowStateManager.handleCloseRequest(null);
        } else {
            close();
        }
    }
    
    private void togglePlayPause() {
        if (playbackController != null) {
            if (playbackController.isPlaybackActive()) {
                eventPublisher.publish(new MediaControlEvent.RequestPauseEvent());
            } else {
                eventPublisher.publish(new MediaControlEvent.RequestPlayEvent());
            }
        }
    }
    
    private void stopPlayback() {
        eventPublisher.publish(new MediaControlEvent.RequestStopEvent());
    }
    
    private void playPrevious() {
        eventPublisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
    }
    
    private void playNext() {
        eventPublisher.publish(new MediaControlEvent.RequestNextMusicEvent());
    }
    
    private void showKeyboardShortcuts() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("키보드 단축키");
        dialog.setHeaderText("SyncTune 키보드 단축키");
        
        String shortcuts = """
            재생/제어:
            Space - 재생/일시정지
            Ctrl+S - 정지
            Ctrl+← - 이전 곡
            Ctrl+→ - 다음 곡
            
            파일:
            Ctrl+O - 파일 열기
            Ctrl+Shift+O - 폴더 열기
            Ctrl+Q - 종료
            
            기타:
            F1 - 이 도움말
            Ctrl+Enter - 선택한 곡 재생
            
            팁:
            • 음악 파일이나 폴더를 창에 드래그해서 추가할 수 있습니다
            • 볼륨 슬라이더에서 마우스 휠로 볼륨 조절이 가능합니다
            """;
        
        dialog.setContentText(shortcuts);
        dialog.showAndWait();
    }
    
    private void showAbout() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("SyncTune 정보");
        dialog.setHeaderText("SyncTune Music Player");
        
        String about = """
            SyncTune v1.0.0
            
            간단하고 효율적인 음악 플레이어
            
            주요 기능:
            • 다양한 음악 형식 지원 (MP3, WAV, FLAC, M4A, AAC, OGG)
            • 실제 재생 시간 자동 계산
            • LRC 가사 파일 지원 및 실시간 동기화
            • 간편한 재생목록 관리
            • 드래그 앤 드롭 지원
            • 실시간 볼륨 조절
            • 모듈형 아키텍처
            • 이벤트 기반 모듈 간 통신
            
            개발: OOP 1팀
            팀원: 동헌희, 김민재, 김대영, 임민수
            """;
        
        dialog.setContentText(about);
        dialog.showAndWait();
    }
    
    private void playSelectedMusic() {
        MusicInfo selectedMusic = playlistView.getSelectedMusic();
        if (selectedMusic != null) {
            log.debug("메인 윈도우에서 곡 재생 요청: {}", selectedMusic.getTitle());
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(selectedMusic));
        } else {
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent());
        }
    }

    // ========== UI 업데이트 메서드들 ==========
    
    public void updateCurrentMusic(MusicInfo music) {
        if (music != null) {
            controlsView.updateMusicInfo(music);
            setTitle("SyncTune - " + music.getTitle() + " - " + music.getArtist());
            lyricsView.showLyricsLoading();
        }
    }

    public void updateProgress(long currentMs, long totalMs) {
        if (controlsView != null) {
            controlsView.updateProgress(currentMs, totalMs);
        }
    }
    
    public void showLyricsFound(String lrcPath) {
        if (lyricsView != null) {
            lyricsView.showLyricsLoading();
        }
    }

    public void showLyricsNotFound() {
        if (lyricsView != null) {
            lyricsView.showLyricsNotFound();
        }
    }

    public void forceClose() {
        try {
            setOnCloseRequest(null);
            windowStateManager = null;
            super.close();
        } catch (Exception e) {
            System.err.println("윈도우 강제 종료 중 오류: " + e.getMessage());
        }
    }

    public int findCurrentLyricIndex(String lyric, long timestamp) {
        List<LrcLine> allLyricsLines = lyricsView.getFullLyricsLines();
        if (allLyricsLines == null) return -1;

        String target = lyric.trim().toLowerCase();

        // 탐색 범위: lastIndex ~ lastIndex+5
        int start = lastIndex >= 0 ? lastIndex : 0;
        int end = Math.min(allLyricsLines.size(), start + 5);

        // 1. 근처 탐색
        for (int i = start; i < end; i++) {
            LrcLine line = allLyricsLines.get(i);
            String lineText = line.getText().trim().toLowerCase();

            if (lineText.equals(target) && Math.abs(line.getTimeMillis() - timestamp) <= 1000) {
                if (i >= lastIndex) { // 인덱스가 뒤로 가는 경우만 허용
                    lastIndex = i;
                    return i;
                }
            }
        }

        // 2. 전체 탐색 (이전 탐색 범위 제외)
        for (int i = 0; i < allLyricsLines.size(); i++) {
            if (i >= start && i < end) continue;

            LrcLine line = allLyricsLines.get(i);
            String lineText = line.getText().trim().toLowerCase();

            if (lineText.equals(target) && Math.abs(line.getTimeMillis() - timestamp) <= 1000) {
                if (i >= lastIndex) {
                    lastIndex = i;
                    return i;
                }
            }
        }

        return -1;
    }

    public void updateLyrics(String lyric, long timestamp) {
        if (lyricsView != null) {
            if (lyric == null || lyric.trim().isEmpty() || lyric.equals("가사를 찾을 수 없습니다")) {
                lyricsView.showLyricsNotFound();
            } else {
                int idx = findCurrentLyricIndex(lyric, timestamp);
                lyricsView.updateLyrics(lyric, idx);
            }
        }
    }

    public void setFullLyrics(List<LrcLine> lines) {
        if (lyricsView != null) {
            lyricsView.setFullLyrics(lines);
        }
    }

    @Override
    public void close() {
        if (windowStateManager != null && !windowStateManager.isCloseRequested()) {
            windowStateManager.handleCloseRequest(null);
        } else {
            super.close();
        }
    }
    
    // Getter 메서드들
    public PlaybackController getPlaybackController() {
        return playbackController;
    }
    
    public PlayerControlsView getControlsView() {
        return controlsView;
    }
    
    public PlaylistView getPlaylistView() {
        return playlistView;
    }
    
    public LyricsView getLyricsView() {
        return lyricsView;
    }
    
    public PlaylistActionHandler getPlaylistActionHandler() {
        return playlistActionHandler;
    }
}