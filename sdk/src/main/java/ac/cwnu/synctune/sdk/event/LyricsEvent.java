package ac.cwnu.synctune.sdk.event;

import java.util.ArrayList;
import java.util.List;

import ac.cwnu.synctune.sdk.model.LrcLine;

/**
 * 가사 처리와 관련된 이벤트들을 정의합니다.
 */
public class LyricsEvent {

    private LyricsEvent() {
    } // 인스턴스화 방지

    public static class LyricsFoundEvent extends BaseEvent {
        private final String musicFilePath;
        private final String lrcFilePath;

        public LyricsFoundEvent(String musicFilePath, String lrcFilePath) {
            this.musicFilePath = musicFilePath;
            this.lrcFilePath = lrcFilePath;
        }

        public String getMusicFilePath() {
            return musicFilePath;
        }

        public String getLrcFilePath() {
            return lrcFilePath;
        }

        @Override
        public String toString() {
            return super.toString() + " {musicFile=" + musicFilePath + ", lrcFile=" + lrcFilePath + "}";
        }
    }

    public static class LyricsNotFoundEvent extends BaseEvent {
        private final String musicFilePath;

        public LyricsNotFoundEvent(String musicFilePath) {
            this.musicFilePath = musicFilePath;
        }

        public String getMusicFilePath() {
            return musicFilePath;
        }

        @Override
        public String toString() {
            return super.toString() + " {musicFile=" + musicFilePath + "}";
        }
    }

    public static class NextLyricsEvent extends BaseEvent {
        private final String lyricLine;
        private final long startTimeMillis; // 해당 가사 라인의 시작 시간 (선택적)

        public NextLyricsEvent(String lyricLine, long startTimeMillis) {
            this.lyricLine = lyricLine;
            this.startTimeMillis = startTimeMillis;
        }

        public String getLyricLine() {
            return lyricLine;
        }

        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        @Override
        public String toString() {
            return super.toString() + " {line=\"" + lyricLine + "\", time=" + startTimeMillis + "ms}";
        }
    }

    public static class LyricsParseCompleteEvent extends BaseEvent { // 추가: LRC 파싱 완료
        private final String musicFilePath;
        private final boolean success;

        public LyricsParseCompleteEvent(String musicFilePath, boolean success) {
            this.musicFilePath = musicFilePath;
            this.success = success;
        }

        public String getMusicFilePath() {
            return musicFilePath;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return super.toString() + " {musicFile=" + musicFilePath + ", success=" + success + "}";
        }
    }
    
    /**
     * 전체 가사 텍스트 이벤트 - List<LrcLine>과 String[] 모두 지원
     */
    public static class LyricsFullTextEvent extends BaseEvent {
        private final String musicFilePath;
        private final List<LrcLine> fullLyricsLines;

        /**
         * List<LrcLine>을 받는 생성자 (권장)
         */
        public LyricsFullTextEvent(String musicFilePath, List<LrcLine> fullLyricsLines) {
            this.musicFilePath = musicFilePath;
            this.fullLyricsLines = fullLyricsLines != null ? List.copyOf(fullLyricsLines) : List.of();
        }

        /**
         * String[]을 받는 생성자 (하위 호환성)
         * @deprecated List<LrcLine> 생성자를 사용하세요
         */
        @Deprecated
        public LyricsFullTextEvent(String musicFilePath, String[] lyricsArray) {
            this.musicFilePath = musicFilePath;
            
            if (lyricsArray != null && lyricsArray.length > 0) {
                List<LrcLine> tempList = new ArrayList<>();
                for (int i = 0; i < lyricsArray.length; i++) {
                    // String[]에서는 타임스탬프 정보가 없으므로 인덱스 기반으로 임시 시간 할당
                    // 실제로는 LyricsModule에서 List<LrcLine>을 전달하는 것이 권장됨
                    long estimatedTime = i * 3000L; // 3초 간격으로 임시 할당
                    tempList.add(new LrcLine(estimatedTime, lyricsArray[i]));
                }
                this.fullLyricsLines = List.copyOf(tempList);
            } else {
                this.fullLyricsLines = List.of();
            }
        }

        public String getMusicFilePath() {
            return musicFilePath;
        }

        /**
         * 전체 가사 라인들을 LrcLine 리스트로 반환
         */
        public List<LrcLine> getFullLyricsLines() {
            return fullLyricsLines;
        }

        /**
         * 하위 호환성을 위한 메서드 - String 배열로 반환
         * @deprecated getFullLyricsLines()를 사용하세요
         */
        @Deprecated
        public String[] getFullLyricsLinesAsArray() {
            return fullLyricsLines.stream()
                .map(LrcLine::getText)
                .toArray(String[]::new);
        }

        @Override
        public String toString() {
            return super.toString() + " {musicFile=" + musicFilePath + ", lines=" + fullLyricsLines.size() + "}";
        }
    }
}