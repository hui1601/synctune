package ac.cwnu.synctune.player.playlist;

import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.model.Playlist;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 곡 순서 섞기 등 부가 기능을 제공하는 클래스
 * 스마트 셔플, 히스토리 기반 셔플 등 다양한 셔플 알고리즘을 지원합니다.
 */
public class TrackShuffler {
    private static final Logger log = LogManager.getLogger(TrackShuffler.class);
    
    // 셔플 히스토리 (최근 재생된 곡들의 인덱스)
    private final Deque<Integer> shuffleHistory = new ArrayDeque<>();
    
    // 현재 셔플된 순서 (원본 인덱스 -> 셔플된 순서)
    private List<Integer> currentShuffleOrder = new ArrayList<>();
    
    // 셔플 설정
    private ShuffleMode shuffleMode = ShuffleMode.SMART;
    private int maxHistorySize = 50; // 최대 히스토리 크기
    
    /**
     * 셔플 모드
     */
    public enum ShuffleMode {
        RANDOM,     // 완전 랜덤
        SMART,      // 스마트 셔플 (최근 재생된 곡 회피)
        WEIGHTED    // 가중치 기반 셔플 (TODO: 곡 선호도 기반)
    }
    
    /**
     * 플레이리스트의 곡 순서를 섞습니다
     */
    public List<Integer> shufflePlaylist(PlaylistManager.PlaylistInfo playlist) {
        if (playlist == null || playlist.getSongs().isEmpty()) {
            currentShuffleOrder.clear();
            return new ArrayList<>();
        }
        
        int playlistSize = playlist.getSongs().size();
        List<Integer> indices = createIndexList(playlistSize);
        
        switch (shuffleMode) {
            case RANDOM:
                shuffleRandom(indices);
                break;
            case SMART:
                shuffleSmart(indices);
                break;
            case WEIGHTED:
                shuffleWeighted(indices, playlist);
                break;
        }
        
        currentShuffleOrder = new ArrayList<>(indices);
        
        log.debug("플레이리스트 셔플됨: {} ({}곡, 모드: {})", 
            playlist.getName(), playlistSize, shuffleMode);
        
        return new ArrayList<>(currentShuffleOrder);
    }
    
    /**
     * 다음 셔플된 곡의 인덱스를 반환합니다
     */
    public int getNextShuffledIndex(int currentIndex, int playlistSize) {
        if (currentShuffleOrder.isEmpty() || playlistSize == 0) {
            return generateRandomIndex(playlistSize, currentIndex);
        }
        
        // 현재 인덱스가 셔플 순서에서 몇 번째인지 찾기
        int currentPosition = currentShuffleOrder.indexOf(currentIndex);
        
        if (currentPosition < 0) {
            // 현재 인덱스가 셔플 순서에 없으면 랜덤 선택
            return generateRandomIndex(playlistSize, currentIndex);
        }
        
        // 다음 위치의 인덱스 반환
        int nextPosition = (currentPosition + 1) % currentShuffleOrder.size();
        return currentShuffleOrder.get(nextPosition);
    }
    
    /**
     * 이전 셔플된 곡의 인덱스를 반환합니다
     */
    public int getPreviousShuffledIndex(int currentIndex, int playlistSize) {
        if (currentShuffleOrder.isEmpty() || playlistSize == 0) {
            return generateRandomIndex(playlistSize, currentIndex);
        }
        
        // 현재 인덱스가 셔플 순서에서 몇 번째인지 찾기
        int currentPosition = currentShuffleOrder.indexOf(currentIndex);
        
        if (currentPosition < 0) {
            // 현재 인덱스가 셔플 순서에 없으면 랜덤 선택
            return generateRandomIndex(playlistSize, currentIndex);
        }
        
        // 이전 위치의 인덱스 반환
        int previousPosition = (currentPosition - 1 + currentShuffleOrder.size()) % currentShuffleOrder.size();
        return currentShuffleOrder.get(previousPosition);
    }
    
    /**
     * 히스토리에 재생된 곡을 추가합니다
     */
    public void addToHistory(int trackIndex) {
        shuffleHistory.addLast(trackIndex);
        
        // 히스토리 크기 제한
        while (shuffleHistory.size() > maxHistorySize) {
            shuffleHistory.removeFirst();
        }
        
        log.debug("셔플 히스토리에 추가: {} (히스토리 크기: {})", trackIndex, shuffleHistory.size());
    }
    
    /**
     * 셔플 히스토리를 초기화합니다
     */
    public void clearHistory() {
        shuffleHistory.clear();
        log.debug("셔플 히스토리 초기화됨");
    }
    
