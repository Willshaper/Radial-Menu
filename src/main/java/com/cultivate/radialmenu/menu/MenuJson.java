package com.cultivate.radialmenu.menu;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.menu.action.ActionType;
import com.cultivate.radialmenu.menu.action.ChatAction;
import com.cultivate.radialmenu.menu.action.CommandAction;
import com.cultivate.radialmenu.menu.action.HotbarAction;
import com.cultivate.radialmenu.menu.action.KeybindAction;
import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.action.SubmenuAction;
import com.cultivate.radialmenu.menu.icon.EffectIcon;
import com.cultivate.radialmenu.menu.icon.HeadIcon;
import com.cultivate.radialmenu.menu.icon.ImageIcon;
import com.cultivate.radialmenu.menu.icon.ItemIcon;
import com.cultivate.radialmenu.menu.icon.SectionIcon;
import com.cultivate.radialmenu.menu.icon.SymbolIcon;
import com.cultivate.radialmenu.menu.icon.TextIcon;
import com.cultivate.radialmenu.util.ColorMath;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * Converts menus to and from the JSON stored in menus.json.
 *
 * <p>
 * Written by hand instead of through Gson's reflection so the file stays readable and the format does not depend on
 * field names. Item NBT is stored as base64 of the compressed tag because 1.7.10's NBT-to-text conversion does not
 * round-trip reliably.
 */
final class MenuJson {

    private static final int FORMAT_VERSION = 1;
    private static final String KEY_VERSION = "version";
    private static final String KEY_LAST_MENU = "lastMenu";
    private static final String KEY_MENUS = "menus";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SECTION_COUNT = "sectionCount";
    private static final String KEY_IN_CYCLE = "inCycle";
    private static final String KEY_SECTIONS = "sections";
    private static final String KEY_LABEL = "label";
    private static final String KEY_ICON = "icon";
    private static final String KEY_ACTION = "action";
    private static final String KEY_TYPE = "type";

    private static final String ICON_ITEM = "item";
    private static final String ICON_TEXT = "text";
    private static final String ICON_SYMBOL = "symbol";
    private static final String ICON_EFFECT = "effect";
    private static final String ICON_IMAGE = "image";
    private static final String ICON_HEAD = "head";
    private static final String KEY_COLOR = "color";

    private MenuJson() {}

    static JsonObject write(List<RadialMenu> menus, @Nullable String lastMenuId) {
        JsonObject root = new JsonObject();
        JsonArray menuArray = new JsonArray();

        root.addProperty(KEY_VERSION, FORMAT_VERSION);
        if (lastMenuId != null) {
            root.addProperty(KEY_LAST_MENU, lastMenuId);
        }
        for (RadialMenu menu : menus) {
            menuArray.add(writeMenu(menu));
        }
        root.add(KEY_MENUS, menuArray);
        return root;
    }

    /**
     * Reads every valid menu from the file. Broken menus and sections are dropped with a warning instead of failing
     * the whole file.
     */
    static List<RadialMenu> readMenus(JsonObject root) {
        List<RadialMenu> menus = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        if (!root.has(KEY_MENUS)) {
            return menus;
        }
        for (JsonElement element : root.getAsJsonArray(KEY_MENUS)) {
            RadialMenu menu;

            try {
                menu = readMenu(element.getAsJsonObject());
            } catch (RuntimeException e) {
                RadialMenuMod.LOG.warn("skipping unreadable menu {}", element, e);
                continue;
            }
            if (seenIds.add(menu.getId())) {
                menus.add(menu);
            } else {
                RadialMenuMod.LOG.warn("skipping menu with duplicate id {}", menu.getId());
            }
        }
        return menus;
    }

    @Nullable
    static String readLastMenuId(JsonObject root) {
        return root.has(KEY_LAST_MENU) ? root.get(KEY_LAST_MENU)
            .getAsString() : null;
    }

