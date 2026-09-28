package com.cultivate.radialmenu.client;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class KeyBindings {

    private static final String CATEGORY = "key.categories.radialmenu";

    /** Hold to show the radial menu; releasing it closes the menu. */
    public static final KeyBinding OPEN = new KeyBinding("key.radialmenu.open", Keyboard.KEY_R, CATEGORY);
    /** While the menu is open, switches to the next menu. Shift switches back. */
    public static final KeyBinding SWITCH = new KeyBinding("key.radialmenu.switch", Keyboard.KEY_TAB, CATEGORY);
    /** Opens the menu manager. */
    public static final KeyBinding MANAGER = new KeyBinding("key.radialmenu.manager", Keyboard.KEY_NONE, CATEGORY);

    private KeyBindings() {}

    static void register() {
        ClientRegistry.registerKeyBinding(OPEN);
        ClientRegistry.registerKeyBinding(SWITCH);
        ClientRegistry.registerKeyBinding(MANAGER);
    }
}
