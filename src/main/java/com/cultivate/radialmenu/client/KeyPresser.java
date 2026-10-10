package com.cultivate.radialmenu.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/**
 * Presses keybindings from code.
 *
 * <p>
 * Setting {@code pressTime} covers bindings read with {@link KeyBinding#isPressed()}, setting {@code pressed} covers
 * {@link KeyBinding#getIsKeyPressed()}, and posting a {@link InputEvent.KeyInputEvent} wakes up mods that only check
 * their bindings when a key event arrives. The key is also written to the keyboard state (see {@link KeyBuffer}) for
 * mods that ask {@link Keyboard#isKeyDown(int)} about the binding's key.
 *
 * <p>
 * Registered on both the FML and the Forge event bus.
 */
public final class KeyPresser {

    // Mouse buttons are stored in key codes offset by this amount, as negative numbers.
    private static final int MOUSE_BUTTON_OFFSET = 100;
    // Lent to unbound bindings for the tick they are tapped. LWJGL defines F15, but no common keyboard has the key.
    private static final int LENT_KEY_CODE = Keyboard.KEY_F15;

    private static final List<TappedKey> PENDING_RELEASE = new ArrayList<>();
    private static final Set<KeyBinding> TOGGLED = new HashSet<>();
    // True while this class posts its own key event, so the handler below can tell it apart from a real key.
    private static boolean postingEvent;

    /**
     * A binding pressed for one tick, and what to undo when the tick is over.
     */
    private static final class TappedKey {

        private final KeyBinding binding;
        private final boolean lentKeyCode;
        private final boolean wroteKeyboard;

        private TappedKey(KeyBinding binding, boolean lentKeyCode, boolean wroteKeyboard) {
            this.binding = binding;
            this.lentKeyCode = lentKeyCode;
            this.wroteKeyboard = wroteKeyboard;
        }
    }

    /**
     * Presses the binding for one tick.
     *
     * <p>
     * An unbound binding gets a key code for that tick, because some mods ignore a binding without one before
     * checking anything else. JourneyMap's zoom and minimap keys are like this.
     */
    public static void tap(KeyBinding binding) {
        boolean lentKeyCode = binding.getKeyCode() == 0;
        boolean wroteKeyboard;

        if (applyVanillaEffect(binding)) {
            return;
        }
        if (lentKeyCode) {
            binding.setKeyCode(LENT_KEY_CODE);
        }
        // A key the player is really holding is left alone, so the release next tick doesn't let go of it.
        wroteKeyboard = !isPhysicallyDown(binding.getKeyCode());
        if (wroteKeyboard) {
            KeyBuffer.set(binding.getKeyCode(), true);
        }
        binding.pressed = true;
        binding.pressTime++;
        PENDING_RELEASE.add(new TappedKey(binding, lentKeyCode, wroteKeyboard));
        postKeyEvent();
    }

    /**
     * Holds the binding down, or lets go of it if it is held already. The hold lasts until the real key is pressed or
     * the world is left, and survives opening and closing screens.
     */
    public static void toggle(KeyBinding binding) {
        if (applyVanillaEffect(binding)) {
            return;
        }
        if (TOGGLED.remove(binding)) {
            release(binding);
            return;
        }
        TOGGLED.add(binding);
        hold(binding);
        binding.pressTime++;
        postKeyEvent();
    }

    /**
     * Toggle perspective and smooth camera are only checked inside vanilla's keyboard event loop, which a press from
     * code never reaches, so their effect is applied here instead.
     *
     * @return true if the binding was one of them
     */
    private static boolean applyVanillaEffect(KeyBinding binding) {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;

        if (binding == settings.keyBindTogglePerspective) {
            settings.thirdPersonView = (settings.thirdPersonView + 1) % 3;
            return true;
        }
        if (binding == settings.keyBindSmoothCamera) {
            settings.smoothCamera = !settings.smoothCamera;
            return true;
        }
        return false;
    }

    private static void hold(KeyBinding binding) {
        binding.pressed = true;
        KeyBuffer.set(binding.getKeyCode(), true);
    }

    private static void release(KeyBinding binding) {
        binding.pressed = false;
        binding.pressTime = 0;
        KeyBuffer.set(binding.getKeyCode(), false);
    }

    private static void postKeyEvent() {
        postingEvent = true;
        try {
            FMLCommonHandler.instance()
                .bus()
                .post(new InputEvent.KeyInputEvent());
        } finally {
            postingEvent = false;
        }
    }

    /**
     * Releases bindings tapped on an earlier tick and keeps toggled bindings held. Called at the start of every client
     * tick. A tap no mod consumed is dropped, so it can't trigger on some later tick.
     */
    static void onClientTick(Minecraft mc) {
        for (TappedKey tapped : PENDING_RELEASE) {
            tapped.binding.pressed = false;
            tapped.binding.pressTime = 0;
            if (tapped.wroteKeyboard) {
                KeyBuffer.set(tapped.binding.getKeyCode(), false);
            }
            if (tapped.lentKeyCode) {
                tapped.binding.setKeyCode(0);
            }
        }
        PENDING_RELEASE.clear();
        if (mc.thePlayer == null) {
            releaseToggled();
        } else if (mc.currentScreen == null) {
            // Opening a screen releases every binding; this presses the toggled ones again once it is closed.
            TOGGLED.forEach(KeyPresser::hold);
        }
    }

    private static void releaseToggled() {
        TOGGLED.forEach(KeyPresser::release);
        TOGGLED.clear();
    }

    /**
     * Keeps a toggled key out of the keyboard state while a screen is open, so a held sneak doesn't turn every click
     * in the inventory into a Shift click. Writes it back as the screen closes, before Hodgepodge re-reads the keys.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        boolean closing = event.gui == null;

        for (KeyBinding binding : TOGGLED) {
            if (closing) {
                hold(binding);
            } else {
                KeyBuffer.set(binding.getKeyCode(), false);
            }
        }
    }

    /**
     * Ends a toggle when its real key is pressed or released. Both count: under lwjgl3ify the press can be swallowed as
     * a key repeat, since the key already looks held.
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!postingEvent) {
            endToggleFor(Keyboard.getEventKey());
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();

        if (button >= 0) {
            endToggleFor(button - MOUSE_BUTTON_OFFSET);
        }
    }

    /**
     * Stops tracking toggles on this key without touching their state; the real key event owns it now.
     */
    private static void endToggleFor(int keyCode) {
        if (keyCode != 0) {
            TOGGLED.removeIf(binding -> binding.getKeyCode() == keyCode);
        }
    }

    /**
     * @return true if the key or mouse button with this key code is held down right now
     */
    public static boolean isPhysicallyDown(int keyCode) {
        if (keyCode > 0) {
            return (keyCode < Keyboard.KEYBOARD_SIZE) && Keyboard.isKeyDown(keyCode);
        }
        if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + MOUSE_BUTTON_OFFSET);
        }
        return false;
    }
}