    private static JsonObject writeMenu(RadialMenu menu) {
        JsonObject json = new JsonObject();
        JsonArray sectionArray = new JsonArray();

        json.addProperty(KEY_ID, menu.getId());
        json.addProperty(KEY_NAME, menu.getName());
        json.addProperty(KEY_SECTION_COUNT, menu.getSectionCount());
        json.addProperty(KEY_IN_CYCLE, menu.isInCycle());
        for (MenuSection section : menu.getStoredSections()) {
            sectionArray.add(section.isEmpty() ? JsonNull.INSTANCE : writeSection(section));
        }
        json.add(KEY_SECTIONS, sectionArray);
        return json;
    }

    private static RadialMenu readMenu(JsonObject json) {
        RadialMenu menu = new RadialMenu(
            json.get(KEY_ID)
                .getAsString(),
            getString(json, KEY_NAME, "Menu"),
            json.has(KEY_SECTION_COUNT) ? json.get(KEY_SECTION_COUNT)
                .getAsInt() : RadialMenu.DEFAULT_SECTIONS,
            !json.has(KEY_IN_CYCLE) || json.get(KEY_IN_CYCLE)
                .getAsBoolean());
        JsonArray sectionArray = json.has(KEY_SECTIONS) ? json.getAsJsonArray(KEY_SECTIONS) : new JsonArray();

        for (int i = 0; i < sectionArray.size(); i++) {
            JsonElement element = sectionArray.get(i);

            if (element.isJsonNull()) {
                continue;
            }
            try {
                menu.setSection(i, readSection(element.getAsJsonObject()));
            } catch (RuntimeException e) {
                RadialMenuMod.LOG.warn("skipping broken section {} of menu {}", i, menu.getId(), e);
            }
        }
        return menu;
    }

    private static JsonObject writeSection(MenuSection section) {
        JsonObject json = new JsonObject();

        json.addProperty(KEY_LABEL, section.getLabel());
        if (section.getIcon() != null) {
            json.add(KEY_ICON, writeIcon(section.getIcon()));
        }
        if (section.getAction() != null) {
            json.add(KEY_ACTION, writeAction(section.getAction()));
        }
        return json;
    }

    private static MenuSection readSection(JsonObject json) {
        return new MenuSection(
            getString(json, KEY_LABEL, ""),
            json.has(KEY_ICON) ? readIcon(json.getAsJsonObject(KEY_ICON)) : null,
            json.has(KEY_ACTION) ? readAction(json.getAsJsonObject(KEY_ACTION)) : null);
    }

    private static JsonObject writeIcon(SectionIcon icon) {
        JsonObject json = new JsonObject();

        if (icon instanceof ItemIcon itemIcon) {
            json.addProperty(KEY_TYPE, ICON_ITEM);
            json.addProperty("item", itemIcon.getItemId());
            json.addProperty("meta", itemIcon.getMeta());
            if (itemIcon.getNbt() != null) {
                json.addProperty("nbt", encodeNbt(itemIcon.getNbt()));
            }
        } else if (icon instanceof TextIcon textIcon) {
            json.addProperty(KEY_TYPE, ICON_TEXT);
            json.addProperty("text", textIcon.getText());
            json.addProperty(KEY_COLOR, ColorMath.formatRgb(textIcon.getColor()));
        } else if (icon instanceof SymbolIcon symbolIcon) {
            json.addProperty(KEY_TYPE, ICON_SYMBOL);
            json.addProperty("symbol", symbolIcon.getName());
            json.addProperty(KEY_COLOR, ColorMath.formatRgb(symbolIcon.getColor()));
        } else if (icon instanceof EffectIcon effectIcon) {
            json.addProperty(KEY_TYPE, ICON_EFFECT);
            json.addProperty("effect", effectIcon.getEffectName());
        } else if (icon instanceof ImageIcon imageIcon) {
            json.addProperty(KEY_TYPE, ICON_IMAGE);
            json.addProperty("file", imageIcon.getFileName());
        } else if (icon instanceof HeadIcon headIcon) {
            json.addProperty(KEY_TYPE, ICON_HEAD);
            json.addProperty("player", headIcon.getPlayerName());
        }
        return json;
    }

