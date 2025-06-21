package ac.cwnu.synctune.ui.dialog;

import ac.cwnu.synctune.ui.util.UIUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * SyncTune 환경설정 다이얼로그
 */
public class PreferencesDialog extends Stage {
    
    // 설정 프로퍼티들
    private final BooleanProperty autoScanLibrary = new SimpleBooleanProperty(true);
    private final StringProperty defaultMusicLibrary = new SimpleStringProperty(System.getProperty("user.home") + "/Music");
    private final StringProperty defaultLyricsFolder = new SimpleStringProperty("lyrics");
    private final BooleanProperty enableLyricsSync = new SimpleBooleanProperty(true);
    private final BooleanProperty autoLoadLyrics = new SimpleBooleanProperty(true);
    private final DoubleProperty defaultVolume = new SimpleDoubleProperty(50.0);
    private final BooleanProperty rememberWindowPosition = new SimpleBooleanProperty(true);
    private final BooleanProperty minimizeToTray = new SimpleBooleanProperty(false);
    private final BooleanProperty showNotifications = new SimpleBooleanProperty(true);
    private final BooleanProperty enableKeyboardShortcuts = new SimpleBooleanProperty(true);
    private final StringProperty audioOutputDevice = new SimpleStringProperty("기본 장치");
    private final BooleanProperty enableEqualizer = new SimpleBooleanProperty(false);
    
    // UI 컴포넌트들
    private TabPane tabPane;
    private boolean isOkPressed = false;
    
    // 설정 파일 경로
    private static final String CONFIG_FILE = "synctune.properties";
    
    public PreferencesDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("환경설정");
        setResizable(false);
        
