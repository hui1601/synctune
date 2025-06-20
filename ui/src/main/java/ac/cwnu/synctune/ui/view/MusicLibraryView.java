package ac.cwnu.synctune.ui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 음악 라이브러리를 표시하고 관리하는 뷰
 */
public class MusicLibraryView extends VBox {
    private final EventPublisher eventPublisher;
    
    // UI 컴포넌트들
    private TableView<MusicInfo> musicTable;
    private TextField searchField;
    private Label libraryStatsLabel;
    private ComboBox<String> sortByComboBox;
    private ComboBox<String> filterByComboBox;
    private Button refreshButton;
    private Button addToPlaylistButton;
    private Button playButton;
    
    // 데이터
    private final ObservableList<MusicInfo> allMusic = FXCollections.observableArrayList();
    private final ObservableList<MusicInfo> filteredMusic = FXCollections.observableArrayList();
    private MusicInfo currentlyHighlighted = null;
    
    // 정렬 및 필터 옵션
    private String currentSortBy = "제목";
    private String currentFilter = "전체";
    private String currentSearchText = "";

    public MusicLibraryView(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // 검색 필드
        searchField = new TextField();
        searchField.setPromptText("음악 검색 (제목, 아티스트, 앨범)");
        searchField.setPrefWidth(300);

        // 통계 라벨
        libraryStatsLabel = new Label("라이브러리: 0곡");
        libraryStatsLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        // 정렬 콤보박스
        sortByComboBox = new ComboBox<>();
        sortByComboBox.getItems().addAll("제목", "아티스트", "앨범", "재생시간", "파일명");
        sortByComboBox.setValue("제목");
        sortByComboBox.setPrefWidth(100);

        // 필터 콤보박스
        filterByComboBox = new ComboBox<>();
        filterByComboBox.getItems().addAll("전체", "최근 추가", "긴 곡 (5분+)", "짧은 곡 (3분-)", "가사 있음");
        filterByComboBox.setValue("전체");
        filterByComboBox.setPrefWidth(120);

        // 버튼들
        refreshButton = new Button("🔄");
        refreshButton.setTooltip(new Tooltip("새로고침"));
        
        addToPlaylistButton = new Button("📝");
        addToPlaylistButton.setTooltip(new Tooltip("플레이리스트에 추가"));
        
        playButton = new Button("▶");
        playButton.setTooltip(new Tooltip("선택된 곡 재생"));

        // 테이블 뷰 설정
        setupMusicTable();
    }

    private void setupMusicTable() {
        musicTable = new TableView<>();
        musicTable.setItems(filteredMusic);
        musicTable.setPrefHeight(500);
        musicTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 제목 컬럼
        TableColumn<MusicInfo, String> titleColumn = new TableColumn<>("제목");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(200);

        // 아티스트 컬럼
        TableColumn<MusicInfo, String> artistColumn = new TableColumn<>("아티스트");
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        artistColumn.setPrefWidth(150);

        // 앨범 컬럼
        TableColumn<MusicInfo, String> albumColumn = new TableColumn<>("앨범");
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("album"));
        albumColumn.setPrefWidth(150);