    @Nullable
    private static SectionIcon readIcon(JsonObject json) {
        String type = getString(json, KEY_TYPE, "");

        if (ICON_ITEM.equals(type)) {
            return new ItemIcon(
                json.get("item")
                    .getAsString(),
                json.has("meta") ? json.get("meta")
                    .getAsInt() : 0,
                json.has("nbt") ? decodeNbt(
                    json.get("nbt")
                        .getAsString())
                    : null);
        }
        if (ICON_TEXT.equals(type)) {
            return new TextIcon(
                getString(json, "text", ""),
                ColorMath.parseRgb(getString(json, KEY_COLOR, ""), TextIcon.DEFAULT_COLOR));
        }
        if (ICON_SYMBOL.equals(type)) {
            return new SymbolIcon(
                getString(json, "symbol", ""),
                ColorMath.parseRgb(getString(json, KEY_COLOR, ""), SymbolIcon.DEFAULT_COLOR));
        }
        if (ICON_EFFECT.equals(type)) {
            return new EffectIcon(getString(json, "effect", ""));
        }
        if (ICON_IMAGE.equals(type)) {
            return new ImageIcon(getString(json, "file", ""));
        }
        if (ICON_HEAD.equals(type)) {
            return new HeadIcon(getString(json, "player", ""));
        }
        RadialMenuMod.LOG.warn("ignoring unknown icon type {}", type);
        return null;
    }

    private static JsonObject writeAction(SectionAction action) {
        JsonObject json = new JsonObject();

        json.addProperty(
            KEY_TYPE,
            action.getType()
                .getId());
        switch (action.getType()) {
            case COMMAND -> json.addProperty("command", ((CommandAction) action).getCommand());
            case CHAT -> json.addProperty("message", ((ChatAction) action).getMessage());
            case KEYBIND -> {
                KeybindAction keybind = (KeybindAction) action;

                json.addProperty("key", keybind.getKeyDescription());
                json.addProperty(
                    "mode",
                    keybind.getMode()
                        .getId());
            }
            case HOTBAR -> json.addProperty("slot", ((HotbarAction) action).getSlot());
            case SUBMENU -> json.addProperty("menu", ((SubmenuAction) action).getMenuId());
            default -> {}
        }
        return json;
    }

    @Nullable
    private static SectionAction readAction(JsonObject json) {
        ActionType type = ActionType.fromId(getString(json, KEY_TYPE, ""));

        return switch (type) {
            case COMMAND -> new CommandAction(getString(json, "command", ""));
            case CHAT -> new ChatAction(getString(json, "message", ""));
            case KEYBIND -> new KeybindAction(
                getString(json, "key", ""),
                KeybindAction.Mode.fromId(getString(json, "mode", "")));
            case HOTBAR -> new HotbarAction(
                json.get("slot")
                    .getAsInt());
            case SUBMENU -> new SubmenuAction(getString(json, "menu", ""));
            case NONE -> null;
        };
    }

    private static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key)
            .getAsString() : fallback;
    }

    private static String encodeNbt(NBTTagCompound nbt) {
        try {
            return Base64.getEncoder()
                .encodeToString(CompressedStreamTools.compress(nbt));
        } catch (IOException e) {
            throw new IllegalStateException("failed to encode item nbt", e);
        }
    }

    @Nullable
    private static NBTTagCompound decodeNbt(String encoded) {
        try {
            return CompressedStreamTools.readCompressed(
                new ByteArrayInputStream(
                    Base64.getDecoder()
                        .decode(encoded)));
        } catch (IOException | IllegalArgumentException e) {
            RadialMenuMod.LOG.warn("dropping unreadable item nbt", e);
            return null;
        }
    }
}
