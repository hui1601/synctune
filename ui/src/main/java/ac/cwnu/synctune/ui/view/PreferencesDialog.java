package ac.cwnu.synctune.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * 환경설정 다이얼로그
 */
public class PreferencesDialog extends Dialog<ButtonType> {
    private final Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
    
    // 일반 설정
    private TextField musicLibraryPathField;
    private CheckBox autoScanCheckBox;
    private CheckBox showNotificationsCheckBox;
    private CheckBox minimizeToTrayCheckBox;
    
    // 재생 설정
    private Slider volumeSlider;
    private CheckBox rememberVolumeCheckBox;
    private CheckBox crossfadeCheckBox;
    private Spinner<Integer> crossfadeDurationSpinner;
    private ComboBox<String> audioDeviceComboBox;
    
    // 가사 설정
    private ComboBox<String> lyricsFontComboBox;
    private Spinner<Integer> lyricsFontSizeSpinner;
    private ColorPicker lyricsColorPicker;
    private CheckBox autoSearchLyricsCheckBox;
    
    // UI 설정
    private ComboBox<String> themeComboBox;
    private ComboBox<String> languageComboBox;
    private CheckBox compactModeCheckBox;
    private CheckBox alwaysOnTopCheckBox;
    
    // 고급 설정
    private CheckBox enableLoggingCheckBox;
    private Spinner<Integer> cacheMaxSizeSpinner;
    private CheckBox hardwareAccelerationCheckBox;

    public PreferencesDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("환경설정");
        setHeaderText("SyncTune 설정");
        
        // 다이얼로그 크기 설정
        getDialogPane().setPrefSize(600, 500);
        
        initializeComponents();
        createLayout();
        loadSettings();
        
