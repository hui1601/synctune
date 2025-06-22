package ac.cwnu.synctune.lyrics.synchronizer;

import ac.cwnu.synctune.sdk.model.LrcLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LyricsTimelineMatcher {
    private static final Logger log = LoggerFactory.getLogger(LyricsTimelineMatcher.class);
    
    /**
     * 현재 재생 시간에 맞는 가사 라인을 찾습니다.
     * @param lines 가사 라인 목록 (시간순으로 정렬되어 있어야 함)
     * @param currentTimeMillis 현재 재생 시간 (밀리초)
     * @return 현재 시간에 해당하는 가사 라인, 없으면 null
     */
    public static LrcLine findCurrentLine(List<LrcLine> lines, long currentTimeMillis) {
        if (lines == null || lines.isEmpty()) {
            log.trace("가사 라인이 없습니다.");
            return null;
        }

        LrcLine result = null;
        int matchedIndex = -1;
        
        for (int i = 0; i < lines.size(); i++) {
            LrcLine line = lines.get(i);
            
            if (line.getTimeMillis() <= currentTimeMillis) {
                result = line;
                matchedIndex = i;
            } else {
                // 현재 시간보다 큰 시간의 가사가 나오면 중단
                break;
            }
        }
        
        if (result != null) {
            log.trace("가사 매칭 성공: 시간={}ms, 인덱스={}/{}, 텍스트='{}'", 
                currentTimeMillis, matchedIndex + 1, lines.size(), result.getText());
        } else {
            log.trace("가사 매칭 실패: 시간={}ms (첫 번째 가사 시간: {}ms)", 
                currentTimeMillis, lines.get(0).getTimeMillis());
        }
        
        return result;
    }
    
    /**
     * 다음 가사 라인을 찾습니다.
     * @param lines 가사 라인 목록
     * @param currentLine 현재 가사 라인
     * @return 다음 가사 라인, 없으면 null
     */
    public static LrcLine findNextLine(List<LrcLine> lines, LrcLine currentLine) {
        if (lines == null || lines.isEmpty() || currentLine == null) {
            return null;
        }
        
        for (int i = 0; i < lines.size() - 1; i++) {
            if (lines.get(i).equals(currentLine)) {
                LrcLine nextLine = lines.get(i + 1);
                log.debug("다음 가사 라인 찾음: '{}' -> '{}'", 
                    currentLine.getText(), nextLine.getText());
                return nextLine;
            }
        }
        
        return null;
    }
    
    /**
     * 이전 가사 라인을 찾습니다.
     * @param lines 가사 라인 목록
     * @param currentLine 현재 가사 라인
     * @return 이전 가사 라인, 없으면 null
     */
    public static LrcLine findPreviousLine(List<LrcLine> lines, LrcLine currentLine) {
        if (lines == null || lines.isEmpty() || currentLine == null) {
            return null;
        }
        
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).equals(currentLine)) {
                LrcLine previousLine = lines.get(i - 1);
                log.debug("이전 가사 라인 찾음: '{}' -> '{}'", 
                    currentLine.getText(), previousLine.getText());
                return previousLine;
            }
        }
        
        return null;
    }
    
    /**
     * 특정 시간 범위 내의 모든 가사 라인을 찾습니다.
     * @param lines 가사 라인 목록
     * @param startTime 시작 시간 (밀리초)
     * @param endTime 종료 시간 (밀리초)
     * @return 해당 시간 범위의 가사 라인들
     */
    public static List<LrcLine> findLinesInRange(List<LrcLine> lines, long startTime, long endTime) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        
        return lines.stream()
            .filter(line -> line.getTimeMillis() >= startTime && line.getTimeMillis() <= endTime)
            .toList();
    }
    
    /**
     * 현재 가사가 언제까지 표시되어야 하는지 계산합니다.
     * @param lines 가사 라인 목록
     * @param currentLine 현재 가사 라인
     * @return 현재 가사의 종료 시간 (밀리초), 마지막 라인이면 Long.MAX_VALUE
     */
    public static long calculateLineEndTime(List<LrcLine> lines, LrcLine currentLine) {
        if (lines == null || lines.isEmpty() || currentLine == null) {
            return Long.MAX_VALUE;
        }
        
        LrcLine nextLine = findNextLine(lines, currentLine);
        return nextLine != null ? nextLine.getTimeMillis() : Long.MAX_VALUE;
    }
    
    /**
     * 가사 라인들이 시간순으로 정렬되어 있는지 확인합니다.
     * @param lines 확인할 가사 라인 목록
     * @return 정렬되어 있으면 true
     */
    public static boolean isTimelineSorted(List<LrcLine> lines) {
        if (lines == null || lines.size() <= 1) {
            return true;
        }
        
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i - 1).getTimeMillis() > lines.get(i).getTimeMillis()) {
                log.warn("가사 타임라인이 정렬되지 않음: {}ms 다음에 {}ms", 
                    lines.get(i - 1).getTimeMillis(), lines.get(i).getTimeMillis());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 가사 라인 목록의 통계 정보를 로깅합니다.
     * @param lines 가사 라인 목록
     */
    public static void logLyricsStatistics(List<LrcLine> lines) {
        if (lines == null || lines.isEmpty()) {
            log.info("가사 통계: 가사 없음");
            return;
        }
        
        long firstTime = lines.get(0).getTimeMillis();
        long lastTime = lines.get(lines.size() - 1).getTimeMillis();
        long totalDuration = lastTime - firstTime;
        
        // 한글 포함 라인 수 계산
        long koreanLines = lines.stream()
            .mapToLong(line -> containsKorean(line.getText()) ? 1 : 0)
            .sum();
        
        log.info("가사 통계: 총 {}줄, 한글 포함 {}줄, 시작 {}ms, 종료 {}ms, 총 길이 {}ms", 
            lines.size(), koreanLines, firstTime, lastTime, totalDuration);
        
        if (!isTimelineSorted(lines)) {
            log.warn("경고: 가사 타임라인이 정렬되지 않았습니다!");
        }
    }
    
    /**
     * 텍스트에 한글이 포함되어 있는지 확인합니다.
     * @param text 확인할 텍스트
     * @return 한글이 포함되어 있으면 true
     */
    private static boolean containsKorean(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return text.chars()
            .anyMatch(c -> (c >= 0xAC00 && c <= 0xD7AF) || // 한글 완성형
                          (c >= 0x3131 && c <= 0x318E));   // 한글 자모
    }
}