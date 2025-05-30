package ac.cwnu.synctune.sdk.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 플레이리스트 정보를 담는 클래스입니다.
 */
public final class Playlist {
    private final String name;
    private final List<MusicInfo> musicList;

    public Playlist(String name, List<MusicInfo> musicList) {
        this.name = Objects.requireNonNull(name, "Playlist name cannot be null");
        this.musicList = musicList != null ? List.copyOf(musicList) : List.of(); // 불변 리스트로 저장
    }

    public Playlist(String name) {
        this(name, new ArrayList<>());
    }

    public String getName() {
        return name;
    }

    public List<MusicInfo> getMusicList() {
        return musicList; // 이미 불변 리스트이므로 그대로 반환
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return name.equals(playlist.name); // 이름이 고유하다고 가정
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "name='" + name + '\'' +
                ", musicCount=" + musicList.size() +
                '}';
    }
}