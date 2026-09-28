package com.cultivate.radialmenu.menu.icon;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.drawable.GuiDraw;

import cpw.mods.fml.common.registry.GameData;

/**
 * An item or block, including its metadata and NBT.
 *
 * <p>
 * The registry name is kept even when the item no longer exists (a mod was removed), so saving the menus does not
 * throw the section away; it just draws as a question mark.
 */
public class ItemIcon implements SectionIcon {

    private static final String MISSING_ITEM_TEXT = "?";
    private static final int MISSING_ITEM_COLOR = 0xFF5555;

    private final String itemId;
    private final int meta;
    @Nullable
    private final NBTTagCompound nbt;
    @Nullable
    private final ItemStack stack;

    public ItemIcon(ItemStack source) {
        this(getRegistryName(source.getItem()), source.getItemDamage(), source.getTagCompound());
    }

    public ItemIcon(String itemId, int meta, @Nullable NBTTagCompound nbt) {
        Item item = GameData.getItemRegistry()
            .getObject(itemId);
        this.itemId = itemId;
        this.meta = meta;
        this.nbt = nbt == null ? null : (NBTTagCompound) nbt.copy();
        if (item == null) {
            this.stack = null;
        } else {
            this.stack = new ItemStack(item, 1, meta);
            if (this.nbt != null) {
                this.stack.setTagCompound((NBTTagCompound) this.nbt.copy());
            }
        }
    }

    public static String getRegistryName(Item item) {
        return GameData.getItemRegistry()
            .getNameForObject(item);
    }

    public String getItemId() {
        return itemId;
    }

    public int getMeta() {
        return meta;
    }

    @Nullable
    public NBTTagCompound getNbt() {
        return nbt;
    }

    /**
     * @return the item as a stack of one, or null if the item is not in the game
     */
    @Nullable
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        if (stack == null) {
            TextIcon.drawCenteredText(MISSING_ITEM_TEXT, MISSING_ITEM_COLOR, centerX, centerY, size);
            return;
        }
        GuiDraw.drawItem(stack, Math.round(centerX - (size / 2)), Math.round(centerY - (size / 2)), size, size, 0);
    }
}