    /**
     * 현재 셔플 순서를 반환합니다
     */
    public List<Integer> getCurrentShuffleOrder() {
        return new ArrayList<>(currentShuffleOrder);
    }
    
    /**
     * 셔플 모드를 설정합니다
     */
    public void setShuffleMode(ShuffleMode mode) {
        if (this.shuffleMode != mode) {
            this.shuffleMode = mode;
            log.info("셔플 모드 변경: {}", mode);
        }
    }
    
    /**
     * 현재 셔플 모드를 반환합니다
     */
    public ShuffleMode getShuffleMode() {
        return shuffleMode;
    }
    
    /**
     * 히스토리 최대 크기를 설정합니다
     */
    public void setMaxHistorySize(int maxSize) {
        this.maxHistorySize = Math.max(1, maxSize);
        
        // 현재 히스토리가 새 크기보다 크면 조정
        while (shuffleHistory.size() > this.maxHistorySize) {
            shuffleHistory.removeFirst();
        }
        
        log.debug("히스토리 최대 크기 설정: {}", this.maxHistorySize);
    }
    
    /**
     * 히스토리 크기를 반환합니다
     */
    public int getHistorySize() {
        return shuffleHistory.size();
    }
    
    /**
     * 특정 곡이 최근에 재생되었는지 확인합니다
     */
    public boolean wasRecentlyPlayed(int trackIndex) {
        return shuffleHistory.contains(trackIndex);
    }
    
    /**
     * 곡이 얼마나 최근에 재생되었는지 반환합니다 (0이 가장 최근)
     */
    public int getRecencyScore(int trackIndex) {
        List<Integer> historyList = new ArrayList<>(shuffleHistory);
        Collections.reverse(historyList); // 최근 것부터
        
        int index = historyList.indexOf(trackIndex);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
    
    // ========== Private 셔플 알고리즘들 ==========
    
    private List<Integer> createIndexList(int size) {
        List<Integer> indices = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            indices.add(i);
        }
        return indices;
    }
    
    /**
     * 완전 랜덤 셔플
     */
    private void shuffleRandom(List<Integer> indices) {
        Collections.shuffle(indices, ThreadLocalRandom.current());
        log.debug("완전 랜덤 셔플 적용");
    }
    
    /**
     * 스마트 셔플 (최근 재생된 곡 회피)
     */
    private void shuffleSmart(List<Integer> indices) {
        int size = indices.size();
        
        if (size <= 1) {
            return;
        }
        
        // Fisher-Yates 셔플의 변형으로 최근 재생된 곡을 뒤로 배치
        for (int i = size - 1; i > 0; i--) {
            int candidateIndex = selectSmartCandidate(indices, i);
            if (candidateIndex != i) {
                Collections.swap(indices, i, candidateIndex);
            }
        }
        
        log.debug("스마트 셔플 적용 (히스토리 크기: {})", shuffleHistory.size());
    }
    
    /**
     * 가중치 기반 셔플 (TODO: 향후 곡 선호도/재생 횟수 기반으로 확장)
     */
    private void shuffleWeighted(List<Integer> indices, PlaylistManager.PlaylistInfo playlist) {
        // 현재는 스마트 셔플과 동일하게 구현
        // 향후 곡의 재생 횟수, 좋아요 등의 메타데이터를 활용하여 가중치 적용 예정
        shuffleSmart(indices);
        log.debug("가중치 기반 셔플 적용 (현재는 스마트 셔플과 동일)");
    }
    
    /**
     * 스마트 셔플을 위한 후보 선택
     */
    private int selectSmartCandidate(List<Integer> indices, int maxIndex) {
        List<Integer> candidates = new ArrayList<>();
        List<Integer> recentlyPlayed = new ArrayList<>();
        
        // 후보군을 최근 재생된 곡과 그렇지 않은 곡으로 분류
        for (int i = 0; i <= maxIndex; i++) {
            int trackIndex = indices.get(i);
            if (wasRecentlyPlayed(trackIndex)) {
                recentlyPlayed.add(i);
            } else {
                candidates.add(i);
            }
        }
        
        // 최근 재생되지 않은 곡이 있으면 그 중에서 선택
        if (!candidates.isEmpty()) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        
        // 모든 곡이 최근 재생되었다면, 가장 오래된 것부터 우선 선택
        if (!recentlyPlayed.isEmpty()) {
            return selectLeastRecentCandidate(indices, recentlyPlayed);
        }
        
        // 기본적으로 랜덤 선택
        return ThreadLocalRandom.current().nextInt(maxIndex + 1);
    }
    
