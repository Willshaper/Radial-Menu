package com.cultivate.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.client.ClientTickHandler;
import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.cultivate.radialmenu.config.RingColor;
import com.cultivate.radialmenu.menu.MenuSection;
import com.cultivate.radialmenu.menu.MenuStore;
import com.cultivate.radialmenu.menu.RadialMenu;
import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.icon.SectionIcon;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

/**
 * Lists every menu and edits the selected one: name, section count, whether the switch key reaches it, its colors,
 * and its sections.
 *
 * <p>
 * Adding, removing or reordering menus reopens the screen so the menu list is rebuilt.
 */
public final class MenuManager {

    private static final String PANEL_NAME = "menu_manager";
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 250;
    private static final int PADDING = 7;
    private static final int MENU_LIST_WIDTH = 120;
    // Given as a fixed width: MUI2 does not size rows inside a column that only has a relative width.
    private static final int SELECTED_COLUMN_WIDTH = PANEL_WIDTH - (2 * PADDING) - MENU_LIST_WIDTH - PADDING;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int MOVE_BUTTON_WIDTH = 12;
    private static final int ICON_SIZE = 16;
    private static final int NUMBER_WIDTH = 18;
    private static final int SELECTED_FILL = 0x503C78D8;
    private static final int SELECTED_ACCENT = 0xFF3C78D8;
    private static final int ACCENT_WIDTH = 2;
    private static final int WARNING_COLOR = 0xFFFFAA40;
    private static final int WARNING_HEIGHT = 18;
    private static final int SWATCH_WIDTH = 28;
    private static final int SWATCH_INSET = 3;
    private static final int RESET_WIDTH = 40;
    // Drawn under each swatch so see-through colors look see-through.
    private static final int SWATCH_BACKDROP = 0xFF8B8B8B;
    private static final String SUBMENU_MARK = "↳ ";

    private final MenuStore store = RadialMenuClient.getMenuStore();
    private final RadialMenu selected;
    private ModularPanel panel;
    // Deleting takes two clicks so a menu isn't lost to a stray click.
    private boolean deleteArmed;

    private MenuManager(RadialMenu selected) {
        this.selected = selected;
    }

    /**
     * @param selected the menu to show first, or null for the first menu
     */
    public static void open(@Nullable RadialMenu selected) {
        ClientTickHandler.openScreenNextTick(() -> {
            MenuStore store = RadialMenuClient.getMenuStore();
            RadialMenu shown = (selected != null) && store.getMenus()
                .contains(selected) ? selected
                    : store.getMenus()
                        .get(0);
            MenuManager manager = new MenuManager(shown);

            ClientGUI.open(new ModularScreen(RadialMenuMod.MODID, manager.buildPanel()) {

                @Override
                public void onClose() {
                    super.onClose();
                    manager.store.save();
                }
            });
        });
    }

    private ModularPanel buildPanel() {
        panel = new ModularPanel(PANEL_NAME).size(PANEL_WIDTH, PANEL_HEIGHT)
            .padding(PADDING);
        return panel.child(
            Flow.row()
                .sizeRel(1f)
                .childPadding(PADDING)
                .child(buildMenuColumn())
                .child(buildSelectedColumn()));
    }

    private Flow buildMenuColumn() {
        List<RadialMenu> menus = store.getMenus();

        return Flow.column()
            .width(MENU_LIST_WIDTH)
            .heightRel(1f)
            .childPadding(3)
            .child(
                IKey.lang("radialmenu.manager.menus")
                    .asWidget()
                    .widthRel(1f)
                    .height(12))
            .child(
                new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                    .widthRel(1f)
                    .expanded()
                    .children(createMenuButtons(menus)))
            .child(
                Flow.row()
                    .widthRel(1f)
                    .height(EditorWidgets.ROW_HEIGHT)
                    .childPadding(2)
                    .child(
                        EditorWidgets.button("radialmenu.manager.new", this::createMenu)
                            .width(MENU_LIST_WIDTH - (2 * (EditorWidgets.ROW_HEIGHT + 2))))
                    .child(
                        new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                            .overlay(IKey.str("▲"))
                            .onMousePressed(mouseButton -> {
                                moveSelected(-1);
                                return true;
                            }))
                    .child(
                        new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                            .overlay(IKey.str("▼"))
                            .onMousePressed(mouseButton -> {
                                moveSelected(1);
                                return true;
                            })))
            .child(
                new ButtonWidget<>().widthRel(1f)
                    .height(EditorWidgets.ROW_HEIGHT)
                    .overlay(
                        IKey.dynamic(
                            () -> I18n.format(
                                deleteArmed ? "radialmenu.manager.delete_confirm" : "radialmenu.manager.delete")))
                    .onUpdateListener(
                        EditorWidgets.showIf(
                            () -> store.getMenus()
                                .size() > 1),
                        true)
                    .onMousePressed(mouseButton -> {
                        deleteSelected();
                        return true;
                    }));
    }

