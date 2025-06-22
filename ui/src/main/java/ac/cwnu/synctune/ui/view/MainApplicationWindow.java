package ac.cwnu.synctune.ui.view;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.controller.PlaybackController;
import ac.cwnu.synctune.ui.controller.PlaylistActionHandler;
import ac.cwnu.synctune.ui.controller.WindowStateManager;
import ac.cwnu.synctune.ui.util.MusicInfoHelper;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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

    public MainApplicationWindow(EventPublisher publisher) {
        this.eventPublisher = publisher;
        setTitle("SyncTune Player");
        setWidth(1000);
        setHeight(700);
        setMinWidth(800);
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
        
        MenuItem newPlaylistMenuItem = new MenuItem("새 플레이리스트...");
        newPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newPlaylistMenuItem.setOnAction(e -> createNewPlaylist());
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        MenuItem exitMenuItem = new MenuItem("종료");
        exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitMenuItem.setOnAction(e -> requestClose());
        
        fileMenu.getItems().addAll(
            openFileMenuItem, openFolderMenuItem, separator,
            newPlaylistMenuItem, separator,
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

    // 메뉴 액션 구현들
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
            
            // 첫 번째 파일 재생
            File firstFile = validMusicFiles.get(0);
            playMusicFile(firstFile);
            
            // 나머지 파일들을 현재 플레이리스트에 추가
            for (int i = 1; i < validMusicFiles.size(); i++) {
                addFileToCurrentPlaylist(validMusicFiles.get(i));
            }
            
            if (validMusicFiles.size() > 1) {
                UIUtils.showInfo("완료", String.format("총 %d개의 음악 파일이 처리되었습니다.", validMusicFiles.size()));
            }
            
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
            // 폴더 내 음악 파일 검색
            List<File> musicFiles = findMusicFilesInDirectory(directory);
            
            if (musicFiles.isEmpty()) {
                UIUtils.showInfo("알림", "선택한 폴더에서 지원되는 음악 파일을 찾을 수 없습니다.");
                return;
            }
            
            // 확인 다이얼로그
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("폴더 로드");
            confirmAlert.setHeaderText("음악 파일 로드");
            confirmAlert.setContentText(String.format("%d개의 음악 파일을 발견했습니다.\n모두 플레이리스트에 추가하시겠습니까?", musicFiles.size()));
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 첫 번째 파일 재생하고 나머지는 플레이리스트에 추가
                if (!musicFiles.isEmpty()) {
                    playMusicFile(musicFiles.get(0));
                    
                    for (int i = 1; i < musicFiles.size(); i++) {
                        addFileToCurrentPlaylist(musicFiles.get(i));
                    }
                }
                
                UIUtils.showInfo("완료", String.format("%d개의 음악 파일이 로드되었습니다.", musicFiles.size()));
            }
            
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
            } else if (file.isDirectory() && recursive) {
                musicFiles.addAll(findMusicFilesRecursive(file, true));
            }
        }
        
        // 파일명으로 정렬
        musicFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        
        return musicFiles;
    }
    
    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("새 플레이리스트");
        dialog.setHeaderText("새 플레이리스트를 만듭니다");
        dialog.setContentText("플레이리스트 이름:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                playlistView.addPlaylist(name.trim());
            }
        });
    }
    
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
            Ctrl+N - 새 플레이리스트
            Ctrl+Q - 종료
            
            기타:
            F1 - 이 도움말
            Ctrl+Enter - 선택한 곡 재생
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
            • 플레이리스트 관리
            • 모듈형 아키텍처
            • 이벤트 기반 모듈 간 통신
            
            개발: OOP 1팀
            팀원: 동헌희, 김민재, 김대영, 임민수
            """;
        
        dialog.setContentText(about);
        dialog.showAndWait();
    }
    
    // 헬퍼 메서드들
    private void playMusicFile(File musicFile) {
        try {
            log.info("음악 파일 재생 시도: {}", musicFile.getName());
            
            // MusicInfoHelper를 사용하여 정확한 메타데이터와 재생시간 계산
            MusicInfo musicInfo = MusicInfoHelper.createFromFile(musicFile);
            
            log.info("음악 파일 재생 요청: {} - {} ({}ms)", 
                musicInfo.getArtist(), musicInfo.getTitle(), musicInfo.getDurationMillis());
            
            eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(musicInfo));
            
        } catch (Exception e) {
            UIUtils.showError("오류", "음악 파일을 재생할 수 없습니다: " + e.getMessage());
            log.error("음악 파일 재생 실패: {}", musicFile.getAbsolutePath(), e);
        }
    }
    
    private void addFileToCurrentPlaylist(File musicFile) {
        try {
            MusicInfo musicInfo = MusicInfoHelper.createFromFile(musicFile);
            playlistActionHandler.addMusicToCurrentPlaylist(musicInfo);
            log.debug("플레이리스트에 파일 추가: {}", musicInfo.getTitle());
        } catch (Exception e) {
            log.error("플레이리스트에 파일 추가 실패: {}", musicFile.getAbsolutePath(), e);
        }
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

    // UI 업데이트 메서드들
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

    public void updateLyrics(String lyrics, long timestamp) {
        if (lyricsView != null) {
            if (lyrics == null || lyrics.trim().isEmpty() || lyrics.equals("가사를 찾을 수 없습니다")) {
                lyricsView.showLyricsNotFound();
            } else {
                int idx = findCurrentLyricIndex(lyrics, timestamp);
                lyricsView.updateLyrics(lyrics, idx);
            }
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

    /**
     * UIModule.stop()에서 호출되는 강제 종료 메서드
     */
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
        // lyricsView.getFullLyricsLines() 같은 전체 가사 배열을 가지고 있어야 함
        String[] allLines = lyricsView.getFullLyricsLines(); // 만약 없으면 setFullLyrics에서 배열을 저장해둬야 함
        if (allLines == null) return -1;

        // 완벽하게 일치하는 첫 번째 라인 인덱스 반환 (동일 가사 여러줄이면 가장 빠른 거)
        for (int i = 0; i < allLines.length; i++) {
            if (allLines[i].equals(lyric)) {
                return i;
            }
        }
        return -1;
    }

    // index로 넘기는 updateLyrics
    public void updateLyrics(String lyric, int index) {
        if (lyricsView != null) {
            lyricsView.updateLyrics(lyric, index);
        }
    }


    public void setFullLyrics(String[] lines) {
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
    
    // PlaybackController getter
    public PlaybackController getPlaybackController() {
        return playbackController;
    }
    public PlayerControlsView getControlsView() {
        return controlsView;
    }
    public PlaylistView getPlaylistView() {
        return playlistView;
    }
}
