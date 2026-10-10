package com.cultivate.radialmenu.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cultivate.radialmenu.menu.action.ChatAction;
import com.cultivate.radialmenu.menu.action.CommandAction;
import com.cultivate.radialmenu.menu.action.HotbarAction;
import com.cultivate.radialmenu.menu.action.KeybindAction;
import com.cultivate.radialmenu.menu.action.SubmenuAction;
import com.cultivate.radialmenu.menu.icon.EffectIcon;
import com.cultivate.radialmenu.menu.icon.HeadIcon;
import com.cultivate.radialmenu.menu.icon.ImageIcon;
import com.cultivate.radialmenu.menu.icon.SymbolIcon;
import com.cultivate.radialmenu.menu.icon.TextIcon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Item icons are left out: creating one looks the item up in Forge's registry, which needs a running game.
 */
class MenuJsonTest {

    private static List<RadialMenu> roundTrip(List<RadialMenu> menus) {
        return MenuJson.readMenus(MenuJson.write(menus, null));
    }

    private static List<RadialMenu> read(String json) {
        return MenuJson.readMenus(
            new JsonParser().parse(json)
                .getAsJsonObject());
    }

    @Test
    void keepsMenuSettings() {
        RadialMenu menu = new RadialMenu("tools", "Tools", 12, false);
        RadialMenu read = roundTrip(Collections.singletonList(menu)).get(0);

        assertEquals("tools", read.getId());
        assertEquals("Tools", read.getName());
        assertEquals(12, read.getSectionCount());
        assertFalse(read.isInCycle());
    }

    @Test
    void keepsEveryActionType() {
        RadialMenu menu = new RadialMenu("main", "Main", 8, true);
        RadialMenu read;

        menu.setSection(0, new MenuSection("Home", null, new CommandAction("/home")));
        menu.setSection(1, new MenuSection("Hi", null, new ChatAction("hello")));
        menu.setSection(2, new MenuSection("", null, new KeybindAction("key.sneak", KeybindAction.Mode.TOGGLE)));
        menu.setSection(3, new MenuSection("", null, new HotbarAction(4)));
        menu.setSection(4, new MenuSection("", null, new SubmenuAction("tools")));
        read = roundTrip(Collections.singletonList(menu)).get(0);

        assertEquals(
            "Home",
            read.getSection(0)
                .getLabel());
        assertEquals(
            "/home",
            ((CommandAction) read.getSection(0)
                .getAction()).getCommand());
        assertEquals(
            "hello",
            ((ChatAction) read.getSection(1)
                .getAction()).getMessage());
        assertEquals(
            "key.sneak",
            ((KeybindAction) read.getSection(2)
                .getAction()).getKeyDescription());
        assertEquals(
            KeybindAction.Mode.TOGGLE,
            ((KeybindAction) read.getSection(2)
                .getAction()).getMode());
        assertEquals(
            4,
            ((HotbarAction) read.getSection(3)
                .getAction()).getSlot());
        assertEquals(
            "tools",
            ((SubmenuAction) read.getSection(4)
                .getAction()).getMenuId());
    }

    @Test
    void keepsEveryIconTypeExceptItems() {
        RadialMenu menu = new RadialMenu("main", "Main", 8, true);
        RadialMenu read;

        menu.setSection(0, new MenuSection("", new TextIcon("TP", 0xFF8800), null));
        menu.setSection(1, new MenuSection("", new SymbolIcon("house", 0x3C78D8), null));
        menu.setSection(2, new MenuSection("", new EffectIcon("potion.moveSpeed"), null));
        menu.setSection(3, new MenuSection("", new ImageIcon("home.png"), null));
        menu.setSection(4, new MenuSection("", new HeadIcon("Steve"), null));
        read = roundTrip(Collections.singletonList(menu)).get(0);

        assertEquals(
            "TP",
            ((TextIcon) read.getSection(0)
                .getIcon()).getText());
        assertEquals(
            0xFF8800,
            ((TextIcon) read.getSection(0)
                .getIcon()).getColor());
        assertEquals(
            "house",
            ((SymbolIcon) read.getSection(1)
                .getIcon()).getName());
        assertEquals(
            0x3C78D8,
            ((SymbolIcon) read.getSection(1)
                .getIcon()).getColor());
        assertEquals(
            "potion.moveSpeed",
            ((EffectIcon) read.getSection(2)
                .getIcon()).getEffectName());
        assertEquals(
            "home.png",
            ((ImageIcon) read.getSection(3)
                .getIcon()).getFileName());
        assertEquals(
            "Steve",
            ((HeadIcon) read.getSection(4)
                .getIcon()).getPlayerName());
    }

