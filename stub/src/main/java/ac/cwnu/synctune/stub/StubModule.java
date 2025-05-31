package ac.cwnu.synctune.stub;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.annotation.Module;
import ac.cwnu.synctune.sdk.event.*;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import ac.cwnu.synctune.sdk.module.ModuleLifecycleListener;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.slf4j.Logger;

@Module(name = "Stub", version = "1.0.0")
public class StubModule extends SyncTuneModule implements ModuleLifecycleListener {
    private static final Logger log = LogManager.getLogger(StubModule.class);

    @Override
    public void start(EventPublisher publisher) {
        super.eventPublisher = publisher;
        log.info("[{}] Module starting...", getModuleName());

        // Example: Publish an event on startup
        MusicInfo dummyMusic = new MusicInfo(
                "Stub AutoPlay Song",
                "StubSystem",
                "Initialization Tracks",
                "/dev/null/stub_song.mp3",
                10000L
        );
        log.info("[{}] Publishing RequestPlayEvent for: {}", getModuleName(), dummyMusic.getTitle());
        publish(new MediaControlEvent.RequestPlayEvent(dummyMusic));

        log.info("[{}] Publishing ScanStartedEvent for example directory: /stub_media_library", getModuleName());
        publish(new FileScanEvent.ScanStartedEvent("/stub_media_library"));

        log.info("[{}] Module started successfully.", getModuleName());
    }

    @Override
    public void stop() {
        log.info("[{}] Module stopping...", getModuleName());
        // Cleanup resources if any
        log.info("[{}] Module stopped.", getModuleName());
    }

    // SDK Event Listeners Demonstration
    @EventListener
    public void onErrorEvent(ErrorEvent event) {
        log.info("[{}] Received ErrorEvent: Message='{}', IsFatal={}, Exception={}",
                getModuleName(), event.getMessage(), event.isFatal(),
                event.getException() != null ? event.getException().getClass().getSimpleName() : "null");
    }

    // FileScanEvents
    @EventListener
    public void onFileScanStarted(FileScanEvent.ScanStartedEvent event) {
        log.info("[{}] Received FileScanEvent.ScanStartedEvent: Directory='{}'", getModuleName(), event.getDirectoryPath());
    }

    @EventListener
    public void onFileFound(FileScanEvent.FileFoundEvent event) {
        log.info("[{}] Received FileScanEvent.FileFoundEvent: File='{}'", getModuleName(), event.getFoundFile().getAbsolutePath());
    }

    @EventListener
    public void onFileScanCompleted(FileScanEvent.ScanCompletedEvent event) {
        log.info("[{}] Received FileScanEvent.ScanCompletedEvent: Directory='{}', FoundCount={}",
                getModuleName(), event.getDirectoryPath(), event.getTotalFilesFound());
    }

    @EventListener
    public void onFileScanError(FileScanEvent.ScanErrorEvent event) {
        log.info("[{}] Received FileScanEvent.ScanErrorEvent: Directory='{}', Error='{}'",
                getModuleName(), event.getDirectoryPath(), event.getErrorMessage());
    }

    // LyricsEvents
    @EventListener
    public void onLyricsFound(LyricsEvent.LyricsFoundEvent event) {
        log.info("[{}] Received LyricsEvent.LyricsFoundEvent: MusicFile='{}', LrcFile='{}'",
                getModuleName(), event.getMusicFilePath(), event.getLrcFilePath());
    }

    @EventListener
    public void onLyricsNotFound(LyricsEvent.LyricsNotFoundEvent event) {
        log.info("[{}] Received LyricsEvent.LyricsNotFoundEvent: MusicFile='{}'", getModuleName(), event.getMusicFilePath());
    }

    @EventListener
    public void onNextLyrics(LyricsEvent.NextLyricsEvent event) {
        log.info("[{}] Received LyricsEvent.NextLyricsEvent: Line='{}', Time={}ms",
                getModuleName(), event.getLyricLine(), event.getStartTimeMillis());
    }

    @EventListener
    public void onLyricsParseComplete(LyricsEvent.LyricsParseCompleteEvent event) {
        log.info("[{}] Received LyricsEvent.LyricsParseCompleteEvent: MusicFile='{}', Success={}",
                getModuleName(), event.getMusicFilePath(), event.isSuccess());
    }

    // MediaControlEvents (These are requests, StubModule typically wouldn't handle them but can log them)
    @EventListener
    public void onRequestPlay(MediaControlEvent.RequestPlayEvent event) {
        String musicTitle = event.getMusicToPlay() != null ? event.getMusicToPlay().getTitle() : "current/default";
        log.info("[{}] Received MediaControlEvent.RequestPlayEvent: Music='{}'", getModuleName(), musicTitle);
    }

