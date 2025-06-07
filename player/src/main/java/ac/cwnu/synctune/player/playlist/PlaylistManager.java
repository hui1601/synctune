package ac.cwnu.synctune.player.playlist;

import ac.cwnu.synctune.sdk.event.EventPublisher;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.MusicInfo;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 플레이리스트 CRUD, 현재 곡 추적을 담당하는 관리자
 * SDK의 Playlist 클래스 API 확인 전까지 임시로 자체 PlaylistInfo 사용
 */
public class PlaylistManager {
    private static final Logger log = LogManager.getLogger(PlaylistManager.class);
    
    private final EventPublisher eventPublisher;
    private final TrackShuffler shuffler;
    
    // 플레이리스트 저장소 (이름 -> 플레이리스트)
    private final Map<String, PlaylistInfo> playlists = new LinkedHashMap<>();
    
    // 현재 활성 플레이리스트
    private final AtomicReference<PlaylistInfo> currentPlaylist = new AtomicReference<>();
    
    // 현재 재생 중인 곡의 인덱스
    private final AtomicInteger currentTrackIndex = new AtomicInteger(-1);
    
    // 재생 모드
    private final AtomicReference<PlayMode> playMode = new AtomicReference<>(PlayMode.NORMAL);
    
    // 반복 모드
    private final AtomicReference<RepeatMode> repeatMode = new AtomicReference<>(RepeatMode.NONE);
    
    /**
     * 재생 모드
     */
    public enum PlayMode {
        NORMAL,    // 순서대로 재생
        SHUFFLE    // 랜덤 재생
    }
    
    /**
     * 반복 모드
     */
    public enum RepeatMode {
        NONE,      // 반복 없음
        ONE,       // 한 곡 반복
        ALL        // 전체 반복
    }
    
    /**
     * 임시 플레이리스트 정보 클래스 (SDK Playlist API 확인 전까지 사용)
     */
    public static class PlaylistInfo {
        private final String name;
        private final List<MusicInfo> songs;
        
        public PlaylistInfo(String name, List<MusicInfo> songs) {
            this.name = name;
            this.songs = new ArrayList<>(songs);
        }
        
        public String getName() { return name; }
        public List<MusicInfo> getSongs() { return new ArrayList<>(songs); }
        public int size() { return songs.size(); }
        public boolean isEmpty() { return songs.isEmpty(); }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PlaylistInfo that = (PlaylistInfo) obj;
            return Objects.equals(name, that.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
    
    public PlaylistManager(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.shuffler = new TrackShuffler();
        
        // 기본 플레이리스트 생성
        createDefaultPlaylists();
    }
    
    // ========== 플레이리스트 CRUD ==========
    
    /**
     * 새 플레이리스트를 생성합니다
     */
    public boolean createPlaylist(String name) {
        return createPlaylist(name, new ArrayList<>());
    }
    
    /**
     * 곡 목록과 함께 새 플레이리스트를 생성합니다
     */
    public boolean createPlaylist(String name, List<MusicInfo> songs) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("플레이리스트 이름이 비어있습니다.");
            return false;
        }
        
        String normalizedName = name.trim();
        if (playlists.containsKey(normalizedName)) {
            log.warn("이미 존재하는 플레이리스트입니다: {}", normalizedName);
            return false;
        }
        
        try {
            PlaylistInfo newPlaylist = new PlaylistInfo(normalizedName, songs);
            playlists.put(normalizedName, newPlaylist);
            
            log.info("플레이리스트 생성됨: {} ({}곡)", normalizedName, songs.size());
            return true;
            
        } catch (Exception e) {
            log.error("플레이리스트 생성 중 오류: {}", normalizedName, e);
            return false;
        }
    }
    
