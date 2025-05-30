package ac.cwnu.synctune.sdk.event;

import ac.cwnu.synctune.sdk.model.MusicInfo;
import java.util.List;

/**
 * 미디어 정보 스캔 및 메타데이터 관련 이벤트를 정의합니다.
 */
public class MediaInfoEvent {

    private MediaInfoEvent() {}

    public static class MediaScanStartedEvent extends BaseEvent {
        private final String directoryPath;
        public MediaScanStartedEvent(String directoryPath) { this.directoryPath = directoryPath; }
        public String getDirectoryPath() { return directoryPath; }
        @Override public String toString() { return super.toString() + " {directory=" + directoryPath + "}"; }
    }

    public static class MediaScanProgressEvent extends BaseEvent {
        private final int scannedFiles;
        private final int totalFiles;
        public MediaScanProgressEvent(int scannedFiles, int totalFiles) {
            this.scannedFiles = scannedFiles;
            this.totalFiles = totalFiles;
        }
        public int getScannedFiles() { return scannedFiles; }
        public int getTotalFiles() { return totalFiles; }
        @Override public String toString() { return super.toString() + " {scanned=" + scannedFiles + "/" + totalFiles + "}"; }
    }

    public static class MediaScanCompletedEvent extends BaseEvent {
        private final List<MusicInfo> scannedMusicInfos;
        public MediaScanCompletedEvent(List<MusicInfo> scannedMusicInfos) {
            this.scannedMusicInfos = List.copyOf(scannedMusicInfos); // 불변 리스트로
        }
        public List<MusicInfo> getScannedMusicInfos() { return scannedMusicInfos; }
        @Override public String toString() { return super.toString() + " {count=" + scannedMusicInfos.size() + "}"; }
    }

    public static class MetadataUpdatedEvent extends BaseEvent { // LRC 등에서 메타데이터 업데이트 시
        private final MusicInfo updatedMusicInfo;
        public MetadataUpdatedEvent(MusicInfo updatedMusicInfo) {
            this.updatedMusicInfo = updatedMusicInfo;
        }
        public MusicInfo getUpdatedMusicInfo() { return updatedMusicInfo; }
        @Override public String toString() { return super.toString() + " {music=" + updatedMusicInfo.getTitle() + "}"; }
    }
}