    @EventListener
    public void onRequestPause(MediaControlEvent.RequestPauseEvent event) {
        log.info("[{}] Received MediaControlEvent.RequestPauseEvent", getModuleName());
    }

    @EventListener
    public void onRequestStop(MediaControlEvent.RequestStopEvent event) {
        log.info("[{}] Received MediaControlEvent.RequestStopEvent", getModuleName());
    }

    @EventListener
    public void onRequestNextMusic(MediaControlEvent.RequestNextMusicEvent event) {
        log.info("[{}] Received MediaControlEvent.RequestNextMusicEvent", getModuleName());
    }

    @EventListener
    public void onRequestPreviousMusic(MediaControlEvent.RequestPreviousMusicEvent event) {
        log.info("[{}] Received MediaControlEvent.RequestPreviousMusicEvent", getModuleName());
    }

    @EventListener
    public void onRequestSeek(MediaControlEvent.RequestSeekEvent event) {
        log.info("[{}] Received MediaControlEvent.RequestSeekEvent: Position={}ms", getModuleName(), event.getPositionMillis());
    }

    // MediaInfoEvents
    @EventListener
    public void onMediaScanStarted(MediaInfoEvent.MediaScanStartedEvent event) {
        log.info("[{}] Received MediaInfoEvent.MediaScanStartedEvent: Directory='{}'", getModuleName(), event.getDirectoryPath());
    }

    @EventListener
    public void onMediaScanProgress(MediaInfoEvent.MediaScanProgressEvent event) {
        log.info("[{}] Received MediaInfoEvent.MediaScanProgressEvent: Scanned={}/{}",
                getModuleName(), event.getScannedFiles(), event.getTotalFiles());
    }

    @EventListener
    public void onMediaScanCompleted(MediaInfoEvent.MediaScanCompletedEvent event) {
        log.info("[{}] Received MediaInfoEvent.MediaScanCompletedEvent: FoundCount={}",
                getModuleName(), event.getScannedMusicInfos().size());
        if (!event.getScannedMusicInfos().isEmpty()) {
            log.debug("[{}] First scanned music: {}", getModuleName(), event.getScannedMusicInfos().getFirst());
        }
    }

    @EventListener
    public void onMetadataUpdated(MediaInfoEvent.MetadataUpdatedEvent event) {
        log.info("[{}] Received MediaInfoEvent.MetadataUpdatedEvent: Music='{}'",
                getModuleName(), event.getUpdatedMusicInfo().getTitle());
    }

    // PlaybackStatusEvents
    @EventListener
    public void onPlaybackStarted(PlaybackStatusEvent.PlaybackStartedEvent event) {
        log.info("[{}] Received PlaybackStatusEvent.PlaybackStartedEvent: Music='{}'",
                getModuleName(), event.getCurrentMusic() != null ? event.getCurrentMusic().getTitle() : "N/A");
    }

    @EventListener
    public void onPlaybackPaused(PlaybackStatusEvent.PlaybackPausedEvent event) {
        log.info("[{}] Received PlaybackStatusEvent.PlaybackPausedEvent", getModuleName());
    }

    @EventListener
    public void onPlaybackStopped(PlaybackStatusEvent.PlaybackStoppedEvent event) {
        log.info("[{}] Received PlaybackStatusEvent.PlaybackStoppedEvent", getModuleName());
    }

    @EventListener
    public void onMusicChanged(PlaybackStatusEvent.MusicChangedEvent event) {
        log.info("[{}] Received PlaybackStatusEvent.MusicChangedEvent: NewMusic='{}'",
                getModuleName(), event.getNewMusic() != null ? event.getNewMusic().getTitle() : "N/A");
    }

    @EventListener
    public void onPlaybackProgressUpdate(PlaybackStatusEvent.PlaybackProgressUpdateEvent event) {
        log.trace("[{}] Received PlaybackStatusEvent.PlaybackProgressUpdateEvent: CurrentTime={}ms, TotalTime={}ms", // Usually too frequent for INFO
                getModuleName(), event.getCurrentTimeMillis(), event.getTotalTimeMillis());
    }

    // PlayerUIEvents
    @EventListener
    public void onMainWindowClosed(PlayerUIEvent.MainWindowClosedEvent event) {
        log.info("[{}] Received PlayerUIEvent.MainWindowClosedEvent", getModuleName());
    }

    @EventListener
    public void onMainWindowRestored(PlayerUIEvent.MainWindowRestoredEvent event) {
        log.info("[{}] Received PlayerUIEvent.MainWindowRestoredEvent", getModuleName());
    }

    // PlaylistEvents
    @EventListener
    public void onPlaylistCreated(PlaylistEvent.PlaylistCreatedEvent event) {
        log.info("[{}] Received PlaylistEvent.PlaylistCreatedEvent: PlaylistName='{}'",
                getModuleName(), event.getPlaylist().getName());
    }

