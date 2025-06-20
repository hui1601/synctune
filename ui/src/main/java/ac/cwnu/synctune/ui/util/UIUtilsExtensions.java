package ac.cwnu.synctune.ui.util;

// UIUtils 클래스에 추가할 메서드들

public class UIUtilsExtensions {
    
    /**
     * 음악 재생 시간을 다양한 형식으로 포맷
     */
    public static String formatTimeDetailed(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
    }
    
    /**
     * 파일 크기를 더 정확하게 포맷
     */
    public static String formatFileSizeDetailed(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        if (unitIndex == 0) {
            return String.format("%.0f %s", size, units[unitIndex]);
        } else {
            return String.format("%.2f %s", size, units[unitIndex]);
        }
    }
    
    /**
     * 재생 목록 시간 통계 포맷
     */
    public static String formatPlaylistDuration(long totalMillis, int songCount) {
        long hours = totalMillis / (1000 * 60 * 60);
        long minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60);
        
        StringBuilder result = new StringBuilder();
        result.append(songCount).append("곡");
        
        if (hours > 0) {
            result.append(", ").append(hours).append("시간");
            if (minutes > 0) {
                result.append(" ").append(minutes).append("분");
            }
        } else if (minutes > 0) {
            result.append(", ").append(minutes).append("분");
        }
        
        return result.toString();
    }
    
    /**
     * 색상 헥스 코드 검증
     */
    public static boolean isValidHexColor(String hexColor) {
        if (hexColor == null) return false;
        return hexColor.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }
    
    /**
     * 색상을 어둡게/밝게 조정
     */
    public static String adjustColorBrightness(String hexColor, double factor) {
        if (!isValidHexColor(hexColor)) return hexColor;
        
        try {
            javafx.scene.paint.Color color = javafx.scene.paint.Color.web(hexColor);
            javafx.scene.paint.Color adjusted = color.deriveColor(0, 1, factor, 1);
            
            return String.format("#%02X%02X%02X",
                (int) (adjusted.getRed() * 255),
                (int) (adjusted.getGreen() * 255),
                (int) (adjusted.getBlue() * 255));
        } catch (Exception e) {
            return hexColor;
        }
    }
    
    /**
     * 애니메이션 지속시간을 시스템 성능에 따라 조정
     */
    public static javafx.util.Duration getOptimalAnimationDuration(javafx.util.Duration baseDuration) {
        // 시스템 성능이 낮으면 애니메이션 시간 단축
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        double memoryUsage = 1.0 - ((double) freeMemory / totalMemory);
        
        if (memoryUsage > 0.8) {
            return baseDuration.multiply(0.5); // 메모리 사용률이 높으면 절반으로
        } else if (memoryUsage > 0.6) {
            return baseDuration.multiply(0.7);
        }
        
        return baseDuration;
    }
}