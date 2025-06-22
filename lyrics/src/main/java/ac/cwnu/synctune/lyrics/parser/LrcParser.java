package ac.cwnu.synctune.lyrics.parser;

import ac.cwnu.synctune.sdk.model.LrcLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class LrcParser {
    private static final Logger log = LoggerFactory.getLogger(LrcParser.class);

    private static final Pattern LINE_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})] ?(.*)");

    // 시도할 인코딩 목록 (한국어 환경에서 자주 사용되는 순서대로)
    private static final List<Charset> ENCODINGS_TO_TRY = Arrays.asList(
        StandardCharsets.UTF_8,
        Charset.forName("EUC-KR"),
        Charset.forName("CP949"),
        Charset.forName("MS949"),
        StandardCharsets.ISO_8859_1,
        Charset.defaultCharset()
    );

    public static List<LrcLine> parse(File file) throws IOException {
        List<LrcLine> lines = new ArrayList<>();
        
        // 여러 인코딩을 시도해서 읽기
        for (Charset charset : ENCODINGS_TO_TRY) {
            try {
                lines = parseWithEncoding(file, charset);
                
                // 한글이 제대로 읽혔는지 확인
                if (isValidKoreanContent(lines)) {
                    log.debug("LRC 파일을 {}로 성공적으로 파싱: {}", charset.name(), file.getName());
                    return lines;
                }
                
            } catch (Exception e) {
                log.debug("인코딩 {} 시도 실패: {} - {}", charset.name(), file.getName(), e.getMessage());
            }
        }
        
        // 모든 인코딩 시도가 실패했다면 UTF-8로 강제 읽기
        log.warn("모든 인코딩 시도 실패, UTF-8로 강제 읽기: {}", file.getName());
        return parseWithEncoding(file, StandardCharsets.UTF_8);
    }

    private static List<LrcLine> parseWithEncoding(File file, Charset charset) throws IOException {
        List<LrcLine> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // BOM 제거 (UTF-8 BOM인 경우)
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                
                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    try {
                        int minutes = Integer.parseInt(matcher.group(1));
                        int seconds = Integer.parseInt(matcher.group(2));
                        int hundredths = Integer.parseInt(matcher.group(3));
                        String lyric = matcher.group(4).trim();

                        long timeMillis = (minutes * 60 + seconds) * 1000 + hundredths * 10;
                        lines.add(new LrcLine(timeMillis, lyric));
                    } catch (NumberFormatException e) {
                        log.debug("시간 파싱 실패: {}", line);
                    }
                }
            }
        }

        // 시간 순 정렬
        Collections.sort(lines);
        return lines;
    }

    /**
     * 파싱된 내용이 한글을 제대로 포함하고 있는지 확인
     */
    private static boolean isValidKoreanContent(List<LrcLine> lines) {
        if (lines.isEmpty()) {
            return true; // 빈 파일은 유효한 것으로 간주
        }
        
        int totalChars = 0;
        int koreanChars = 0;
        int questionMarkChars = 0;
        
        for (LrcLine line : lines) {
            String text = line.getText();
            if (text == null || text.isEmpty()) {
                continue;
            }
            
            for (char c : text.toCharArray()) {
                totalChars++;
                
                // 한글 문자 범위 확인
                if (isKoreanCharacter(c)) {
                    koreanChars++;
                }
                
                // 물음표나 깨진 문자 확인
                if (c == '?' || c == '\uFFFD') {
                    questionMarkChars++;
                }
            }
        }
        
        // 전체 문자의 20% 이상이 물음표나 깨진 문자면 잘못된 인코딩으로 판단
        if (totalChars > 0 && questionMarkChars > totalChars * 0.2) {
            return false;
        }
        
        // 한글이 포함되어 있거나, 영어만 있는 경우 유효한 것으로 판단
        return true;
    }

    /**
     * 한글 문자인지 확인
     */
    private static boolean isKoreanCharacter(char c) {
        // 한글 완성형 (가-힣)
        if (c >= 0xAC00 && c <= 0xD7AF) {
            return true;
        }
        // 한글 자모 (ㄱ-ㅎ, ㅏ-ㅣ)
        if (c >= 0x3131 && c <= 0x318E) {
            return true;
        }
        return false;
    }

    /**
     * 파일의 인코딩을 감지 시도 (간단한 휴리스틱)
     */
    public static Charset detectEncoding(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[Math.min(4096, (int) file.length())];
            int bytesRead = fis.read(buffer);
            
            // UTF-8 BOM 확인
            if (bytesRead >= 3 && 
                buffer[0] == (byte) 0xEF && 
                buffer[1] == (byte) 0xBB && 
                buffer[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
            
            // UTF-16 BOM 확인
            if (bytesRead >= 2) {
                if (buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) {
                    return StandardCharsets.UTF_16LE;
                }
                if (buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF) {
                    return StandardCharsets.UTF_16BE;
                }
            }
            
            // ASCII 범위 외의 문자가 많으면 EUC-KR 시도
            int nonAsciiCount = 0;
            for (int i = 0; i < bytesRead; i++) {
                if ((buffer[i] & 0xFF) > 127) {
                    nonAsciiCount++;
                }
            }
            
            if (nonAsciiCount > bytesRead * 0.1) {
                return Charset.forName("EUC-KR");
            }
            
        } catch (Exception e) {
            log.debug("인코딩 감지 실패: {}", file.getName());
        }
        
        return StandardCharsets.UTF_8;
    }
}