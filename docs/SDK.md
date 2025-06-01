## SDK 가이드

### 1. 주요 구성 요소

SDK는 다음과 같은 주요 구성 요소로 이루어져 있습니다:

*   **Annotations (`sdk.annotation`)**: 모듈 및 이벤트 리스너를 식별합니다.
*   **Events (`sdk.event`)**: 모듈 간 통신을 위한 표준화된 메시지입니다.
*   **Models (`sdk.model`)**: 애플리케이션 전체에서 사용되는 데이터 구조(DTO)입니다.
*   **Module Abstractions (`sdk.module`)**: 모든 기능 모듈의 기본 틀과 생명주기 관리를 제공합니다.
*   **Logging (`sdk.log`)**: 일관된 로깅 방식을 제공합니다.

### 2. Annotations

#### 2.1. `@Module`

`ac.cwnu.synctune.sdk.annotation.Module`

*   **설명**: 클래스가 SyncTune 애플리케이션의 모듈임을 나타냅니다. Core 모듈은 이 어노테이션을 사용하여 클래스패스에서 모듈들을 스캔하고 로드합니다.
*   **속성**:
    *   `name` (String, optional): 모듈의 이름을 지정합니다. 지정하지 않으면 클래스 이름이 사용됩니다.
    *   `version` (String, optional): 모듈의 버전을 지정합니다. 기본값은 "1.0"입니다.
*   **사용 예시**:
    ```java
    @Module(name = "Player", version = "1.0.0")
    public class PlayerModule extends SyncTuneModule {
        // ... 모듈 구현 ...
    }
    ```

#### 2.2. `@EventListener`

`ac.cwnu.synctune.sdk.annotation.EventListener`

*   **설명**: 메서드가 특정 이벤트를 수신하여 처리하는 리스너임을 나타냅니다. Core 모듈의 EventBus는 이 어노테이션이 붙은 메서드를 자동으로 등록합니다.
*   **규칙**:
    *   메서드는 반드시 하나의 파라미터를 가져야 합니다.
    *   파라미터의 타입은 `ac.cwnu.synctune.sdk.event.BaseEvent`를 상속받는 특정 이벤트 클래스여야 합니다.
*   **사용 예시**:
    ```java
    @Module(name = "Player", version = "1.0.0")
    public class PlayerModule extends SyncTuneModule {
        // ...

        @EventListener
        public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
            log.debug("[{}] Received MainWindowClosedEvent: {}", getModuleName(), event);
            // 창 닫힘 관련 처리 로직
        }

        // ...
    }
    ```

### 3. Modules

#### 3.1. `SyncTuneModule` (추상 클래스)

`ac.cwnu.synctune.sdk.module.SyncTuneModule`

