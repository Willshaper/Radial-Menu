package com.cultivate.radialmenu.menu.action;

import net.minecraft.client.resources.I18n;

/**
 * Every kind of section action, in the order the editor cycles through them. The ids are stored in menus.json and
 * must not change.
 */
public enum ActionType {

    NONE("none"),
    COMMAND("command"),
    CHAT("chat"),
    KEYBIND("keybind"),
    HOTBAR("hotbar"),
    SUBMENU("submenu");

    private final String id;

    ActionType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return I18n.format("radialmenu.action." + id);
    }

    public static ActionType fromId(String id) {
        for (ActionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return NONE;
    }
}
