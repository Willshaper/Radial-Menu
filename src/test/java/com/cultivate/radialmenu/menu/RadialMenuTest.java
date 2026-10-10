package com.cultivate.radialmenu.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

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