    private List<IWidget> createMenuButtons(List<RadialMenu> menus) {
        List<IWidget> buttons = new ArrayList<>();

        for (RadialMenu menu : menus) {
            buttons.add(createMenuButton(menu));
        }
        return buttons;
    }

    /**
     * A menu in the list, drawn like the section rows: name on the left, section count on the right. The selected menu
     * gets a tinted fill and an accent bar. Menus only reachable as submenus are marked with an arrow.
     */
    private ButtonWidget<?> createMenuButton(RadialMenu menu) {
        return new ButtonWidget<>().widthRel(1f)
            .marginRight(EditorWidgets.LIST_ROW_INSET)
            .height(LIST_ROW_HEIGHT)
            .overlay((context, x, y, width, height, widgetTheme) -> {
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                String name = (menu.isInCycle() ? "" : SUBMENU_MARK) + menu.getName();
                String count = String.valueOf(menu.getSectionCount());
                int countWidth = font.getStringWidth(count);
                float textY = y + ((height - EditorWidgets.FONT_HEIGHT) / 2f);

                if (menu == selected) {
                    GuiDraw.drawRect(x, y, width, height, SELECTED_FILL);
                    GuiDraw.drawRect(x, y, ACCENT_WIDTH, height, SELECTED_ACCENT);
                }
                GuiDraw.drawText(
                    EditorWidgets.fitToWidth(font, name, width - (3 * EditorWidgets.TEXT_INSET) - countWidth),
                    x + EditorWidgets.TEXT_INSET + ACCENT_WIDTH,
                    textY,
                    1f,
                    EditorWidgets.TEXT_COLOR,
                    true);
                GuiDraw.drawText(
                    count,
                    x + width - EditorWidgets.TEXT_INSET - countWidth,
                    textY,
                    1f,
                    EditorWidgets.DETAIL_COLOR,
                    true);
            })
            .tooltip(tooltip -> tooltip.addLine(I18n.format("radialmenu.manager.menu_tooltip")))
            .onMousePressed(mouseButton -> {
                if (menu != selected) {
                    open(menu);
                }
                return true;
            });
    }

