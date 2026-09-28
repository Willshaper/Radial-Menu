package com.cultivate.radialmenu.menu.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

/**
 * Selects a hotbar slot. The server is told on the next tick by the vanilla held-item sync.
 */
public class HotbarAction implements SectionAction {

    public static final int FIRST_SLOT = 1;
    public static final int LAST_SLOT = InventoryPlayer.getHotbarSize();

    // Slot as the player sees it, 1 to 9.
    private final int slot;

    public HotbarAction(int slot) {
        this.slot = Math.max(FIRST_SLOT, Math.min(LAST_SLOT, slot));
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public ActionType getType() {
        return ActionType.HOTBAR;
    }

    @Override
    public void run() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if (player != null) {
            player.inventory.currentItem = slot - 1;
        }
    }

    @Override
    public String describe() {
        return I18n.format("radialmenu.action.hotbar.describe", slot);
    }
}
