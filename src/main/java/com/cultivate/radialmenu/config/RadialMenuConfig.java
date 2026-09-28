package com.cultivate.radialmenu.config;

import java.util.Locale;

import com.cultivate.radialmenu.RadialMenuMod;
import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

/**
 * Appearance and behaviour settings. The menus themselves live in {@code config/radialmenu/menus.json} and are
 * edited in-game.
 *
 * <p>
 * Colors are stored as {@code #RRGGBB} plus a separate opacity so they are easy to read and type. They can also be
 * picked with a color picker in the menu manager, which writes them back here.
 */
@Config(modid = RadialMenuMod.MODID, category = "general")
public class RadialMenuConfig {

    private static final int MAX_PERCENT = 100;
    private static final int MAX_CHANNEL = 255;

    @Config.Comment("Distance in GUI pixels from the center of the screen to the middle of the ring.")
    @Config.RangeInt(min = 32, max = 160)
    @Config.DefaultInt(64)
    @Config.Name("Ring Radius")
    public static int ringRadius = 64;

    @Config.Comment("Width of the ring in GUI pixels.")
    @Config.RangeInt(min = 16, max = 96)
    @Config.DefaultInt(44)
    @Config.Name("Ring Thickness")
    public static int ringThickness = 44;

    @Config.Comment("Radius of the center circle, in GUI pixels. Pointing inside it selects nothing; clicking it goes back from a submenu.")
    @Config.RangeInt(min = 0, max = 64)
    @Config.DefaultInt(20)
    @Config.Name("Center Radius")
    public static int centerRadius = 20;

    @Config.Comment("Grow the ring out from the center when it opens.")
    @Config.DefaultBoolean(true)
    @Config.Name("Open Animation")
    public static boolean openAnimation = true;

    @Config.Comment("Show the hovered section's name under the ring and the menu's name above it.")
    @Config.DefaultBoolean(true)
    @Config.Name("Show Labels")
    public static boolean showLabels = true;

    @Config.Comment("Reopen the menu that was open last time instead of always starting at the first menu.")
    @Config.DefaultBoolean(true)
    @Config.Name("Remember Last Menu")
    public static boolean rememberLastMenu = true;

    @Config.Comment("Color of the sections, like #141414.")
    @Config.DefaultString("#141414")
    @Config.Name("Section Color")
    public static String sectionColor = "#141414";

    @Config.Comment("How opaque the sections are, in percent.")
    @Config.RangeInt(min = 0, max = 100)
    @Config.DefaultInt(63)
    @Config.Name("Section Opacity")
    public static int sectionOpacity = 63;

    @Config.Comment("Color of the section under the mouse, like #3C78D8.")
    @Config.DefaultString("#3C78D8")
    @Config.Name("Hover Color")
    public static String hoverColor = "#3C78D8";

    @Config.Comment("How opaque the section under the mouse is, in percent.")
    @Config.RangeInt(min = 0, max = 100)
    @Config.DefaultInt(75)
    @Config.Name("Hover Opacity")
    public static int hoverOpacity = 75;

    @Config.Comment("Color of the center circle, like #141414.")
    @Config.DefaultString("#141414")
    @Config.Name("Center Color")
    public static String centerColor = "#141414";

    @Config.Comment("How opaque the center circle is, in percent.")
    @Config.RangeInt(min = 0, max = 100)
    @Config.DefaultInt(75)
    @Config.Name("Center Opacity")
    public static int centerOpacity = 75;

    /**
     * Reads the config file into the fields, then saves it back if any color had to be cleaned up.
     */
    public static void load() throws ConfigException {
        ConfigurationManager.registerConfig(RadialMenuConfig.class);
        if (normalizeColors()) {
            ConfigurationManager.save(RadialMenuConfig.class);
        }
    }

    /**
     * Rewrites every color in the standard {@code #RRGGBB} form, so hand-typed or old values show cleanly in the config
     * screen.
     *
     * @return true if anything changed and the config should be saved
     */
    private static boolean normalizeColors() {
        boolean changed = false;

        for (RingColor color : RingColor.values()) {
            changed |= color.normalize();
        }
        return changed;
    }

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
