package com.cultivate.radialmenu.config;

/**
 * The ring's three configurable colors, each stored in {@link RadialMenuConfig} as a {@code #RRGGBB} text and an
 * opacity percentage.
 */
public enum RingColor {

    // Keep the defaults in step with the field defaults in RadialMenuConfig.
    SECTION("radialmenu.color.section", 0x141414, 63),
    HOVER("radialmenu.color.hover", 0x3C78D8, 75),
    CENTER("radialmenu.color.center", 0x141414, 75);

    private final String langKey;
    private final int defaultRgb;
    private final int defaultOpacity;

    RingColor(String langKey, int defaultRgb, int defaultOpacity) {
        this.langKey = langKey;
        this.defaultRgb = defaultRgb;
        this.defaultOpacity = defaultOpacity;
    }

    public String getLangKey() {
        return langKey;
    }

    public int getArgb() {
        return ColorMath.toArgb(ColorMath.parseRgb(getText(), defaultRgb), getOpacity());
    }

    /**
     * Stores a color picked in the game. The caller saves the config.
     */
    public void setArgb(int argb) {
        setText(ColorMath.formatRgb(argb));
        setOpacity(ColorMath.toOpacityPercent(argb));
    }

    /**
     * Puts the color and opacity back to their defaults. The caller saves the config.
     */
    public void reset() {
        setText(ColorMath.formatRgb(defaultRgb));
        setOpacity(defaultOpacity);
    }

    /**
     * Rewrites the stored text in the standard {@code #RRGGBB} form.
     *
     * @return true if the text changed
     */
    boolean normalize() {
        String before = getText();
        String after = ColorMath.formatRgb(ColorMath.parseRgb(before, defaultRgb));

        setText(after);
        return !after.equals(before);
    }

    private String getText() {
        return switch (this) {
            case SECTION -> RadialMenuConfig.sectionColor;
            case HOVER -> RadialMenuConfig.hoverColor;
            case CENTER -> RadialMenuConfig.centerColor;
        };
    }

    private void setText(String text) {
        switch (this) {
            case SECTION -> RadialMenuConfig.sectionColor = text;
            case HOVER -> RadialMenuConfig.hoverColor = text;
            case CENTER -> RadialMenuConfig.centerColor = text;
        }
    }

    private int getOpacity() {
        return switch (this) {
            case SECTION -> RadialMenuConfig.sectionOpacity;
            case HOVER -> RadialMenuConfig.hoverOpacity;
            case CENTER -> RadialMenuConfig.centerOpacity;
        };
    }

    private void setOpacity(int opacity) {
        switch (this) {
            case SECTION -> RadialMenuConfig.sectionOpacity = opacity;
            case HOVER -> RadialMenuConfig.hoverOpacity = opacity;
            case CENTER -> RadialMenuConfig.centerOpacity = opacity;
        }
    }
}
