package com.cultivate.radialmenu.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import com.cultivate.radialmenu.RadialMenuMod;

/**
 * Writes the keyboard state that {@link Keyboard#isKeyDown(int)} reads, so a key pressed from code also looks held to
 * code that asks the keyboard instead of a keybinding.
 *
 * <p>
 * GTNH needs this in two places. Hodgepodge re-reads every keybinding from {@code Keyboard.isKeyDown} whenever a screen
 * closes, which would undo a press made from code. Controlling answers {@code getIsKeyPressed()} from
 * {@code Keyboard.isKeyDown} for bindings on a modifier key, which is where sneak usually is.
 *
 * <p>
 * Plain LWJGL 2 keeps the state in {@code Keyboard.keyDownBuffer}, indexed by key code, and refills it from the
 * hardware every frame. lwjgl3ify (GTNH on Java 17+) forwards {@code org.lwjgl.input.Keyboard} to
 * {@code org.lwjglx.input.Keyboard}, whose {@code sdlKeyPressedArray} is indexed by SDL scancode and only changes on
 * real key events, so a value written there stays until it is written again or the real key is pressed or released.
 * Both are found by reflection; if neither is, writes do nothing.
 */
final class KeyBuffer {

    private static boolean resolved;
    @Nullable
    private static Field store;
    // Key code to SDL scancode, for lwjgl3ify. Null when the store is indexed by key code.
    @Nullable
    private static Method scancodeOf;

    private KeyBuffer() {}

    /**
     * Marks a keyboard key as down or up. Mouse buttons and key code 0 are ignored.
     */
    static void set(int keyCode, boolean down) {
        Field resolvedStore;

        if ((keyCode <= 0) || (keyCode >= Keyboard.KEYBOARD_SIZE)) {
            return;
        }
        resolve();
        resolvedStore = store;
        if (resolvedStore == null) {
            return;
        }
        try {
            ByteBuffer keys = (ByteBuffer) resolvedStore.get(null);
            int index = scancodeOf == null ? keyCode : (Integer) scancodeOf.invoke(null, keyCode);

            if ((keys != null) && (index > 0) && (index < keys.limit())) {
                keys.put(index, (byte) (down ? 1 : 0));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            store = null;
            RadialMenuMod.LOG.warn("could not write the keyboard state, pressed keys may be released early", e);
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        store = findField("org.lwjgl.input.Keyboard", "keyDownBuffer");
        if (store != null) {
            return;
        }
        store = findField("org.lwjglx.input.Keyboard", "sdlKeyPressedArray");
        if (store == null) {
            RadialMenuMod.LOG.warn("found no keyboard state to write, pressed keys may be released early");
            return;
        }
        try {
            scancodeOf = Class.forName("org.lwjglx.input.KeyCodes")
                .getDeclaredMethod("lwjglToSdlScancode", int.class);
            scancodeOf.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // Without the scancode mapping every write would land on the wrong key.
            store = null;
            RadialMenuMod.LOG.warn("found lwjgl3ify's keyboard state but not its scancode mapping", e);
        }
    }

    @Nullable
    private static Field findField(String className, String fieldName) {
        try {
            Field field = Class.forName(className)
                .getDeclaredField(fieldName);

            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
