package com.cultivate.radialmenu.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.client.gui.RadialScreen;
import com.cultivate.radialmenu.client.gui.editor.MenuManager;
import com.cultivate.radialmenu.menu.action.SectionAction;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Opens the radial menu while its key is held, and runs the action picked in it once the menu has closed.
 */
public class ClientTickHandler {

    // How long a picked action waits for the menu to close before it is dropped, in ticks.
    private static final int ACTION_TIMEOUT_TICKS = 20;

    @Nullable
    private static SectionAction pendingAction;
    private static int pendingActionAge;

    // Set when the menu opens, so it does not reopen until the key has been let go.
    private boolean waitingForKeyRelease;

    /**
     * Runs the action at the start of the next tick with no screen open, so key presses reach the game instead of a
     * GUI.
     */
    public static void scheduleAction(SectionAction action) {
        pendingAction = action;
        pendingActionAge = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean openKeyDown = KeyPresser.isPhysicallyDown(KeyBindings.OPEN.getKeyCode());

        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        KeyPresser.releaseTappedKeys();
        runPendingAction(mc);
        if (!openKeyDown) {
            waitingForKeyRelease = false;
        }
        if ((mc.currentScreen != null) || (mc.thePlayer == null)) {
            return;
        }
        if (openKeyDown && !waitingForKeyRelease) {
            waitingForKeyRelease = true;
            RadialScreen.open();
        } else if (KeyBindings.EDITOR.isPressed()) {
            MenuManager.open(null);
        }
    }

    private static void runPendingAction(Minecraft mc) {
        SectionAction action = pendingAction;

        if (action == null) {
            return;
        }
        if (mc.currentScreen != null) {
            pendingActionAge++;
            if (pendingActionAge > ACTION_TIMEOUT_TICKS) {
                pendingAction = null;
            }
            return;
        }
        pendingAction = null;
        if (mc.thePlayer == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException e) {
            RadialMenuMod.LOG.error("the {} action failed", action.describe(), e);
        }
    }
}
