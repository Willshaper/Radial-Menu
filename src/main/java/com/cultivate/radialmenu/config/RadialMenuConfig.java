package com.cultivate.radialmenu.config;

import com.cultivate.radialmenu.RadialMenuMod;
import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Appearance and behaviour settings. The menus themselves live in {@code config/radialmenu/menus.json} and are
 * edited in-game.
 */
@Config(modid = RadialMenuMod.MODID, category = "general")
public class RadialMenuConfig {

    private static final String HEX_COLOR_PATTERN = "^[0-9A-Fa-f]{8}$";

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

    @Config.Comment("Section background color, as AARRGGBB hex.")
    @Config.Pattern(HEX_COLOR_PATTERN)
    @Config.DefaultString("A0141414")
    @Config.Name("Section Color")
    public static String sectionColor = "A0141414";

    @Config.Comment("Hovered section color, as AARRGGBB hex.")
    @Config.Pattern(HEX_COLOR_PATTERN)
    @Config.DefaultString("C03C78D8")
    @Config.Name("Hover Color")
    public static String hoverColor = "C03C78D8";

    @Config.Comment("Center circle color, as AARRGGBB hex.")
    @Config.Pattern(HEX_COLOR_PATTERN)
    @Config.DefaultString("C0141414")
    @Config.Name("Center Color")
    public static String centerColor = "C0141414";

    public static int getSectionColor() {
        return parseColor(sectionColor, 0xA0141414);
    }

    public static int getHoverColor() {
        return parseColor(hoverColor, 0xC03C78D8);
    }

    public static int getCenterColor() {
        return parseColor(centerColor, 0xC0141414);
    }

    private static int parseColor(String hex, int fallback) {
        try {
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
