package com.cultivate.radialmenu.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.GuiScreenWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.client.ClientTickHandler;
import com.cultivate.radialmenu.client.KeyBindings;
import com.cultivate.radialmenu.client.KeyPresser;
import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.client.gui.editor.MenuManager;
import com.cultivate.radialmenu.client.gui.editor.SectionEditor;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.cultivate.radialmenu.menu.MenuSection;
import com.cultivate.radialmenu.menu.MenuStore;
import com.cultivate.radialmenu.menu.RadialMenu;
import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.action.SubmenuAction;

/**
 * The radial menu itself. Stays open while the open key is held; clicking a section runs it and closes the menu.
 *
 * <ul>
 * <li>Left click a section: run it, or step into it if it is a submenu. Clicking an empty section edits it.</li>
 * <li>Shift + left click a section: edit it. Shift + left click the center: open the menu manager.</li>
 * <li>Right click, or left click the center: go back from a submenu.</li>
 * <li>Switch key or scroll wheel: change to the next or previous menu.</li>
 * </ul>
 */
public class RadialScreen extends CustomModularScreen {

    private static final String PANEL_NAME = "radial_menu";
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final long OPEN_ANIMATION_NANOS = 150_000_000L;
    private static final float CLOSED_SCALE = 0.5f;

    // Menus above the current one, the root first. Empty when not in a submenu.
    private final Deque<RadialMenu> parentMenus = new ArrayDeque<>();
    private RadialMenu menu;
    private long openedAtNanos;
    private boolean lastMenuChanged;

    private RadialScreen(RadialMenu menu) {
        super(RadialMenuMod.MODID);
        this.menu = menu;
    }

    /**
     * Opens the radial on the menu used last time, or the first menu.
     */
    public static void open() {
        MenuStore store = RadialMenuClient.getMenuStore();
        List<RadialMenu> cycleMenus = store.getCycleMenus();
        RadialMenu start = cycleMenus.get(0);
        String lastMenuId = store.getLastMenuId();

        if (RadialMenuConfig.rememberLastMenu && (lastMenuId != null)) {
            RadialMenu lastMenu = store.getMenu(lastMenuId);

            if ((lastMenu != null) && cycleMenus.contains(lastMenu)) {
                start = lastMenu;
            }
        }
        openWithoutDarkBackground(new RadialScreen(start));
    }

    /**
     * Opens the screen the way {@code ClientGUI.open} does, but keeps the world visible behind it. ModularUI2 2.2.18
     * always darkens the background and has no setting to turn that off.
     */
    private static void openWithoutDarkBackground(RadialScreen screen) {
        screen.getContext()
            .setSettings(new UISettings());
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiScreenWrapper(screen) {

                @Override
                public void drawWorldBackground(int tint) {}
            });
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        return new ModularPanel(PANEL_NAME).full()
            .background(IDrawable.EMPTY)
            .child(new RadialWidget(this).full());
    }

    @Override
    public void onOpen() {
        super.onOpen();
        openedAtNanos = System.nanoTime();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!KeyPresser.isPhysicallyDown(KeyBindings.OPEN.getKeyCode())) {
            close();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (lastMenuChanged) {
            RadialMenuClient.getMenuStore()
                .save();
        }
    }

    RadialMenu getMenu() {
        return menu;
    }

    boolean isInSubmenu() {
        return !parentMenus.isEmpty();
    }

    /**
     * @return the menu names from the root down to the current menu
     */
    List<String> getMenuPath() {
        List<String> path = new ArrayList<>();

        for (RadialMenu parent : parentMenus) {
            path.add(parent.getName());
        }
        path.add(menu.getName());
        return path;
    }

    /**
     * @return the root menu's position in the switch cycle, counting from 1, or 0 if it is not in the cycle
     */
    int getCyclePosition() {
        return RadialMenuClient.getMenuStore()
            .getCycleMenus()
            .indexOf(getRootMenu()) + 1;
    }

    /**
     * @return the ring's size relative to its full size: grows from 0.5 to 1 while the open animation plays
     */
    float getOpenScale() {
        float progress;

        if (!RadialMenuConfig.openAnimation) {
            return 1f;
        }
        progress = Math.min(1f, (float) (System.nanoTime() - openedAtNanos) / OPEN_ANIMATION_NANOS);
        progress = 1 - ((1 - progress) * (1 - progress) * (1 - progress));
        return CLOSED_SCALE + ((1 - CLOSED_SCALE) * progress);
    }

    RadialLayout createLayout(float scale) {
        return new RadialLayout(getScreenArea().w(), getScreenArea().h(), menu.getSectionCount(), scale);
    }

    /**
     * @return the section under the mouse, measured on the ring as it is drawn right now
     */
    int getHoveredSection() {
        return createLayout(getOpenScale()).sectionAt(getContext().getAbsMouseX(), getContext().getAbsMouseY());
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        int hovered = getHoveredSection();
        boolean shift = Interactable.hasShiftDown();

        if (mouseButton == RIGHT_BUTTON) {
            goBack();
            return true;
        }
        if (mouseButton != LEFT_BUTTON) {
            return true;
        }
        if (hovered == RadialLayout.CENTER) {
            if (shift) {
                MenuManager.open(menu);
                close();
            } else {
                goBack();
            }
            return true;
        }
        clickSection(hovered, shift);
        return true;
    }

    private void clickSection(int index, boolean shift) {
        MenuSection section = menu.getSection(index);
        SectionAction action = section.getAction();

        if (shift || (action == null)) {
            SectionEditor.open(menu, index);
            close();
            return;
        }
        if (action instanceof SubmenuAction submenuAction) {
            enterSubmenu(submenuAction.getMenuId());
            return;
        }
        ClientTickHandler.scheduleAction(action);
        close();
    }

    private void enterSubmenu(String menuId) {
        RadialMenu submenu = RadialMenuClient.getMenuStore()
            .getMenu(menuId);

        if ((submenu == null) || (submenu == menu) || parentMenus.contains(submenu)) {
            return;
        }
        parentMenus.addLast(menu);
        menu = submenu;
    }

    private void goBack() {
        if (!parentMenus.isEmpty()) {
            menu = parentMenus.removeLast();
        }
    }

    @Override
    public boolean onMouseScroll(ModularScreen.UpOrDown scrollDirection, int amount) {
        switchMenu(scrollDirection.isUp() ? -1 : 1);
        return true;
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        // Unbound keybindings have key code 0, as do typed characters without a key; neither should match.
        if (keyCode == 0) {
            return super.onKeyPressed(typedChar, keyCode);
        }
        if (keyCode == KeyBindings.SWITCH.getKeyCode()) {
            switchMenu(Interactable.hasShiftDown() ? -1 : 1);
            return true;
        }
        // Holding the open key sends repeat presses; they must not reach anything else.
        if (keyCode == KeyBindings.OPEN.getKeyCode()) {
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    /**
     * Leaves any submenu and moves through the cycle by {@code offset} menus.
     */
    private void switchMenu(int offset) {
        List<RadialMenu> cycleMenus = RadialMenuClient.getMenuStore()
            .getCycleMenus();
        int index = cycleMenus.indexOf(getRootMenu());
        RadialMenu next = cycleMenus.get(Math.floorMod(index + offset, cycleMenus.size()));

        parentMenus.clear();
        if (next != menu) {
            menu = next;
            RadialMenuClient.getMenuStore()
                .setLastMenuId(next.getId());
            lastMenuChanged = true;
        }
    }

    private RadialMenu getRootMenu() {
        return parentMenus.isEmpty() ? menu : parentMenus.peekFirst();
    }
}