*   **설명**: 애플리케이션의 모든 기능 모듈(Player, Lyrics, UI 등)이 상속해야 하는 기본 클래스입니다. 모듈의 생명주기 관리(시작, 중지)와 이벤트 발행 기능을 제공합니다.
*   **주요 메서드**:
    *   `public abstract void start(EventPublisher publisher)`: 모듈이 로드된 후 Core 모듈에 의해 호출됩니다. `publisher`는 모듈이 이벤트를 발행할 수 있도록 Core로부터 주입받습니다. 이 메서드 내에서 `super.eventPublisher = publisher;`를 호출하여 내부 `eventPublisher`를 설정해야 합니다.
        *   **주의사항: `start()` 메서드 내에서의 장시간 동기 작업**
            `CoreModule`의 `ModuleLoader`는 각 모듈의 `start()` 메서드를 **순차적으로 그리고 동기적으로 호출**합니다. 따라서 특정 모듈의 `start()` 메서드 내부에서 파일 전체 스캔, 네트워크 요청 대기 등 시간이 오래 걸리는 작업을 동기적으로 수행하면, **해당 작업이 완료될 때까지 다른 모듈들의 `start()` 메서드 호출이 지연**됩니다. 이는 전체 애플리케이션의 시작 시간을 크게 늘리고, 경우에 따라 UI 반응성 문제를 일으킬 수 있습니다.
        *   **권장 방안: 비동기 처리**
            `start()` 메서드 내에서 시간이 오래 걸리거나 지속적으로 멈추지 않고 수행하는 작업(예시. 미디어 스캔 작업, 타 이벤트 대기, 무한 루프)은 **별도의 백그라운드 스레드를 생성하여 비동기적으로 처리**하는 것이 좋습니다. `start()` 메서드 자체는 스레드를 시작시키고 빠르게 반환되어야 다른 모듈의 초기화에 영향을 주지 않아야합니다. 작업의 진행 상태나 결과는 이벤트를 통해 다른 모듈에 알릴 수 있습니다.
            ```java
            // 예시: Module의 start() 메서드에서 비동기 작업 수행
            @Override
            public void start(EventPublisher publisher) {
                super.eventPublisher = publisher; // 이벤트 발행기 설정
                log.info("[{}] Module starting...", getModuleName());

                // 시간이 오래 걸리는 작업을 위한 새 스레드 생성 및 시작
                Thread longRunningTaskThread = new Thread(() -> {
                    try {
                        log.info("[{}] Background task started.", getModuleName());
                        publish(new SystemEvent.CustomTaskStartedEvent(getModuleName() + " background task")); // 작업 시작 알림 (예시)

                        // === 여기에 시간이 오래 걸리는 실제 작업 로직 ===
                        // 예: Files.walk(Paths.get("...")); 등
                        Thread.sleep(5000); // 5초 대기 작업 (예시)

                        publish(new SystemEvent.CustomTaskCompletedEvent(getModuleName() + " background task")); // 작업 완료 알림 (예시)
                        log.info("[{}] Background task finished successfully.", getModuleName());
                    } catch (InterruptedException e) {
                        log.warn("[{}] Background task interrupted.", getModuleName(), e);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.error("[{}] Error in background task.", getModuleName(), e);
                        // 필요시 ErrorEvent 발행
                        publish(new ErrorEvent("Background task failed in " + getModuleName(), e, false));
                    }
                });
                longRunningTaskThread.setName(getModuleName() + "-BackgroundTaskThread");
                longRunningTaskThread.setDaemon(true); // 주 애플리케이션 종료 시 스레드도 함께 종료 (선택 사항)
                longRunningTaskThread.start();

                log.info("[{}] Module start method finished (background task running).", getModuleName());
                // start() 메서드는 여기서 빠르게 반환됩니다.
            }
            ```
    *   `public void stop()`: 모듈이 종료될 때 호출됩니다. 리소스 정리 등의 작업을 수행합니다. (기본 구현은 비어 있음)
    *   `public String getModuleName()`: 모듈의 이름을 반환합니다. `@Module` 어노테이션의 `name` 속성 값을 우선 사용하며, 없으면 클래스의 간단한 이름을 반환합니다.
    *   `protected void publish(BaseEvent event)`: `start` 메서드에서 전달받은 `eventPublisher`를 사용하여 이벤트를 발행하는 편의 메서드입니다.
*   **사용 예시**:
    ```java
    @Module(name = "Lyrics", version = "1.0.0")
    public class LyricsModule extends SyncTuneModule {
        private static final Logger log = LogManager.getLogger(LyricsModule.class);

        @Override
        public void start(EventPublisher publisher) {
            super.eventPublisher = publisher; // 매우 중요! 이벤트 발행기를 설정합니다.
            log.info("LyricsModule이 시작되었습니다.");
            // 모듈 초기화 로직 (예: 리스너 등록, 내부 상태 설정 등)
            // 만약 여기서 무거운 작업을 해야 한다면 위 "비동기 처리" 권장 방안을 따릅니다.
        }

        @Override
        public void stop() {
            log.info("LyricsModule이 종료되었습니다.");
            // 모듈 정리 로직
        }

        // 예시: 특정 조건에서 이벤트 발행
        public void lyricsFoundForSong(String musicPath, String lrcPath) {
            publish(new ac.cwnu.synctune.sdk.event.LyricsEvent.LyricsFoundEvent(musicPath, lrcPath));
        }
    }
    ```

#### 3.2. `ModuleLifecycleListener` (인터페이스)

`ac.cwnu.synctune.sdk.module.ModuleLifecycleListener`

*   **설명**: 모듈의 생명주기(로드 전/후, 시작 전/후, 중지 전/후, 언로드 전/후) 변경 시 알림을 받기 위한 인터페이스입니다. Core 모듈 자체가 이 리스너를 구현하여 다른 모듈들의 생명주기를 로깅합니다. 필요에 따라 다른 모듈도 이를 구현하여 특정 모듈의 상태 변화에 반응할 수 있습니다.
*   **메서드**: `beforeModuleLoad`, `afterModuleLoad`, `beforeModuleStart`, `afterModuleStart`, `beforeModuleStop`, `afterModuleStop`, `beforeModuleUnload`, `afterModuleUnload`. (모두 default 메서드)

