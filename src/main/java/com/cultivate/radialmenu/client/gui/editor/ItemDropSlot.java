package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

/**
 * A slot that takes an item dragged in from NEI's item list or bookmarks. Right click clears it.
 *
 * <p>
 * Nothing is ever put into the player's inventory; the slot only remembers which item was dropped.
 */
final class ItemDropSlot extends Widget<ItemDropSlot>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, Interactable {

    private static final int SIZE = 18;
    private static final int ITEM_SIZE = 16;
    private static final int RIGHT_BUTTON = 1;

    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;

    ItemDropSlot(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        this.getter = getter;
        this.setter = setter;
        size(SIZE);
        background(GuiTextures.SLOT_ITEM);
        tooltipDynamic(tooltip -> {
            ItemStack stack = getter.get();

            if (stack != null) {
                tooltip.addFromItem(stack);
            }
        });
    }

    @Override
    public boolean handleDragAndDrop(ItemStack draggedStack, int button) {
        ItemStack copy = draggedStack.copy();

        copy.stackSize = 1;
        setter.accept(copy);
        return true;
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (mouseButton == RIGHT_BUTTON) {
            setter.accept(null);
            return Result.SUCCESS;
        }
        return Result.ACCEPT;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ItemStack stack = getter.get();

        if (stack != null) {
            GuiDraw.drawItem(stack, 1, 1, ITEM_SIZE, ITEM_SIZE, 0);
        }
    }
}
