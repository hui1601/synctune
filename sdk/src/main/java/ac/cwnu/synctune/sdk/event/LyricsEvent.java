package ac.cwnu.synctune.sdk.event;

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
    public static class LyricsFullTextEvent extends BaseEvent {
        private final String musicFilePath;
        private final String[] fullLyricsLines;

        public LyricsFullTextEvent(String musicFilePath, String[] fullLyricsLines) {
        this.musicFilePath = musicFilePath;
        this.fullLyricsLines = fullLyricsLines;
        }

        public String getMusicFilePath() {
            return musicFilePath;
        }

        public String[] getFullLyricsLines() {
            return fullLyricsLines;
        }

        @Override
        public String toString() {
            return super.toString() + " {musicFile=" + musicFilePath + ", lines=" + fullLyricsLines.length + "}";
        }
    }

}