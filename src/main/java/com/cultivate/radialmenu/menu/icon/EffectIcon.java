package com.cultivate.radialmenu.menu.icon;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * The icon of a status effect, as shown in the inventory, from vanilla or any mod.
 *
 * <p>
 * Effects are stored by name, such as {@code potion.moveSpeed}, because their numeric ids depend on which mods are
 * installed. An effect that no longer exists draws as a question mark.
 */
public class EffectIcon implements SectionIcon {

    // Vanilla's effect icons: 18 pixel cells, 8 to a row, starting this far down the inventory texture.
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation(
        "textures/gui/container/inventory.png");
    private static final int CELL = 18;
    private static final int COLUMNS = 8;
    private static final int FIRST_ROW_Y = 198;
    private static final float TEXTURE_SIZE = 256f;
    private static final String MISSING_TEXT = "?";
    private static final int MISSING_COLOR = 0xFF5555;
    private static final int WHITE = 0xFFFFFF;

    private final String effectName;

    public EffectIcon(String effectName) {
        this.effectName = effectName;
    }

    public String getEffectName() {
        return effectName;
    }

    /**
     * @return every registered effect that has an icon, in id order
     */
    public static List<Potion> getEffectsWithIcons() {
        List<Potion> effects = new ArrayList<>();

        for (Potion potion : Potion.potionTypes) {
            if ((potion != null) && potion.hasStatusIcon()) {
                effects.add(potion);
            }
        }
        return effects;
    }

    @Nullable
    private static Potion findEffect(String name) {
        for (Potion potion : Potion.potionTypes) {
            if ((potion != null) && potion.getName()
                .equals(name)) {
                return potion;
            }
        }
        return null;
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        Potion potion = findEffect(effectName);
        int index;

        if ((potion == null) || !potion.hasStatusIcon()) {
            TextIcon.drawCenteredText(MISSING_TEXT, MISSING_COLOR, centerX, centerY, size);
            return;
        }
        // Bound before asking for the index, like vanilla does: mods with their own icon sheet bind it from
        // getStatusIconIndex().
        IconTextures.bind(INVENTORY_TEXTURE);
        index = potion.getStatusIconIndex();
        IconTextures.drawBound(
            centerX,
            centerY,
            size,
            ((index % COLUMNS) * CELL) / TEXTURE_SIZE,
            (FIRST_ROW_Y + ((index / COLUMNS) * CELL)) / TEXTURE_SIZE,
            (((index % COLUMNS) + 1) * CELL) / TEXTURE_SIZE,
            (FIRST_ROW_Y + (((index / COLUMNS) + 1) * CELL)) / TEXTURE_SIZE,
            WHITE);
    }
}
