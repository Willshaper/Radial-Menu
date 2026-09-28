package com.cultivate.radialmenu.util;

import java.util.Locale;

/**
 * Converts between colors written as {@code #RRGGBB} text, opacity percentages and ARGB integers.
 */
public final class ColorMath {

    private static final int MAX_PERCENT = 100;
    private static final int MAX_CHANNEL = 255;

    private ColorMath() {}

    /**
     * Reads a color typed as {@code #RRGGBB}, {@code RRGGBB} or the short {@code #RGB}. Anything else gives
     * {@code fallback}.
     */
    public static int parseRgb(String text, int fallback) {
        String hex = text == null ? "" : text.trim();

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            hex = new String(
                new char[] { hex.charAt(0), hex.charAt(0), hex.charAt(1), hex.charAt(1), hex.charAt(2),
                    hex.charAt(2) });
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String formatRgb(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /**
     * @return a color and an opacity in percent combined into one ARGB value
     */
    public static int toArgb(int rgb, int opacityPercent) {
        int percent = Math.max(0, Math.min(MAX_PERCENT, opacityPercent));
        int alpha = Math.round((percent * MAX_CHANNEL) / (float) MAX_PERCENT);

        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int toOpacityPercent(int argb) {
        return Math.round(((argb >>> 24) * MAX_PERCENT) / (float) MAX_CHANNEL);
    }
}
