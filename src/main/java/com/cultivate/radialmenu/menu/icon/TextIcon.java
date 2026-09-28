package com.cultivate.radialmenu.menu.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

/**
 * A short piece of colored text, such as "TP" or a symbol.
 */
public class TextIcon implements SectionIcon {

    public static final int DEFAULT_COLOR = 0xFFFFFF;

    // Glyph height of the vanilla font, used to scale text to the icon size.
    private static final float FONT_HEIGHT = 8f;
    // Share of the icon's height the letters may fill.
    private static final float HEIGHT_FILL = 0.75f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2f;

    private final String text;
    private final int color;

    public TextIcon(String text, int color) {
        this.text = text;
        this.color = color & 0xFFFFFF;
    }

    public String getText() {
        return text;
    }

    /**
     * @return the text color as RGB, without alpha
     */
    public int getColor() {
        return color;
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        drawCenteredText(text, color, centerX, centerY, size);
    }

    /**
     * Draws text centered on a point, scaled up or down so it fits in a {@code size} square.
     */
    public static void drawCenteredText(String text, int color, float centerX, float centerY, float size) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = Math.max(1, font.getStringWidth(text));
        float scale = Math.min(size / width, (size * HEIGHT_FILL) / FONT_HEIGHT);

        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.drawStringWithShadow(text, -width / 2, -Math.round(FONT_HEIGHT / 2), color);
        GL11.glPopMatrix();
    }
}