    private Flow buildSelectedColumn() {
        return Flow.column()
            .width(SELECTED_COLUMN_WIDTH)
            .heightRel(1f)
            .childPadding(3)
            .collapseDisabledChild()
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.name",
                    new LiveTextField(selected::getName, value -> selected.setName(value)).expanded()
                        .height(EditorWidgets.ROW_HEIGHT)))
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.manager.sections",
                    new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                        .overlay(IKey.str("-"))
                        .onMousePressed(mouseButton -> {
                            selected.setSectionCount(selected.getSectionCount() - 1);
                            return true;
                        }),
                    IKey.dynamic(() -> String.valueOf(selected.getSectionCount()))
                        .asWidget()
                        .width(20)
                        .heightRel(1f),
                    new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                        .overlay(IKey.str("+"))
                        .onMousePressed(mouseButton -> {
                            selected.setSectionCount(selected.getSectionCount() + 1);
                            return true;
                        })))
            .child(
                IKey.dynamic(this::getHiddenSectionsWarning)
                    .color(WARNING_COLOR)
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .widthRel(1f)
                    .height(WARNING_HEIGHT)
                    .onUpdateListener(
                        EditorWidgets.showIf(
                            () -> !selected.getHiddenSectionsWithContent()
                                .isEmpty()),
                        true))
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.manager.in_cycle",
                    EditorWidgets
                        .cycleButton(
                            () -> I18n.format(selected.isInCycle() ? "gui.yes" : "gui.no"),
                            offset -> selected.setInCycle(!selected.isInCycle()))
                        .expanded()))
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.manager.colors",
                    createColorSwatch(RingColor.SECTION),
                    createColorSwatch(RingColor.HOVER),
                    createColorSwatch(RingColor.CENTER),
                    EditorWidgets.button("radialmenu.manager.reset_colors", MenuManager::resetColors)
                        .width(RESET_WIDTH)))
            .child(
                new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                    .widthRel(1f)
                    .expanded()
                    .collapseDisabledChild()
                    .children(RadialMenu.MAX_SECTIONS, this::createSectionRow));
    }

    /**
     * @return a warning naming the sections, counted from 1, that are hidden by the section count but not empty
     */
    private String getHiddenSectionsWarning() {
        String numbers = selected.getHiddenSectionsWithContent()
            .stream()
            .map(index -> String.valueOf(index + 1))
            .collect(Collectors.joining(", "));

        return I18n.format("radialmenu.manager.hidden_warning", numbers);
    }

    /**
     * A button filled with one of the ring colors. Clicking it opens a color picker that saves straight to the config.
     */
    private ButtonWidget<?> createColorSwatch(RingColor color) {
        return new ButtonWidget<>().size(SWATCH_WIDTH, EditorWidgets.ROW_HEIGHT)
            .overlay((context, x, y, width, height, widgetTheme) -> {
                GuiDraw.drawRect(
                    x + SWATCH_INSET,
                    y + SWATCH_INSET,
                    width - (2 * SWATCH_INSET),
                    height - (2 * SWATCH_INSET),
                    SWATCH_BACKDROP);
                GuiDraw.drawRect(
                    x + SWATCH_INSET,
                    y + SWATCH_INSET,
                    width - (2 * SWATCH_INSET),
                    height - (2 * SWATCH_INSET),
                    color.getArgb());
            })
            .tooltip(tooltip -> tooltip.addLine(I18n.format(color.getLangKey())))
            .onMousePressed(mouseButton -> {
                IPanelHandler.simple(panel, (parentPanel, player) -> new ColorPickerDialog("color_picker", argb -> {
                    color.setArgb(argb);
                    ConfigurationManager.save(RadialMenuConfig.class);
                }, color.getArgb(), true), true)
                    .openPanel();
                return true;
            });
    }

    private static void resetColors() {
        for (RingColor color : RingColor.values()) {
            color.reset();
        }
        ConfigurationManager.save(RadialMenuConfig.class);
    }

    /**
     * One section in the list: a button that edits it, and arrows that move it up or down the ring.
     */
    private Flow createSectionRow(int index) {
        return Flow.row()
            .widthRel(1f)
            .marginRight(EditorWidgets.LIST_ROW_INSET)
            .height(LIST_ROW_HEIGHT)
            .childPadding(1)
            .onUpdateListener(EditorWidgets.showIf(() -> index < selected.getSectionCount()), true)
            .child(
                new ButtonWidget<>().expanded()
                    .heightRel(1f)
                    .overlay(sectionRowDrawable(index))
                    .onMousePressed(mouseButton -> {
                        store.save();
                        SectionEditor.open(selected, index, true);
                        return true;
                    }))
            .child(createMoveButton(index, -1, "▲"))
            .child(createMoveButton(index, 1, "▼"));
    }

    private ButtonWidget<?> createMoveButton(int index, int offset, String arrow) {
        return new ButtonWidget<>().width(MOVE_BUTTON_WIDTH)
            .heightRel(1f)
            .overlay(IKey.str(arrow))
            .tooltip(
                tooltip -> tooltip
                    .addLine(I18n.format(offset < 0 ? "radialmenu.manager.move_up" : "radialmenu.manager.move_down")))
            .onMousePressed(mouseButton -> {
                moveSection(index, offset);
                return true;
            });
    }

    /**
     * Swaps a section with its neighbour. Moving past either end of the ring does nothing.
     */
    private void moveSection(int index, int offset) {
        int target = index + offset;

        if ((target < 0) || (target >= selected.getSectionCount())) {
            return;
        }
        selected.swapSections(index, target);
        store.save();
    }

    private IDrawable sectionRowDrawable(int index) {
        return (context, x, y, width, height, widgetTheme) -> {
            MenuSection section = selected.getSection(index);
            SectionIcon icon = section.getIcon();
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            float textY = y + ((height - EditorWidgets.FONT_HEIGHT) / 2f);
            float iconX = x + NUMBER_WIDTH;
            int textX = Math.round(iconX + ICON_SIZE + EditorWidgets.TEXT_INSET);

            GuiDraw
                .drawText((index + 1) + ".", x + EditorWidgets.TEXT_INSET, textY, 1f, EditorWidgets.TEXT_COLOR, true);
            if (icon != null) {
                icon.draw(iconX + (ICON_SIZE / 2f), y + (height / 2f), ICON_SIZE);
            }
            GuiDraw.drawText(
                EditorWidgets
                    .fitToWidth(font, describeSection(section), (x + width) - textX - EditorWidgets.TEXT_INSET),
                textX,
                textY,
                1f,
                section.isEmpty() ? EditorWidgets.DETAIL_COLOR : EditorWidgets.TEXT_COLOR,
                true);
        };
    }

    private static String describeSection(MenuSection section) {
        SectionAction action = section.getAction();

        if (section.isEmpty()) {
            return I18n.format("radialmenu.manager.empty_section");
        }
        if (!section.getLabel()
            .isEmpty()) {
            return section.getLabel();
        }
        return action == null ? "" : action.describe();
    }

    private void createMenu() {
        RadialMenu menu = store.createMenu(I18n.format("radialmenu.manager.default_menu_name"), true);

        store.save();
        open(menu);
    }

    private void moveSelected(int offset) {
        store.moveMenu(selected, offset);
        store.save();
        open(selected);
    }

    private void deleteSelected() {
        if (!deleteArmed) {
            deleteArmed = true;
            return;
        }
        if (store.deleteMenu(selected)) {
            store.save();
            open(null);
        }
    }
}
