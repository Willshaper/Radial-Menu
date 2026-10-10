import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * Makes the symbol sheet and its index from the Phosphor icon font: symbols.png, white on transparent, and symbols.txt,
 * one line per symbol in sheet order with its name and search words.
 *
 * <p>
 * Phosphor is MIT licensed (https://phosphoricons.com). Download the npm packages {@code @phosphor-icons/web} (the
 * font and CSS) and {@code @phosphor-icons/core} (names and search tags), unpack them, then run with Java 11 or newer:
 *
 * <pre>
 * java tools/SymbolSheet.java web/src/fill/Phosphor-Fill.ttf web/src/fill/style.css core/dist/index.mjs out
 * </pre>
 *
 * Brand logos are left out. The cell and sheet sizes must match the constants in
 * {@code com.cultivate.radialmenu.menu.icon.SymbolSheet}.
 */
public class SymbolSheet {

    private static final int CELL = 48;
    private static final int SHEET_WIDTH = 2016;

    public static void main(String[] args) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(args[0])).deriveFont((float) CELL);
        Map<String, Integer> codepoints = readCodepoints(Files.readString(new File(args[1]).toPath()));
        List<String> lines = readIndex(Files.readString(new File(args[2]).toPath()), codepoints);
        File out = new File(args[3]);
        int columns = SHEET_WIDTH / CELL;
        int rows = (lines.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(SHEET_WIDTH, Integer.highestOneBit((rows * CELL) - 1) << 1,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        FontRenderContext context;
        LineMetrics metrics;

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setColor(Color.WHITE);
        context = graphics.getFontRenderContext();
        metrics = font.getLineMetrics("x", context);
        for (int i = 0; i < lines.size(); i++) {
            String name = lines.get(i)
                .split(" ")[0];
            String glyph = new String(Character.toChars(codepoints.get(name)));
            // Each glyph fills a square em box; this puts the box's bottom on the cell's bottom.
            float baseline = ((i / columns) * CELL) + ((CELL * metrics.getAscent()) / (metrics.getAscent()
                + metrics.getDescent()));

            graphics.drawGlyphVector(font.createGlyphVector(context, glyph), (i % columns) * CELL, baseline);
        }
        graphics.dispose();
        out.mkdirs();
        ImageIO.write(sheet, "png", new File(out, "symbols.png"));
        Files.write(new File(out, "symbols.txt").toPath(), (String.join("\n", lines) + "\n").getBytes(
            StandardCharsets.UTF_8));
        System.out.println(lines.size() + " symbols");
    }

    /**
     * @return the codepoint of each icon in the font, from lines like {@code .ph-fill.ph-house:before { content:
     *         "\e2c2"; }}
     */
    private static Map<String, Integer> readCodepoints(String css) {
        Map<String, Integer> codepoints = new HashMap<>();
        Matcher matcher = Pattern.compile("\\.ph-fill\\.ph-([a-z0-9-]+):before \\{\\s*content: \"\\\\([0-9a-f]+)\";")
            .matcher(css);

        while (matcher.find()) {
            codepoints.put(matcher.group(1), Integer.parseInt(matcher.group(2), 16));
        }
        return codepoints;
    }

    /**
     * @return one line per icon: its name, then its search tags, leaving out brand logos
     */
    private static List<String> readIndex(String metadata, Map<String, Integer> codepoints) {
        List<String> lines = new ArrayList<>();
        Matcher matcher = Pattern.compile(
            "name: \"([a-z0-9-]+)\",\\s*pascal_name: \"\\w+\",\\s*(?:alias: \\{[^}]*\\},\\s*)?"
                + "categories: \\[([^\\]]*)\\],[\\s\\S]*?tags: \\[([^\\]]*)\\]")
            .matcher(metadata);

        while (matcher.find()) {
            String name = matcher.group(1);
            String tags = matcher.group(3)
                .replaceAll("[\"\\s]|\\*new\\*|\\*updated\\*", "")
                .replace(',', ' ')
                .trim()
                .replaceAll(" +", " ");

            if (matcher.group(2)
                .contains("BRAND") || !codepoints.containsKey(name)) {
                continue;
            }
            lines.add(tags.isEmpty() ? name : name + " " + tags);
        }
        return lines;
    }
}