    /**
     * 가장 오래 전에 재생된 곡을 선택
     */
    private int selectLeastRecentCandidate(List<Integer> indices, List<Integer> candidatePositions) {
        int bestPosition = candidatePositions.get(0);
        int bestRecencyScore = getRecencyScore(indices.get(bestPosition));
        
        for (int position : candidatePositions) {
            int trackIndex = indices.get(position);
            int recencyScore = getRecencyScore(trackIndex);
            
            if (recencyScore > bestRecencyScore) {
                bestRecencyScore = recencyScore;
                bestPosition = position;
            }
        }
        
        return bestPosition;
    }
    
    /**
     * 랜덤 인덱스 생성 (현재 인덱스 제외)
     */
    private int generateRandomIndex(int playlistSize, int excludeIndex) {
        if (playlistSize <= 1) {
            return 0;
        }
        
        int randomIndex;
        do {
            randomIndex = ThreadLocalRandom.current().nextInt(playlistSize);
        } while (randomIndex == excludeIndex);
        
        return randomIndex;
    }
    
    // ========== 고급 셔플 기능들 ==========
    
    /**
     * 특정 장르나 아티스트를 우선하는 셔플 (TODO: 향후 구현)
     */
    public List<Integer> shuffleWithPreference(PlaylistManager.PlaylistInfo playlist, String preferredGenre, String preferredArtist) {
        // TODO: MusicInfo에 장르, 아티스트 정보가 추가되면 구현
        log.debug("선호도 기반 셔플 (미구현): 장르={}, 아티스트={}", preferredGenre, preferredArtist);
        return shufflePlaylist(playlist);
    }
    
    /**
     * 시간대별 분위기에 맞는 셔플 (TODO: 향후 구현)
     */
    public List<Integer> shuffleByTimeOfDay(PlaylistManager.PlaylistInfo playlist) {
        // TODO: 시간대별 음악 분위기 분석 기능 추가 시 구현
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        log.debug("시간대별 셔플 (미구현): 현재 시간={}", hour);
        return shufflePlaylist(playlist);
    }
    
    /**
     * 에너지 레벨 기반 셔플 (TODO: 향후 구현)
     */
    public List<Integer> shuffleByEnergyLevel(PlaylistManager.PlaylistInfo playlist, EnergyLevel targetLevel) {
        // TODO: 곡의 BPM, 음성 분석 데이터 등을 활용한 에너지 레벨 기반 셔플
        log.debug("에너지 레벨 기반 셔플 (미구현): 목표 레벨={}", targetLevel);
        return shufflePlaylist(playlist);
    }
    
    /**
     * 에너지 레벨 열거형 (향후 사용)
     */
    public enum EnergyLevel {
        LOW,     // 차분한 음악
        MEDIUM,  // 보통 에너지
        HIGH     // 역동적인 음악
    }
    
    // ========== 유틸리티 메서드들 ==========
    
    /**
     * 셔플 상태를 초기화합니다
     */
    public void reset() {
        clearHistory();
        currentShuffleOrder.clear();
        log.debug("셔플러 상태 초기화됨");
    }
    
    /**
     * 현재 셔플 통계를 반환합니다
     */
    public ShuffleStatistics getStatistics() {
        return new ShuffleStatistics(
            shuffleMode,
            currentShuffleOrder.size(),
            shuffleHistory.size(),
            maxHistorySize
        );
    }
    
    /**
     * 셔플러 상태를 문자열로 반환합니다 (디버깅용)
     */
    @Override
    public String toString() {
        return String.format("TrackShuffler{mode=%s, order=%d, history=%d/%d}",
            shuffleMode, currentShuffleOrder.size(), shuffleHistory.size(), maxHistorySize);
    }
    
    // ========== Inner Classes ==========
    
    /**
     * 셔플 통계 정보
     */
    public static class ShuffleStatistics {
        private final ShuffleMode mode;
        private final int shuffleOrderSize;
        private final int historySize;
        private final int maxHistorySize;
        
        public ShuffleStatistics(ShuffleMode mode, int shuffleOrderSize, int historySize, int maxHistorySize) {
            this.mode = mode;
            this.shuffleOrderSize = shuffleOrderSize;
            this.historySize = historySize;
            this.maxHistorySize = maxHistorySize;
        }
        
        public ShuffleMode getMode() { return mode; }
        public int getShuffleOrderSize() { return shuffleOrderSize; }
        public int getHistorySize() { return historySize; }
        public int getMaxHistorySize() { return maxHistorySize; }
        
        @Override
        public String toString() {
            return String.format("ShuffleStatistics{mode=%s, order=%d, history=%d/%d}",
                mode, shuffleOrderSize, historySize, maxHistorySize);
        }
    }
}