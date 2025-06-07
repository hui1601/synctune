package ac.cwnu.synctune.lyrics.provider;

import ac.cwnu.synctune.lyrics.synchronizer.LyricsTimelineMatcher;
import ac.cwnu.synctune.lyrics.synchronizer.PlaybackTimeReceiver;
import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.event.LyricsEvent.NextLyricsEvent;

import java.util.List;

public class CurrentLyricsProvider {
    private final List<LrcLine> lrclines;
    private final PlaybackTimeReceiver timeReceiver;
    private final EventPublisher eventPublisher;

    private LrcLine lastLine; // 중복 발행 방지용

    public CurrentLyricsProvider(List<LrcLine> lrclines, PlaybackTimeReceiver timeReceiver, EventPublisher eventPublisher){
        this.lrclines = lrclines;
        this.timeReceiver = timeReceiver;
        this.eventPublisher = eventPublisher;
        this.lastLine = null;
    }

    public void update(){
        long currentTime = timeReceiver.getCurrentTimeMillis();
        LrcLine currentLine = LyricsTimelineMatcher.findCurrentLine(lrclines, currentTime);

        if(currentLine != null && !currentLine.equals(lastLine)){
            lastLine = currentLine;
            eventPublisher.publish(new NextLyricsEvent(currentLine.getText(), currentLine.getTimeMillis()));
        }
    }
}
