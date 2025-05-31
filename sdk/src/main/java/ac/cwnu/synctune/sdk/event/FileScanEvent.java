package ac.cwnu.synctune.sdk.event;

import java.io.File;

/**
 * 파일 스캔과 관련된 이벤트들을 정의합니다.
 */
public class FileScanEvent {

    private FileScanEvent() {
    }

    /**
     * 지정된 디렉토리에 대한 파일 스캔 시작을 알리는 이벤트입니다.
     */
    public static class ScanStartedEvent extends BaseEvent {
        private final String directoryPath;

        public ScanStartedEvent(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        @Override
        public String toString() {
            return super.toString() + " {directoryPath=\"" + directoryPath + "\"}";
        }
    }

    /**
     * 파일 스캔 중 개별 파일을 발견했을 때 발생하는 이벤트입니다.
     */
    public static class FileFoundEvent extends BaseEvent {
        private final File foundFile;
        // 필요에 따라 MusicInfo 객체로 변환되기 전의 정보를 담을 수 있습니다.

        public FileFoundEvent(File foundFile) {
            this.foundFile = foundFile;
        }

        public File getFoundFile() {
            return foundFile;
        }

        @Override
        public String toString() {
            return super.toString() + " {foundFile=\"" + foundFile.getAbsolutePath() + "\"}";
        }
    }

    /**
     * 파일 스캔 완료를 알리는 이벤트입니다.
     */
    public static class ScanCompletedEvent extends BaseEvent {
        private final String directoryPath;
        private final int totalFilesFound;

        public ScanCompletedEvent(String directoryPath, int totalFilesFound) {
            this.directoryPath = directoryPath;
            this.totalFilesFound = totalFilesFound;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        public int getTotalFilesFound() {
            return totalFilesFound;
        }

        @Override
        public String toString() {
            return super.toString() + " {directoryPath=\"" + directoryPath + "\", totalFilesFound=" + totalFilesFound + "}";
        }
    }

    /**
     * 파일 스캔 중 오류 발생을 알리는 이벤트입니다.
     */
    public static class ScanErrorEvent extends BaseEvent {
        private final String directoryPath;
        private final String errorMessage;
        private final Throwable cause;

        public ScanErrorEvent(String directoryPath, String errorMessage, Throwable cause) {
            this.directoryPath = directoryPath;
            this.errorMessage = errorMessage;
            this.cause = cause;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return super.toString() + " {directoryPath=\"" + directoryPath + "\", error=\"" + errorMessage + "\"}";
        }
    }
}