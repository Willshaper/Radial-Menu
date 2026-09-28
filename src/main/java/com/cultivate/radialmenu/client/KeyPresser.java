package com.cultivate.radialmenu.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.InputEvent;

/**
 * Presses keybindings from code.
 *
 * <p>
 * Setting {@code pressTime} covers bindings read with {@link KeyBinding#isPressed()}, setting {@code pressed} covers
 * {@link KeyBinding#getIsKeyPressed()}, and posting a {@link InputEvent.KeyInputEvent} wakes up mods that only check
 * their bindings when a key event arrives. Mods that read {@link Keyboard#isKeyDown(int)} directly can't be reached.
 */
public final class KeyPresser {

    // Mouse buttons are stored in key codes offset by this amount, as negative numbers.
    private static final int MOUSE_BUTTON_OFFSET = 100;

    private static final List<KeyBinding> PENDING_RELEASE = new ArrayList<>();

    private KeyPresser() {}

    /**
     * Presses the binding for one tick.
     */
    public static void tap(KeyBinding binding) {
        binding.pressed = true;
        binding.pressTime++;
        PENDING_RELEASE.add(binding);
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
    }

    /**
     * Flips the binding between held and released. The held state lasts until the real key is pressed or a GUI opens.
     */
    public static void toggle(KeyBinding binding) {
        binding.pressed = !binding.pressed;
        if (binding.pressed) {
            binding.pressTime++;
            FMLCommonHandler.instance()
                .bus()
                .post(new InputEvent.KeyInputEvent());
        }
    }

    /**
     * Releases bindings tapped on an earlier tick. Called once per client tick. A press nobody consumed is dropped so
     * it can't fire later out of nowhere.
     */
    static void releaseTappedKeys() {
        for (KeyBinding binding : PENDING_RELEASE) {
            binding.pressed = isPhysicallyDown(binding.getKeyCode());
            binding.pressTime = 0;
        }
        PENDING_RELEASE.clear();
    }

    /**
     * @return true if the key or mouse button with this key code is held down right now
     */
    public static boolean isPhysicallyDown(int keyCode) {
        if (keyCode > 0) {
            return Keyboard.isKeyDown(keyCode);
        }
        if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + MOUSE_BUTTON_OFFSET);
        }
        return false;
    }
}
