package ac.cwnu.synctune.lyrics.synchronizer;

import ac.cwnu.synctune.sdk.annotation.EventListener;
import ac.cwnu.synctune.sdk.event.PlaybackStatusEvent.PlaybackProgressUpdateEvent;

public class PlaybackTimeReceiver {

    private long currentTimeMillis = 0;

    @EventListener
    public void onPlaybackProgress(PlaybackProgressUpdateEvent event) {
        this.currentTimeMillis = event.getCurrentTimeMillis();
    }

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }
}