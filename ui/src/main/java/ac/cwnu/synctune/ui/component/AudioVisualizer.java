package ac.cwnu.synctune.ui.component;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class AudioVisualizer extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer animator;
    private double[] audioData = new double[64];
    private VisualizerType currentType = VisualizerType.BARS;
    
    public enum VisualizerType {
        BARS, WAVE, CIRCULAR, SPECTRUM
    }
    
    public AudioVisualizer(double width, double height) {
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        
        // 더미 데이터로 초기화
        initializeDummyData();
        
        // 배경 설정
        setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a1a, #2d2d2d);");
        
        startAnimation();
    }
    
    private void initializeDummyData() {
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = Math.random() * 50 + 10;
        }
    }
    
    private void startAnimation() {
        animator = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAudioData();
                drawVisualization();
            }
        };
        animator.start();
    }
    
    private void updateAudioData() {
        // 실제로는 오디오 엔진에서 데이터를 받아와야 함
        // 시뮬레이션을 위한 랜덤 데이터
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = audioData[i] * 0.9 + Math.random() * 20;
        }
    }
    
    private void drawVisualization() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        // 캔버스 클리어
        gc.clearRect(0, 0, width, height);
        
        switch (currentType) {
            case BARS:
                drawBars(width, height);
                break;
            case WAVE:
                drawWave(width, height);
                break;
            case CIRCULAR:
                drawCircular(width, height);
                break;
            case SPECTRUM:
                drawSpectrum(width, height);
                break;
        }
    }
    
    private void drawBars(double width, double height) {
        double barWidth = width / audioData.length;
        
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, null,
            new Stop(0, Color.CYAN),
            new Stop(0.5, Color.LIME),
            new Stop(1, Color.YELLOW)
        );
        
        gc.setFill(gradient);
        
        for (int i = 0; i < audioData.length; i++) {
            double barHeight = (audioData[i] / 100.0) * height * 0.8;
            double x = i * barWidth;
            double y = height - barHeight;
            
            gc.fillRoundRect(x + 1, y, barWidth - 2, barHeight, 3, 3);
        }
    }
    
    private void drawWave(double width, double height) {
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);
        
        gc.beginPath();
        
        for (int i = 0; i < audioData.length; i++) {
            double x = (i / (double) audioData.length) * width;
            double y = height / 2 + (audioData[i] - 50) * height / 100;
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        
        gc.stroke();
    }
    
    private void drawCircular(double width, double height) {
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) / 4;
        
        gc.setStroke(Color.LIME);
        gc.setLineWidth(3);
        
        for (int i = 0; i < audioData.length; i++) {
            double angle = (2 * Math.PI * i) / audioData.length;
            double length = (audioData[i] / 100.0) * radius;
            
            double x1 = centerX + Math.cos(angle) * radius;
            double y1 = centerY + Math.sin(angle) * radius;
            double x2 = centerX + Math.cos(angle) * (radius + length);
            double y2 = centerY + Math.sin(angle) * (radius + length);
            
            gc.strokeLine(x1, y1, x2, y2);
        }
    }
    
    private void drawSpectrum(double width, double height) {
        double bandWidth = width / audioData.length;
        
        for (int i = 0; i < audioData.length; i++) {
            double intensity = audioData[i] / 100.0;
            Color color = Color.hsb(i * 360.0 / audioData.length, 1.0, intensity);
            gc.setFill(color);
            
            double bandHeight = intensity * height;
            double x = i * bandWidth;
            double y = height - bandHeight;
            
            gc.fillRect(x, y, bandWidth - 1, bandHeight);
        }
    }
    
    public void setVisualizerType(VisualizerType type) {
        this.currentType = type;
    }
    
    public void updateAudioData(double[] newData) {
        if (newData.length == audioData.length) {
            System.arraycopy(newData, 0, audioData, 0, newData.length);
        }
    }
    
    public void stopAnimation() {
        if (animator != null) {
            animator.stop();
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        stopAnimation();
        super.finalize();
    }
}