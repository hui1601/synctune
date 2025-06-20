# SyncTune 모듈 설계 문서
## 역할 분담
팀원: 동헌희, 김민재, 김대영, 임민수

* 전체 설계 및 문서화: 동헌희
* 주제 발표 및 모듈 구조 설계: 동헌희
* GitHub Actions 설정 및 CI/CD 파이프라인 구축: 동헌희
* 프로젝트 최종 발표: (미정)
* `sdk` 모듈 개발: 동헌희
* `core` 모듈 개발: 동헌희
* `player` 모듈 개발: 김민재
* `lyrics` 모듈 개발: 김대영
* `ui` 모듈 개발: 임민수

## 프로젝트 구조

해당 프로젝트는 각 팀원별로 모듈을 나누어 개발하는 구조로 설계되었습니다.

각 모듈은 `SyncTuneModule`을 상속받아 구현하며, 공통 SDK를 사용하여 이벤트 기반으로 상호작용합니다.

모듈은 `@Module` 어노테이션을 통해 식별되며, Core 모듈이 이를 스캔하고 로드합니다.

각 모듈이 독립적으로 개발될 수 있도록 하여, 각자의 기여분과 작업을 명확히 하고, 느슨한 결합(Loose Coupling)으로 최대한 유연성을 제공합니다.

Publish-Subscribe 패턴을 사용하여 모듈 간의 통신을 처리하며, 이벤트를 통해 서로의 상태 변경이나 요청을 알리는 구조로 설계했습니다.

즉, 종합적으로 디버깅과 유지보수, 확장성이 높은 구조로 설계했습니다.

GitHub Actions를 통해 각 모듈이 독립적으로 빌드되도록 설정했습니다.

또한, Git의 `.gitignore` 파일을 통해 불필요한 파일이 커밋되지 않도록 관리했습니다.

GitHub Actions에서 `.gitignore`에 정의된 파일이 추가된 경우 모든 빌드가 실패하도록 해, 이슈를 조기에 발견할 수 있도록 했습니다.

