package com.cultivate.radialmenu.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Uses the default ring size from the config: radius 64, thickness 44, center radius 20.
 */
class RadialLayoutTest {

    private static final float SCREEN_WIDTH = 480;
    private static final float SCREEN_HEIGHT = 270;
    private static final float CENTER_X = SCREEN_WIDTH / 2;
    private static final float CENTER_Y = SCREEN_HEIGHT / 2;
    private static final double DELTA = 1e-9;

    private static RadialLayout layout(int sections, boolean inline) {
        return new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, sections, 1f, inline);
    }

    /**
     * @return a point at the given angle (radians, clockwise from up) and distance from the screen center
     */
    private static float[] point(double angle, float distance) {
        return new float[] { CENTER_X + (float) (Math.sin(angle) * distance),
            CENTER_Y - (float) (Math.cos(angle) * distance) };
    }

    private static int sectionAt(RadialLayout layout, double angle, float distance) {
        float[] point = point(angle, distance);

        return layout.sectionAt(point[0], point[1]);
    }

    @Test
    void sectionZeroIsAtTheTopAndSectionsRunClockwise() {
        RadialLayout layout = layout(4, false);

        assertEquals(0, sectionAt(layout, 0, 60));
        assertEquals(1, sectionAt(layout, Math.PI / 2, 60));
        assertEquals(2, sectionAt(layout, Math.PI, 60));
        assertEquals(3, sectionAt(layout, 3 * Math.PI / 2, 60));
    }

    @Test
    void sectionsAreCenteredOnTheirAngle() {
        RadialLayout layout = layout(4, false);

        // Section 0 covers 45 degrees either side of straight up.
        assertEquals(0, sectionAt(layout, Math.toRadians(-44), 60));
        assertEquals(0, sectionAt(layout, Math.toRadians(44), 60));
        assertEquals(1, sectionAt(layout, Math.toRadians(46), 60));
        assertEquals(3, sectionAt(layout, Math.toRadians(-46), 60));
    }

    @Test
    void centerCircleSelectsTheCenter() {
        RadialLayout layout = layout(8, false);

        assertEquals(RadialLayout.CENTER, layout.sectionAt(CENTER_X, CENTER_Y));
        assertEquals(RadialLayout.CENTER, sectionAt(layout, 1, layout.centerRadius - 1));
    }

    @Test
    void pointsOutsideTheRingStillSelectByDirection() {
        RadialLayout layout = layout(8, false);

        assertEquals(2, sectionAt(layout, Math.PI / 2, layout.outerRadius + 100));
    }

    @Test
    void ringFitsOnTheScreenWithRoomForLabels() {
        RadialLayout plain = layout(24, false);
        RadialLayout inline = layout(24, true);

        assertTrue(plain.extentRadius <= (SCREEN_HEIGHT / 2));
        assertTrue(inline.extentRadius <= (SCREEN_HEIGHT / 2));
        assertTrue(inline.inlineInnerRadius > inline.outerRadius);
        assertEquals(inline.inlineOuterRadius, inline.extentRadius, DELTA);
    }

    @Test
    void openAnimationScalesTheRing() {
        RadialLayout full = new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, 8, 1f, false);
        RadialLayout half = new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, 8, 0.5f, false);

        assertEquals(full.outerRadius / 2, half.outerRadius, 1e-3);
    }

    @Test
    void inlineRingIsCenteredOnItsParentSection() {
        RadialLayout layout = layout(8, true);
        float distance = layout.inlineMiddleRadius();
        double parentAngle = layout.angleOf(2);
        double sectionWidth = layout.inlineSectionAngle(3);
        float[] middle = point(parentAngle, distance);
        float[] first = point(parentAngle - sectionWidth, distance);
        float[] last = point(parentAngle + sectionWidth, distance);

        assertEquals(1, layout.inlineSectionAt(middle[0], middle[1], 2, 3));
        assertEquals(0, layout.inlineSectionAt(first[0], first[1], 2, 3));
        assertEquals(2, layout.inlineSectionAt(last[0], last[1], 2, 3));
    }

    @Test
    void inlineRingIgnoresTheMainRingAndPointsBesideIt() {
        RadialLayout layout = layout(8, true);
        double parentAngle = layout.angleOf(0);
        float[] onMainRing = point(parentAngle, layout.middleRadius());
        float[] beside = point(parentAngle + Math.PI, layout.inlineMiddleRadius());
        float[] farOut = point(parentAngle, layout.inlineOuterRadius + 50);

        assertEquals(RadialLayout.NONE, layout.inlineSectionAt(onMainRing[0], onMainRing[1], 0, 3));
        assertEquals(RadialLayout.NONE, layout.inlineSectionAt(beside[0], beside[1], 0, 3));
        assertEquals(1, layout.inlineSectionAt(farOut[0], farOut[1], 0, 3));
    }

    @Test
    void inlineRingWrapsPastStraightUp() {
        RadialLayout layout = layout(8, true);
        double sectionWidth = layout.inlineSectionAngle(2);
        float[] left = point(-sectionWidth / 2, layout.inlineMiddleRadius());
        float[] right = point(sectionWidth / 2, layout.inlineMiddleRadius());

        // Centered on section 0, so the inline ring starts left of straight up.
        assertEquals(0, layout.inlineSectionAt(left[0], left[1], 0, 2));
        assertEquals(1, layout.inlineSectionAt(right[0], right[1], 0, 2));
    }

    @Test
    void inlineSectionsMatchTheMainSectionWidthButNeverGoAllTheWayRound() {
        RadialLayout layout = layout(8, true);
        double mainWidth = layout.sectionAngle * layout.middleRadius();

        assertEquals(mainWidth, layout.inlineSectionAngle(3) * layout.inlineMiddleRadius(), 1e-3);
        assertTrue((24 * layout.inlineSectionAngle(24)) <= (RadialLayout.FULL_CIRCLE + DELTA));
    }

    @Test
    void normalizeKeepsAnglesWithinOneTurn() {
        assertEquals(0, RadialLayout.normalize(RadialLayout.FULL_CIRCLE), DELTA);
        assertEquals(RadialLayout.FULL_CIRCLE - 1, RadialLayout.normalize(-1), DELTA);
        assertEquals(1, RadialLayout.normalize(1 + (3 * RadialLayout.FULL_CIRCLE)), DELTA);
    }
}
