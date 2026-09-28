package com.cultivate.radialmenu.menu.icon;

/**
 * What a section shows in the ring.
 */
public interface SectionIcon {

    /**
     * Draws the icon centered on the given point, fitting inside a {@code size} x {@code size} square.
     */
    void draw(float centerX, float centerY, float size);
}