    @Test
    void keepsHiddenSectionsAndEmptyGaps() {
        RadialMenu menu = new RadialMenu("main", "Main", 8, true);
        RadialMenu read;

        menu.setSection(5, new MenuSection("Kept", null, new CommandAction("/spawn")));
        menu.setSectionCount(4);
        read = roundTrip(Collections.singletonList(menu)).get(0);

        assertEquals(4, read.getSectionCount());
        assertTrue(
            read.getSection(0)
                .isEmpty());
        assertEquals(Collections.singletonList(5), read.getHiddenSectionsWithContent());
    }

    @Test
    void storesTheLastMenu() {
        JsonObject root = MenuJson.write(Collections.emptyList(), "tools");

        assertEquals("tools", MenuJson.readLastMenuId(root));
        assertNull(MenuJson.readLastMenuId(MenuJson.write(Collections.emptyList(), null)));
    }

    @Test
    void skipsBrokenMenusAndDuplicateIds() {
        List<RadialMenu> menus = read(
            "{\"menus\": [" + "{\"id\": \"a\", \"name\": \"First\"},"
                + "{\"name\": \"no id\"},"
                + "{\"id\": \"a\", \"name\": \"Duplicate\"},"
                + "{\"id\": \"b\"}"
                + "]}");

        assertEquals(2, menus.size());
        assertEquals(
            "First",
            menus.get(0)
                .getName());
        assertEquals(
            "b",
            menus.get(1)
                .getId());
        assertEquals(
            RadialMenu.DEFAULT_SECTIONS,
            menus.get(1)
                .getSectionCount());
        assertTrue(
            menus.get(1)
                .isInCycle());
    }

    @Test
    void dropsBrokenSectionsButKeepsTheRest() {
        RadialMenu menu = read(
            "{\"menus\": [{\"id\": \"a\", \"sections\": ["
                + "{\"label\": \"Good\", \"action\": {\"type\": \"command\", \"command\": \"/home\"}},"
                + "{\"label\": \"Bad\", \"action\": {\"type\": \"hotbar\"}},"
                + "{\"label\": \"Unknown icon\", \"icon\": {\"type\": \"sprite\"}}"
                + "]}]}").get(0);

        assertEquals(
            "Good",
            menu.getSection(0)
                .getLabel());
        assertTrue(
            menu.getSection(1)
                .isEmpty());
        assertEquals(
            "Unknown icon",
            menu.getSection(2)
                .getLabel());
        assertNull(
            menu.getSection(2)
                .getIcon());
    }

    @Test
    void readsShortColorsAndFallsBackOnBadOnes() {
        RadialMenu menu = read(
            "{\"menus\": [{\"id\": \"a\", \"sections\": ["
                + "{\"icon\": {\"type\": \"symbol\", \"symbol\": \"house\", \"color\": \"#F80\"}},"
                + "{\"icon\": {\"type\": \"text\", \"text\": \"X\", \"color\": \"nope\"}}"
                + "]}]}").get(0);

        assertEquals(
            0xFF8800,
            ((SymbolIcon) menu.getSection(0)
                .getIcon()).getColor());
        assertInstanceOf(
            TextIcon.class,
            menu.getSection(1)
                .getIcon());
        assertEquals(
            TextIcon.DEFAULT_COLOR,
            ((TextIcon) menu.getSection(1)
                .getIcon()).getColor());
    }

    @Test
    void readsNothingFromAFileWithoutMenus() {
        assertEquals(0, read("{}").size());
        assertEquals(Arrays.asList(), read("{\"menus\": []}"));
    }
}
