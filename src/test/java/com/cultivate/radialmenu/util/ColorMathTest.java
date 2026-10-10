package com.cultivate.radialmenu.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColorMathTest {

    private static final int FALLBACK = 0x123456;

    @Test
    void parsesSixDigitsWithOrWithoutHash() {
        assertEquals(0x3C78D8, ColorMath.parseRgb("#3C78D8", FALLBACK));
        assertEquals(0x3C78D8, ColorMath.parseRgb("3c78d8", FALLBACK));
        assertEquals(0x3C78D8, ColorMath.parseRgb("  #3C78D8 ", FALLBACK));
    }

    @Test
    void expandsThreeDigits() {
        assertEquals(0x3388DD, ColorMath.parseRgb("#38D", FALLBACK));
    }

    @Test
    void fallsBackOnAnythingElse() {
        assertEquals(FALLBACK, ColorMath.parseRgb(null, FALLBACK));
        assertEquals(FALLBACK, ColorMath.parseRgb("", FALLBACK));
        assertEquals(FALLBACK, ColorMath.parseRgb("#12345", FALLBACK));
        assertEquals(FALLBACK, ColorMath.parseRgb("#GGGGGG", FALLBACK));
        assertEquals(FALLBACK, ColorMath.parseRgb("#FF3C78D8", FALLBACK));
    }

    @Test
    void formatsAsUppercaseHexAndDropsAlpha() {
        assertEquals("#3C78D8", ColorMath.formatRgb(0x3C78D8));
        assertEquals("#000000", ColorMath.formatRgb(0));
        assertEquals("#3C78D8", ColorMath.formatRgb(0xFF3C78D8));
    }

    @Test
    void combinesColorAndOpacity() {
        assertEquals(0xFF3C78D8, ColorMath.toArgb(0x3C78D8, 100));
        assertEquals(0x003C78D8, ColorMath.toArgb(0x3C78D8, 0));
        assertEquals(0x803C78D8, ColorMath.toArgb(0x3C78D8, 50));
    }

    @Test
    void clampsOpacity() {
        assertEquals(0xFF000000, ColorMath.toArgb(0, 150));
        assertEquals(0, ColorMath.toArgb(0, -5));
    }

    @Test
    void opacityRoundTrips() {
        for (int percent = 0; percent <= 100; percent++) {
            assertEquals(percent, ColorMath.toOpacityPercent(ColorMath.toArgb(0xFFFFFF, percent)));
        }
    }
}
