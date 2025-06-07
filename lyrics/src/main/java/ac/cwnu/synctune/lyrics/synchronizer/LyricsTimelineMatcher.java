package ac.cwnu.synctune.lyrics.synchronizer;

import ac.cwnu.synctune.sdk.model.LrcLine;

import java.util.List;

public class LyricsTimelineMatcher {
    public static LrcLine findCurrentLine(List<LrcLine> lines, long currentTimeMillis){
        if(lines == null || lines.isEmpty()) return null;

        LrcLine result = null;
        for(LrcLine line : lines){
            if(line.getTimeMillis() <= currentTimeMillis){
                result = line;
            } else {
                break;
            }
        }
        return result;
    }
}
