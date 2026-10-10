package com.cultivate.radialmenu.menu.icon;

/**
 * One of the bundled symbols (see {@link SymbolSheet}), in a color.
 *
 * <p>
 * The name is kept even if no symbol has it, so a menu file from a newer version keeps the section; it draws as a
 * question mark.
 */
public class SymbolIcon implements SectionIcon {

    public static final int DEFAULT_COLOR = 0xFFFFFF;

    private static final String MISSING_TEXT = "?";
    private static final int MISSING_COLOR = 0xFF5555;

    private final String name;
    private final int color;

    public SymbolIcon(String name, int color) {
        this.name = name;
        this.color = color & 0xFFFFFF;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the color as RGB, without alpha
     */
    public int getColor() {
        return color;
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        SymbolSheet.Symbol symbol = SymbolSheet.find(name);

        if (symbol == null) {
            TextIcon.drawCenteredText(MISSING_TEXT, MISSING_COLOR, centerX, centerY, size);
            return;
        }
        SymbolSheet.draw(symbol, centerX, centerY, size, color);
    }
}
