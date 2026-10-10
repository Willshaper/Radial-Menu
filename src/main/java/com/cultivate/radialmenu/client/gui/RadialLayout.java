package com.cultivate.radialmenu.client.gui;

import com.cultivate.radialmenu.config.RadialMenuConfig;

/**
 * Geometry of the ring for a given screen size and section count, and of the inline submenu ring outside it.
 *
 * <p>
 * Angles are in radians, measured clockwise from straight up, so section 0 sits at the top.
 */
final class RadialLayout {

    /** Returned by {@link #sectionAt} when the pointer is over the center circle. */
    static final int CENTER = -1;
    /** Returned when the pointer is over no section of the ring asked about. */
    static final int NONE = -2;

    static final double FULL_CIRCLE = 2 * Math.PI;
    // Arc length each section needs at the middle of the ring, so icons don't crowd together.
    private static final float MIN_SECTION_ARC = 22f;
    // Room left between the ring and the screen edge for the labels above and below it.
    private static final float LABEL_MARGIN = 16f;
    private static final float MAX_ICON_SIZE = 16f;
    // Share of a section's width and of the ring's thickness an icon may fill.
    private static final float ICON_FILL = 0.7f;
    private static final float CENTER_GAP = 4f;
    // The inline ring sits this far outside the main ring, and is this thick compared to it.
    private static final float INLINE_GAP = 4f;
    private static final float INLINE_THICKNESS_SHARE = 0.8f;

    final float centerX;
    final float centerY;
    final float innerRadius;
    final float outerRadius;
    final float centerRadius;
    final float iconSize;
    final int sectionCount;
    final double sectionAngle;
    final float inlineInnerRadius;
    final float inlineOuterRadius;
    // Outermost radius anything is drawn at, counting the inline ring if room is kept for it.
    final float extentRadius;

    /**
     * @param keepInlineRoom shrink the ring if needed so an inline ring fits on screen around it
     */
    RadialLayout(float screenWidth, float screenHeight, int sectionCount, float scale, boolean keepInlineRoom) {
        float thickness = RadialMenuConfig.ringThickness;
        float middleRadius = Math
            .max(RadialMenuConfig.ringRadius, (float) ((sectionCount * MIN_SECTION_ARC) / FULL_CIRCLE));
        float inlineRoom = keepInlineRoom ? INLINE_GAP + (thickness * INLINE_THICKNESS_SHARE) : 0;
        float maxExtent = (Math.min(screenWidth, screenHeight) / 2) - LABEL_MARGIN;
        float fit = Math.min(1f, maxExtent / (middleRadius + (thickness / 2) + inlineRoom));
        float sectionArc;

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
        sectionArc = (float) (sectionAngle * middleRadius);
        this.iconSize = fitIcon(sectionArc, thickness);
        this.inlineInnerRadius = outerRadius + (INLINE_GAP * fit * scale);
        this.inlineOuterRadius = inlineInnerRadius + (thickness * INLINE_THICKNESS_SHARE);
        this.extentRadius = keepInlineRoom ? inlineOuterRadius : outerRadius;
    }

    private static float fitIcon(float sectionArc, float thickness) {
        return Math.min(MAX_ICON_SIZE, Math.min(sectionArc * ICON_FILL, thickness * ICON_FILL));
    }

    float middleRadius() {
        return (innerRadius + outerRadius) / 2;
    }

    float inlineMiddleRadius() {
        return (inlineInnerRadius + inlineOuterRadius) / 2;
    }

    /**
     * @return the section under the point, or {@link #CENTER} if the point is inside the center circle. Points outside
     *         the ring still select by direction, so the mouse does not have to stay on the ring.
     */
    int sectionAt(float x, float y) {
        if (distanceTo(x, y) < centerRadius) {
            return CENTER;
        }
        return (int) Math.floor((angleTo(x, y) + (sectionAngle / 2)) / sectionAngle) % sectionCount;
    }

    /**
     * @return the angle at the middle of section {@code index}
     */
    double angleOf(int index) {
        return index * sectionAngle;
    }

    /**
     * @return the angle each section of an inline ring with {@code count} sections takes: as wide as a main section,
     *         measured along the middle of each ring, but narrower if the ring would otherwise go all the way round
     */
    double inlineSectionAngle(int count) {
        double sameWidth = (sectionAngle * middleRadius()) / inlineMiddleRadius();

        return Math.min(sameWidth, FULL_CIRCLE / count);
    }

    /**
     * @return the angle where the inline ring for section {@code parent} starts, so the ring is centered on it
     */
    double inlineStartAngle(int parent, int count) {
        return angleOf(parent) - ((count * inlineSectionAngle(count)) / 2);
    }

    /**
     * @return the section of the inline ring under the point, or {@link #NONE}. Like the main ring, points further out
     *         than the inline ring select by direction.
     */
    int inlineSectionAt(float x, float y, int parent, int count) {
        double sectionWidth = inlineSectionAngle(count);
        double offset;

        if (distanceTo(x, y) < (outerRadius + inlineInnerRadius) / 2) {
            return NONE;
        }
        offset = normalize(angleTo(x, y) - inlineStartAngle(parent, count));
        if (offset >= count * sectionWidth) {
            return NONE;
        }
        return Math.min(count - 1, (int) (offset / sectionWidth));
    }

    float inlineIconSize(int count) {
        float thickness = inlineOuterRadius - inlineInnerRadius;

        return fitIcon((float) (inlineSectionAngle(count) * inlineMiddleRadius()), thickness);
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
