package ac.cwnu.synctune.sdk.model;

import java.util.Objects;

/**
 * 음악 파일의 메타데이터를 담는 클래스입니다.
 */
public final class MusicInfo {
    private final String title;
    private final String artist;
    private final String album;
    private final String filePath; // 음악 파일의 실제 경로
    private final long durationMillis; // 음악 길이 (밀리초)
    private final String lrcPath; // LRC 파일 경로 (선택적)

    public MusicInfo(String title, String artist, String album, String filePath, long durationMillis, String lrcPath) {
        this.title = title != null ? title : "Unknown Title";
        this.artist = artist != null ? artist : "Unknown Artist";
        this.album = album != null ? album : "Unknown Album";
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.durationMillis = durationMillis;
        this.lrcPath = lrcPath;
    }

    public MusicInfo(String title, String artist, String album, String filePath, long durationMillis) {
        this(title, artist, album, filePath, durationMillis, null);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getLrcPath() {
        return lrcPath;
    }

    // LRC 경로가 변경될 수 있으므로, 새로운 MusicInfo 객체를 반환하는 메서드 추가
    public MusicInfo withLrcPath(String newLrcPath) {
        return new MusicInfo(this.title, this.artist, this.album, this.filePath, this.durationMillis, newLrcPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicInfo musicInfo = (MusicInfo) o;
        return filePath.equals(musicInfo.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }

    @Override
    public String toString() {
        return "MusicInfo{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", filePath='" + filePath + '\'' +
                ", durationMillis=" + durationMillis +
                (lrcPath != null ? ", lrcPath='" + lrcPath + '\'' : "") +
                '}';
    }
}