## 1. `sdk` Module
담당: 동헌희
* `ac.cwnu.synctune.sdk`
    * `annotation`
        * `Module.java`<br>모듈 클래스를 식별하기 위한 어노테이션. 모듈 이름과 버전을 속성으로 가짐. Core가 스캔할 대상.
        * `EventListener.java`<br>이벤트 리스너 메서드를 식별하기 위한 어노테이션.
    * `event`
        * `BaseEvent.java`<br>모든 이벤트의 부모 추상 클래스. 이벤트 발생 시간을 기록.
        * `ErrorEvent.java`<br>예외 및 오류 발생 시 사용되는 이벤트. 오류 메시지, 예외 객체, 치명적 오류 여부 포함.
        * `EventPublisher.java`<br>이벤트를 발행하는 기능을 정의한 인터페이스. 모듈이 이벤트를 시스템 전체에 알릴 때 사용.
        * `FileScanEvent.java`<br>파일 스캔 관련 이벤트 네임스페이스.
            * `ScanStartedEvent`<br>지정된 디렉토리에 대한 파일 스캔 시작 알림.
            * `FileFoundEvent`<br>스캔 중 개별 파일을 발견했을 때 발생.
            * `ScanCompletedEvent`<br>파일 스캔 완료 알림. 스캔된 디렉토리 경로와 찾은 파일 총 개수 포함.
            * `ScanErrorEvent`<br>파일 스캔 중 오류 발생 알림.
        * `LyricsEvent.java`<br>가사 처리 관련 이벤트 네임스페이스.
            * `LyricsFoundEvent`<br>음악 파일에 대한 가사 파일 발견 알림.
            * `LyricsNotFoundEvent`<br>음악 파일에 대한 가사 파일을 찾지 못했을 때 알림.
            * `NextLyricsEvent`<br>현재 재생 시간에 맞는 다음 가사 라인 알림. 가사 내용과 시작 시간 포함.
            * `LyricsParseCompleteEvent`<br>LRC 파일 파싱 완료 알림. 성공 여부 포함.
        * `MediaControlEvent.java`<br>미디어 재생 제어 "요청" 관련 이벤트 네임스페이스.
            * `RequestPlayEvent`<br>재생 요청. 특정 곡 정보 포함 가능.
            * `RequestPauseEvent`<br>일시 정지 요청.
            * `RequestStopEvent`<br>정지 요청.
            * `RequestNextMusicEvent`<br>다음 곡 재생 요청.
            * `RequestPreviousMusicEvent`<br>이전 곡 재생 요청.
            * `RequestSeekEvent`<br>특정 시간으로 탐색 요청. 밀리초 단위 시간 포함.
        * `MediaInfoEvent.java`<br>미디어 정보 스캔 및 메타데이터 관련 이벤트 네임스페이스.
            * `MediaScanStartedEvent`<br>미디어 스캔 시작 알림.
            * `MediaScanProgressEvent`<br>미디어 스캔 진행 상태 알림. (스캔한 파일 수, 전체 파일 수)
            * `MediaScanCompletedEvent`<br>미디어 스캔 완료 알림. 스캔된 `MusicInfo` 리스트 포함.
            * `MetadataUpdatedEvent`<br>음악 메타데이터 업데이트 알림. (예: LRC 파일 정보 추가)
        * `PlaybackStatusEvent.java`<br>미디어 재생 "상태 변경" 관련 이벤트 네임스페이스.
            * `PlaybackStartedEvent`<br>재생 시작 알림. 현재 곡 정보 포함.
            * `PlaybackPausedEvent`<br>일시 정지됨 알림.
            * `PlaybackStoppedEvent`<br>정지됨 알림.
            * `MusicChangedEvent`<br>재생 곡 변경 알림. 새 곡 정보 포함.
            * `PlaybackProgressUpdateEvent`<br>재생 시간 업데이트 알림. 현재 재생 시간, 전체 길이 포함.
        * `PlayerUIEvent.java`<br>플레이어 UI 관련 이벤트를 위한 네임스페이스 클래스.
            * `MainWindowClosedEvent`<br>플레이어 메인 UI 창 닫힘 이벤트.
            * `MainWindowRestoredEvent`<br>플레이어 메인 UI 창 복구(최소화 해제 등) 이벤트.
        * `PlaylistEvent.java`<br>플레이리스트 관련 이벤트 네임스페이스.
            * `PlaylistCreatedEvent`<br>플레이리스트 생성 알림.
            * `PlaylistDeletedEvent`<br>플레이리스트 삭제 알림.
            * `MusicAddedToPlaylistEvent`<br>플레이리스트에 곡 추가 알림.
            * `MusicRemovedFromPlaylistEvent`<br>플레이리스트에서 곡 제거 알림.
            * `PlaylistOrderChangedEvent`<br>플레이리스트 순서 변경 알림.
            * `AllPlaylistsLoadedEvent`<br>모든 플레이리스트 로드 완료 알림.
        * `SystemEvent.java`<br>시스템 레벨 이벤트 네임스페이스 (예: 애플리케이션 시작, 종료).
            * `ApplicationReadyEvent`<br>애플리케이션 준비 완료 (모든 모듈 로드 및 시작 완료) 알림.
            * `ApplicationShutdownEvent`<br>애플리케이션 종료 시작/진행 알림.
            * `RequestApplicationShutdownEvent`<br>애플리케이션 종료 요청 이벤트. 해당 이벤트를 발행하면 core 모듈이 이를 수신하고 안전하게 종료를 시도합니다.
    * `log`
        * `LogManager.java`<br>SLF4J Logger 인스턴스를 생성하고 관리하는 유틸리티 클래스.
    * `model`
        * `LrcLine.java`<br>LRC 가사 한 줄을 나타내는 DTO. 시간(밀리초)과 가사 텍스트 포함. `Comparable` 구현.
        * `ModuleInfo.java`<br>스캔된 모듈의 정보를 담는 DTO. 모듈 이름, 버전, 클래스 정보 포함.
        * `MusicInfo.java`<br>음악 파일 메타데이터(제목, 아티스트, 앨범, 파일 경로, 길이, LRC 경로 등) DTO. 파일 경로 기반으로 동등성 비교.
        * `Playlist.java`<br>플레이리스트(이름, 곡 목록) DTO. 이름 기반으로 동등성 비교.
    * `module`
        * `SyncTuneModule.java`<br>모든 모듈이 상속해야 하는 추상 클래스. `start()`, `stop()` 추상/일반 메서드 및 `eventPublisher`를 통한 이벤트 발행 편의 메서드(`publish`) 제공. 모듈 이름을 `@Module` 어노테이션 또는 클래스명에서 가져옴.
        * `ModuleLifecycleListener.java`<br>모듈 생명주기(로드 전/후, 시작 전/후, 중지 전/후, 언로드 전/후) 이벤트를 수신하기 위한 리스너 인터페이스.

## 2. `core` Module
담당: 동헌희
* `ac.cwnu.synctune.core`
    * `CoreModule.java`<br>애플리케이션의 핵심 모듈. `SyncTuneModule` 구현, `ModuleLifecycleListener`, `EventPublisher` 구현. 모듈 스캔 및 로딩, 애플리케이션 생명주기(시작, 종료, 셧다운 훅) 관리. `ErrorEvent`를 구독하여 치명적 오류 처리.
    * `EventBus.java`<br>이벤트 발행 및 구독 관리. `@EventListener` 어노테이션 기반으로 리스너 등록/해제. 동기/비동기 이벤트 처리 지원 (생성자 파라미터로 제어, 기본은 동기). 리스너 실행 중 예외 발생 시 `ErrorEvent` 발행.
    * `initializer`
        * `ModuleScanner.java`<br>클래스패스에서 지정된 기본 패키지 내 `@Module` 어노테이션이 붙은 `SyncTuneModule` 구현 클래스 스캔. `CoreModule` 자체는 결과에서 제외.
        * `ModuleLoader.java`<br>스캔된 모듈 클래스들을 인스턴스화하고 초기화 (`start()`) 및 종료 (`stop()`). 모듈 생명주기 이벤트를 `ModuleLifecycleListener`에게 알림. 모듈 로딩/시작/중지 시 발생하는 오류를 `ErrorEvent`로 발행.
    * `error`
        * `ModuleInitializationException.java`<br>모듈 초기화 과정에서 심각한 오류 발생 시 사용되는 `RuntimeException`.
        * `GlobalExceptionHandler.java`<br>처리되지 않은 예외(Uncaught Exception)를 중앙에서 처리. 모든 스레드의 예외 로깅. `VirtualMachineError` 등 치명적 예외 발생 시 `FatalErrorReporter` 호출, 그 외에는 `ErrorEvent` 발행. `Thread.setDefaultUncaughtExceptionHandler`로 등록.
        * `FatalErrorReporter.java`<br>심각한(Fatal) 오류 발생 시 이를 로깅하고, `CoreModule`의 `stop()`을 호출하여 안전한 종료를 시도한 후, `System.exit(1)`로 애플리케이션 강제 종료.
    * `logging`
        * `EventLogger.java`<br>모든 `BaseEvent`를 구독하여 DEBUG 레벨로 로깅하는 기본 이벤트 리스너.

