package ac.cwnu.synctune.sdk.model;

import java.util.Objects;

/**
 * LRC 가사 파일의 한 줄을 나타내는 DTO입니다.
 * 시간 태그와 해당 시간의 가사 텍스트를 포함합니다.
 */
public final class LrcLine implements Comparable<LrcLine> {
    private final long timeMillis; // 해당 가사 라인의 시작 시간 (밀리초)
    private final String text;     // 가사 텍스트

    public LrcLine(long timeMillis, String text) {
        this.timeMillis = timeMillis;
        this.text = Objects.requireNonNull(text, "LRC line text cannot be null");
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(LrcLine other) {
        return Long.compare(this.timeMillis, other.timeMillis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LrcLine lrcLine = (LrcLine) o;
        return timeMillis == lrcLine.timeMillis && text.equals(lrcLine.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeMillis, text);
    }

    @Override
    public String toString() {
        return "LrcLine{" +
                "timeMillis=" + timeMillis +
                ", text='" + text + '\'' +
                '}';
    }
}