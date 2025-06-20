package ac.cwnu.synctune.lyrics.synchronizer;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackProgressUpdateEvent;
import ac.cwnu.synctune.lyrics.provider.CurrentLyricsProvider;

public class PlaybackTimeReceiver {

    private final CurrentLyricsProvider provider;

    public PlaybackTimeReceiver(CurrentLyricsProvider provider) {
        this.provider = provider;
    }

    @EventListener
    public void onPlaybackProgress(PlaybackProgressUpdateEvent event) {
        provider.update(event.getCurrentTimeMillis());
    }
}
