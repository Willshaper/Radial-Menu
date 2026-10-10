package com.cultivate.radialmenu.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * <li>Right click, or left click the center: fold away the outermost inline ring if one is shown, otherwise go back
 * from a submenu.</li>
 * <li>Switch key or scroll wheel: change to the next or previous menu.</li>
 * <li>With Release to Select on, letting go of the open key also runs the section under the mouse.</li>
 * <li>With Inline Submenus on, submenus never replace the menu. Resting the mouse on a submenu section, or clicking it,
 * shows the submenu as a ring outside the ring the section is in, up to {@link RadialLayout#MAX_INLINE_RINGS} rings
 * deep. Their sections work like any other.</li>
 * </ul>
 */
public class RadialScreen extends CustomModularScreen {

    private static final String PANEL_NAME = "radial_menu";
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final long OPEN_ANIMATION_NANOS = 150_000_000L;
    private static final float CLOSED_SCALE = 0.5f;
    // How long the mouse has to rest on a section before the inline rings change to match it.
    private static final long INLINE_DELAY_NANOS = 200_000_000L;

    // Menus above the current one, the root first. Empty when not in a submenu.
    private final Deque<RadialMenu> parentMenus = new ArrayDeque<>();
    // Submenus shown as inline rings, innermost first.
    private final List<InlineRing> inlineRings = new ArrayList<>();
    private RadialMenu menu;
    // How many inline rings the layout keeps room for, so the rings don't move as inline rings open and close.
    private int inlineRoom;
    private long openedAtNanos;
    private boolean lastMenuChanged;
    // The section the mouse is resting on, and since when, for the inline rings' delay.
    @Nullable
    private Target restingOn;
    private long restingSinceNanos;

    /**
     * A section of one of the rings: {@code level} 0 is the main ring, 1 the first inline ring, and so on.
     */
    static final class Target {

        final int level;
        final int index;

        Target(int level, int index) {
            this.level = level;
            this.index = index;
        }

        boolean isSection() {
            return index >= 0;
        }

        boolean sameAs(@Nullable Target other) {
            return (other != null) && (other.level == level) && (other.index == index);
        }
    }

    /**
     * A submenu shown as an inline ring, and the section of the ring inside it that opened it.
     */
    private static final class InlineRing {

        private final RadialMenu menu;
        private final int parentIndex;

        private InlineRing(RadialMenu menu, int parentIndex) {
            this.menu = menu;
            this.parentIndex = parentIndex;
        }
    }

    private RadialScreen(RadialMenu menu) {
        super(RadialMenuMod.MODID);
        drawDarkBackground(false);
        setMenu(menu);
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
            updateInlineRings();
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

    /**
     * @return the menu shown in the ring at {@code level}: the current menu for 0, an inline ring's submenu above that
     */
    RadialMenu getMenuAt(int level) {
        return level == 0 ? menu : inlineRings.get(level - 1).menu;
    }

    int getInlineRingCount() {
        return inlineRings.size();
    }

    /**
     * @return the section of the ring at {@code level} whose submenu is shown as the next ring out, or
     *         {@link RadialLayout#NONE}
     */
    int getOpenSubmenuSection(int level) {
        return level < inlineRings.size() ? inlineRings.get(level).parentIndex : RadialLayout.NONE;
    }

    /**
     * @return true if right click or the center would do something: fold an inline ring or leave a submenu
     */
    boolean canGoBack() {
        return !inlineRings.isEmpty() || !parentMenus.isEmpty();
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

    RadialLayout createLayout() {
        return new RadialLayout(
            getScreenArea().w(),
            getScreenArea().h(),
            menu.getSectionCount(),
            getOpenScale(),
            inlineRoom);
    }

    /**
     * @return the main ring followed by the inline rings, innermost first
     */
    List<RadialLayout.Ring> createRings(RadialLayout layout) {
        List<RadialLayout.Ring> rings = new ArrayList<>();
        RadialLayout.Ring ring = layout.mainRing();

        rings.add(ring);
        for (InlineRing inline : inlineRings) {
            ring = layout.inlineRing(ring, inline.parentIndex, inline.menu.getSectionCount());
            rings.add(ring);
        }
        return rings;
    }

    /**
     * @return the section under the mouse, measured on the rings as they are drawn right now. Outer rings are checked
     *         first. On the main ring the index can be {@link RadialLayout#CENTER}.
     */
    Target getHovered() {
        RadialLayout layout = createLayout();
        List<RadialLayout.Ring> rings = createRings(layout);
        float x = getContext().getAbsMouseX();
        float y = getContext().getAbsMouseY();

        for (int level = rings.size() - 1; level > 0; level--) {
            int index = layout.indexAt(rings.get(level), x, y);

            if (index != RadialLayout.NONE) {
                return new Target(level, index);
            }
        }
        return new Target(0, layout.sectionAt(x, y));
    }

    private MenuSection getSection(Target target) {
        return getMenuAt(target.level).getSection(target.index);
    }

    private void setMenu(RadialMenu newMenu) {
        Set<RadialMenu> path = new HashSet<>(parentMenus);

        menu = newMenu;
        inlineRings.clear();
        path.add(newMenu);
        inlineRoom = RadialMenuConfig.inlineSubmenus ? submenuDepth(newMenu, path, RadialLayout.MAX_INLINE_RINGS) : 0;
    }

    /**
     * @return how many inline rings deep the submenus of {@code from} go, counting at most {@code limit}
     */
    private static int submenuDepth(RadialMenu from, Set<RadialMenu> path, int limit) {
        int deepest = 0;

        if (limit == 0) {
            return 0;
        }
        for (int i = 0; i < from.getSectionCount(); i++) {
            RadialMenu submenu = findMenu(from.getSection(i));

            if ((submenu != null) && path.add(submenu)) {
                deepest = Math.max(deepest, 1 + submenuDepth(submenu, path, limit - 1));
                path.remove(submenu);
            }
        }
        return deepest;
    }

    @Nullable
    private static RadialMenu findMenu(MenuSection section) {
        if (section.getAction() instanceof SubmenuAction submenuAction) {
            return RadialMenuClient.getMenuStore()
                .getMenu(submenuAction.getMenuId());
        }
        return null;
    }

    /**
     * @return the menu the section at {@code level} opens, or null if it opens none or that menu is already shown
     */
    @Nullable
    private RadialMenu findSubmenu(MenuSection section, int level) {
        RadialMenu submenu = findMenu(section);

        if ((submenu == null) || parentMenus.contains(submenu)) {
            return null;
        }
        for (int i = 0; i <= level; i++) {
            if (getMenuAt(i) == submenu) {
                return null;
            }
        }
        return submenu;
    }

    /**
     * Follows the section the mouse has rested on: shows its submenu as the next ring out, or folds away the rings
     * outside it if it has none. Resting on the center, or on the section whose ring is already shown, changes
     * nothing.
     */
    private void updateInlineRings() {
        Target hovered = getHovered();
        long now = System.nanoTime();

        if (!hovered.isSection() || (getOpenSubmenuSection(hovered.level) == hovered.index)) {
            restingOn = null;
            return;
        }
        if (!hovered.sameAs(restingOn)) {
            restingOn = hovered;
            restingSinceNanos = now;
            return;
        }
        if ((now - restingSinceNanos) >= INLINE_DELAY_NANOS) {
            showInline(hovered);
            restingOn = null;
        }
    }

    /**
     * Folds away the rings outside the target's ring, then shows the target's submenu as the next ring if it has one
     * and there is room.
     */
    private void showInline(Target target) {
        RadialMenu submenu = findSubmenu(getSection(target), target.level);

        foldInlineRingsOutside(target.level);
        if ((submenu != null) && (target.level < RadialLayout.MAX_INLINE_RINGS)) {
            inlineRings.add(new InlineRing(submenu, target.index));
        }
    }

    private void foldInlineRingsOutside(int level) {
        while (inlineRings.size() > level) {
            inlineRings.remove(inlineRings.size() - 1);
        }
    }

    /**
     * Runs the section under the mouse as the open key is let go, unless it is empty or opens a submenu.
     */
    private void runHoveredSection() {
        Target hovered = getHovered();
        SectionAction action;

        if (!hovered.isSection()) {
            return;
        }
        action = getSection(hovered).getAction();
        if ((action != null) && !(action instanceof SubmenuAction)) {
            ClientTickHandler.scheduleAction(action);
        }
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        Target hovered = getHovered();
        boolean shift = Interactable.hasShiftDown();

        if (mouseButton == RIGHT_BUTTON) {
            goBack();
            return true;
        }
        if (mouseButton != LEFT_BUTTON) {
            return true;
        }
        if (hovered.isSection()) {
            clickSection(hovered, shift);
        } else if (shift) {
            MenuManager.open(menu);
            close();
        } else {
            goBack();
        }
        return true;
    }

    private void clickSection(Target target, boolean shift) {
        MenuSection section = getSection(target);
        SectionAction action = section.getAction();

        if (shift || (action == null)) {
            SectionEditor.open(getMenuAt(target.level), target.index);
            close();
            return;
        }
        if (!(action instanceof SubmenuAction)) {
            ClientTickHandler.scheduleAction(action);
            close();
            return;
        }
        if (!RadialMenuConfig.inlineSubmenus) {
            enterSubmenu(section);
        } else if (getOpenSubmenuSection(target.level) == target.index) {
            foldInlineRingsOutside(target.level);
        } else {
            showInline(target);
        }
    }

    private void enterSubmenu(MenuSection section) {
        RadialMenu submenu = findSubmenu(section, 0);

        if (submenu != null) {
            parentMenus.addLast(menu);
            setMenu(submenu);
        }
    }

    /**
     * Folds away the outermost inline ring if one is shown, otherwise goes back from a submenu.
     */
    private void goBack() {
        if (!inlineRings.isEmpty()) {
            foldInlineRingsOutside(inlineRings.size() - 1);
        } else if (!parentMenus.isEmpty()) {
            setMenu(parentMenus.removeLast());
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
        if (next != menu) {
            RadialMenuClient.getMenuStore()
                .setLastMenuId(next.getId());
            lastMenuChanged = true;
        }
        setMenu(next);
    }

    private RadialMenu getRootMenu() {
        return parentMenus.isEmpty() ? menu : parentMenus.peekFirst();
    }
}