        loadSettings();
        initUI();
        setupEventHandlers();
    }
    
    private void initUI() {
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 탭들 생성
        Tab generalTab = createGeneralTab();
        Tab libraryTab = createLibraryTab();
        Tab lyricsTab = createLyricsTab();
        Tab audioTab = createAudioTab();
        Tab interfaceTab = createInterfaceTab();
        Tab shortcutsTab = createShortcutsTab();
        
        tabPane.getTabs().addAll(generalTab, libraryTab, lyricsTab, audioTab, interfaceTab, shortcutsTab);
        
        // 버튼 영역
        HBox buttonBox = createButtonBox();
        
        // 메인 레이아웃
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(buttonBox);
        
        Scene scene = new Scene(root, 600, 500);
        setScene(scene);
    }
    
    private Tab createGeneralTab() {
        Tab tab = new Tab("일반");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 시작 설정
        TitledPane startupPane = new TitledPane();
        startupPane.setText("시작 설정");
        startupPane.setCollapsible(false);
        
        VBox startupContent = new VBox(10);
        CheckBox autoScanCheck = new CheckBox("시작 시 음악 라이브러리 자동 스캔");
        autoScanCheck.selectedProperty().bindBidirectional(autoScanLibrary);
        
        CheckBox rememberPositionCheck = new CheckBox("창 위치 및 크기 기억");
        rememberPositionCheck.selectedProperty().bindBidirectional(rememberWindowPosition);
        
        startupContent.getChildren().addAll(autoScanCheck, rememberPositionCheck);
        startupPane.setContent(startupContent);
        
        // 종료 설정
        TitledPane exitPane = new TitledPane();
        exitPane.setText("종료 설정");
        exitPane.setCollapsible(false);
        
        VBox exitContent = new VBox(10);
        CheckBox minimizeCheck = new CheckBox("창 닫기 시 시스템 트레이로 최소화");
        minimizeCheck.selectedProperty().bindBidirectional(minimizeToTray);
        
        exitContent.getChildren().add(minimizeCheck);
        exitPane.setContent(exitContent);
        
        content.getChildren().addAll(startupPane, exitPane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private Tab createLibraryTab() {
        Tab tab = new Tab("음악 라이브러리");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 기본 음악 폴더
        TitledPane libraryPane = new TitledPane();
        libraryPane.setText("음악 라이브러리 설정");
        libraryPane.setCollapsible(false);
        
        VBox libraryContent = new VBox(10);
        
        Label libraryLabel = new Label("기본 음악 폴더:");
        HBox libraryBox = new HBox(10);
        TextField libraryField = new TextField();
        libraryField.textProperty().bindBidirectional(defaultMusicLibrary);
        libraryField.setPrefWidth(300);
        
        Button browseButton = new Button("찾아보기...");
        browseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("기본 음악 폴더 선택");
            File currentDir = new File(defaultMusicLibrary.get());
            if (currentDir.exists()) {
                chooser.setInitialDirectory(currentDir);
            }
            File selected = chooser.showDialog(this);
            if (selected != null) {
                defaultMusicLibrary.set(selected.getAbsolutePath());
            }
        });
        
        libraryBox.getChildren().addAll(libraryField, browseButton);
        
        // 스캔 설정
        CheckBox autoScanCheck = new CheckBox("하위 폴더까지 자동 스캔");
        CheckBox watchFolderCheck = new CheckBox("폴더 변경사항 감시 (실시간 업데이트)");
        
        // 지원 형식
        Label formatLabel = new Label("지원되는 음악 형식:");
        Label formatList = new Label("MP3, WAV, FLAC, M4A, AAC, OGG, WMA");
        formatList.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        libraryContent.getChildren().addAll(
            libraryLabel, libraryBox, 
            new Separator(),
            autoScanCheck, watchFolderCheck,
            new Separator(),
            formatLabel, formatList
        );
        libraryPane.setContent(libraryContent);
        
        content.getChildren().add(libraryPane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private Tab createLyricsTab() {
        Tab tab = new Tab("가사");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 가사 설정
        TitledPane lyricsPane = new TitledPane();
        lyricsPane.setText("가사 설정");
        lyricsPane.setCollapsible(false);
        
        VBox lyricsContent = new VBox(10);
        
        CheckBox enableSyncCheck = new CheckBox("가사 동기화 활성화");
        enableSyncCheck.selectedProperty().bindBidirectional(enableLyricsSync);
        
        CheckBox autoLoadCheck = new CheckBox("가사 파일 자동 로드");
        autoLoadCheck.selectedProperty().bindBidirectional(autoLoadLyrics);
        
        // 가사 폴더 설정
        Label lyricsLabel = new Label("기본 가사 폴더:");
        HBox lyricsBox = new HBox(10);
        TextField lyricsField = new TextField();
        lyricsField.textProperty().bindBidirectional(defaultLyricsFolder);
        lyricsField.setPrefWidth(300);
        
        Button browseLyricsButton = new Button("찾아보기...");
        browseLyricsButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("가사 폴더 선택");
            File currentDir = new File(defaultLyricsFolder.get());
            if (currentDir.exists()) {
                chooser.setInitialDirectory(currentDir);
            } else {
                chooser.setInitialDirectory(new File("."));
            }
            File selected = chooser.showDialog(this);
            if (selected != null) {
                defaultLyricsFolder.set(selected.getAbsolutePath());
            }
        });
        
        lyricsBox.getChildren().addAll(lyricsField, browseLyricsButton);
        
        // 검색 순서
        Label searchOrderLabel = new Label("가사 파일 검색 순서:");
        VBox searchOrderBox = new VBox(5);
        searchOrderBox.getChildren().addAll(
            new Label("1. 음악 파일과 같은 폴더"),
            new Label("2. 지정된 가사 폴더"),
            new Label("3. 음악 파일 폴더의 lyrics 하위폴더"),
            new Label("4. 제목 기반 검색")
        );
        searchOrderBox.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        lyricsContent.getChildren().addAll(
            enableSyncCheck, autoLoadCheck,
            new Separator(),
            lyricsLabel, lyricsBox,
            new Separator(),
            searchOrderLabel, searchOrderBox
        );
        lyricsPane.setContent(lyricsContent);
        
        content.getChildren().add(lyricsPane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private Tab createAudioTab() {
        Tab tab = new Tab("오디오");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 재생 설정
        TitledPane playbackPane = new TitledPane();
        playbackPane.setText("재생 설정");
        playbackPane.setCollapsible(false);
        
        VBox playbackContent = new VBox(10);
        
        // 기본 볼륨
        Label volumeLabel = new Label("기본 볼륨:");
        HBox volumeBox = new HBox(10);
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.valueProperty().bindBidirectional(defaultVolume);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setPrefWidth(200);
        
        Label volumeValueLabel = new Label();
        volumeValueLabel.textProperty().bind(defaultVolume.asString("%.0f%%"));
        
        volumeBox.getChildren().addAll(volumeSlider, volumeValueLabel);
        
        // 출력 장치
        Label outputLabel = new Label("출력 장치:");
        ComboBox<String> outputCombo = new ComboBox<>();
        outputCombo.getItems().addAll("기본 장치", "스피커", "헤드폰", "HDMI");
        outputCombo.setValue(audioOutputDevice.get());
        outputCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) audioOutputDevice.set(newVal);
        });
        
        // 이퀄라이저
        CheckBox equalizerCheck = new CheckBox("이퀄라이저 사용");
        equalizerCheck.selectedProperty().bindBidirectional(enableEqualizer);
        
        Button equalizerButton = new Button("이퀄라이저 설정...");
        equalizerButton.setDisable(true); // 향후 구현
        equalizerButton.disableProperty().bind(enableEqualizer.not());
        
        playbackContent.getChildren().addAll(
            volumeLabel, volumeBox,
            new Separator(),
            outputLabel, outputCombo,
            new Separator(),
            equalizerCheck, equalizerButton
        );
        playbackPane.setContent(playbackContent);
        
        content.getChildren().add(playbackPane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private Tab createInterfaceTab() {
        Tab tab = new Tab("인터페이스");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 알림 설정
        TitledPane notificationPane = new TitledPane();
        notificationPane.setText("알림 설정");
        notificationPane.setCollapsible(false);
        
        VBox notificationContent = new VBox(10);
        CheckBox showNotificationsCheck = new CheckBox("알림 표시");
        showNotificationsCheck.selectedProperty().bindBidirectional(showNotifications);
        
        CheckBox songChangeCheck = new CheckBox("곡 변경 시 알림");
        CheckBox errorNotificationCheck = new CheckBox("오류 발생 시 알림");
        
        notificationContent.getChildren().addAll(
            showNotificationsCheck, songChangeCheck, errorNotificationCheck
        );
        notificationPane.setContent(notificationContent);
        
        // 외관 설정
        TitledPane appearancePane = new TitledPane();
        appearancePane.setText("외관 설정");
        appearancePane.setCollapsible(false);
        
        VBox appearanceContent = new VBox(10);
        
        Label themeLabel = new Label("테마:");
        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("기본", "다크", "라이트");
        themeCombo.setValue("기본");
        
        Label languageLabel = new Label("언어:");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("한국어", "English");
        languageCombo.setValue("한국어");
        
        appearanceContent.getChildren().addAll(
            themeLabel, themeCombo,
            languageLabel, languageCombo
        );
        appearancePane.setContent(appearanceContent);
        
        content.getChildren().addAll(notificationPane, appearancePane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private Tab createShortcutsTab() {
        Tab tab = new Tab("단축키");
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        TitledPane shortcutsPane = new TitledPane();
        shortcutsPane.setText("키보드 단축키");
        shortcutsPane.setCollapsible(false);
        
        VBox shortcutsContent = new VBox(10);
        
        CheckBox enableShortcutsCheck = new CheckBox("키보드 단축키 사용");
        enableShortcutsCheck.selectedProperty().bindBidirectional(enableKeyboardShortcuts);
        
        // 단축키 목록
        TableView<ShortcutItem> shortcutsTable = new TableView<>();
        shortcutsTable.setPrefHeight(300);
        
        TableColumn<ShortcutItem, String> actionColumn = new TableColumn<>("기능");
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().actionProperty());
        actionColumn.setPrefWidth(200);
        
        TableColumn<ShortcutItem, String> keyColumn = new TableColumn<>("단축키");
        keyColumn.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyColumn.setPrefWidth(150);
        
        shortcutsTable.getColumns().addAll(actionColumn, keyColumn);
        
        // 샘플 단축키 데이터
        shortcutsTable.getItems().addAll(
            new ShortcutItem("재생/일시정지", "Space"),
            new ShortcutItem("정지", "Ctrl+S"),
            new ShortcutItem("이전 곡", "Ctrl+←"),
            new ShortcutItem("다음 곡", "Ctrl+→"),
            new ShortcutItem("볼륨 증가", "Ctrl+↑"),
            new ShortcutItem("볼륨 감소", "Ctrl+↓"),
            new ShortcutItem("음소거", "Ctrl+M"),
            new ShortcutItem("파일 열기", "Ctrl+O"),
            new ShortcutItem("새 플레이리스트", "Ctrl+N"),
            new ShortcutItem("종료", "Ctrl+Q")
        );
        
        Button resetButton = new Button("기본값으로 초기화");
        resetButton.setOnAction(e -> {
            // TODO: 단축키 초기화 기능 구현
            UIUtils.showInfo("단축키 초기화", "단축키가 기본값으로 초기화되었습니다.");
        });
        
        shortcutsContent.getChildren().addAll(
            enableShortcutsCheck,
            new Separator(),
            new Label("현재 설정된 단축키:"),
            shortcutsTable,
            resetButton
        );
        shortcutsPane.setContent(shortcutsContent);
        
        content.getChildren().add(shortcutsPane);
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button okButton = new Button("확인");
        okButton.setPrefWidth(80);
        okButton.setOnAction(e -> {
            saveSettings();
            isOkPressed = true;
            close();
        });
        
        Button cancelButton = new Button("취소");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> close());
        
        Button applyButton = new Button("적용");
        applyButton.setPrefWidth(80);
        applyButton.setOnAction(e -> saveSettings());
        
        Button resetButton = new Button("기본값");
        resetButton.setPrefWidth(80);
        resetButton.setOnAction(e -> resetToDefaults());
        
        buttonBox.getChildren().addAll(resetButton, applyButton, cancelButton, okButton);
        return buttonBox;
    }
    
    private void setupEventHandlers() {
        // 창 닫기 시 처리
        setOnCloseRequest(e -> {
            if (!isOkPressed) {
                loadSettings(); // 취소 시 설정 복원
            }
        });
    }
    
    private void loadSettings() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                
                autoScanLibrary.set(Boolean.parseBoolean(props.getProperty("autoScanLibrary", "true")));
                defaultMusicLibrary.set(props.getProperty("defaultMusicLibrary", System.getProperty("user.home") + "/Music"));
                defaultLyricsFolder.set(props.getProperty("defaultLyricsFolder", "lyrics"));
                enableLyricsSync.set(Boolean.parseBoolean(props.getProperty("enableLyricsSync", "true")));
                autoLoadLyrics.set(Boolean.parseBoolean(props.getProperty("autoLoadLyrics", "true")));
                defaultVolume.set(Double.parseDouble(props.getProperty("defaultVolume", "50.0")));
                rememberWindowPosition.set(Boolean.parseBoolean(props.getProperty("rememberWindowPosition", "true")));
                minimizeToTray.set(Boolean.parseBoolean(props.getProperty("minimizeToTray", "false")));
                showNotifications.set(Boolean.parseBoolean(props.getProperty("showNotifications", "true")));
                enableKeyboardShortcuts.set(Boolean.parseBoolean(props.getProperty("enableKeyboardShortcuts", "true")));
                audioOutputDevice.set(props.getProperty("audioOutputDevice", "기본 장치"));
                enableEqualizer.set(Boolean.parseBoolean(props.getProperty("enableEqualizer", "false")));
                
            } catch (IOException e) {
                System.err.println("설정 파일 로드 실패: " + e.getMessage());
            }
        }
    }
    
    private void saveSettings() {
        Properties props = new Properties();
        
        props.setProperty("autoScanLibrary", String.valueOf(autoScanLibrary.get()));
        props.setProperty("defaultMusicLibrary", defaultMusicLibrary.get());
        props.setProperty("defaultLyricsFolder", defaultLyricsFolder.get());
        props.setProperty("enableLyricsSync", String.valueOf(enableLyricsSync.get()));
        props.setProperty("autoLoadLyrics", String.valueOf(autoLoadLyrics.get()));
        props.setProperty("defaultVolume", String.valueOf(defaultVolume.get()));
        props.setProperty("rememberWindowPosition", String.valueOf(rememberWindowPosition.get()));
        props.setProperty("minimizeToTray", String.valueOf(minimizeToTray.get()));
        props.setProperty("showNotifications", String.valueOf(showNotifications.get()));
        props.setProperty("enableKeyboardShortcuts", String.valueOf(enableKeyboardShortcuts.get()));
        props.setProperty("audioOutputDevice", audioOutputDevice.get());
        props.setProperty("enableEqualizer", String.valueOf(enableEqualizer.get()));
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "SyncTune Configuration");
            System.out.println("설정이 저장되었습니다.");
        } catch (IOException e) {
            UIUtils.showError("설정 저장 실패", "설정을 저장할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void resetToDefaults() {
        autoScanLibrary.set(true);
        defaultMusicLibrary.set(System.getProperty("user.home") + "/Music");
        defaultLyricsFolder.set("lyrics");
        enableLyricsSync.set(true);
        autoLoadLyrics.set(true);
        defaultVolume.set(50.0);
        rememberWindowPosition.set(true);
        minimizeToTray.set(false);
        showNotifications.set(true);
        enableKeyboardShortcuts.set(true);
        audioOutputDevice.set("기본 장치");
        enableEqualizer.set(false);
        
        UIUtils.showInfo("기본값 복원", "모든 설정이 기본값으로 복원되었습니다.");
    }
    
    public boolean isOkPressed() {
        return isOkPressed;
    }
    
    // Getter 메서드들
    public boolean getAutoScanLibrary() { return autoScanLibrary.get(); }
    public String getDefaultMusicLibrary() { return defaultMusicLibrary.get(); }
    public String getDefaultLyricsFolder() { return defaultLyricsFolder.get(); }
    public boolean getEnableLyricsSync() { return enableLyricsSync.get(); }
    public boolean getAutoLoadLyrics() { return autoLoadLyrics.get(); }
    public double getDefaultVolume() { return defaultVolume.get(); }
    public boolean getRememberWindowPosition() { return rememberWindowPosition.get(); }
    public boolean getMinimizeToTray() { return minimizeToTray.get(); }
    public boolean getShowNotifications() { return showNotifications.get(); }
    public boolean getEnableKeyboardShortcuts() { return enableKeyboardShortcuts.get(); }
    public String getAudioOutputDevice() { return audioOutputDevice.get(); }
    public boolean getEnableEqualizer() { return enableEqualizer.get(); }
    
    /**
     * 단축키 항목을 위한 클래스
     */
    private static class ShortcutItem {
        private final StringProperty action = new SimpleStringProperty();
        private final StringProperty key = new SimpleStringProperty();
        
        public ShortcutItem(String action, String key) {
            this.action.set(action);
            this.key.set(key);
        }
        
        public StringProperty actionProperty() { return action; }
        public StringProperty keyProperty() { return key; }
    }
}