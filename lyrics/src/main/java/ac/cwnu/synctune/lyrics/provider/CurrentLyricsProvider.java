package ac.cwnu.synctune.lyrics.provider;

import ac.cwnu.synctune.lyrics.synchronizer.LyricsTimelineMatcher;
import ac.cwnu.synctune.sdk.event.LyricsEvent;
import ac.cwnu.synctune.sdk.model.LrcLine;
import ac.cwnu.synctune.sdk.event.EventPublisher;

import java.util.List;

public class CurrentLyricsProvider {

    private final List<LrcLine> lrcLines;
    private final EventPublisher publisher;
    private LrcLine lastLine;

    public CurrentLyricsProvider(List<LrcLine> lrcLines, EventPublisher publisher) {
        this.lrcLines = lrcLines;
        this.publisher = publisher;
        this.lastLine = null;
    }

    public void update(long currentTimeMillis) {
        if (lrcLines == null || lrcLines.isEmpty()) return;

        LrcLine currentLine = LyricsTimelineMatcher.findCurrentLine(lrcLines, currentTimeMillis);

        // 중복 발행 방지
        if (currentLine != null && !currentLine.equals(lastLine)) {
            lastLine = currentLine;
            publisher.publish(new LyricsEvent.NextLyricsEvent(currentLine.getText(), currentLine.getTimeMillis()));
        }
    }
}
