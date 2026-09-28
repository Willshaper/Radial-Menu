package com.cultivate.radialmenu.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * One radial menu: a name and a ring of sections.
 *
 * <p>
 * Sections past {@link #getSectionCount()} are kept rather than deleted, so shrinking a menu and growing it again
 * does not lose anything.
 */
public class RadialMenu {

    public static final int MIN_SECTIONS = 2;
    public static final int MAX_SECTIONS = 24;
    public static final int DEFAULT_SECTIONS = 8;

    private final String id;
    private final List<MenuSection> sections = new ArrayList<>();
    private String name;
    private int sectionCount;
    private boolean inCycle;

    public RadialMenu(String id, String name, int sectionCount, boolean inCycle) {
        this.id = id;
        this.name = name;
        this.inCycle = inCycle;
        setSectionCount(sectionCount);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return true if the switch key and scroll wheel cycle through this menu; false if it is only reachable as a
     *         submenu
     */
    public boolean isInCycle() {
        return inCycle;
    }

    public void setInCycle(boolean inCycle) {
        this.inCycle = inCycle;
    }

    public int getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(int sectionCount) {
        this.sectionCount = Math.max(MIN_SECTIONS, Math.min(MAX_SECTIONS, sectionCount));
    }

    /**
     * @return the section at {@code index}, or an empty section if none has been set there yet
     */
    public MenuSection getSection(int index) {
        if (index < sections.size()) {
            return sections.get(index);
        }
        return MenuSection.EMPTY;
    }

    public void setSection(int index, MenuSection section) {
        while (sections.size() <= index) {
            sections.add(MenuSection.EMPTY);
        }
        sections.set(index, section);
    }

    /**
     * @return every stored section, including hidden ones past the section count
     */
    public List<MenuSection> getStoredSections() {
        return sections;
    }
}
