package ac.cwnu.synctune.lyrics.provider;

import ac.cwnu.synctune.sdk.model.LrcLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LyricsCache {

    // key: lrc file path (절대 경로 기준), value: parsed LrcLine list
    private final Map<String, List<LrcLine>> cache = new HashMap<>();

    // 이미 캐싱된 경우 반환
    public List<LrcLine> get(String lrcPath) {
        return cache.get(lrcPath);
    }

    // 새로 캐싱
    public void put(String lrcPath, List<LrcLine> lines) {
        cache.put(lrcPath, lines);
    }

    // 캐시 존재 여부
    public boolean contains(String lrcPath) {
        return cache.containsKey(lrcPath);
    }

    // 전체 삭제
    public void clear() {
        cache.clear();
    }
}
