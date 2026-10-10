package com.cultivate.radialmenu.client.gui;

import com.cultivate.radialmenu.config.RadialMenuConfig;

/**
 * Geometry of the ring for a given screen size and section count, and of the inline submenu rings outside it.
 *
 * <p>
 * Angles are in radians, measured clockwise from straight up, so section 0 sits at the top.
 */
final class RadialLayout {

    /** Returned by {@link #sectionAt} when the pointer is over the center circle. */
    static final int CENTER = -1;
    /** Returned when the pointer is over no section of the ring asked about. */
    static final int NONE = -2;
    /** The most inline rings shown at once, outside the main ring. */
    static final int MAX_INLINE_RINGS = 3;

    static final double FULL_CIRCLE = 2 * Math.PI;
    // Arc length each section needs at the middle of the ring, so icons don't crowd together.
    private static final float MIN_SECTION_ARC = 22f;
    // Room left between the ring and the screen edge for the labels above and below it.
    private static final float LABEL_MARGIN = 16f;
    private static final float MAX_ICON_SIZE = 16f;
    // Share of a section's width and of the ring's thickness an icon may fill.
    private static final float ICON_FILL = 0.7f;
    private static final float CENTER_GAP = 4f;
    // Each inline ring sits this far outside the ring inside it, and is this thick compared to the main ring.
    private static final float INLINE_GAP = 4f;
    private static final float INLINE_THICKNESS_SHARE = 0.8f;

    final float centerX;
    final float centerY;
    final float innerRadius;
    final float outerRadius;
    final float centerRadius;
    final int sectionCount;
    final double sectionAngle;
    // Outermost radius anything is drawn at, counting the inline rings room is kept for.
    final float extentRadius;
    private final float inlineGap;
    private final float inlineThickness;

    /**
     * A ring of sections: the main ring, or an inline ring of a submenu centered on the section that opens it.
     */
    static final class Ring {

        /** 0 for the main ring, 1 for the first inline ring outside it, and so on. */
        final int level;
        final int count;
        final double startAngle;
        final double sectionAngle;
        final float innerRadius;
        final float outerRadius;

        private Ring(int level, int count, double startAngle, double sectionAngle, float innerRadius,
            float outerRadius) {
            this.level = level;
            this.count = count;
            this.startAngle = startAngle;
            this.sectionAngle = sectionAngle;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
        }

        float middleRadius() {
            return (innerRadius + outerRadius) / 2;
        }

        /**
         * @return the angle at the middle of section {@code index}
         */
        double angleOf(int index) {
            return startAngle + ((index + 0.5) * sectionAngle);
        }

        /**
         * @return the section at this angle, or {@link #NONE} if the ring doesn't reach it
         */
        int indexAt(double angle) {
            double offset = normalize(angle - startAngle);

            if (offset >= (count * sectionAngle)) {
                return NONE;
            }
            return Math.min(count - 1, (int) (offset / sectionAngle));
        }
    }

    /**
     * @param inlineRings how many inline rings to keep room for; the whole menu shrinks if needed so they fit
     */
    RadialLayout(float screenWidth, float screenHeight, int sectionCount, float scale, int inlineRings) {
        float thickness = RadialMenuConfig.ringThickness;
        float middleRadius = Math
            .max(RadialMenuConfig.ringRadius, (float) ((sectionCount * MIN_SECTION_ARC) / FULL_CIRCLE));
        float inlineRoom = inlineRings * (INLINE_GAP + (thickness * INLINE_THICKNESS_SHARE));
        float maxExtent = (Math.min(screenWidth, screenHeight) / 2) - LABEL_MARGIN;
        float fit = Math.min(1f, maxExtent / (middleRadius + (thickness / 2) + inlineRoom));

        middleRadius *= fit * scale;
        thickness *= fit * scale;
        this.centerX = screenWidth / 2;
        this.centerY = screenHeight / 2;
        this.sectionCount = sectionCount;
        this.sectionAngle = FULL_CIRCLE / sectionCount;
        this.innerRadius = middleRadius - (thickness / 2);
        this.outerRadius = middleRadius + (thickness / 2);
        this.centerRadius = Math
            .max(0, Math.min(RadialMenuConfig.centerRadius * fit * scale, innerRadius - CENTER_GAP));
        this.inlineGap = INLINE_GAP * fit * scale;
        this.inlineThickness = thickness * INLINE_THICKNESS_SHARE;
        this.extentRadius = outerRadius + (inlineRings * (inlineGap + inlineThickness));
    }

    float middleRadius() {
        return (innerRadius + outerRadius) / 2;
    }

    Ring mainRing() {
        return new Ring(0, sectionCount, -sectionAngle / 2, sectionAngle, innerRadius, outerRadius);
    }

    /**
     * @return the inline ring with {@code count} sections outside {@code parent}, centered on its section
     *         {@code parentIndex}. Its sections are as wide as the main ring's, measured along the middle of each ring,
     *         but narrower if the ring would otherwise go all the way round.
     */
    Ring inlineRing(Ring parent, int parentIndex, int count) {
        int level = parent.level + 1;
        float inner = outerRadius + (level * inlineGap) + ((level - 1) * inlineThickness);
        float outer = inner + inlineThickness;
        double sameWidth = (sectionAngle * middleRadius()) / ((inner + outer) / 2);
        double width = Math.min(sameWidth, FULL_CIRCLE / count);

        return new Ring(level, count, parent.angleOf(parentIndex) - ((count * width) / 2), width, inner, outer);
    }

    /**
     * @return the section of the ring under the point, or {@link #NONE}. On the main ring this is
     *         {@link #sectionAt}. An inline ring only counts points past the middle of the gap inside it, and like the
     *         main ring, points further out than it select by direction.
     */
    int indexAt(Ring ring, float x, float y) {
        float gapMiddle = ring.innerRadius - (inlineGap / 2);

        if (ring.level == 0) {
            return sectionAt(x, y);
        }
        if (distanceTo(x, y) < gapMiddle) {
            return NONE;
        }
        return ring.indexAt(angleTo(x, y));
    }

    /**
     * @return the main ring section under the point, or {@link #CENTER} if the point is inside the center circle.
     *         Points outside the ring still select by direction, so the mouse does not have to stay on the ring.
     */
    int sectionAt(float x, float y) {
        if (distanceTo(x, y) < centerRadius) {
            return CENTER;
        }
        // Not mainRing().indexAt(): rounding could leave a sliver at the top that belongs to no section.
        return (int) Math.floor((angleTo(x, y) + (sectionAngle / 2)) / sectionAngle) % sectionCount;
    }

    float iconSize(Ring ring) {
        float sectionArc = (float) (ring.sectionAngle * ring.middleRadius());
        float thickness = ring.outerRadius - ring.innerRadius;

        return Math.min(MAX_ICON_SIZE, Math.min(sectionArc * ICON_FILL, thickness * ICON_FILL));
    }

    float xAt(double angle, float radius) {
        return centerX + (float) (Math.sin(angle) * radius);
    }

    float yAt(double angle, float radius) {
        return centerY - (float) (Math.cos(angle) * radius);
    }

    private double distanceTo(float x, float y) {
        return Math.hypot(x - centerX, y - centerY);
    }

    private double angleTo(float x, float y) {
        return normalize(Math.atan2(x - centerX, centerY - y));
    }

    /**
     * @return the same angle, between 0 and a full circle
     */
    static double normalize(double angle) {
        double normalized = angle % FULL_CIRCLE;

        return normalized < 0 ? normalized + FULL_CIRCLE : normalized;
    }
}
