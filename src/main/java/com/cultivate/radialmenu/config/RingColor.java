package com.cultivate.radialmenu.config;

/**
 * The ring's three configurable colors, each stored in {@link RadialMenuConfig} as a {@code #RRGGBB} text and an
 * opacity percentage.
 */
public enum RingColor {

    SECTION("radialmenu.color.section", 0x141414),
    HOVER("radialmenu.color.hover", 0x3C78D8),
    CENTER("radialmenu.color.center", 0x141414);

    private final String langKey;
    private final int defaultRgb;

    RingColor(String langKey, int defaultRgb) {
        this.langKey = langKey;
        this.defaultRgb = defaultRgb;
    }

    public String getLangKey() {
        return langKey;
    }

    public int getArgb() {
        return RadialMenuConfig.toArgb(RadialMenuConfig.parseRgb(getText(), defaultRgb), getOpacity());
    }

    /**
     * Stores a color picked in the game. The caller saves the config.
     */
    public void setArgb(int argb) {
        setText(RadialMenuConfig.formatRgb(argb));
        setOpacity(RadialMenuConfig.toOpacityPercent(argb));
    }

    /**
     * Rewrites the stored text in the standard {@code #RRGGBB} form.
     *
     * @return true if the text changed
     */
    boolean normalize() {
        String before = getText();
        String after = RadialMenuConfig.formatRgb(RadialMenuConfig.parseRgb(before, defaultRgb));

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
