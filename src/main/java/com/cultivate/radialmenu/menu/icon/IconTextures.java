package com.cultivate.radialmenu.menu.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * Draws part of a texture as a square icon.
 */
final class IconTextures {

    private IconTextures() {}

    /**
     * Binds a texture this mod owns and makes it smooth when drawn smaller than it is, so a large image doesn't lose
     * whole rows of pixels. Drawn larger, it stays sharp. Only used on the mod's own textures, since the filter sticks
     * to the texture everywhere it is drawn.
     */
    static void bindSmoothed(ResourceLocation texture) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    static void bind(ResourceLocation texture) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
    }

    /**
     * Draws the given texture coordinates of the bound texture into a {@code size} square centered on a point, tinted
     * with an RGB color.
     */
    static void drawBound(float centerX, float centerY, float size, float minU, float minV, float maxU, float maxV,
        int rgb) {
        drawBound(centerX, centerY, size, size, minU, minV, maxU, maxV, rgb);
    }

    static void drawBound(float centerX, float centerY, float width, float height, float minU, float minV, float maxU,
        float maxV, int rgb) {
        Tessellator tessellator = Tessellator.instance;
        float left = centerX - (width / 2);
        float top = centerY - (height / 2);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(rgb);
        tessellator.addVertexWithUV(left, top + height, 0, minU, maxV);
        tessellator.addVertexWithUV(left + width, top + height, 0, maxU, maxV);
        tessellator.addVertexWithUV(left + width, top, 0, maxU, minV);
        tessellator.addVertexWithUV(left, top, 0, minU, minV);
        tessellator.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