### 4. Events

`ac.cwnu.synctune.sdk.event.*`

모듈 간 통신은 이벤트를 통해 이루어집니다. 모든 이벤트는 `BaseEvent`를 상속받으며, 발생 시간을 자동으로 기록합니다.

#### 4.1. `BaseEvent` (추상 클래스)

*   모든 이벤트의 최상위 부모 클래스입니다. `LocalDateTime timestamp` 필드를 가집니다.

#### 4.2. `EventPublisher` (인터페이스)

*   `void publish(BaseEvent event)` 메서드를 정의합니다. `CoreModule`이 이 인터페이스를 구현하며, 각 `SyncTuneModule`의 `start` 메서드를 통해 `EventPublisher` 인스턴스를 전달받습니다. 모듈은 이 인스턴스를 사용하여 이벤트를 시스템 전체에 발행할 수 있습니다.

#### 4.3. 주요 이벤트 네임스페이스 (예시)

각 이벤트는 특정 상황을 나타내며, 관련된 데이터를 포함할 수 있습니다. `docs/Design.md` 파일에 정의된 다양한 이벤트들이 있으며, 대표적인 예시는 다음과 같습니다:

*   **`ErrorEvent`**: 오류 발생 시 사용됩니다. 메시지, 예외 객체, 치명적 오류 여부(`isFatal`)를 포함합니다.
    ```java
    // 오류 발생 시
    publish(new ErrorEvent("파일 처리 중 심각한 오류 발생", e, true));
    ```

*   **`FileScanEvent`**: 파일 스캔 관련 이벤트.
    *   `ScanStartedEvent(String directoryPath)`: 스캔 시작.
    *   `FileFoundEvent(File foundFile)`: 파일 발견.
    *   `ScanCompletedEvent(String directoryPath, int totalFilesFound)`: 스캔 완료.

*   **`LyricsEvent`**: 가사 처리 관련 이벤트.
    *   `LyricsFoundEvent(String musicFilePath, String lrcFilePath)`: 가사 발견.
    *   `NextLyricsEvent(String lyricLine, long startTimeMillis)`: 다음 가사 라인 알림.

*   **`MediaControlEvent`**: 미디어 재생 제어 "요청" 이벤트. UI 모듈 등이 발행하여 Player 모듈에 작업을 요청합니다.
    *   `RequestPlayEvent(MusicInfo musicToPlay)`: 재생 요청.
    *   `RequestPauseEvent()`: 일시정지 요청.
    *   `RequestSeekEvent(long positionMillis)`: 탐색 요청.

*   **`PlaybackStatusEvent`**: 미디어 재생 "상태 변경" 이벤트. Player 모듈 등이 발행하여 현재 재생 상태를 알립니다.
    *   `PlaybackStartedEvent(MusicInfo currentMusic)`: 재생 시작됨.
    *   `PlaybackPausedEvent()`: 일시정지됨.
    *   `PlaybackProgressUpdateEvent(long currentTimeMillis, long totalTimeMillis)`: 재생 시간 업데이트.

*   **`SystemEvent`**: 시스템 수준 이벤트.
    *   `ApplicationReadyEvent()`: 모든 모듈 로드 및 시작 완료.
    *   `ApplicationShutdownEvent()`: 애플리케이션 종료 시작.

#### 4.4. 이벤트 발행 및 구독 방법

*   **발행 (Publishing)**: `SyncTuneModule` 내에서 `publish(BaseEvent event)` 메서드를 사용합니다.
    ```java
    // PlayerModule 내부에서 곡이 변경되었을 때
    MusicInfo newTrack = ...;
    publish(new PlaybackStatusEvent.MusicChangedEvent(newTrack));
    ```

*   **구독 (Subscribing)**: 이벤트를 수신하고자 하는 모듈의 메서드에 `@EventListener` 어노테이션을 붙이고, 해당 이벤트 타입의 파라미터를 선언합니다.
    ```java
    // UIModule 내부에서 곡 변경 이벤트를 받아 UI 업데이트
    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        MusicInfo newMusic = event.getNewMusic();
        // UI에 새 곡 정보 표시 로직
        updateTrackInfoDisplay(newMusic.getTitle(), newMusic.getArtist());
    }
    ```

### 5. Models (Data Transfer Objects - DTOs)

`ac.cwnu.synctune.sdk.model.*`

