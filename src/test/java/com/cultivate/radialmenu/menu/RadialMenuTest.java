package com.cultivate.radialmenu.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cultivate.radialmenu.menu.action.CommandAction;

class RadialMenuTest {

    private static MenuSection command(String command) {
        return new MenuSection("", null, new CommandAction(command));
    }

    @Test
    void clampsTheSectionCount() {
        RadialMenu menu = new RadialMenu("a", "A", 100, true);

        assertEquals(RadialMenu.MAX_SECTIONS, menu.getSectionCount());
        menu.setSectionCount(0);
        assertEquals(RadialMenu.MIN_SECTIONS, menu.getSectionCount());
    }

    @Test
    void unsetSectionsAreEmpty() {
        RadialMenu menu = new RadialMenu("a", "A", 8, true);

        assertSame(MenuSection.EMPTY, menu.getSection(7));
        assertTrue(
            menu.getSection(23)
                .isEmpty());
    }

    @Test
    void swapsSections() {
        RadialMenu menu = new RadialMenu("a", "A", 8, true);
        MenuSection home = command("/home");

        menu.setSection(0, home);
        menu.swapSections(0, 3);
        assertSame(home, menu.getSection(3));
        assertTrue(
            menu.getSection(0)
                .isEmpty());
    }

    @Test
    void addedSectionsFillEmptySlotsAndGrowTheMenu() {
        RadialMenu menu = new RadialMenu("a", "A", 3, true);
        MenuSection home = command("/home");

        menu.setSection(0, home);
        menu.setSection(2, home);
        assertEquals(3, menu.addSections(Arrays.asList(command("/a"), command("/b"), command("/c"))));
        assertEquals(
            "/a",
            ((CommandAction) menu.getSection(1)
                .getAction()).getCommand());
        assertEquals(
            "/c",
            ((CommandAction) menu.getSection(4)
                .getAction()).getCommand());
        assertEquals(5, menu.getSectionCount());
    }

    @Test
    void addingStopsWhenTheMenuIsFull() {
        RadialMenu menu = new RadialMenu("a", "A", 8, true);
        List<MenuSection> many = new ArrayList<>();

        for (int i = 0; i < RadialMenu.MAX_SECTIONS + 5; i++) {
            many.add(command("/" + i));
        }
        menu.setSection(0, command("/taken"));
        assertEquals(RadialMenu.MAX_SECTIONS - 1, menu.addSections(many));
        assertEquals(RadialMenu.MAX_SECTIONS, menu.getSectionCount());
    }

    @Test
    void shrinkingHidesSectionsWithoutDeletingThem() {
        RadialMenu menu = new RadialMenu("a", "A", 8, true);
        MenuSection spawn = command("/spawn");

        menu.setSection(5, spawn);
        menu.setSection(7, command("/back"));
        menu.setSection(6, MenuSection.EMPTY);
        menu.setSectionCount(4);
        assertEquals(Arrays.asList(5, 7), menu.getHiddenSectionsWithContent());

        menu.setSectionCount(8);
        assertSame(spawn, menu.getSection(5));
        assertEquals(Collections.emptyList(), menu.getHiddenSectionsWithContent());
    }
}
