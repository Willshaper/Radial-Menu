package com.cultivate.radialmenu.menu;

import javax.annotation.Nullable;

import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.icon.SectionIcon;

/**
 * One slice of a radial menu. Immutable; the editor replaces sections instead of changing them.
 */
public class MenuSection {

    public static final MenuSection EMPTY = new MenuSection("", null, null);

    private final String label;
    @Nullable
    private final SectionIcon icon;
    @Nullable
    private final SectionAction action;

    public MenuSection(String label, @Nullable SectionIcon icon, @Nullable SectionAction action) {
        this.label = label == null ? "" : label;
        this.icon = icon;
        this.action = action;
    }

    public String getLabel() {
        return label;
    }

    @Nullable
    public SectionIcon getIcon() {
        return icon;
    }

    @Nullable
    public SectionAction getAction() {
        return action;
    }

    public boolean isEmpty() {
        return label.isEmpty() && (icon == null) && (action == null);
    }
}
