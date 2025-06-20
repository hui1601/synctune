package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.application.Platform;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isUserSeeking = false;
    private MusicInfo currentMusic = null;

    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        attachEventHandlers();
        setupKeyboardShortcuts();
        log.debug("PlaybackController 초기화 완료");
    }

    private void attachEventHandlers() {
        // 재생 버튼
        view.getPlayButton().setOnAction(e -> requestPlay());

        // 일시정지 버튼
        view.getPauseButton().setOnAction(e -> requestPause());

        // 정지 버튼
        view.getStopButton().setOnAction(e -> requestStop());

        // 이전 곡 버튼
        view.getPrevButton().setOnAction(e -> requestPrevious());

        // 다음 곡 버튼
        view.getNextButton().setOnAction(e -> requestNext());

        // 진행 바 드래그 시작/종료 감지
        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (wasChanging && !isChanging) {
                // 드래그 종료 시 탐색 이벤트 발행
                long seekPosition = (long) view.getProgressSlider().getValue();
                log.debug("진행 바 탐색: {}ms", seekPosition);
                publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                isUserSeeking = false;
            } else if (!wasChanging && isChanging) {
                // 드래그 시작
                isUserSeeking = true;
            }
        });

        // 볼륨 슬라이더
        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!view.getVolumeSlider().isValueChanging()) {
                // TODO: 볼륨 변경 이벤트 추가 시 구현
                log.debug("볼륨 변경: {}%", newVal.intValue());
                // publisher.publish(new MediaControlEvent.RequestVolumeChangeEvent(newVal.floatValue() / 100f));
            }
        });

        // 셔플 버튼 (만약 있다면)
        if (view.getShuffleButton() != null) {
            view.getShuffleButton().setOnAction(e -> toggleShuffle());
        }

        // 반복 버튼 (만약 있다면)
        if (view.getRepeatButton() != null) {
            view.getRepeatButton().setOnAction(e -> toggleRepeat());
        }
    }

    private void setupKeyboardShortcuts() {
        // 키보드 단축키 설정 (Scene에 추가되어야 함)
        Platform.runLater(() -> {
            if (view.getScene() != null) {
                view.getScene().setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case SPACE:
                            if (isPlaybackActive) {
                                requestPause();
                            } else {
                                requestPlay();
                            }
                            event.consume();
                            break;
                        case S:
                            if (event.isControlDown()) {
                                requestStop();
                                event.consume();
                            }
                            break;
                        case RIGHT:
                            if (event.isControlDown()) {
                                requestNext();
                                event.consume();
                            } else if (event.isShiftDown()) {
                                seekForward();
                                event.consume();
                            }
                            break;
                        case LEFT:
                            if (event.isControlDown()) {
                                requestPrevious();
                                event.consume();
                            } else if (event.isShiftDown()) {
                                seekBackward();
                                event.consume();
                            }
                            break;
                        case UP:
                            if (event.isControlDown()) {
                                adjustVolume(0.1f);
                                event.consume();
                            }
                            break;
                        case DOWN:
                            if (event.isControlDown()) {
                                adjustVolume(-0.1f);
                                event.consume();
                            }
                            break;
                        case M:
                            if (event.isControlDown()) {
                                toggleMute();
                                event.consume();
                            }
                            break;
                    }
                });
            }
        });
    }

    // ========== 공개 제어 메서드들 ==========

    public void requestPlay() {
        log.debug("재생 요청");
        if (currentMusic != null) {
            publisher.publish(new MediaControlEvent.RequestPlayEvent(currentMusic));
        } else {
            publisher.publish(new MediaControlEvent.RequestPlayEvent());
        }
        updatePlaybackState(true, false, false);
    }

    public void requestPause() {
        log.debug("일시정지 요청");
        publisher.publish(new MediaControlEvent.RequestPauseEvent());
        updatePlaybackState(false, true, false);
    }

    public void requestStop() {
        log.debug("정지 요청");
        publisher.publish(new MediaControlEvent.RequestStopEvent());
        updatePlaybackState(false, false, true);
    }

    public void requestNext() {
        log.debug("다음 곡 요청");
        publisher.publish(new MediaControlEvent.RequestNextMusicEvent());
    }

    public void requestPrevious() {
        log.debug("이전 곡 요청");
        publisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
    }

    public void seekTo(long positionMs) {
        log.debug("탐색 요청: {}ms", positionMs);
        publisher.publish(new MediaControlEvent.RequestSeekEvent(positionMs));
    }

    // ========== 고급 제어 메서드들 ==========

    private void seekForward() {
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.min(currentPos + 10000, (long) view.getProgressSlider().getMax()); // 10초 앞으로
        seekTo(newPos);
    }

    private void seekBackward() {
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.max(currentPos - 10000, 0); // 10초 뒤로
        seekTo(newPos);
    }

    private void adjustVolume(float delta) {
        double currentVolume = view.getVolumeSlider().getValue();
        double newVolume = Math.max(0, Math.min(100, currentVolume + (delta * 100)));
        view.getVolumeSlider().setValue(newVolume);
        log.debug("볼륨 조정: {}%", (int) newVolume);
    }

    private void toggleMute() {
        // TODO: 음소거 토글 이벤트 구현
        log.debug("음소거 토글 요청");
        // publisher.publish(new MediaControlEvent.RequestMuteToggleEvent());
    }

    private void toggleShuffle() {
        // TODO: 셔플 토글 이벤트 구현
        log.debug("셔플 토글 요청");
        // publisher.publish(new MediaControlEvent.RequestShuffleToggleEvent());
    }

    private void toggleRepeat() {
        // TODO: 반복 토글 이벤트 구현
        log.debug("반복 토글 요청");
        // publisher.publish(new MediaControlEvent.RequestRepeatToggleEvent());
    }

    // ========== 상태 업데이트 메서드들 ==========

    private void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        Platform.runLater(() -> {
            view.updatePlaybackState(isPlaying, isPaused, isStopped);
            isPlaybackActive = isPlaying;
        });
    }

    public void updateCurrentMusic(MusicInfo music) {
        this.currentMusic = music;
        Platform.runLater(() -> {
            view.updateMusicInfo(music);
        });
    }

    public void updateProgress(long currentMs, long totalMs) {
        Platform.runLater(() -> {
            if (!isUserSeeking) {
                view.updateProgress(currentMs, totalMs);
            }
        });
    }

    public void updateVolume(float volume) {
        Platform.runLater(() -> {
            view.getVolumeSlider().setValue(volume * 100);
        });
    }

    public void updateShuffleState(boolean enabled) {
        Platform.runLater(() -> {
            if (view.getShuffleButton() != null) {
                view.getShuffleButton().setStyle(enabled ? 
                    "-fx-background-color: #007bff;" : 
                    "-fx-background-color: #6c757d;");
            }
        });
    }

    public void updateRepeatState(String repeatMode) {
        Platform.runLater(() -> {
            if (view.getRepeatButton() != null) {
                switch (repeatMode.toLowerCase()) {
                    case "none":
                        view.getRepeatButton().setText("🔁");
                        view.getRepeatButton().setStyle("-fx-background-color: #6c757d;");
                        break;
                    case "all":
                        view.getRepeatButton().setText("🔁");
                        view.getRepeatButton().setStyle("-fx-background-color: #007bff;");
                        break;
                    case "one":
                        view.getRepeatButton().setText("🔂");
                        view.getRepeatButton().setStyle("-fx-background-color: #007bff;");
                        break;
                }
            }
        });
    }

    // ========== 재생 대기열 관리 ==========

    public void playMusicFromQueue(MusicInfo music) {
        this.currentMusic = music;
        log.info("대기열에서 음악 재생: {}", music.getTitle());
        publisher.publish(new MediaControlEvent.RequestPlayEvent(music));
        updatePlaybackState(true, false, false);
    }

    public void addToQueue(MusicInfo music) {
        log.debug("대기열에 추가: {}", music.getTitle());
        // TODO: 대기열 추가 이벤트 구현
        // publisher.publish(new MediaControlEvent.AddToQueueEvent(music));
    }

    public void clearQueue() {
        log.debug("대기열 초기화");
        // TODO: 대기열 초기화 이벤트 구현
        // publisher.publish(new MediaControlEvent.ClearQueueEvent());
    }

    // ========== 상태 조회 메서드들 ==========

    public boolean isUserSeeking() {
        return isUserSeeking;
    }

    public boolean isPlaybackActive() {
        return isPlaybackActive;
    }

    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }

    public float getCurrentVolume() {
        return (float) (view.getVolumeSlider().getValue() / 100.0);
    }

    public long getCurrentPosition() {
        return (long) view.getProgressSlider().getValue();
    }

    // ========== 오류 처리 ==========

    public void handlePlaybackError(String errorMessage) {
        Platform.runLater(() -> {
            updatePlaybackState(false, false, true);
            log.error("재생 오류: {}", errorMessage);
            
            // 사용자에게 오류 알림
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("재생 오류");
            alert.setHeaderText("음악 재생 중 오류가 발생했습니다");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    public void handleSeekError(long requestedPosition) {
        log.warn("탐색 실패: {}ms 위치로 이동할 수 없습니다", requestedPosition);
        Platform.runLater(() -> {
            // 진행 바를 이전 위치로 복원
            if (currentMusic != null) {
                // TODO: 현재 실제 위치를 가져와서 복원
            }
        });
    }

    // ========== 정리 ==========

    public void dispose() {
        log.debug("PlaybackController 정리 중...");
        // 리소스 정리 (필요한 경우)
    }
}