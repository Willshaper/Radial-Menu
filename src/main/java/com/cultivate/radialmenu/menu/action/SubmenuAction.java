package com.cultivate.radialmenu.menu.action;

import net.minecraft.client.resources.I18n;

import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.menu.RadialMenu;

/**
 * Opens another menu inside the radial. The radial screen handles this itself, so {@link #run()} does nothing.
 */
public class SubmenuAction implements SectionAction {

    private final String menuId;

    public SubmenuAction(String menuId) {
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }

    @Override
    public ActionType getType() {
        return ActionType.SUBMENU;
    }

    @Override
    public void run() {}

    @Override
    public String describe() {
        RadialMenu menu = RadialMenuClient.getMenuStore()
            .getMenu(menuId);

        if (menu == null) {
            return I18n.format("radialmenu.action.submenu.missing");
        }
        return I18n.format("radialmenu.action.submenu.describe", menu.getName());
    }
}
