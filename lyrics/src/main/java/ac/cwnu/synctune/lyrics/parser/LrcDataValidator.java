package ac.cwnu.synctune.lyrics.parser;

import ac.cwnu.synctune.sdk.model.LrcLine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LrcDataValidator {

    public static boolean validate(List<LrcLine> lines) {
        if (lines == null || lines.isEmpty()) return false;

        Set<Long> timestamps = new HashSet<>();
        long previousTime = -1;

        for (LrcLine line : lines) {
            long currentTime = line.getTimeMillis();

            // 중복 시간 체크
            if (!timestamps.add(currentTime)) {
                return false; // 중복 시간 존재
            }

            // 오름차순 정렬 여부 체크
            if (previousTime > currentTime) {
                return false;
            }

            previousTime = currentTime;
        }

        return true;
    }
}
