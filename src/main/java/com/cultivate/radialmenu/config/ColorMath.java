package com.cultivate.radialmenu.config;

import java.util.Locale;

/**
 * Converts between the config's {@code #RRGGBB} text plus opacity percentage and ARGB colors.
 *
 * <p>
 * Kept out of {@link RadialMenuConfig} because GTNHLib turns every field of a config class, constants included, into
 * an option.
 */
final class ColorMath {

    private static final int MAX_PERCENT = 100;
    private static final int MAX_CHANNEL = 255;

    private ColorMath() {}

    /**
     * Reads a color typed as {@code #RRGGBB}, {@code RRGGBB} or the short {@code #RGB}. Eight digits (the old
     * {@code AARRGGBB} form) keep only the color part. Anything else gives {@code fallback}.
     */
    static int parseRgb(String text, int fallback) {
        String hex = text == null ? "" : text.trim();

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            hex = new String(
                new char[] { hex.charAt(0), hex.charAt(0), hex.charAt(1), hex.charAt(1), hex.charAt(2),
                    hex.charAt(2) });
        } else if (hex.length() == 8) {
            hex = hex.substring(2);
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

    static String formatRgb(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /**
     * @return a color and an opacity in percent combined into one ARGB value
     */
    static int toArgb(int rgb, int opacityPercent) {
        int percent = Math.max(0, Math.min(MAX_PERCENT, opacityPercent));
        int alpha = Math.round((percent * MAX_CHANNEL) / (float) MAX_PERCENT);

        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    static int toOpacityPercent(int argb) {
        return Math.round(((argb >>> 24) * MAX_PERCENT) / (float) MAX_CHANNEL);
    }
}