    @EventListener
    public void onPlaylistDeleted(PlaylistEvent.PlaylistDeletedEvent event) {
        log.info("[{}] Received PlaylistEvent.PlaylistDeletedEvent: PlaylistName='{}'", getModuleName(), event.getPlaylistName());
    }

    @EventListener
    public void onMusicAddedToPlaylist(PlaylistEvent.MusicAddedToPlaylistEvent event) {
        log.info("[{}] Received PlaylistEvent.MusicAddedToPlaylistEvent: PlaylistName='{}', Music='{}'",
                getModuleName(), event.getPlaylistName(), event.getMusicInfo().getTitle());
    }

    @EventListener
    public void onMusicRemovedFromPlaylist(PlaylistEvent.MusicRemovedFromPlaylistEvent event) {
        log.info("[{}] Received PlaylistEvent.MusicRemovedFromPlaylistEvent: PlaylistName='{}', Music='{}'",
                getModuleName(), event.getPlaylistName(), event.getMusicInfo().getTitle());
    }

    @EventListener
    public void onPlaylistOrderChanged(PlaylistEvent.PlaylistOrderChangedEvent event) {
        log.info("[{}] Received PlaylistEvent.PlaylistOrderChangedEvent: PlaylistName='{}'",
                getModuleName(), event.getPlaylist().getName());
    }

    @EventListener
    public void onAllPlaylistsLoaded(PlaylistEvent.AllPlaylistsLoadedEvent event) {
        log.info("[{}] Received PlaylistEvent.AllPlaylistsLoadedEvent: Count={}",
                getModuleName(), event.getPlaylists().size());
    }

    // SystemEvents
    @EventListener
    public void onApplicationReady(SystemEvent.ApplicationReadyEvent event) {
        log.info("[{}] Received SystemEvent.ApplicationReadyEvent. The application is fully initialized.", getModuleName());
    }

    @EventListener
    public void onApplicationShutdown(SystemEvent.ApplicationShutdownEvent event) {
        log.info("[{}] Received SystemEvent.ApplicationShutdownEvent. The application is shutting down.", getModuleName());
    }


    // ModuleLifecycleListener methods
    // These will be called by ModuleLoader if this StubModule instance is registered as a lifecycle listener.
    // For self-monitoring, simple logs in start()/stop() are more direct.
    // Implementing this interface demonstrates its usage.
    @Override
    public void beforeModuleLoad(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo) {
        // This method would be called for ALL modules if StubModule were registered as a global lifecycle listener.
        log.debug("[{}] Lifecycle: beforeModuleLoad for Module: {}", getModuleName(), moduleInfo.getName());
    }

    @Override
    public void afterModuleLoad(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo, SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: afterModuleLoad for Module: {} (Instance: {})", getModuleName(), moduleInfo.getName(), moduleInstance.getClass().getSimpleName());
        if (moduleInstance == this) {
            log.info("[{}] Lifecycle: StubModule itself has been loaded.", getModuleName());
        }
    }

    @Override
    public void beforeModuleStart(SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: beforeModuleStart for Module: {}", getModuleName(), moduleInstance.getModuleName());
        if (moduleInstance == this) {
            log.info("[{}] Lifecycle: StubModule itself is about to start.", getModuleName());
        }
    }

    @Override
    public void afterModuleStart(SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: afterModuleStart for Module: {}", getModuleName(), moduleInstance.getModuleName());
        if (moduleInstance == this) {
            log.info("[{}] Lifecycle: StubModule itself has started.", getModuleName());
        }
    }

    @Override
    public void beforeModuleStop(SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: beforeModuleStop for Module: {}", getModuleName(), moduleInstance.getModuleName());
        if (moduleInstance == this) {
            log.info("[{}] Lifecycle: StubModule itself is about to stop.", getModuleName());
        }
    }

    @Override
    public void afterModuleStop(SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: afterModuleStop for Module: {}", getModuleName(), moduleInstance.getModuleName());
        if (moduleInstance == this) {
            log.info("[{}] Lifecycle: StubModule itself has stopped.", getModuleName());
        }
    }

    @Override
    public void beforeModuleUnload(SyncTuneModule moduleInstance) {
        log.debug("[{}] Lifecycle: beforeModuleUnload for Module: {}", getModuleName(), moduleInstance.getModuleName());
    }

    @Override
    public void afterModuleUnload(ac.cwnu.synctune.sdk.model.ModuleInfo moduleInfo) {
        log.debug("[{}] Lifecycle: afterModuleUnload for Module: {}", getModuleName(), moduleInfo.getName());
    }
}