    /**
     * 플레이리스트를 가져옵니다
     */
    public Optional<PlaylistInfo> getPlaylist(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playlists.get(name.trim()));
    }
    
    /**
     * 모든 플레이리스트 목록을 반환합니다
     */
    public List<PlaylistInfo> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }
    
    /**
     * 플레이리스트 이름 목록을 반환합니다
     */
    public List<String> getPlaylistNames() {
        return new ArrayList<>(playlists.keySet());
    }
    
    /**
     * 플레이리스트를 삭제합니다
     */
    public boolean deletePlaylist(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String normalizedName = name.trim();
        PlaylistInfo removed = playlists.remove(normalizedName);
        
        if (removed != null) {
            log.info("플레이리스트 삭제됨: {}", normalizedName);
            
            // 현재 플레이리스트였다면 해제
            if (removed.equals(currentPlaylist.get())) {
                setCurrentPlaylist(null);
            }
            
            return true;
        }
        
        return false;
    }
    
    // ========== 현재 재생 관리 ==========
    
    /**
     * 현재 활성 플레이리스트를 설정합니다
     */
    public void setCurrentPlaylist(PlaylistInfo playlist) {
        PlaylistInfo oldPlaylist = currentPlaylist.getAndSet(playlist);
        
        if (playlist != oldPlaylist) {
            currentTrackIndex.set(-1); // 곡 인덱스 초기화
            
            if (playlist != null) {
                log.info("현재 플레이리스트 설정: {} ({}곡)", playlist.getName(), playlist.getSongs().size());
            } else {
                log.info("현재 플레이리스트 해제");
            }
        }
    }
    
    /**
     * 현재 활성 플레이리스트를 반환합니다
     */
    public Optional<PlaylistInfo> getCurrentPlaylist() {
        return Optional.ofNullable(currentPlaylist.get());
    }
    
    /**
     * 현재 재생 중인 곡을 설정합니다
     */
    public void setCurrentTrack(int index) {
        PlaylistInfo playlist = currentPlaylist.get();
        if (playlist == null) {
            currentTrackIndex.set(-1);
            return;
        }
        
        if (index < 0 || index >= playlist.getSongs().size()) {
            currentTrackIndex.set(-1);
            return;
        }
        
        int oldIndex = currentTrackIndex.getAndSet(index);
        if (oldIndex != index) {
            MusicInfo currentSong = playlist.getSongs().get(index);
            log.debug("현재 곡 설정: {} [{}]", currentSong.getTitle(), index);
        }
    }
    
    /**
     * 현재 재생 중인 곡을 반환합니다
     */
    public Optional<MusicInfo> getCurrentTrack() {
        PlaylistInfo playlist = currentPlaylist.get();
        int index = currentTrackIndex.get();
        
        if (playlist == null || index < 0 || index >= playlist.getSongs().size()) {
            return Optional.empty();
        }
        
        return Optional.of(playlist.getSongs().get(index));
    }
    
    /**
     * 현재 곡 인덱스를 반환합니다
     */
    public int getCurrentTrackIndex() {
        return currentTrackIndex.get();
    }
    
    /**
     * 다음 곡으로 이동합니다
     */
    public Optional<MusicInfo> nextTrack() {
        PlaylistInfo playlist = currentPlaylist.get();
        if (playlist == null || playlist.getSongs().isEmpty()) {
            return Optional.empty();
        }
        
        int currentIndex = currentTrackIndex.get();
        int nextIndex = calculateNextTrackIndex(currentIndex, playlist.getSongs().size());
        
        if (nextIndex >= 0) {
            setCurrentTrack(nextIndex);
            return getCurrentTrack();
        }
        
        return Optional.empty();
    }
    
    /**
     * 이전 곡으로 이동합니다
     */
    public Optional<MusicInfo> previousTrack() {
        PlaylistInfo playlist = currentPlaylist.get();
        if (playlist == null || playlist.getSongs().isEmpty()) {
            return Optional.empty();
        }
        
        int currentIndex = currentTrackIndex.get();
        int previousIndex = calculatePreviousTrackIndex(currentIndex, playlist.getSongs().size());
        
        if (previousIndex >= 0) {
            setCurrentTrack(previousIndex);
            return getCurrentTrack();
        }
        
        return Optional.empty();
    }
    
    // ========== 재생 모드 관리 ==========
    
    /**
     * 재생 모드를 설정합니다
     */
    public void setPlayMode(PlayMode mode) {
        PlayMode oldMode = playMode.getAndSet(mode);
        if (oldMode != mode) {
            log.info("재생 모드 변경: {} -> {}", oldMode, mode);
            
            // 셔플 모드 변경 시 처리
            if (mode == PlayMode.SHUFFLE) {
                shuffler.shufflePlaylist(getCurrentPlaylist().orElse(null));
            }
        }
    }
    
    /**
     * 현재 재생 모드를 반환합니다
     */
    public PlayMode getPlayMode() {
        return playMode.get();
    }
    
    /**
     * 반복 모드를 설정합니다
     */
    public void setRepeatMode(RepeatMode mode) {
        RepeatMode oldMode = repeatMode.getAndSet(mode);
        if (oldMode != mode) {
            log.info("반복 모드 변경: {} -> {}", oldMode, mode);
        }
    }
    
    /**
     * 현재 반복 모드를 반환합니다
     */
    public RepeatMode getRepeatMode() {
        return repeatMode.get();
    }
    
    // ========== Private 헬퍼 메서드들 ==========
    
    private void createDefaultPlaylists() {
        createPlaylist("즐겨찾기");
        createPlaylist("최근 재생");
        log.debug("기본 플레이리스트 생성됨");
    }
    
    private int calculateNextTrackIndex(int currentIndex, int playlistSize) {
        if (playlistSize == 0) {
            return -1;
        }
        
        RepeatMode repeat = getRepeatMode();
        PlayMode play = getPlayMode();
        
        // 한 곡 반복
        if (repeat == RepeatMode.ONE) {
            return currentIndex;
        }
        
        // 셔플 모드
        if (play == PlayMode.SHUFFLE) {
            return shuffler.getNextShuffledIndex(currentIndex, playlistSize);
        }
        
        // 일반 순서 재생
        int nextIndex = currentIndex + 1;
        
        if (nextIndex >= playlistSize) {
            // 마지막 곡에 도달
            if (repeat == RepeatMode.ALL) {
                return 0; // 처음부터 다시
            } else {
                return -1; // 재생 종료
            }
        }
        
        return nextIndex;
    }
    
    private int calculatePreviousTrackIndex(int currentIndex, int playlistSize) {
        if (playlistSize == 0) {
            return -1;
        }
        
        RepeatMode repeat = getRepeatMode();
        PlayMode play = getPlayMode();
        
        // 한 곡 반복
        if (repeat == RepeatMode.ONE) {
            return currentIndex;
        }
        
        // 셔플 모드
        if (play == PlayMode.SHUFFLE) {
            return shuffler.getPreviousShuffledIndex(currentIndex, playlistSize);
        }
        
        // 일반 순서 재생
        int previousIndex = currentIndex - 1;
        
        if (previousIndex < 0) {
            // 첫 번째 곡에 도달
            if (repeat == RepeatMode.ALL) {
                return playlistSize - 1; // 마지막 곡으로
            } else {
                return -1; // 재생 종료
            }
        }
        
        return previousIndex;
    }
    
    // ========== 유틸리티 메서드들 ==========
    
    /**
     * 총 플레이리스트 개수를 반환합니다
     */
    public int getPlaylistCount() {
        return playlists.size();
    }
    
    /**
     * 현재 상태를 문자열로 반환합니다 (디버깅용)
     */
    @Override
    public String toString() {
        PlaylistInfo current = currentPlaylist.get();
        return String.format("PlaylistManager{playlists=%d, current=%s, track=%d/%d, mode=%s, repeat=%s}",
            playlists.size(),
            current != null ? current.getName() : "none",
            currentTrackIndex.get(),
            current != null ? current.getSongs().size() : 0,
            playMode.get(),
            repeatMode.get()
        );
    }
}