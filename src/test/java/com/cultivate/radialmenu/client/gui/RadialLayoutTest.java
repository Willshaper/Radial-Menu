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

    private static RadialLayout layout(int sections, int inlineRings) {
        return new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, sections, 1f, inlineRings);
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
        RadialLayout layout = layout(4, 0);

        assertEquals(0, sectionAt(layout, 0, 60));
        assertEquals(1, sectionAt(layout, Math.PI / 2, 60));
        assertEquals(2, sectionAt(layout, Math.PI, 60));
        assertEquals(3, sectionAt(layout, 3 * Math.PI / 2, 60));
    }

    @Test
    void sectionsAreCenteredOnTheirAngle() {
        RadialLayout layout = layout(4, 0);

        // Section 0 covers 45 degrees either side of straight up.
        assertEquals(0, sectionAt(layout, Math.toRadians(-44), 60));
        assertEquals(0, sectionAt(layout, Math.toRadians(44), 60));
        assertEquals(1, sectionAt(layout, Math.toRadians(46), 60));
        assertEquals(3, sectionAt(layout, Math.toRadians(-46), 60));
    }

    @Test
    void centerCircleSelectsTheCenter() {
        RadialLayout layout = layout(8, 0);

        assertEquals(RadialLayout.CENTER, layout.sectionAt(CENTER_X, CENTER_Y));
        assertEquals(RadialLayout.CENTER, sectionAt(layout, 1, layout.centerRadius - 1));
    }

    @Test
    void pointsOutsideTheRingStillSelectByDirection() {
        RadialLayout layout = layout(8, 0);

        assertEquals(2, sectionAt(layout, Math.PI / 2, layout.outerRadius + 100));
    }

    @Test
    void mainRingSectionsAreCenteredOnMultiplesOfTheirWidth() {
        RadialLayout.Ring ring = layout(8, 0).mainRing();

        assertEquals(0, ring.angleOf(0), DELTA);
        assertEquals(3 * ring.sectionAngle, ring.angleOf(3), DELTA);
    }

    @Test
    void ringFitsOnTheScreenWithRoomForLabels() {
        float maxExtent = (SCREEN_HEIGHT / 2) - 16;
        RadialLayout plain = layout(24, 0);
        RadialLayout deep = layout(24, RadialLayout.MAX_INLINE_RINGS);
        RadialLayout.Ring first = deep.inlineRing(deep.mainRing(), 0, 4);
        RadialLayout.Ring last = deep.inlineRing(deep.inlineRing(first, 0, 4), 0, 4);

        assertTrue(plain.extentRadius <= maxExtent);
        assertTrue(deep.extentRadius <= (maxExtent + DELTA));
        assertEquals(last.outerRadius, deep.extentRadius, 1e-3);
    }

    @Test
    void openAnimationScalesTheRing() {
        RadialLayout full = new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, 8, 1f, 0);
        RadialLayout half = new RadialLayout(SCREEN_WIDTH, SCREEN_HEIGHT, 8, 0.5f, 0);

        assertEquals(full.outerRadius / 2, half.outerRadius, 1e-3);
    }

    private static int indexAt(RadialLayout layout, RadialLayout.Ring ring, double angle, float distance) {
        float[] point = point(angle, distance);

        return layout.indexAt(ring, point[0], point[1]);
    }

    @Test
    void inlineRingIsCenteredOnItsParentSection() {
        RadialLayout layout = layout(8, 1);
        RadialLayout.Ring main = layout.mainRing();
        RadialLayout.Ring ring = layout.inlineRing(main, 2, 3);

        assertEquals(main.angleOf(2), ring.angleOf(1), DELTA);
        for (int i = 0; i < 3; i++) {
            assertEquals(i, indexAt(layout, ring, ring.angleOf(i), ring.middleRadius()));
        }
    }

    @Test
    void inlineRingIgnoresTheRingInsideItAndPointsBesideIt() {
        RadialLayout layout = layout(8, 1);
        RadialLayout.Ring main = layout.mainRing();
        RadialLayout.Ring ring = layout.inlineRing(main, 0, 3);

        assertEquals(RadialLayout.NONE, indexAt(layout, ring, 0, main.middleRadius()));
        assertEquals(RadialLayout.NONE, indexAt(layout, ring, Math.PI, ring.middleRadius()));
        assertEquals(1, indexAt(layout, ring, 0, ring.outerRadius + 50));
    }

    @Test
    void inlineRingWrapsPastStraightUp() {
        RadialLayout layout = layout(8, 1);
        RadialLayout.Ring ring = layout.inlineRing(layout.mainRing(), 0, 2);

        // Centered on section 0, so the ring starts left of straight up.
        assertEquals(0, indexAt(layout, ring, -ring.sectionAngle / 2, ring.middleRadius()));
        assertEquals(1, indexAt(layout, ring, ring.sectionAngle / 2, ring.middleRadius()));
    }

    @Test
    void inlineSectionsMatchTheMainSectionWidthButNeverGoAllTheWayRound() {
        RadialLayout layout = layout(8, 1);
        RadialLayout.Ring main = layout.mainRing();
        RadialLayout.Ring three = layout.inlineRing(main, 0, 3);
        RadialLayout.Ring many = layout.inlineRing(main, 0, 24);

        assertEquals(main.sectionAngle * main.middleRadius(), three.sectionAngle * three.middleRadius(), 1e-3);
        assertTrue((24 * many.sectionAngle) <= (RadialLayout.FULL_CIRCLE + DELTA));
    }

    @Test
    void nestedInlineRingsSitFurtherOutAndFollowTheirParent() {
        RadialLayout layout = layout(8, 2);
        RadialLayout.Ring first = layout.inlineRing(layout.mainRing(), 1, 5);
        RadialLayout.Ring second = layout.inlineRing(first, 4, 3);

        assertEquals(2, second.level);
        assertTrue(second.innerRadius > first.outerRadius);
        assertEquals(first.angleOf(4), second.angleOf(1), DELTA);
        assertEquals(RadialLayout.NONE, indexAt(layout, second, first.angleOf(4), first.middleRadius()));
        assertEquals(1, indexAt(layout, second, first.angleOf(4), second.middleRadius()));
    }

    @Test
    void normalizeKeepsAnglesWithinOneTurn() {
        assertEquals(0, RadialLayout.normalize(RadialLayout.FULL_CIRCLE), DELTA);
        assertEquals(RadialLayout.FULL_CIRCLE - 1, RadialLayout.normalize(-1), DELTA);
        assertEquals(1, RadialLayout.normalize(1 + (3 * RadialLayout.FULL_CIRCLE)), DELTA);
    }
}
