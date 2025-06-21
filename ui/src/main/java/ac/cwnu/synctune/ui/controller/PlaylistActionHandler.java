package ac.cwnu.synctune.ui.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.event.PlaylistEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.model.Playlist;
import ac.cwnu.synctune.ui.view.PlaylistView;
import ac.cwnu.synctune.ui.UIModule;
import ac.cwnu.synctune.ui.scanner.MusicFolderScanner;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
            // UIModule의 MusicFolderScanner 사용
            scanFolderForMusicWithScanner(selectedDirectory, selectedPlaylist);
        }
    }
    private void scanFolderForMusicWithScanner(File directory, String playlistName) {
    UIModule uiModule = UIModule.getInstance();
    if (uiModule == null || uiModule.getMusicFolderScanner() == null) {
        // 폴백: 기존 방식으로 처리
        scanFolderForMusic(directory, playlistName);
        return;
    }

    // 스캔 옵션 선택 다이얼로그
    Alert optionDialog = new Alert(Alert.AlertType.CONFIRMATION);
    optionDialog.setTitle("폴더 스캔 옵션");
    optionDialog.setHeaderText("스캔 방식을 선택하세요");
    optionDialog.setContentText("폴더: " + directory.getName());
    
    ButtonType quickScanButton = new ButtonType("빠른 스캔");
    ButtonType fullScanButton = new ButtonType("전체 스캔");
    ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    optionDialog.getButtonTypes().setAll(quickScanButton, fullScanButton, cancelButton);
    
    Optional<ButtonType> choice = optionDialog.showAndWait();
    if (choice.isEmpty() || choice.get() == cancelButton) {
        return;
    }
    
    // 스캔 옵션 결정
    MusicFolderScanner.ScanOptions options = choice.get() == quickScanButton ? 
        uiModule.getQuickScanOptions() : uiModule.getDefaultScanOptions();

    // 진행 상황 다이얼로그
    Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
    progressAlert.setTitle("폴더 스캔");
    progressAlert.setHeaderText("음악 파일을 검색하고 있습니다...");
    progressAlert.setContentText("폴더: " + directory.getName());
    
    ButtonType cancelScanButton = new ButtonType("취소");
    progressAlert.getButtonTypes().setAll(cancelScanButton);
    
    // 진행률 표시
    ProgressBar progressBar = new ProgressBar();
    progressBar.setPrefWidth(300);
    Label progressLabel = new Label("준비 중...");
    
    VBox progressContent = new VBox(10);
    progressContent.getChildren().addAll(progressLabel, progressBar);
    progressAlert.getDialogPane().setExpandableContent(progressContent);
    progressAlert.getDialogPane().setExpanded(true);
    
    progressAlert.show();
    
    // 스캔 진행 상황 콜백
    MusicFolderScanner.ScanProgressCallback callback = new MusicFolderScanner.ScanProgressCallback() {
        @Override
        public void onProgress(int scannedFiles, int foundMusic, String currentFile) {
            Platform.runLater(() -> {
                progressAlert.setContentText(String.format(
                    "검사 완료: %d개 파일\n발견: %d개 음악 파일\n현재: %s", 
                    scannedFiles, foundMusic, truncateFileName(currentFile, 40)));
                progressLabel.setText(String.format("진행: %d개 파일 검사됨", scannedFiles));
                
                // 대략적인 진행률
                if (scannedFiles > 0) {
                    progressBar.setProgress(Math.min(0.9, scannedFiles / 5000.0));
                }
            });
        }
        
        @Override
        public void onDirectoryEntered(String directoryPath) {
            Platform.runLater(() -> {
                progressAlert.setHeaderText("스캔 중: " + truncateFileName(directoryPath, 50));
                });
            }
        
            @Override
            public void onComplete(MusicFolderScanner.ScanResult result) {
                Platform.runLater(() -> {
                progressAlert.close();
                    handleFolderScanResult(result, directory, playlistName);
                });
            }
        
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert("스캔 오류", "폴더 스캔 중 오류가 발생했습니다: " + error, Alert.AlertType.ERROR);
                });
            }
        };
    
        // Task 생성 및 실행
        Task<MusicFolderScanner.ScanResult> scanTask = uiModule.createCustomMusicScanTask(directory, options, callback);
    
        // 취소 처리
        progressAlert.setOnCloseRequest(e -> {
            scanTask.cancel();
            uiModule.cancelCurrentScan();
        });
    
        progressAlert.showAndWait().ifPresent(result -> {
            if (result == cancelScanButton) {
                scanTask.cancel();
                uiModule.cancelCurrentScan();
            }
        });
    
        // 백그라운드에서 실행
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
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
    private void handleFolderScanResult(MusicFolderScanner.ScanResult result, File scannedFolder, String playlistName) {
    if (result.isCancelled()) {
        view.updateStatusLabel("폴더 스캔이 취소되었습니다.", false);
        return;
    }
    
    if (!result.isSuccess()) {
        String errorMsg = result.getErrorMessage() != null ? 
            result.getErrorMessage() : "알 수 없는 오류가 발생했습니다.";
        showAlert("스캔 실패", "폴더 스캔에 실패했습니다: " + errorMsg, Alert.AlertType.ERROR);
        return;
    }
    
    List<MusicInfo> foundMusic = result.getMusicFiles();
    
    if (foundMusic.isEmpty()) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("스캔 완료");
        infoAlert.setHeaderText("음악 파일을 찾을 수 없습니다");
        infoAlert.setContentText(String.format(
            "폴더: %s\n검사한 파일: %d개\n검사한 폴더: %d개\n소요 시간: %.1f초",
            scannedFolder.getName(),
            result.getTotalFilesScanned(),
            result.getDirectoriesScanned(),
            result.getScanTimeMs() / 1000.0));
        infoAlert.showAndWait();
        return;
    }
    
    // 발견된 음악 파일 확인 다이얼로그
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("음악 파일 발견");
    confirmAlert.setHeaderText("음악 파일을 발견했습니다!");
    
    String content = String.format(
        "폴더: %s\n" +
        "발견된 음악 파일: %d개\n" +
        "검사한 파일: %d개\n" +
        "검사한 폴더: %d개\n" +
        "소요 시간: %.1f초\n\n" +
        "'%s' 플레이리스트에 추가하시겠습니까?",
        scannedFolder.getName(),
        foundMusic.size(),
        result.getTotalFilesScanned(),
        result.getDirectoriesScanned(),
        result.getScanTimeMs() / 1000.0,
        playlistName);
    
    confirmAlert.setContentText(content);
    
    ButtonType addAllButton = new ButtonType("모두 추가");
    ButtonType selectiveButton = new ButtonType("선택적 추가");
    ButtonType previewButton = new ButtonType("미리보기");
    ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    confirmAlert.getButtonTypes().setAll(addAllButton, selectiveButton, previewButton, cancelButton);
    
    Optional<ButtonType> choice = confirmAlert.showAndWait();
    
    if (choice.isPresent()) {
        if (choice.get() == addAllButton) {
            addMusicListToPlaylist(foundMusic, playlistName);
        } else if (choice.get() == selectiveButton) {
            showSelectiveMusicDialog(foundMusic, playlistName);
        } else if (choice.get() == previewButton) {
            showMusicPreviewDialog(foundMusic, playlistName);
        }
    }
}
    private void showSelectiveMusicDialog(List<MusicInfo> musicList, String playlistName) {
    Alert selectDialog = new Alert(Alert.AlertType.CONFIRMATION);
    selectDialog.setTitle("음악 파일 선택");
    selectDialog.setHeaderText("추가할 음악 파일을 선택하세요");
    selectDialog.setContentText("플레이리스트: " + playlistName);
    
    // 음악 파일 리스트뷰
    ListView<MusicInfoSelectItem> musicListView = new ListView<>();
    musicListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    musicListView.setPrefHeight(400);
    musicListView.setPrefWidth(600);
    
    // 커스텀 셀 팩토리 (체크박스 포함)
    musicListView.setCellFactory(lv -> new CheckBoxListCell<MusicInfoSelectItem>() {
        @Override
        public void updateItem(MusicInfoSelectItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getDisplayText());
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(item.isSelected());
                checkBox.setOnAction(e -> item.setSelected(checkBox.isSelected()));
                setGraphic(checkBox);
            }
        }
    });
    
    // MusicInfo를 선택 가능한 아이템으로 변환
    List<MusicInfoSelectItem> selectItems = musicList.stream()
        .map(MusicInfoSelectItem::new)
        .collect(Collectors.toList());
    musicListView.getItems().addAll(selectItems);
    
    // 컨트롤 버튼들
    HBox controlBox = new HBox(10);
    Button selectAllButton = new Button("전체 선택");
    Button deselectAllButton = new Button("선택 해제");
    Button selectByArtistButton = new Button("아티스트별 선택");
    
    selectAllButton.setOnAction(e -> {
        selectItems.forEach(item -> item.setSelected(true));
        musicListView.refresh();
    });
    
    deselectAllButton.setOnAction(e -> {
        selectItems.forEach(item -> item.setSelected(false));
        musicListView.refresh();
    });
    
    selectByArtistButton.setOnAction(e -> showArtistSelectionDialog(selectItems, musicListView));
    
    controlBox.getChildren().addAll(selectAllButton, deselectAllButton, selectByArtistButton);
    
    // 검색 기능
    TextField searchField = new TextField();
    searchField.setPromptText("음악 검색...");
    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        filterMusicList(musicListView, selectItems, newVal);
    });
    
    VBox content = new VBox(10);
    content.getChildren().addAll(
        new Label("검색:"), searchField,
        controlBox,
        new Label("음악 파일 목록:"), musicListView
    );
    
    selectDialog.getDialogPane().setContent(content);
    selectDialog.getDialogPane().setPrefSize(700, 600);
    
    Optional<ButtonType> result = selectDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        List<MusicInfo> selectedMusic = selectItems.stream()
            .filter(MusicInfoSelectItem::isSelected)
            .map(MusicInfoSelectItem::getMusicInfo)
            .collect(Collectors.toList());
        
        if (!selectedMusic.isEmpty()) {
            addMusicListToPlaylist(selectedMusic, playlistName);
        } else {
            showAlert("알림", "선택된 음악 파일이 없습니다.", Alert.AlertType.INFORMATION);
        }
    }
}
    private void showArtistSelectionDialog(List<MusicInfoSelectItem> allItems, ListView<MusicInfoSelectItem> musicListView) {
    // 아티스트별로 그룹화
    Map<String, List<MusicInfoSelectItem>> artistGroups = allItems.stream()
        .collect(Collectors.groupingBy(item -> item.getMusicInfo().getArtist()));
    
    Alert artistDialog = new Alert(Alert.AlertType.CONFIRMATION);
    artistDialog.setTitle("아티스트별 선택");
    artistDialog.setHeaderText("아티스트를 선택하세요");
    
    ListView<String> artistListView = new ListView<>();
    artistListView.getItems().addAll(artistGroups.keySet().stream().sorted().collect(Collectors.toList()));
    artistListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    artistListView.setPrefHeight(300);
    
    artistDialog.getDialogPane().setContent(artistListView);
    
    Optional<ButtonType> result = artistDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        List<String> selectedArtists = artistListView.getSelectionModel().getSelectedItems();
        
        // 선택된 아티스트의 모든 곡 선택
        allItems.forEach(item -> {
            if (selectedArtists.contains(item.getMusicInfo().getArtist())) {
                item.setSelected(true);
            }
        });
        
        musicListView.refresh();
    }
}
    private void filterMusicList(ListView<MusicInfoSelectItem> listView, List<MusicInfoSelectItem> allItems, String searchText) {
    if (searchText == null || searchText.trim().isEmpty()) {
        listView.getItems().setAll(allItems);
    } else {
        String lowerSearchText = searchText.toLowerCase();
        List<MusicInfoSelectItem> filteredItems = allItems.stream()
            .filter(item -> {
                MusicInfo music = item.getMusicInfo();
                return music.getTitle().toLowerCase().contains(lowerSearchText) ||
                       music.getArtist().toLowerCase().contains(lowerSearchText) ||
                       music.getAlbum().toLowerCase().contains(lowerSearchText);
            })
            .collect(Collectors.toList());
        listView.getItems().setAll(filteredItems);
    }
}
    private void showMusicPreviewDialog(List<MusicInfo> musicList, String playlistName) {
    Alert previewDialog = new Alert(Alert.AlertType.INFORMATION);
    previewDialog.setTitle("음악 파일 미리보기");
    previewDialog.setHeaderText("발견된 음악 파일 목록");
    previewDialog.setContentText("플레이리스트: " + playlistName);
    
    // 음악 파일 목록을 테이블로 표시
    TableView<MusicInfo> tableView = new TableView<>();
    tableView.setPrefHeight(400);
    tableView.setPrefWidth(700);
    
    // 컬럼 정의
    TableColumn<MusicInfo, String> titleColumn = new TableColumn<>("제목");
    titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
    titleColumn.setPrefWidth(200);
    
    TableColumn<MusicInfo, String> artistColumn = new TableColumn<>("아티스트");
    artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtist()));
    artistColumn.setPrefWidth(150);
    
    TableColumn<MusicInfo, String> albumColumn = new TableColumn<>("앨범");
    albumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAlbum()));
    albumColumn.setPrefWidth(150);
    
    TableColumn<MusicInfo, String> durationColumn = new TableColumn<>("재생시간");
    durationColumn.setCellValueFactory(cellData -> 
        new SimpleStringProperty(UIUtils.formatTime(cellData.getValue().getDurationMillis())));
    durationColumn.setPrefWidth(80);
    
    TableColumn<MusicInfo, String> statusColumn = new TableColumn<>("상태");
    statusColumn.setCellValueFactory(cellData -> {
        File file = new File(cellData.getValue().getFilePath());
        return new SimpleStringProperty(file.exists() ? "✓" : "❌");
    });
    statusColumn.setPrefWidth(50);
    
    tableView.getColumns().addAll(titleColumn, artistColumn, albumColumn, durationColumn, statusColumn);
    tableView.getItems().addAll(musicList);
    
    // 통계 정보
    VBox statsBox = new VBox(5);
    long totalDuration = musicList.stream().mapToLong(MusicInfo::getDurationMillis).sum();
    int existingFiles = (int) musicList.stream()
        .mapToInt(music -> new File(music.getFilePath()).exists() ? 1 : 0)
        .sum();
    
    statsBox.getChildren().addAll(
        new Label("총 " + musicList.size() + "개 파일"),
        new Label("총 재생시간: " + UIUtils.formatLongTime(totalDuration)),
        new Label("존재하는 파일: " + existingFiles + "개"),
        new Label("누락된 파일: " + (musicList.size() - existingFiles) + "개")
    );
    
    VBox content = new VBox(10);
    content.getChildren().addAll(statsBox, tableView);
    
    previewDialog.getDialogPane().setContent(content);
    previewDialog.getDialogPane().setPrefSize(800, 600);
    
    // 추가 버튼들
    ButtonType addAllButton = new ButtonType("모두 추가");
    ButtonType addExistingButton = new ButtonType("존재하는 파일만 추가");
    ButtonType closeButton = new ButtonType("닫기", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    previewDialog.getButtonTypes().setAll(addAllButton, addExistingButton, closeButton);
    
    Optional<ButtonType> result = previewDialog.showAndWait();
    if (result.isPresent()) {
        if (result.get() == addAllButton) {
            addMusicListToPlaylist(musicList, playlistName);
        } else if (result.get() == addExistingButton) {
            List<MusicInfo> existingMusic = musicList.stream()
                .filter(music -> new File(music.getFilePath()).exists())
                .collect(Collectors.toList());
            addMusicListToPlaylist(existingMusic, playlistName);
        }
    }
}
    private String truncateFileName(String fileName, int maxLength) {
    if (fileName == null || fileName.length() <= maxLength) {
        return fileName;
    }
    return "..." + fileName.substring(fileName.length() - maxLength + 3);
}
    private static class MusicInfoSelectItem {
    private final MusicInfo musicInfo;
    private boolean selected;
    private final String displayText;
    
    public MusicInfoSelectItem(MusicInfo musicInfo) {
        this.musicInfo = musicInfo;
        this.selected = true; // 기본적으로 선택됨
        this.displayText = formatDisplayText(musicInfo);
    }
    
    private String formatDisplayText(MusicInfo music) {
        File file = new File(music.getFilePath());
        String status = file.exists() ? "" : " [파일 없음]";
        
        return String.format("%s - %s (%s)%s", 
            music.getArtist(), 
            music.getTitle(),
            UIUtils.formatTime(music.getDurationMillis()),
            status);
    }
    
    public MusicInfo getMusicInfo() { return musicInfo; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public String getDisplayText() { return displayText; }
    
    @Override
    public String toString() { return displayText; }
}
    private void addMusicListToPlaylist(List<MusicInfo> musicList, String playlistName) {
    if (musicList.isEmpty()) return;
    
    // 진행 상황 표시
    view.showProgress(true);
    view.updateStatusLabel("음악 파일을 추가하는 중... (0/" + musicList.size() + ")", false);
    
    // 백그라운드에서 처리
    CompletableFuture.runAsync(() -> {
        int addedCount = 0;
        
        for (int i = 0; i < musicList.size(); i++) {
            MusicInfo music = musicList.get(i);
            
            Platform.runLater(() -> {
                view.addMusicToCurrentPlaylist(music);
                publisher.publish(new PlaylistEvent.MusicAddedToPlaylistEvent(playlistName, music));
            });
            
            addedCount++;
            
            // 진행 상황 업데이트
            final int currentIndex = i + 1;
            Platform.runLater(() -> {
                view.updateStatusLabel(String.format("음악 파일을 추가하는 중... (%d/%d)", 
                                                    currentIndex, musicList.size()), false);
            });
            
            // UI 응답성을 위한 짧은 대기
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 완료 처리
        final int finalAddedCount = addedCount;
        Platform.runLater(() -> {
            view.showProgress(false);
            view.updateStatusLabel(String.format("%d개의 음악 파일이 추가되었습니다.", finalAddedCount), false);
        });
    });
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
        
        // 파일명 파싱 시도 (여러 패턴 지원)
        String title = baseName;
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        
        // 패턴 1: "Artist - Title.mp3"
        if (baseName.contains(" - ")) {
            String[] parts = baseName.split(" - ", 2);
            if (parts.length == 2) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }
        // 패턴 2: "01. Artist - Title.mp3" (트랙 번호 포함)
        else if (baseName.matches("^\\d+\\.\\s*.*")) {
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
        // 패턴 3: "Artist_Title.mp3" (언더스코어로 구분)
        else if (baseName.contains("_")) {
            String[] parts = baseName.split("_", 2);
            if (parts.length == 2) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }
        
        // 파일 크기 기반 대략적인 재생 시간 계산
        long fileSize = file.length();
        long estimatedDuration = estimateDuration(fileSize, fileName);
        
        // LRC 파일 찾기
        String lrcPath = findLrcFile(file);
        
        log.debug("MusicInfo 생성: {} - {} ({}) [LRC: {}]", 
                 artist, title, formatDuration(estimatedDuration), lrcPath != null ? "있음" : "없음");
        
        return new MusicInfo(title, artist, album, file.getAbsolutePath(), estimatedDuration, lrcPath);
        
    } catch (Exception e) {
        log.error("MusicInfo 생성 중 오류: {}", file.getName(), e);
        // 기본값으로 반환
        return new MusicInfo(file.getName(), "Unknown Artist", "Unknown Album", 
                           file.getAbsolutePath(), 180000L, null);
    }
}
    private String formatDuration(long milliseconds) {
    long seconds = milliseconds / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;
    return String.format("%d:%02d", minutes, seconds);
    }

    
    private long estimateDuration(long fileSize, String fileName) {
    String ext = fileName.toLowerCase();
    long duration;
    
    if (ext.endsWith(".mp3")) {
        // MP3: 평균 비트레이트 128kbps 기준으로 계산
        // 128kbps = 16KB/s, 1분 = 960KB
        duration = (fileSize * 1000) / (16 * 1024); // 밀리초
    } else if (ext.endsWith(".wav")) {
        // WAV: 44.1kHz, 16bit, 스테레오 기준 (176.4KB/s)
        duration = (fileSize * 1000) / (176 * 1024);
    } else if (ext.endsWith(".flac")) {
        // FLAC: 대략 WAV의 50-60% 크기
        duration = (fileSize * 1000) / (100 * 1024);
    } else if (ext.endsWith(".m4a") || ext.endsWith(".aac")) {
        // AAC: 대략 MP3와 비슷한 압축률
        duration = (fileSize * 1000) / (20 * 1024);
    } else {
        // 기본값: 3분
        duration = 180000L;
    }
    
    // 최소 10초, 최대 2시간으로 제한
    return Math.max(10000L, Math.min(duration, 7200000L));
}
    
    private String findLrcFile(File musicFile) {
    try {
        String baseName = getFileNameWithoutExtension(musicFile.getName());
        
        // 1. 같은 디렉토리에서 찾기
        File lrcFile = new File(musicFile.getParent(), baseName + ".lrc");
        if (lrcFile.exists()) {
            log.debug("LRC 파일 발견 (같은 디렉토리): {}", lrcFile.getAbsolutePath());
            return lrcFile.getAbsolutePath();
        }
        
        // 2. lyrics 폴더에서 찾기
        File lyricsDir = new File(musicFile.getParent(), "lyrics");
        if (lyricsDir.exists()) {
            File lrcInLyricsDir = new File(lyricsDir, baseName + ".lrc");
            if (lrcInLyricsDir.exists()) {
                log.debug("LRC 파일 발견 (lyrics 폴더): {}", lrcInLyricsDir.getAbsolutePath());
                return lrcInLyricsDir.getAbsolutePath();
            }
        }
        
        // 3. 루트 lyrics 폴더에서 찾기
        File rootLyricsDir = new File("lyrics");
        if (rootLyricsDir.exists()) {
            File lrcInRootLyrics = new File(rootLyricsDir, baseName + ".lrc");
            if (lrcInRootLyrics.exists()) {
                log.debug("LRC 파일 발견 (루트 lyrics 폴더): {}", lrcInRootLyrics.getAbsolutePath());
                return lrcInRootLyrics.getAbsolutePath();
            }
        }
        
        // 4. 음악 제목으로 찾기 (특수문자 제거)
        String sanitizedTitle = baseName.replaceAll("[^a-zA-Z0-9가-힣\\s]", "_");
        File titleBasedLrc = new File(rootLyricsDir, sanitizedTitle + ".lrc");
        if (titleBasedLrc.exists()) {
            log.debug("LRC 파일 발견 (제목 기반): {}", titleBasedLrc.getAbsolutePath());
            return titleBasedLrc.getAbsolutePath();
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

    public void playMusic(MusicInfo music) {
    if (music != null && publisher != null) {
        log.info("곡 재생 요청: {} - {}", music.getArtist(), music.getTitle());
        publisher.publish(new MediaControlEvent.RequestPlayEvent(music));
        
        Platform.runLater(() -> {
            view.updateStatusLabel("재생 중: " + music.getTitle(), false);
        });
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