애플리케이션 모듈 간에 데이터를 전달하거나 상태를 나타내는 데 사용되는 표준 객체들입니다. 모든 모델은 `final` 클래스이며, 주로 불변(immutable) 객체로 설계되어 안정성을 높입니다.

*   **`LrcLine`**: LRC 가사 한 줄 (시간, 텍스트).
    ```java
    LrcLine line = new LrcLine(15000L, "가사 내용입니다."); // 15초에 해당하는 가사
    ```

*   **`ModuleInfo`**: 모듈 정보 (이름, 버전, 클래스). Core 모듈 내부에서 주로 사용됩니다.

*   **`MusicInfo`**: 음악 파일 메타데이터 (제목, 아티스트, 앨범, 파일 경로, 길이, LRC 경로 등).
    ```java
    MusicInfo song = new MusicInfo("노래 제목", "아티스트", "앨범", "/path/to/music.mp3", 180000L, "/path/to/lyrics.lrc");
    ```

*   **`Playlist`**: 플레이리스트 정보 (이름, 곡 목록).
    ```java
    List<MusicInfo> songs = new ArrayList<>();
    songs.add(song1);
    songs.add(song2);
    Playlist myPlaylist = new Playlist("나의 최애곡들", songs);
    ```

### 6. Logging

`ac.cwnu.synctune.sdk.log.LogManager`

*   **설명**: SLF4J 로거 인스턴스를 쉽게 가져올 수 있도록 하는 유틸리티 클래스입니다. 모든 모듈에서 일관된 방식으로 로거를 생성하여 사용합니다.
*   **사용법**:
    ```java
    public class MyCustomClassInAModule {
        private static final Logger log = LogManager.getLogger(MyCustomClassInAModule.class);

        public void performAction() {
            log.debug("Action 수행 시작...");
            try {
                // ... 로직 ...
                log.info("Action 성공적으로 완료.");
            } catch (Exception e) {
                log.error("Action 수행 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }
    ```
    로그 레벨(DEBUG, INFO, WARN, ERROR 등)과 포맷은 프로젝트 루트의 `logback.xml` 또는 각 모듈의 `resources` 내 `logback.xml` 설정을 따릅니다. (현재는 루트의 `src/main/resources/logback.xml` 과 `bin/main/logback.xml` 이 존재하며, 최종적으로 실행 시점의 클래스패스에 따라 하나의 설정이 적용됩니다.)

### 7. 모듈 간 상호작용 예시 (워크플로우)

1.  **UI 모듈**: 사용자가 '재생' 버튼 클릭.
    *   UI 모듈은 `MediaControlEvent.RequestPlayEvent`를 `publish`합니다. (특정 곡 정보 포함 가능)
2.  **Player 모듈**: `@EventListener`로 `MediaControlEvent.RequestPlayEvent`를 수신.
    *   요청된 곡 또는 현재 선택된 곡 재생 시작.
    *   재생이 성공적으로 시작되면 `PlaybackStatusEvent.PlaybackStartedEvent`를 `publish`합니다. (현재 곡 정보 포함)
    *   주기적으로 `PlaybackStatusEvent.PlaybackProgressUpdateEvent`를 `publish`합니다.
3.  **Lyrics 모듈**:
    *   `@EventListener`로 `PlaybackStartedEvent`를 수신하여 해당 곡의 가사 로드/파싱 시도.
    *   `@EventListener`로 `PlaybackProgressUpdateEvent`를 수신하여 현재 재생 시간에 맞는 가사 탐색.
    *   일치하는 가사가 있으면 `LyricsEvent.NextLyricsEvent`를 `publish`합니다.
4.  **UI 모듈**:
    *   `@EventListener`로 `PlaybackStartedEvent`를 수신하여 UI에 곡 정보(제목, 아티스트 등) 업데이트.
    *   `@EventListener`로 `PlaybackProgressUpdateEvent`를 수신하여 재생 진행 바 업데이트.
    *   `@EventListener`로 `LyricsEvent.NextLyricsEvent`를 수신하여 화면에 현재 가사 표시.
5.  **모든 모듈**:
    *   오류 발생 시 `ErrorEvent`를 `publish`할 수 있습니다.
    *   `CoreModule`은 `ErrorEvent`를 기본적으로 수신하여 로깅하며, 치명적 오류(`isFatal=true`)인 경우 애플리케이션 종료를 시도합니다.

`stub` 모듈 (`stub/`)의 `StubModule.java`는 SDK의 다양한 이벤트를 수신하고 발행하는 방법에 대한 예시를 제공하므로 참고하시기 바랍니다.