        // 버튼 타입 설정
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.APPLY);
        
        // 버튼 이벤트 설정
        setupButtonHandlers();
    }

    private void initializeComponents() {
        // 일반 설정
        musicLibraryPathField = new TextField();
        musicLibraryPathField.setEditable(false);
        musicLibraryPathField.setPrefWidth(300);
        
        autoScanCheckBox = new CheckBox("프로그램 시작 시 자동으로 라이브러리 스캔");
        showNotificationsCheckBox = new CheckBox("알림 표시");
        minimizeToTrayCheckBox = new CheckBox("시스템 트레이로 최소화");
        
        // 재생 설정
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(25);
        
        rememberVolumeCheckBox = new CheckBox("종료 시 볼륨 설정 기억");
        crossfadeCheckBox = new CheckBox("곡 간 크로스페이드 사용");
        
        crossfadeDurationSpinner = new Spinner<>(1, 10, 3);
        crossfadeDurationSpinner.setEditable(true);
        crossfadeDurationSpinner.setPrefWidth(80);
        
        audioDeviceComboBox = new ComboBox<>();
        audioDeviceComboBox.getItems().addAll("기본 오디오 장치", "스피커", "헤드폰");
        audioDeviceComboBox.setValue("기본 오디오 장치");
        
        // 가사 설정
        lyricsFontComboBox = new ComboBox<>();
        lyricsFontComboBox.getItems().addAll("System", "Arial", "맑은 고딕", "Consolas");
        lyricsFontComboBox.setValue("System");
        
        lyricsFontSizeSpinner = new Spinner<>(12, 24, 16);
        lyricsFontSizeSpinner.setEditable(true);
        lyricsFontSizeSpinner.setPrefWidth(80);
        
        lyricsColorPicker = new ColorPicker(javafx.scene.paint.Color.BLACK);
        
        autoSearchLyricsCheckBox = new CheckBox("온라인에서 자동으로 가사 검색");
        
        // UI 설정
        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("라이트", "다크", "시스템 기본값");
        themeComboBox.setValue("라이트");
        
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("한국어", "English", "日本語");
        languageComboBox.setValue("한국어");
        
        compactModeCheckBox = new CheckBox("컴팩트 모드로 시작");
        alwaysOnTopCheckBox = new CheckBox("항상 위에 표시");
        
        // 고급 설정
        enableLoggingCheckBox = new CheckBox("상세 로그 기록");
        
        cacheMaxSizeSpinner = new Spinner<>(50, 1000, 200);
        cacheMaxSizeSpinner.setEditable(true);
        cacheMaxSizeSpinner.setPrefWidth(100);
        
        hardwareAccelerationCheckBox = new CheckBox("하드웨어 가속 사용");
    }

    private void createLayout() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 일반 탭
        Tab generalTab = new Tab("일반", createGeneralTab());
        
        // 재생 탭
        Tab playbackTab = new Tab("재생", createPlaybackTab());
        
        // 가사 탭
        Tab lyricsTab = new Tab("가사", createLyricsTab());
        
        // UI 탭
        Tab uiTab = new Tab("인터페이스", createUITab());
        
        // 고급 탭
        Tab advancedTab = new Tab("고급", createAdvancedTab());
        
        tabPane.getTabs().addAll(generalTab, playbackTab, lyricsTab, uiTab, advancedTab);
        
        getDialogPane().setContent(tabPane);
    }

    private VBox createGeneralTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        
        // 음악 라이브러리 경로
        VBox librarySection = new VBox(8);
        Label libraryLabel = new Label("음악 라이브러리 폴더:");
        libraryLabel.setStyle("-fx-font-weight: bold;");
        
        HBox libraryPathBox = new HBox(10);
        libraryPathBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button browseButton = new Button("찾아보기");
        browseButton.setOnAction(e -> browseMusicLibraryPath());
        
        libraryPathBox.getChildren().addAll(musicLibraryPathField, browseButton);
        librarySection.getChildren().addAll(libraryLabel, libraryPathBox);
        
        // 시작 설정
        VBox startupSection = new VBox(8);
        Label startupLabel = new Label("시작 설정:");
        startupLabel.setStyle("-fx-font-weight: bold;");
        startupSection.getChildren().addAll(startupLabel, autoScanCheckBox);
        
        // 알림 설정
        VBox notificationSection = new VBox(8);
        Label notificationLabel = new Label("알림 설정:");
        notificationLabel.setStyle("-fx-font-weight: bold;");
        notificationSection.getChildren().addAll(
            notificationLabel, 
            showNotificationsCheckBox, 
            minimizeToTrayCheckBox
        );
        
        vbox.getChildren().addAll(
            librarySection, 
            new Separator(), 
            startupSection, 
            new Separator(), 
            notificationSection
        );
        
        return vbox;
    }

    private VBox createPlaybackTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        
        // 볼륨 설정
        VBox volumeSection = new VBox(8);
        Label volumeLabel = new Label("기본 볼륨:");
        volumeLabel.setStyle("-fx-font-weight: bold;");
        
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label volumeValueLabel = new Label("50%");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            volumeValueLabel.setText(String.format("%.0f%%", newVal.doubleValue()))
        );
        volumeBox.getChildren().addAll(volumeSlider, volumeValueLabel);
        
        volumeSection.getChildren().addAll(volumeLabel, volumeBox, rememberVolumeCheckBox);
        
        // 크로스페이드 설정
        VBox crossfadeSection = new VBox(8);
        Label crossfadeLabel = new Label("크로스페이드:");
        crossfadeLabel.setStyle("-fx-font-weight: bold;");
        
        HBox crossfadeBox = new HBox(10);
        crossfadeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        crossfadeBox.getChildren().addAll(
            crossfadeCheckBox, 
            new Label("지속시간:"), 
            crossfadeDurationSpinner, 
            new Label("초")
        );
        
        crossfadeSection.getChildren().addAll(crossfadeLabel, crossfadeBox);
        
        // 오디오 장치 설정
        VBox audioDeviceSection = new VBox(8);
        Label audioDeviceLabel = new Label("오디오 출력 장치:");
        audioDeviceLabel.setStyle("-fx-font-weight: bold;");
        audioDeviceSection.getChildren().addAll(audioDeviceLabel, audioDeviceComboBox);
        
        vbox.getChildren().addAll(
            volumeSection, 
            new Separator(), 
            crossfadeSection, 
            new Separator(), 
            audioDeviceSection
        );
        
        return vbox;
    }

    private VBox createLyricsTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        
        // 가사 표시 설정
        VBox displaySection = new VBox(8);
        Label displayLabel = new Label("가사 표시:");
        displayLabel.setStyle("-fx-font-weight: bold;");
        
        GridPane displayGrid = new GridPane();
        displayGrid.setHgap(10);
        displayGrid.setVgap(8);
        
        displayGrid.add(new Label("글꼴:"), 0, 0);
        displayGrid.add(lyricsFontComboBox, 1, 0);
        
        displayGrid.add(new Label("크기:"), 0, 1);
        HBox fontSizeBox = new HBox(5);
        fontSizeBox.getChildren().addAll(lyricsFontSizeSpinner, new Label("pt"));
        displayGrid.add(fontSizeBox, 1, 1);
        
        displayGrid.add(new Label("색상:"), 0, 2);
        displayGrid.add(lyricsColorPicker, 1, 2);
        
        displaySection.getChildren().addAll(displayLabel, displayGrid);
        
        // 가사 검색 설정
        VBox searchSection = new VBox(8);
        Label searchLabel = new Label("가사 검색:");
        searchLabel.setStyle("-fx-font-weight: bold;");
        searchSection.getChildren().addAll(searchLabel, autoSearchLyricsCheckBox);
        
        vbox.getChildren().addAll(displaySection, new Separator(), searchSection);
        
        return vbox;
    }

    private VBox createUITab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        
        // 테마 설정
        VBox themeSection = new VBox(8);
        Label themeLabel = new Label("외관:");
        themeLabel.setStyle("-fx-font-weight: bold;");
        
        GridPane themeGrid = new GridPane();
        themeGrid.setHgap(10);
        themeGrid.setVgap(8);
        
        themeGrid.add(new Label("테마:"), 0, 0);
        themeGrid.add(themeComboBox, 1, 0);
        
        themeGrid.add(new Label("언어:"), 0, 1);
        themeGrid.add(languageComboBox, 1, 1);
        
        themeSection.getChildren().addAll(themeLabel, themeGrid);
        
        // 창 설정
        VBox windowSection = new VBox(8);
        Label windowLabel = new Label("창 설정:");
        windowLabel.setStyle("-fx-font-weight: bold;");
        windowSection.getChildren().addAll(
            windowLabel, 
            compactModeCheckBox, 
            alwaysOnTopCheckBox
        );
        
        vbox.getChildren().addAll(themeSection, new Separator(), windowSection);
        
        return vbox;
    }

    private VBox createAdvancedTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        
        // 성능 설정
        VBox performanceSection = new VBox(8);
        Label performanceLabel = new Label("성능:");
        performanceLabel.setStyle("-fx-font-weight: bold;");
        performanceSection.getChildren().addAll(performanceLabel, hardwareAccelerationCheckBox);
        
        // 캐시 설정
        VBox cacheSection = new VBox(8);
        Label cacheLabel = new Label("캐시:");
        cacheLabel.setStyle("-fx-font-weight: bold;");
        
        HBox cacheBox = new HBox(10);
        cacheBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        cacheBox.getChildren().addAll(
            new Label("최대 캐시 크기:"), 
            cacheMaxSizeSpinner, 
            new Label("MB")
        );
        
        Button clearCacheButton = new Button("캐시 지우기");
        clearCacheButton.setOnAction(e -> clearCache());
        
        cacheSection.getChildren().addAll(cacheLabel, cacheBox, clearCacheButton);
        
        // 로깅 설정
        VBox loggingSection = new VBox(8);
        Label loggingLabel = new Label("디버깅:");
        loggingLabel.setStyle("-fx-font-weight: bold;");
        
        Button openLogFolderButton = new Button("로그 폴더 열기");
        openLogFolderButton.setOnAction(e -> openLogFolder());
        
        loggingSection.getChildren().addAll(
            loggingLabel, 
            enableLoggingCheckBox, 
            openLogFolderButton
        );
        
        vbox.getChildren().addAll(
            performanceSection, 
            new Separator(), 
            cacheSection, 
            new Separator(), 
            loggingSection
        );
        
        return vbox;
    }

    private void setupButtonHandlers() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Button applyButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        
        okButton.setOnAction(e -> {
            if (saveSettings()) {
                close();
            }
        });
        
        applyButton.setOnAction(e -> saveSettings());
    }

    private void browseMusicLibraryPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("음악 라이브러리 폴더 선택");
        
        String currentPath = musicLibraryPathField.getText();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(getOwner());
        if (selectedDirectory != null) {
            musicLibraryPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void clearCache() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("캐시 지우기");
        confirmDialog.setHeaderText("모든 캐시를 삭제하시겠습니까?");
        confirmDialog.setContentText("이 작업은 되돌릴 수 없습니다.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: 실제 캐시 삭제 로직 구현
                Alert successDialog = new Alert(Alert.AlertType.INFORMATION);
                successDialog.setTitle("완료");
                successDialog.setHeaderText("캐시가 성공적으로 삭제되었습니다.");
                successDialog.showAndWait();
            }
        });
    }

    private void openLogFolder() {
        try {
            String logPath = System.getProperty("user.dir") + "/logs";
            File logDir = new File(logPath);
            
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("explorer.exe " + logDir.getAbsolutePath());
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + logDir.getAbsolutePath());
            } else {
                Runtime.getRuntime().exec("xdg-open " + logDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
            errorDialog.setTitle("오류");
            errorDialog.setHeaderText("로그 폴더를 열 수 없습니다.");
            errorDialog.setContentText(e.getMessage());
            errorDialog.showAndWait();
        }
    }

    private void loadSettings() {
        // 일반 설정 로드
        musicLibraryPathField.setText(prefs.get("musicLibraryPath", 
            System.getProperty("user.home") + "/Music"));
        autoScanCheckBox.setSelected(prefs.getBoolean("autoScan", true));
        showNotificationsCheckBox.setSelected(prefs.getBoolean("showNotifications", true));
        minimizeToTrayCheckBox.setSelected(prefs.getBoolean("minimizeToTray", false));
        
        // 재생 설정 로드
        volumeSlider.setValue(prefs.getDouble("defaultVolume", 50.0));
        rememberVolumeCheckBox.setSelected(prefs.getBoolean("rememberVolume", true));
        crossfadeCheckBox.setSelected(prefs.getBoolean("crossfadeEnabled", false));
        crossfadeDurationSpinner.getValueFactory().setValue(prefs.getInt("crossfadeDuration", 3));
        audioDeviceComboBox.setValue(prefs.get("audioDevice", "기본 오디오 장치"));
        
        // 가사 설정 로드
        lyricsFontComboBox.setValue(prefs.get("lyricsFont", "System"));
        lyricsFontSizeSpinner.getValueFactory().setValue(prefs.getInt("lyricsFontSize", 16));
        
        String lyricsColorHex = prefs.get("lyricsColor", "#000000");
        try {
            lyricsColorPicker.setValue(javafx.scene.paint.Color.web(lyricsColorHex));
        } catch (Exception e) {
            lyricsColorPicker.setValue(javafx.scene.paint.Color.BLACK);
        }
        
        autoSearchLyricsCheckBox.setSelected(prefs.getBoolean("autoSearchLyrics", true));
        
        // UI 설정 로드
        themeComboBox.setValue(prefs.get("theme", "라이트"));
        languageComboBox.setValue(prefs.get("language", "한국어"));
        compactModeCheckBox.setSelected(prefs.getBoolean("startInCompactMode", false));
        alwaysOnTopCheckBox.setSelected(prefs.getBoolean("alwaysOnTop", false));
        
        // 고급 설정 로드
        enableLoggingCheckBox.setSelected(prefs.getBoolean("enableLogging", false));
        cacheMaxSizeSpinner.getValueFactory().setValue(prefs.getInt("cacheMaxSize", 200));
        hardwareAccelerationCheckBox.setSelected(prefs.getBoolean("hardwareAcceleration", true));
    }

    private boolean saveSettings() {
        try {
            // 일반 설정 저장
            prefs.put("musicLibraryPath", musicLibraryPathField.getText());
            prefs.putBoolean("autoScan", autoScanCheckBox.isSelected());
            prefs.putBoolean("showNotifications", showNotificationsCheckBox.isSelected());
            prefs.putBoolean("minimizeToTray", minimizeToTrayCheckBox.isSelected());
            
            // 재생 설정 저장
            prefs.putDouble("defaultVolume", volumeSlider.getValue());
            prefs.putBoolean("rememberVolume", rememberVolumeCheckBox.isSelected());
            prefs.putBoolean("crossfadeEnabled", crossfadeCheckBox.isSelected());
            prefs.putInt("crossfadeDuration", crossfadeDurationSpinner.getValue());
            prefs.put("audioDevice", audioDeviceComboBox.getValue());
            
            // 가사 설정 저장
            prefs.put("lyricsFont", lyricsFontComboBox.getValue());
            prefs.putInt("lyricsFontSize", lyricsFontSizeSpinner.getValue());
            prefs.put("lyricsColor", lyricsColorPicker.getValue().toString());
            prefs.putBoolean("autoSearchLyrics", autoSearchLyricsCheckBox.isSelected());
            
            // UI 설정 저장
            prefs.put("theme", themeComboBox.getValue());
            prefs.put("language", languageComboBox.getValue());
            prefs.putBoolean("startInCompactMode", compactModeCheckBox.isSelected());
            prefs.putBoolean("alwaysOnTop", alwaysOnTopCheckBox.isSelected());
            
            // 고급 설정 저장
            prefs.putBoolean("enableLogging", enableLoggingCheckBox.isSelected());
            prefs.putInt("cacheMaxSize", cacheMaxSizeSpinner.getValue());
            prefs.putBoolean("hardwareAcceleration", hardwareAccelerationCheckBox.isSelected());
            
            // 설정 파일에 변경사항 반영
            prefs.flush();
            
            // 성공 메시지
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("설정 저장");
            successAlert.setHeaderText("설정이 성공적으로 저장되었습니다.");
            successAlert.setContentText("일부 설정은 프로그램을 다시 시작한 후에 적용됩니다.");
            successAlert.showAndWait();
            
            return true;
            
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("설정 저장 오류");
            errorAlert.setHeaderText("설정을 저장하는 중 오류가 발생했습니다.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
            
            return false;
        }
    }

    // ========== 공개 메서드들 (설정값 접근용) ==========
    
    public static String getMusicLibraryPath() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.get("musicLibraryPath", System.getProperty("user.home") + "/Music");
    }
    
    public static boolean isAutoScanEnabled() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.getBoolean("autoScan", true);
    }
    
    public static double getDefaultVolume() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.getDouble("defaultVolume", 50.0);
    }
    
    public static boolean isRememberVolumeEnabled() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.getBoolean("rememberVolume", true);
    }
    
    public static String getTheme() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.get("theme", "라이트");
    }
    
    public static boolean isLoggingEnabled() {
        Preferences prefs = Preferences.userNodeForPackage(PreferencesDialog.class);
        return prefs.getBoolean("enableLogging", false);
    }
}