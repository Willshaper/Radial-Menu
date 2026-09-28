package com.cultivate.radialmenu.menu.action;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentTranslation;

import com.cultivate.radialmenu.client.KeyPresser;

/**
 * Presses another keybinding, from vanilla or any mod, as if its key had been pressed.
 */
public class KeybindAction implements SectionAction {

    public enum Mode {

        /** Press and release, like a single key press. */
        TAP("tap"),
        /** Flip between held and released, like holding sneak down. */
        TOGGLE("toggle");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return I18n.format("radialmenu.keybind.mode." + id);
        }

        public static Mode fromId(String id) {
            return TOGGLE.id.equals(id) ? TOGGLE : TAP;
        }
    }

    private final String keyDescription;
    private final Mode mode;

    public KeybindAction(String keyDescription, Mode mode) {
        this.keyDescription = keyDescription;
        this.mode = mode;
    }

    /**
     * @return the binding's description key, e.g. {@code key.inventory}, which is how it is identified
     */
    public String getKeyDescription() {
        return keyDescription;
    }

    public Mode getMode() {
        return mode;
    }

    @Nullable
    public static KeyBinding findBinding(String keyDescription) {
        for (KeyBinding binding : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (binding.getKeyDescription()
                .equals(keyDescription)) {
                return binding;
            }
        }
        return null;
    }

    @Override
    public ActionType getType() {
        return ActionType.KEYBIND;
    }

    @Override
    public void run() {
        KeyBinding binding = findBinding(keyDescription);

        if (binding == null) {
            Minecraft.getMinecraft().thePlayer
                .addChatMessage(new ChatComponentTranslation("radialmenu.action.keybind.missing", keyDescription));
            return;
        }
        if (mode == Mode.TAP) {
            KeyPresser.tap(binding);
        } else {
            KeyPresser.toggle(binding);
        }
    }

    @Override
    public String describe() {
        return mode.getDisplayName() + ": " + I18n.format(keyDescription);
    }
}
