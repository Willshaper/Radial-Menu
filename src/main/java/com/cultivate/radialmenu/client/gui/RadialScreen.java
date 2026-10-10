package com.cultivate.radialmenu.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
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
 * <li>Right click, or left click the center: fold away the inline ring if one is shown, otherwise go back from a
 * submenu.</li>
 * <li>Switch key or scroll wheel: change to the next or previous menu.</li>
 * <li>With Release to Select on, letting go of the open key also runs the section under the mouse.</li>
 * <li>With Inline Submenus on, resting the mouse on a submenu section shows that submenu as a second ring outside the
 * first. Its sections work like any other.</li>
 * </ul>
 */
public class RadialScreen extends CustomModularScreen {

    private static final String PANEL_NAME = "radial_menu";
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final long OPEN_ANIMATION_NANOS = 150_000_000L;
    private static final float CLOSED_SCALE = 0.5f;
    // How long the mouse has to rest on a section before the inline ring changes to match it.
    private static final long INLINE_DELAY_NANOS = 200_000_000L;

    // Menus above the current one, the root first. Empty when not in a submenu.
    private final Deque<RadialMenu> parentMenus = new ArrayDeque<>();
    private RadialMenu menu;
    private long openedAtNanos;
    private boolean lastMenuChanged;
    // The submenu shown as the inline ring, and the section of the current menu it belongs to.
    @Nullable
    private RadialMenu inlineMenu;
    private int inlineParent = RadialLayout.NONE;
    // The section the mouse is resting on, and since when, for the inline ring's delay.
    private int restingSection = RadialLayout.NONE;
    private long restingSinceNanos;