        // 재생시간 컬럼
        TableColumn<MusicInfo, String> durationColumn = new TableColumn<>("재생시간");
        durationColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                UIUtils.formatTime(cellData.getValue().getDurationMillis())
            )
        );
        durationColumn.setPrefWidth(80);

        // 파일 형식 컬럼
        TableColumn<MusicInfo, String> formatColumn = new TableColumn<>("형식");
        formatColumn.setCellValueFactory(cellData -> {
            String filePath = cellData.getValue().getFilePath();
            String extension = UIUtils.getFileExtension(filePath).toUpperCase();
            return new javafx.beans.property.SimpleStringProperty(extension);
        });
        formatColumn.setPrefWidth(60);

        // 가사 여부 컬럼
        TableColumn<MusicInfo, String> lyricsColumn = new TableColumn<>("가사");
        lyricsColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getLrcPath() != null ? "✓" : ""
            )
        );
        lyricsColumn.setPrefWidth(50);

        musicTable.getColumns().addAll(
            titleColumn, artistColumn, albumColumn, 
            durationColumn, formatColumn, lyricsColumn
        );

        // 행 팩토리 설정 (하이라이트 기능)
        musicTable.setRowFactory(tv -> {
            TableRow<MusicInfo> row = new TableRow<>();
            row.itemProperty().addListener((obs, previousMusic, currentMusic) -> {
                if (currentMusic != null && currentMusic.equals(currentlyHighlighted)) {
                    row.setStyle("-fx-background-color: #e3f2fd;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
    }

    private void layoutComponents() {
        setSpacing(10);
        setPadding(new Insets(10));

        // 상단 컨트롤 영역
        HBox topControls = new HBox(10);
        topControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label searchLabel = new Label("검색:");
        Label sortLabel = new Label("정렬:");
        Label filterLabel = new Label("필터:");
        
        // 오른쪽 정렬용 스페이서
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        topControls.getChildren().addAll(
            searchLabel, searchField,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            sortLabel, sortByComboBox,
            filterLabel, filterByComboBox,
            spacer,
            refreshButton, addToPlaylistButton, playButton
        );

        // 하단 정보 영역
        HBox bottomInfo = new HBox(10);
        bottomInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bottomInfo.getChildren().addAll(libraryStatsLabel);

        getChildren().addAll(topControls, musicTable, bottomInfo);
    }

    private void setupEventHandlers() {
        // 검색 필드
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            currentSearchText = newText;
            applyFiltersAndSort();
        });

        // 정렬 콤보박스
        sortByComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentSortBy = newValue;
            applyFiltersAndSort();
        });

        // 필터 콤보박스
        filterByComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentFilter = newValue;
            applyFiltersAndSort();
        });

        // 테이블 더블클릭으로 재생
        musicTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
                if (selectedMusic != null) {
                    playMusic(selectedMusic);
                }
            }
        });

        // 버튼 이벤트
        refreshButton.setOnAction(e -> refreshLibrary());
        playButton.setOnAction(e -> playSelectedMusic());
        addToPlaylistButton.setOnAction(e -> addSelectedToPlaylist());

        // 우클릭 컨텍스트 메뉴
        setupContextMenu();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem playItem = new MenuItem("재생");
        playItem.setOnAction(e -> playSelectedMusic());
        
        MenuItem addToPlaylistItem = new MenuItem("플레이리스트에 추가");
        addToPlaylistItem.setOnAction(e -> addSelectedToPlaylist());
        
        MenuItem showInFolderItem = new MenuItem("폴더에서 보기");
        showInFolderItem.setOnAction(e -> showSelectedInFolder());
        
        MenuItem propertiesItem = new MenuItem("속성");
        propertiesItem.setOnAction(e -> showMusicProperties());
        
        contextMenu.getItems().addAll(
            playItem,
            new SeparatorMenuItem(),
            addToPlaylistItem,
            new SeparatorMenuItem(),
            showInFolderItem,
            propertiesItem
        );
        
        musicTable.setContextMenu(contextMenu);
    }

    // ========== 데이터 관리 메서드들 ==========

    public void updateLibrary(List<MusicInfo> musicList) {
        allMusic.clear();
        allMusic.addAll(musicList);
        applyFiltersAndSort();
        updateLibraryStats();
    }

    public void addMusic(MusicInfo music) {
        if (!allMusic.contains(music)) {
            allMusic.add(music);
            applyFiltersAndSort();
            updateLibraryStats();
        }
    }

    public void removeMusic(MusicInfo music) {
        allMusic.remove(music);
        applyFiltersAndSort();
        updateLibraryStats();
    }

    public void updateMusicMetadata(MusicInfo updatedMusic) {
        for (int i = 0; i < allMusic.size(); i++) {
            MusicInfo music = allMusic.get(i);
            if (music.getFilePath().equals(updatedMusic.getFilePath())) {
                allMusic.set(i, updatedMusic);
                break;
            }
        }
        applyFiltersAndSort();
    }

    public void highlightCurrentMusic(MusicInfo music) {
        currentlyHighlighted = music;
        
        // 테이블 행 스타일 업데이트
        musicTable.refresh();
        
        // 현재 재생 중인 음악으로 스크롤
        if (music != null) {
            int index = filteredMusic.indexOf(music);
            if (index >= 0) {
                musicTable.scrollTo(index);
                musicTable.getSelectionModel().select(index);
            }
        }
    }

    public void clearHighlight() {
        currentlyHighlighted = null;
        musicTable.refresh();
    }

    // ========== 필터링 및 정렬 ==========

    private void applyFiltersAndSort() {
        List<MusicInfo> filtered = allMusic.stream()
            .filter(this::matchesSearchFilter)
            .filter(this::matchesTypeFilter)
            .collect(Collectors.toList());

        // 정렬 적용
        switch (currentSortBy) {
            case "제목":
                filtered.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case "아티스트":
                filtered.sort((a, b) -> a.getArtist().compareToIgnoreCase(b.getArtist()));
                break;
            case "앨범":
                filtered.sort((a, b) -> a.getAlbum().compareToIgnoreCase(b.getAlbum()));
                break;
            case "재생시간":
                filtered.sort((a, b) -> Long.compare(a.getDurationMillis(), b.getDurationMillis()));
                break;
            case "파일명":
                filtered.sort((a, b) -> {
                    String nameA = new java.io.File(a.getFilePath()).getName();
                    String nameB = new java.io.File(b.getFilePath()).getName();
                    return nameA.compareToIgnoreCase(nameB);
                });
                break;
        }

        filteredMusic.clear();
        filteredMusic.addAll(filtered);
        updateLibraryStats();
    }

    private boolean matchesSearchFilter(MusicInfo music) {
        if (currentSearchText == null || currentSearchText.trim().isEmpty()) {
            return true;
        }
        
        String searchLower = currentSearchText.toLowerCase();
        return music.getTitle().toLowerCase().contains(searchLower) ||
               music.getArtist().toLowerCase().contains(searchLower) ||
               music.getAlbum().toLowerCase().contains(searchLower) ||
               new java.io.File(music.getFilePath()).getName().toLowerCase().contains(searchLower);
    }

    private boolean matchesTypeFilter(MusicInfo music) {
        switch (currentFilter) {
            case "전체":
                return true;
            case "최근 추가":
                // TODO: 파일 생성/수정 시간 기반 필터링
                return true;
            case "긴 곡 (5분+)":
                return music.getDurationMillis() >= 5 * 60 * 1000;
            case "짧은 곡 (3분-)":
                return music.getDurationMillis() <= 3 * 60 * 1000;
            case "가사 있음":
                return music.getLrcPath() != null && !music.getLrcPath().isEmpty();
            default:
                return true;
        }
    }

    // ========== 액션 메서드들 ==========

    private void playSelectedMusic() {
        MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
        if (selectedMusic != null) {
            playMusic(selectedMusic);
        } else {
            UIUtils.showWarning("선택 오류", "재생할 음악을 선택해주세요.");
        }
    }

    private void playMusic(MusicInfo music) {
        eventPublisher.publish(new MediaControlEvent.RequestPlayEvent(music));
    }

    private void addSelectedToPlaylist() {
        MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
        if (selectedMusic != null) {
            // TODO: 플레이리스트 선택 다이얼로그 표시
            showAddToPlaylistDialog(selectedMusic);
        } else {
            UIUtils.showWarning("선택 오류", "플레이리스트에 추가할 음악을 선택해주세요.");
        }
    }

    private void showAddToPlaylistDialog(MusicInfo music) {
        List<String> playlistOptions = List.of("즐겨찾기", "최근 재생", "내가 만든 목록");
        String selectedPlaylist = UIUtils.showChoiceDialog(
            "플레이리스트 선택",
            "'" + music.getTitle() + "'을(를) 추가할 플레이리스트를 선택하세요:",
            "즐겨찾기",
            playlistOptions.toArray(new String[0])
        ).orElse(null);

        if (selectedPlaylist != null) {
            // TODO: 플레이리스트 추가 이벤트 발행
            UIUtils.showSuccess("추가 완료", 
                "'" + music.getTitle() + "'이(가) '" + selectedPlaylist + "'에 추가되었습니다.");
        }
    }

    private void showSelectedInFolder() {
        MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
        if (selectedMusic != null) {
            try {
                java.io.File file = new java.io.File(selectedMusic.getFilePath());
                if (file.exists()) {
                    // OS별 폴더 열기 명령어
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open -R " + file.getAbsolutePath());
                    } else {
                        Runtime.getRuntime().exec("xdg-open " + file.getParent());
                    }
                } else {
                    UIUtils.showError("파일 오류", "파일을 찾을 수 없습니다: " + selectedMusic.getFilePath());
                }
            } catch (Exception e) {
                UIUtils.showError("오류", "폴더를 열 수 없습니다: " + e.getMessage());
            }
        }
    }

    private void showMusicProperties() {
        MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
        if (selectedMusic != null) {
            showMusicPropertiesDialog(selectedMusic);
        }
    }

    private void showMusicPropertiesDialog(MusicInfo music) {
        Alert propertiesDialog = new Alert(Alert.AlertType.INFORMATION);
        propertiesDialog.setTitle("음악 속성");
        propertiesDialog.setHeaderText(music.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("제목: ").append(music.getTitle()).append("\n");
        content.append("아티스트: ").append(music.getArtist()).append("\n");
        content.append("앨범: ").append(music.getAlbum()).append("\n");
        content.append("재생시간: ").append(UIUtils.formatTime(music.getDurationMillis())).append("\n");
        content.append("파일 경로: ").append(music.getFilePath()).append("\n");
        
        if (music.getLrcPath() != null) {
            content.append("가사 파일: ").append(music.getLrcPath()).append("\n");
        }

        // 파일 정보 추가
        try {
            java.io.File file = new java.io.File(music.getFilePath());
            if (file.exists()) {
                content.append("파일 크기: ").append(UIUtils.formatFileSize(file.length())).append("\n");
                content.append("수정 날짜: ").append(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .format(java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault()
                        ))
                ).append("\n");
            }
        } catch (Exception e) {
            content.append("파일 정보를 읽을 수 없습니다.\n");
        }

        propertiesDialog.setContentText(content.toString());
        propertiesDialog.showAndWait();
    }

    // ========== 유틸리티 메서드들 ==========

    public void filterMusic(String searchText) {
        currentSearchText = searchText;
        searchField.setText(searchText);
        applyFiltersAndSort();
    }

    public void refreshLibrary() {
        // 현재 선택 상태 저장
        MusicInfo selectedMusic = musicTable.getSelectionModel().getSelectedItem();
        
        // 필터와 정렬 다시 적용
        applyFiltersAndSort();
        
        // 선택 상태 복원
        if (selectedMusic != null) {
            int index = filteredMusic.indexOf(selectedMusic);
            if (index >= 0) {
                musicTable.getSelectionModel().select(index);
            }
        }
        
        UIUtils.showToast((javafx.stage.Stage) getScene().getWindow(), 
            "라이브러리가 새로고침되었습니다", 2000);
    }

    private void updateLibraryStats() {
        long totalDuration = filteredMusic.stream()
            .mapToLong(MusicInfo::getDurationMillis)
            .sum();
        
        String statsText = String.format("라이브러리: %d곡 (%s)", 
            filteredMusic.size(), 
            UIUtils.formatTime(totalDuration)
        );
        
        if (filteredMusic.size() != allMusic.size()) {
            statsText += String.format(" | 전체: %d곡", allMusic.size());
        }
        
        libraryStatsLabel.setText(statsText);
    }

    // ========== 공개 인터페이스 ==========

    public MusicInfo getSelectedMusic() {
        return musicTable.getSelectionModel().getSelectedItem();
    }

    public List<MusicInfo> getAllMusic() {
        return new ArrayList<>(allMusic);
    }

    public List<MusicInfo> getFilteredMusic() {
        return new ArrayList<>(filteredMusic);
    }

    public int getMusicCount() {
        return allMusic.size();
    }

    public long getTotalDuration() {
        return allMusic.stream().mapToLong(MusicInfo::getDurationMillis).sum();
    }

    // ========== 고급 기능들 ==========

    public void selectMusic(MusicInfo music) {
        int index = filteredMusic.indexOf(music);
        if (index >= 0) {
            musicTable.getSelectionModel().select(index);
            musicTable.scrollTo(index);
        }
    }

    public void showLibraryStatistics() {
        Alert statsDialog = new Alert(Alert.AlertType.INFORMATION);
        statsDialog.setTitle("라이브러리 통계");
        statsDialog.setHeaderText("음악 라이브러리 현황");

        StringBuilder content = new StringBuilder();
        content.append("총 음악 수: ").append(allMusic.size()).append("곡\n");
        content.append("총 재생 시간: ").append(UIUtils.formatLongTime(getTotalDuration())).append("\n\n");

        // 아티스트별 통계
        Map<String, Long> artistCounts = allMusic.stream()
            .collect(Collectors.groupingBy(MusicInfo::getArtist, Collectors.counting()));
        content.append("아티스트 수: ").append(artistCounts.size()).append("명\n");

        // 앨범별 통계
        Map<String, Long> albumCounts = allMusic.stream()
            .collect(Collectors.groupingBy(MusicInfo::getAlbum, Collectors.counting()));
        content.append("앨범 수: ").append(albumCounts.size()).append("개\n");

        // 가사 파일 있는 곡 수
        long lyricsCount = allMusic.stream()
            .filter(music -> music.getLrcPath() != null && !music.getLrcPath().isEmpty())
            .count();
        content.append("가사 파일 있는 곡: ").append(lyricsCount).append("곡\n\n");

        // 파일 형식별 통계
        Map<String, Long> formatCounts = allMusic.stream()
            .collect(Collectors.groupingBy(
                music -> UIUtils.getFileExtension(music.getFilePath()).toUpperCase(),
                Collectors.counting()
            ));
        content.append("파일 형식별 분포:\n");
        formatCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> content.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("곡\n"));

        statsDialog.setContentText(content.toString());
        statsDialog.showAndWait();
    }

    public void exportLibraryInfo() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("라이브러리 정보 내보내기");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV 파일", "*.csv")
            );
            
            java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                exportToCsv(file);
                UIUtils.showSuccess("내보내기 완료", "라이브러리 정보가 성공적으로 내보내졌습니다.");
            }
        } catch (Exception e) {
            UIUtils.showError("내보내기 실패", "파일을 저장할 수 없습니다: " + e.getMessage());
        }
    }

    private void exportToCsv(java.io.File file) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(file), 
                    java.nio.charset.StandardCharsets.UTF_8
                )
        )) {
            // CSV 헤더
            writer.println("제목,아티스트,앨범,재생시간(초),파일경로,가사파일");
            
            // 데이터 행들
            for (MusicInfo music : allMusic) {
                writer.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"\n",
                    escapeCSV(music.getTitle()),
                    escapeCSV(music.getArtist()),
                    escapeCSV(music.getAlbum()),
                    music.getDurationMillis() / 1000,
                    escapeCSV(music.getFilePath()),
                    escapeCSV(music.getLrcPath() != null ? music.getLrcPath() : "")
                );
            }
        }
    }

    private String escapeCSV(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}