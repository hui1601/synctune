## 1. `sdk` Module
담당: 동헌희
* `ac.cwnu.synctune.sdk`
    * `annotation`
        * `Module.java`<br>모듈 식별용 어노테이션, Core가 스캔할 대상
        * `EventListener.java`<br>이벤트 리스너 메소드 지정용
    * `event`
        * `BaseEvent.java`<br>모든 이벤트의 부모 클래스
        * `ErrorEvent.java`
        * `MediaControlEvent.java`<br>재생, 정지, 다음곡 등 사용자 요청
        * `PlaybackStatusEvent.java`<br>실제 재생 상태 변경 알림
        * `PlaylistEvent.java`<br>플레이리스트 변경 요청/알림
        * `LyricsEvent.java`<br>파싱된 가사, 현재 가사 라인 등
        * `SystemEvent.java`<br>애플리케이션 시작, 종료 등
        * `FileScanEvent.java`<br>파일 스캔 시작, 완료, 파일 발견 등
    * `model`
        * `MusicInfo.java`<br>음악 메타데이터 DTO
        * `Playlist.java`<br>플레이리스트 DTO
        * `LrcLine.java`<br>LRC 가사 한 줄 DTO - 시간, 가사
        * `ModuleInfo.java`<br>스캔된 모듈 정보
    * `module`
        * `SyncTuneModule.java`<br>모든 모듈이 구현해야 할 인터페이스 - `initialize()`, `shutdown()` 등
        * `ModuleLifecycleListener.java`<br>모듈 생명주기 이벤트를 받을 리스너 인터페이스

## 2. `core` Module
담당: 동헌희
* `ac.cwnu.synctune.core`
    * `CoreModule.java`<br>이 모듈의 진입점 및 메인 로직, `SyncTuneModule` 구현
    * `EventBus.java`<br>이벤트 발행 및 구독 관리
    * `initializer`
        * `ModuleScanner.java`<br>클래스패스 등에서 `@Module` 어노테이션 스캔
        * `ModuleLoader.java`<br>스캔된 모듈 인스턴스화 및 `initialize` 호출
    * `error`
        * `ModuleInitializationException.java`<br>모듈 초기화 실패 예외
        * `GlobalExceptionHandler.java`<br>처리되지 않은 예외 중앙 처리, 로깅
        * `FatalErrorReporter.java`<br>심각한 오류 발생 시 처리
    * **`logging`**
        * `EventLogger.java`<br>모든 주요 이벤트를 파일/콘솔에 로깅

## 3. `player` Module
담당: 김민재
* `ac.cwnu.synctune.player`
    * `PlayerModule.java`<br>`SyncTuneModule` 구현, 이벤트 리스너 등록
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
        * `FileDiscoveryReporter.java`<br>발견된 파일 정보를 `FileScanEvent`로 발행

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