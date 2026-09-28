package com.cultivate.radialmenu.client.gui;

import com.cultivate.radialmenu.config.RadialMenuConfig;

/**
 * Geometry of the ring for a given screen size and section count.
 *
 * <p>
 * Angles are in radians, measured clockwise from straight up, so section 0 sits at the top.
 */
final class RadialLayout {

    /** Returned by {@link #sectionAt} when the pointer is over the center circle. */
    static final int CENTER = -1;

    private static final double FULL_CIRCLE = 2 * Math.PI;
    // Arc length each section needs at the middle of the ring, so icons don't crowd together.
    private static final float MIN_SECTION_ARC = 22f;
    // Room left between the ring and the screen edge for the labels above and below it.
    private static final float LABEL_MARGIN = 16f;
    private static final float MAX_ICON_SIZE = 16f;
    private static final float CENTER_GAP = 4f;

    final float centerX;
    final float centerY;
    final float innerRadius;
    final float outerRadius;
    final float centerRadius;
    final float iconSize;
    final int sectionCount;
    final double sectionAngle;

    RadialLayout(float screenWidth, float screenHeight, int sectionCount, float scale) {
        float thickness = RadialMenuConfig.ringThickness;
        float middleRadius = Math
            .max(RadialMenuConfig.ringRadius, (float) ((sectionCount * MIN_SECTION_ARC) / FULL_CIRCLE));
        float maxOuterRadius = (Math.min(screenWidth, screenHeight) / 2) - LABEL_MARGIN;
        float fit = Math.min(1f, maxOuterRadius / (middleRadius + (thickness / 2)));
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
        this.iconSize = Math.min(MAX_ICON_SIZE, Math.min(sectionArc * 0.7f, thickness * 0.7f));
    }

    float middleRadius() {
        return (innerRadius + outerRadius) / 2;
    }

    /**
     * @return the section under the point, or {@link #CENTER} if the point is inside the center circle. Points outside
     *         the ring still select by direction, so the mouse does not have to stay on the ring.
     */
    int sectionAt(float x, float y) {
        double dx = x - centerX;
        double dy = y - centerY;
        double angle;

        if (Math.hypot(dx, dy) < centerRadius) {
            return CENTER;
        }
        angle = Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += FULL_CIRCLE;
        }
        return (int) Math.floor((angle + (sectionAngle / 2)) / sectionAngle) % sectionCount;
    }

    /**
     * @return the angle at the middle of section {@code index}
     */
    double angleOf(int index) {
        return index * sectionAngle;
    }

    float xAt(double angle, float radius) {
        return centerX + (float) (Math.sin(angle) * radius);
    }

    float yAt(double angle, float radius) {
        return centerY - (float) (Math.cos(angle) * radius);
    }
}
