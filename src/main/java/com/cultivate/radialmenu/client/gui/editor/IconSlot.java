package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cultivate.radialmenu.menu.icon.ItemIcon;
import com.cultivate.radialmenu.menu.icon.SectionIcon;

/**
 * A slot showing an icon the way the ring draws it. An item dragged in from NEI's item list or bookmarks is handed to
 * {@code onItemDropped}; right click clears the slot if {@code onClear} is given.
 *
 * <p>
 * Nothing is ever put into the player's inventory; the slot only remembers which item was dropped.
 */
final class IconSlot extends Widget<IconSlot> implements RecipeViewerGhostIngredientSlot<ItemStack>, Interactable {

    static final int SIZE = 18;
    private static final int ICON_SIZE = 16;
    private static final int RIGHT_BUTTON = 1;

    private final Supplier<SectionIcon> icon;
    private final Consumer<ItemStack> onItemDropped;
    @Nullable
    private final Runnable onClear;

    /**
     * @param icon the icon to show, or null for an empty slot
     */
    IconSlot(Supplier<SectionIcon> icon, Consumer<ItemStack> onItemDropped, @Nullable Runnable onClear) {
        this.icon = icon;
        this.onItemDropped = onItemDropped;
        this.onClear = onClear;
        size(SIZE);
        background(GuiTextures.SLOT_ITEM);
        tooltipDynamic(tooltip -> {
            if ((icon.get() instanceof ItemIcon itemIcon) && (itemIcon.getStack() != null)) {
                tooltip.addFromItem(itemIcon.getStack());
            }
        });
    }

    @Override
    public boolean handleDragAndDrop(ItemStack draggedStack, int button) {
        ItemStack copy = draggedStack.copy();

        copy.stackSize = 1;
        onItemDropped.accept(copy);
        return true;
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if ((mouseButton == RIGHT_BUTTON) && (onClear != null)) {
            onClear.run();
            return Result.SUCCESS;
        }
        return Result.ACCEPT;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        SectionIcon shown = icon.get();

        if (shown != null) {
            shown.draw(SIZE / 2f, SIZE / 2f, ICON_SIZE);
        }
    }
}