    private RadialScreen(RadialMenu menu) {
        super(RadialMenuMod.MODID);
        this.menu = menu;
        drawDarkBackground(false);
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
        ClientGUI.open(new RadialScreen(start));
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        return new ModularPanel(PANEL_NAME).full()
            .background(IDrawable.EMPTY)
            .disableThemeBackground(true)
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
            if (RadialMenuConfig.releaseToSelect) {
                runHoveredSection();
            }
            close();
        }
    }

    @Override
    public void onFrameUpdate() {
        super.onFrameUpdate();
        if (RadialMenuConfig.inlineSubmenus) {
            updateInlineRing();
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

    @Nullable
    RadialMenu getInlineMenu() {
        return inlineMenu;
    }

    int getInlineParent() {
        return inlineParent;
    }

    /**
     * @return true if right click or the center would do something: fold the inline ring or leave a submenu
     */
    boolean canGoBack() {
        return (inlineMenu != null) || !parentMenus.isEmpty();
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
        return new RadialLayout(
            getScreenArea().w(),
            getScreenArea().h(),
            menu.getSectionCount(),
            scale,
            RadialMenuConfig.inlineSubmenus);
    }

    /**
     * @return the section under the mouse, {@link RadialLayout#CENTER}, or {@link RadialLayout#NONE} while the mouse is
     *         over the inline ring. Measured on the ring as it is drawn right now.
     */
    int getHoveredSection() {
        if (getHoveredInlineSection() != RadialLayout.NONE) {
            return RadialLayout.NONE;
        }
        return createLayout(getOpenScale()).sectionAt(getContext().getAbsMouseX(), getContext().getAbsMouseY());
    }

    /**
     * @return the inline ring section under the mouse, or {@link RadialLayout#NONE}
     */
    int getHoveredInlineSection() {
        if (inlineMenu == null) {
            return RadialLayout.NONE;
        }
        return createLayout(getOpenScale()).inlineSectionAt(
            getContext().getAbsMouseX(),
            getContext().getAbsMouseY(),
            inlineParent,
            inlineMenu.getSectionCount());
    }

    /**
     * Shows the submenu of the section the mouse has rested on, or hides the inline ring once the mouse rests on a
     * section without one. Resting on the center or the inline ring changes nothing.
     */
    private void updateInlineRing() {
        int hovered = getHoveredSection();
        long now = System.nanoTime();

        if ((hovered < 0) || (hovered == inlineParent)) {
            restingSection = RadialLayout.NONE;
            return;
        }
        if (hovered != restingSection) {
            restingSection = hovered;
            restingSinceNanos = now;
            return;
        }
        if ((now - restingSinceNanos) >= INLINE_DELAY_NANOS) {
            showInline(hovered);
            restingSection = RadialLayout.NONE;
        }
    }

    private void showInline(int index) {
        RadialMenu submenu = findSubmenu(menu.getSection(index));

        if (submenu == null) {
            hideInline();
            return;
        }
        inlineMenu = submenu;
        inlineParent = index;
    }

    private void hideInline() {
        inlineMenu = null;
        inlineParent = RadialLayout.NONE;
    }

    /**
     * @return the menu the section opens, or null if it opens none or that menu is already on the path
     */
    @Nullable
    private RadialMenu findSubmenu(MenuSection section) {
        RadialMenu submenu;

        if (!(section.getAction() instanceof SubmenuAction submenuAction)) {
            return null;
        }
        submenu = RadialMenuClient.getMenuStore()
            .getMenu(submenuAction.getMenuId());
        if ((submenu == null) || (submenu == menu) || parentMenus.contains(submenu)) {
            return null;
        }
        return submenu;
    }

    /**
     * Runs the section under the mouse as the open key is let go, unless it is empty or opens a submenu.
     */
    private void runHoveredSection() {
        int inlineHovered = getHoveredInlineSection();
        int hovered = getHoveredSection();
        SectionAction action;

        if ((inlineHovered != RadialLayout.NONE) && (inlineMenu != null)) {
            action = inlineMenu.getSection(inlineHovered)
                .getAction();
        } else if (hovered >= 0) {
            action = menu.getSection(hovered)
                .getAction();
        } else {
            return;
        }
        if ((action != null) && !(action instanceof SubmenuAction)) {
            ClientTickHandler.scheduleAction(action);
        }
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        int inlineHovered = getHoveredInlineSection();
        int hovered = getHoveredSection();
        boolean shift = Interactable.hasShiftDown();

        if (mouseButton == RIGHT_BUTTON) {
            goBack();
            return true;
        }
        if (mouseButton != LEFT_BUTTON) {
            return true;
        }
        if ((inlineHovered != RadialLayout.NONE) && (inlineMenu != null)) {
            clickSection(inlineMenu, inlineHovered, shift);
        } else if (hovered == RadialLayout.CENTER) {
            if (shift) {
                MenuManager.open(menu);
                close();
            } else {
                goBack();
            }
        } else {
            clickSection(menu, hovered, shift);
        }
        return true;
    }

    /**
     * @param owner the menu the section is in: the current menu, or the inline ring's menu
     */
    private void clickSection(RadialMenu owner, int index, boolean shift) {
        MenuSection section = owner.getSection(index);
        SectionAction action = section.getAction();

        if (shift || (action == null)) {
            SectionEditor.open(owner, index);
            close();
            return;
        }
        if (action instanceof SubmenuAction) {
            if (owner != menu) {
                // A submenu inside the inline ring opens from the inline ring's menu, which becomes the current one.
                stepInto(owner);
            }
            enterSubmenu(section);
            return;
        }
        ClientTickHandler.scheduleAction(action);
        close();
    }

    private void enterSubmenu(MenuSection section) {
        RadialMenu submenu = findSubmenu(section);

        if (submenu != null) {
            stepInto(submenu);
        }
    }

    private void stepInto(RadialMenu submenu) {
        parentMenus.addLast(menu);
        menu = submenu;
        hideInline();
    }

    /**
     * Folds away the inline ring if one is shown, otherwise goes back from a submenu.
     */
    private void goBack() {
        if (inlineMenu != null) {
            hideInline();
        } else if (!parentMenus.isEmpty()) {
            menu = parentMenus.removeLast();
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
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
        hideInline();
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