## 3. `player` Module
담당: 김민재
* `ac.cwnu.synctune.player`
    * `PlayerModule.java`<br>`SyncTuneModule` 구현, 이벤트 리스너 등록.<br>예를 들어 `@EventListener public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event)` 와 같이 SDK의 `PlayerUIEvent` 내부 클래스 구독.
    * `playback`<br>음악 재생 핵심 로직
        * `AudioEngine.java`<br>실제 오디오 파일 재생/정지/탐색 담당 - JLayer, JavaFX MediaPlayer 등 라이브러리 편한거 써요
        * `PlaybackStateManager.java`<br>현재 재생 상태 관리: 재생중, 일시정지, 정지 등
    * `playlist` <br>재생목록 데이터 관리 및 로직
        * `PlaylistManager.java`<br>플레이리스트 CRUD, 현재 곡 추적
        * `TrackShuffler.java`<br>곡 순서 섞기 등 부가 기능
    * `metadata`<br>음악 파일 메타데이터 처리
        * `MetadataExtractor.java`<br>음악 파일에서 ID3 태그 등 정보 추출
        * `CoverArtService.java`<br>앨범 커버 이미지 로드/캐싱
    * `scanner`<br>음악 파일 스캐닝
        * `MusicFileScanner.java`<br>지정된 디렉토리에서 음악 파일 탐색
        * `FileDiscoveryReporter.java`<br>발견된 파일 정보를 `FileScanEvent`로 발행 (또는 `MediaInfoEvent` 사용)

## 4. `lyrics` Module
담당: 김대영
* `ac.cwnu.synctune.lyrics`
    * `LyricsModule.java`<br>`SyncTuneModule` 구현, 이벤트 리스너 등록
    * `parser`<br>가사 파일 파싱
        * `LrcParser.java`<br>.lrc 파일 형식 파싱
        * `LrcDataValidator.java`<br>파싱된 LRC 데이터 유효성 검사
    * `synchronizer`<br>재생 시간과 가사 동기화
        * `LyricsTimelineMatcher.java`<br>현재 재생 시간에 맞는 가사 라인 검색
        * `PlaybackTimeReceiver.java`<br>`PlaybackStatusEvent`를 구독하여 현재 재생 시간 업데이트
    * `provider`<br>동기화된 가사 정보 제공
        * `CurrentLyricsProvider.java`<br>현재 시간에 맞는 가사를 `LyricsEvent`로 발행
        * `LyricsCache.java`<br>파싱된 LRC 파일 캐싱하여 재파싱 방지

## 5. `ui` Module
담당: 임민수
* `ac.cwnu.synctune.ui`<br>JavaFX, Swing 등 UI 프레임워크 사용<br>자유롭게 편한거 골라주세요
    * `UIModule.java`<br>`SyncTuneModule` 구현, UI 초기화 및 이벤트 리스너 등록
    * `view`<br>화면 구성 요소
        * `MainApplicationWindow.java`<br>메인 윈도우
        * `PlayerControlsView.java`<br>재생/정지 버튼, 진행 바 등
        * `PlaylistView.java`<br>재생 목록 표시 영역
        * `LyricsView.java`<br>가사 표시 영역
    * `controller`<br>UI 이벤트 처리 및 다른 모듈과의 상호작용
        * `PlaybackController.java`<br>재생 관련 UI 이벤트<br>-> `MediaControlEvent` 발행
        * `PlaylistActionHandler.java`<br>재생 목록 UI 이벤트<br>-> `PlaylistEvent` 발행
        * `WindowStateManager.java`<br>창 크기, PIP 모드 등 관리
    * `component`<br>재사용 가능한 커스텀 UI 컴포넌트
        * `StyledButton.java`<br>공통 스타일 버튼
        * `MarqueeLabel.java`<br>긴 텍스트 스크롤 효과 라벨
        * `AlbumArtDisplay.java`<br>앨범아트 표시 컴포넌트