package ac.cwnu.synctune.lyrics.parser;

import ac.cwnu.synctune.sdk.model.LrcLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class LrcParser {

    private static final Pattern LINE_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})] ?(.*)");

    public static List<LrcLine> parse(File file) throws IOException {
        List<LrcLine> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int hundredths = Integer.parseInt(matcher.group(3));
                    String lyric = matcher.group(4).trim();

                    long timeMillis = (minutes * 60 + seconds) * 1000 + hundredths * 10;
                    lines.add(new LrcLine(timeMillis, lyric));
                }
            }
        }

        // 시간 순 정렬
        Collections.sort(lines);
        return lines;
    }
}
