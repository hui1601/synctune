package ac.cwnu.synctune.ui.controller;

import org.slf4j.Logger;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.event.MediaControlEvent;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.ui.view.PlayerControlsView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class PlaybackController {
    private static final Logger log = LogManager.getLogger(PlaybackController.class);
    
    private final PlayerControlsView view;
    private final EventPublisher publisher;
    private boolean isPlaybackActive = false;
    private boolean isUserSeeking = false;
    private MusicInfo currentMusic = null;
    
    // 상태 관리
    private float currentVolume = 0.5f;
    private boolean isMuted = false;
    private float lastVolumeBeforeMute = 0.5f;
    
    // 탐색 관련
    private Timeline seekPreviewTimer;
    private boolean isSeekPreviewActive = false;

    public PlaybackController(PlayerControlsView view, EventPublisher publisher) {
        this.view = view;
        this.publisher = publisher;
        log.debug("PlaybackController 초기화 시작");
        
        // 초기화 순서 중요
        setupInitialVolume();
        attachEventHandlers();
        setupProgressSliderHandlers();
        setupVolumeSliderHandlers();
        setupAdvancedFeatures();
        setupKeyboardShortcuts();
        
        log.debug("PlaybackController 초기화 완료");
    }

    private void setupInitialVolume() {
        // 초기 볼륨 설정
        view.getVolumeSlider().setValue(currentVolume * 100);
        updateVolumeIcon(currentVolume * 100);
    }

    private void attachEventHandlers() {
        log.debug("이벤트 핸들러 연결 시작");
        
        // 재생 버튼 - null 체크 추가
        if (view.getPlayButton() != null) {
            view.getPlayButton().setOnAction(e -> {
                log.debug("재생 버튼 클릭됨");
                requestPlay();
            });
            log.debug("재생 버튼 이벤트 핸들러 연결 완료");
        } else {
            log.error("재생 버튼이 null입니다!");
        }

        // 일시정지 버튼
        if (view.getPauseButton() != null) {
            view.getPauseButton().setOnAction(e -> {
                log.debug("일시정지 버튼 클릭됨");
                requestPause();
            });
            log.debug("일시정지 버튼 이벤트 핸들러 연결 완료");
        } else {
            log.error("일시정지 버튼이 null입니다!");
        }

        // 정지 버튼
        if (view.getStopButton() != null) {
            view.getStopButton().setOnAction(e -> {
                log.debug("정지 버튼 클릭됨");
                requestStop();
            });
            log.debug("정지 버튼 이벤트 핸들러 연결 완료");
        } else {
            log.error("정지 버튼이 null입니다!");
        }

        // 이전 곡 버튼
        if (view.getPrevButton() != null) {
            view.getPrevButton().setOnAction(e -> {
                log.debug("이전 곡 버튼 클릭됨");
                requestPrevious();
            });
            log.debug("이전 곡 버튼 이벤트 핸들러 연결 완료");
        } else {
            log.error("이전 곡 버튼이 null입니다!");
        }

        // 다음 곡 버튼
        if (view.getNextButton() != null) {
            view.getNextButton().setOnAction(e -> {
                log.debug("다음 곡 버튼 클릭됨");
                requestNext();
            });
            log.debug("다음 곡 버튼 이벤트 핸들러 연결 완료");
        } else {
            log.error("다음 곡 버튼이 null입니다!");
        }

        // 셔플 버튼 (선택적)
        if (view.getShuffleButton() != null) {
            view.getShuffleButton().setOnAction(e -> {
                log.debug("셔플 버튼 클릭됨");
                toggleShuffle();
            });
        }

        // 반복 버튼 (선택적)
        if (view.getRepeatButton() != null) {
            view.getRepeatButton().setOnAction(e -> {
                log.debug("반복 버튼 클릭됨");
                toggleRepeat();
            });
        }
        
        log.debug("모든 이벤트 핸들러 연결 완료");
    }

    private void setupProgressSliderHandlers() {
        if (view.getProgressSlider() == null) {
            log.error("진행 바가 null입니다!");
            return;
        }

        // 진행 바 드래그 시작/종료 감지
        view.getProgressSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (wasChanging && !isChanging) {
                // 드래그 종료 시 탐색 이벤트 발행
                long seekPosition = (long) view.getProgressSlider().getValue();
                log.debug("진행 바 탐색: {}ms", seekPosition);
                
                if (publisher != null) {
                    publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                } else {
                    log.error("EventPublisher가 null입니다!");
                }
                
                isUserSeeking = false;
                stopSeekPreview();
                
            } else if (!wasChanging && isChanging) {
                // 드래그 시작
                isUserSeeking = true;
                startSeekPreview();
            }
        });

        // 진행 바 클릭으로 즉시 탐색
        view.getProgressSlider().setOnMouseClicked(event -> {
            if (!view.getProgressSlider().isValueChanging()) {
                double clickPosition = event.getX() / view.getProgressSlider().getWidth();
                long totalDuration = (long) view.getProgressSlider().getMax();
                long seekPosition = (long) (clickPosition * totalDuration);
                
                view.getProgressSlider().setValue(seekPosition);
                
                if (publisher != null) {
                    publisher.publish(new MediaControlEvent.RequestSeekEvent(seekPosition));
                }
                
                log.debug("진행 바 클릭 탐색: {}ms", seekPosition);
            }
        });
    }

    private void setupVolumeSliderHandlers() {
        if (view.getVolumeSlider() == null) {
            log.error("볼륨 슬라이더가 null입니다!");
            return;
        }

        view.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!view.getVolumeSlider().isValueChanging()) {
                float newVolume = newVal.floatValue() / 100f;
                setVolume(newVolume);
                updateVolumeIcon(newVal.doubleValue());
                log.debug("볼륨 변경: {}%", newVal.intValue());
            }
        });

        // 볼륨 슬라이더 클릭으로 즉시 변경
        view.getVolumeSlider().setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 더블클릭으로 음소거 토글
                toggleMute();
            } else {
                // 단일 클릭으로 볼륨 설정
                double clickPosition = event.getX() / view.getVolumeSlider().getWidth();
                float newVolume = (float) (clickPosition * 100);
                view.getVolumeSlider().setValue(newVolume);
                setVolume(newVolume / 100f);
                updateVolumeIcon(newVolume);
            }
        });

        // 마우스 휠로 볼륨 조정
        view.getVolumeSlider().setOnScroll(event -> {
            double delta = event.getDeltaY() > 0 ? 5 : -5;
            double newValue = Math.max(0, Math.min(100, view.getVolumeSlider().getValue() + delta));
            view.getVolumeSlider().setValue(newValue);
            setVolume((float) newValue / 100f);
            updateVolumeIcon(newValue);
            event.consume();
        });
    }

    private void setupAdvancedFeatures() {
        if (view.getProgressSlider() != null) {
            // 진행 바 마우스 휠로 탐색 (10초 단위)
            view.getProgressSlider().setOnScroll(event -> {
                long currentPos = (long) view.getProgressSlider().getValue();
                long delta = event.getDeltaY() > 0 ? 10000 : -10000; // 10초
                long newPos = Math.max(0, Math.min((long) view.getProgressSlider().getMax(), currentPos + delta));
                
                seekTo(newPos);
                event.consume();
            });
        }
    }

    private void setupKeyboardShortcuts() {
        // 키보드 단축키는 MainApplicationWindow에서 처리됨
        // 여기서는 뷰가 Scene에 추가된 후에 설정
        Platform.runLater(() -> {
            if (view.getScene() != null) {
                setupSceneKeyboardShortcuts();
            } else {
                // Scene이 아직 설정되지 않았다면 나중에 다시 시도
                view.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        setupSceneKeyboardShortcuts();
                    }
                });
            }
        });
    }

    private void setupSceneKeyboardShortcuts() {
        if (view.getScene() == null) return;

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
                case HOME:
                    seekToBeginning();
                    event.consume();
                    break;
                case END:
                    seekToEnd();
                    event.consume();
                    break;
            }
        });
    }

    // ========== 공개 제어 메서드들 ==========

    public void requestPlay() {
        log.info("재생 요청 처리 중...");
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다. 재생 요청을 처리할 수 없습니다.");
            showError("재생 오류: 이벤트 시스템이 초기화되지 않았습니다.");
            return;
        }
        
        try {
            if (currentMusic != null) {
                log.info("특정 음악 재생 요청: {}", currentMusic.getTitle());
                publisher.publish(new MediaControlEvent.RequestPlayEvent(currentMusic));
            } else {
                log.info("일반 재생 요청");
                publisher.publish(new MediaControlEvent.RequestPlayEvent());
            }
            
            updatePlaybackState(true, false, false);
            pulsePlayButton(); // 시각적 피드백
            
        } catch (Exception e) {
            log.error("재생 요청 중 오류 발생", e);
            showError("재생 요청 실패: " + e.getMessage());
        }
    }

    public void requestPause() {
        log.info("일시정지 요청 처리 중...");
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다.");
            showError("일시정지 오류: 이벤트 시스템이 초기화되지 않았습니다.");
            return;
        }
        
        try {
            publisher.publish(new MediaControlEvent.RequestPauseEvent());
            updatePlaybackState(false, true, false);
        } catch (Exception e) {
            log.error("일시정지 요청 중 오류 발생", e);
            showError("일시정지 요청 실패: " + e.getMessage());
        }
    }

    public void requestStop() {
        log.info("정지 요청 처리 중...");
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다.");
            showError("정지 오류: 이벤트 시스템이 초기화되지 않았습니다.");
            return;
        }
        
        try {
            publisher.publish(new MediaControlEvent.RequestStopEvent());
            updatePlaybackState(false, false, true);
        } catch (Exception e) {
            log.error("정지 요청 중 오류 발생", e);
            showError("정지 요청 실패: " + e.getMessage());
        }
    }

    public void requestNext() {
        log.info("다음 곡 요청 처리 중...");
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다.");
            return;
        }
        
        try {
            publisher.publish(new MediaControlEvent.RequestNextMusicEvent());
        } catch (Exception e) {
            log.error("다음 곡 요청 중 오류 발생", e);
        }
    }

    public void requestPrevious() {
        log.info("이전 곡 요청 처리 중...");
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다.");
            return;
        }
        
        try {
            publisher.publish(new MediaControlEvent.RequestPreviousMusicEvent());
        } catch (Exception e) {
            log.error("이전 곡 요청 중 오류 발생", e);
        }
    }

    public void seekTo(long positionMs) {
        log.debug("탐색 요청: {}ms", positionMs);
        
        if (publisher == null) {
            log.error("EventPublisher가 null입니다.");
            return;
        }
        
        try {
            publisher.publish(new MediaControlEvent.RequestSeekEvent(positionMs));
            
            // 즉시 UI 업데이트 (사용자 경험 향상)
            if (!isUserSeeking && view.getProgressSlider() != null) {
                view.getProgressSlider().setValue(positionMs);
            }
        } catch (Exception e) {
            log.error("탐색 요청 중 오류 발생", e);
        }
    }

    // ========== 고급 제어 메서드들 ==========

    public void seekForward() {
        if (view.getProgressSlider() == null) return;
        
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.min(currentPos + 10000, (long) view.getProgressSlider().getMax()); // 10초 앞으로
        seekTo(newPos);
    }

    public void seekBackward() {
        if (view.getProgressSlider() == null) return;
        
        long currentPos = (long) view.getProgressSlider().getValue();
        long newPos = Math.max(currentPos - 10000, 0); // 10초 뒤로
        seekTo(newPos);
    }

    public void seekToBeginning() {
        seekTo(0);
    }

    public void seekToEnd() {
        if (view.getProgressSlider() == null) return;
        
        long endPos = (long) view.getProgressSlider().getMax();
        seekTo(Math.max(0, endPos - 1000)); // 끝에서 1초 전
    }

    public void adjustVolume(float delta) {
        float newVolume = Math.max(0f, Math.min(1f, currentVolume + delta));
        setVolume(newVolume);
        
        // 시각적 피드백
        Platform.runLater(() -> {
            if (view.getVolumeSlider() != null) {
                view.getVolumeSlider().setValue(newVolume * 100);
                updateVolumeIcon(newVolume * 100);
            }
        });
        
        log.debug("볼륨 조정: {}%", (int) (newVolume * 100));
    }

    public void setVolume(float volume) {
        float validVolume = Math.max(0f, Math.min(1f, volume));
        currentVolume = validVolume;
        
        // 음소거 상태가 아니라면 실제 볼륨 적용
        if (!isMuted) {
            // TODO: 볼륨 변경 이벤트 발행 (이벤트 클래스 추가 시 구현)
            // if (publisher != null) {
            //     publisher.publish(new MediaControlEvent.RequestVolumeChangeEvent(validVolume));
            // }
        }
        
        log.debug("볼륨 설정: {}%", (int) (validVolume * 100));
    }

    public void toggleMute() {
        if (view.getVolumeSlider() == null) return;
        
        if (isMuted) {
            // 음소거 해제
            isMuted = false;
            currentVolume = lastVolumeBeforeMute;
            view.getVolumeSlider().setValue(currentVolume * 100);
            setVolume(currentVolume);
            updateVolumeIcon(currentVolume * 100);
            log.debug("음소거 해제: {}%", (int) (currentVolume * 100));
        } else {
            // 음소거 설정
            lastVolumeBeforeMute = currentVolume;
            isMuted = true;
            view.getVolumeSlider().setValue(0);
            setVolume(0);
            updateVolumeIcon(0);
            log.debug("음소거 활성화");
        }
        
        // TODO: 음소거 토글 이벤트 발행
        // if (publisher != null) {
        //     publisher.publish(new MediaControlEvent.RequestMuteToggleEvent(isMuted));
        // }
    }

    private void toggleShuffle() {
        // TODO: 셔플 토글 이벤트 구현
        log.debug("셔플 토글 요청");
        // if (publisher != null) {
        //     publisher.publish(new MediaControlEvent.RequestShuffleToggleEvent());
        // }
    }

    private void toggleRepeat() {
        // TODO: 반복 토글 이벤트 구현
        log.debug("반복 토글 요청");
        // if (publisher != null) {
        //     publisher.publish(new MediaControlEvent.RequestRepeatToggleEvent());
        // }
    }

    // ========== 탐색 미리보기 기능 ==========
    
    private void startSeekPreview() {
        if (seekPreviewTimer != null) {
            seekPreviewTimer.stop();
        }
        
        isSeekPreviewActive = true;
        
        // 탐색 미리보기 - 드래그 중에 시간 표시 업데이트
        seekPreviewTimer = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            if (isUserSeeking && view.getProgressSlider() != null) {
                long previewPosition = (long) view.getProgressSlider().getValue();
                // 현재는 별도 메서드가 없으므로 생략
                // updateTimeLabelsOnly(previewPosition);
            }
        }));
        seekPreviewTimer.setCycleCount(Timeline.INDEFINITE);
        seekPreviewTimer.play();
    }

    private void stopSeekPreview() {
        if (seekPreviewTimer != null) {
            seekPreviewTimer.stop();
        }
        isSeekPreviewActive = false;
    }

    // ========== 상태 업데이트 메서드들 ==========

    private void updatePlaybackState(boolean isPlaying, boolean isPaused, boolean isStopped) {
        Platform.runLater(() -> {
            if (view != null) {
                view.updatePlaybackState(isPlaying, isPaused, isStopped);
            }
            
            isPlaybackActive = isPlaying;
            
            // 재생 상태에 따른 추가 UI 업데이트
            if (isPlaying) {
                enableControls(true);
            } else if (isStopped && view.getProgressSlider() != null) {
                // 정지 시 진행바 초기화
                view.getProgressSlider().setValue(0);
            }
        });
    }

    public void updateCurrentMusic(MusicInfo music) {
        this.currentMusic = music;
        Platform.runLater(() -> {
            if (view != null) {
                view.updateMusicInfo(music);
                
                if (music != null && view.getProgressSlider() != null) {
                    // 새 곡이 로드되면 진행바 최대값 설정
                    view.getProgressSlider().setMax(music.getDurationMillis());
                    view.getProgressSlider().setValue(0);
                    log.debug("새 곡 설정: {} ({}ms)", music.getTitle(), music.getDurationMillis());
                }
            }
        });
    }

    public void updateProgress(long currentMs, long totalMs) {
        Platform.runLater(() -> {
            if (!isUserSeeking && !isSeekPreviewActive && view != null) {
                view.updateProgress(currentMs, totalMs);
            }
        });
    }

    private void updateVolumeIcon(double volume) {
        // PlayerControlsView에 볼륨 아이콘 업데이트 메서드가 있다고 가정
        // 실제 구현에서는 view에 이런 메서드를 추가해야 함
    }

    private void enableControls(boolean enabled) {
        Platform.runLater(() -> {
            if (view != null) {
                view.enableControls(enabled);
            }
        });
    }

    private void showError(String errorMessage) {
        Platform.runLater(() -> {
            if (view != null) {
                view.showError(errorMessage);
            }
            log.error("UI 오류 표시: {}", errorMessage);
        });
    }

    private void pulsePlayButton() {
        Platform.runLater(() -> {
            if (view != null) {
                view.pulsePlayButton();
            }
        });
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
        return currentVolume;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public long getCurrentPosition() {
        return view.getProgressSlider() != null ? (long) view.getProgressSlider().getValue() : 0;
    }

    public long getTotalDuration() {
        return view.getProgressSlider() != null ? (long) view.getProgressSlider().getMax() : 0;
    }

    public double getProgress() {
        long total = getTotalDuration();
        return total > 0 ? (double) getCurrentPosition() / total : 0.0;
    }

    // ========== 정리 ==========

    public void dispose() {
        log.debug("PlaybackController 정리 중...");
        
        try {
            // 타이머 정리
            if (seekPreviewTimer != null) {
                seekPreviewTimer.stop();
                seekPreviewTimer = null;
            }
            
            // 상태 초기화
            isUserSeeking = false;
            isSeekPreviewActive = false;
            
            // 뷰 정리
            Platform.runLater(() -> {
                if (view != null) {
                    view.enableControls(false);
                }
            });
            
        } catch (Exception e) {
            log.error("PlaybackController 정리 중 오류", e);
        }
        
        log.debug("PlaybackController 정리 완료");
    }

    // ========== 접근자 메서드들 ==========
    
    public PlayerControlsView getView() { return view; }
    public EventPublisher getEventPublisher() { return publisher; }
}