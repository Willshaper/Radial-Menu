package com.cultivate.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
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
 * Lists every menu and edits the selected one: name, order, section count, whether it is only a submenu, the ring
 * colors, and its sections. Ticked sections can be moved or copied to another menu.
 *
 * <p>
 * Adding, removing or reordering menus reopens the screen so the menu list is rebuilt.
 */
public final class MenuManager {

    private static final String PANEL_NAME = "menu_manager";
    private static final int PANEL_WIDTH = 448;
    private static final int PANEL_HEIGHT = 264;
    private static final int PADDING = 7;
    private static final int GAP = 4;
    // Fixed sizes: MUI2 does not size rows inside a column that only has a relative size.
    private static final int BODY_HEIGHT = PANEL_HEIGHT - (2 * PADDING) - EditorWidgets.TITLE_HEIGHT - GAP;
    private static final int MENU_LIST_WIDTH = 112;
    private static final int SELECTED_COLUMN_WIDTH = PANEL_WIDTH - (2 * PADDING) - MENU_LIST_WIDTH - PADDING;
    private static final int MENU_ROW_HEIGHT = 16;
    private static final int SECTION_ROW_HEIGHT = 18;
    private static final int ROW_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int NUMBER_WIDTH = 16;
    private static final int ACCENT_WIDTH = 2;
    private static final int WARNING_COLOR = 0xFFFFAA40;
    private static final int WARNING_HEIGHT = 12;
    private static final int COUNT_WIDTH = 14;
    private static final int GROUP_GAP = 6;
    private static final int SWATCH_WIDTH = EditorWidgets.ROW_HEIGHT;
    private static final int SWATCH_INSET = 3;
    // Drawn under each swatch so see-through colors look see-through.
    private static final int SWATCH_BACKDROP = 0xFF8B8B8B;
    private static final int TOGGLE_WIDTH = 22;
    private static final int TOGGLE_HEIGHT = 10;
    private static final int TOGGLE_KNOB = 8;
    private static final int TOGGLE_OFF_FILL = 0xFF3A3D44;
    private static final int TOGGLE_KNOB_COLOR = 0xFFE6E7EA;
    private static final int TEXT_BUTTON_PADDING = 12;
    private static final int CHECKBOX_WIDTH = 14;
    private static final int CHECKBOX_SIZE = 8;
    private static final int CHECKBOX_BORDER = 0xFF6B6F78;
    private static final int ROW_BUTTON_WIDTH = 12;
    private static final int ROW_BUTTON_HOVER = 0xFF3A3D44;
    private static final String SUBMENU_MARK = "↳ ";

    private final MenuStore store = RadialMenuClient.getMenuStore();
    private final RadialMenu selected;
    // Indexes of the ticked sections of the selected menu, in ring order.
    private final Set<Integer> ticked = new TreeSet<>();
    private ModularPanel panel;
    // Deleting takes two clicks so a menu isn't lost to a stray click.
    private boolean deleteArmed;
    // What the last move or copy did, shown at the bottom until something is ticked again.
    private String moveResult = "";

    /**
     * A menu sections can be moved to, or a new menu made for them.
     */
    private static final class MoveTarget {

        @Nullable
        private final RadialMenu menu;
        private final String label;

        private MoveTarget(@Nullable RadialMenu menu, String label) {
            this.menu = menu;
            this.label = label;
        }
    }

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
        String backups = I18n.format("radialmenu.manager.backups");

        panel = new ModularPanel(PANEL_NAME).size(PANEL_WIDTH, PANEL_HEIGHT)
            .padding(PADDING);
        return panel.child(
            Flow.column()
                .sizeRel(1f)
                .childPadding(GAP)
                .child(
                    EditorWidgets.titleBar(
                        IKey.lang("radialmenu.manager.title"),
                        panel::closeIfOpen,
                        new ButtonWidget<>()
                            .size(EditorWidgets.textWidth(backups) + TEXT_BUTTON_PADDING, EditorWidgets.TITLE_HEIGHT)
                            .overlay(IKey.str(backups))
                            .tooltip(tooltip -> tooltip.addLine(I18n.format("radialmenu.manager.backups_tooltip")))
                            .onMousePressed(mouseButton -> {
                                openBackupPicker();
                                return true;
                            })))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(BODY_HEIGHT)
                        .childPadding(PADDING)
                        .child(buildMenuColumn())
                        .child(
                            new Widget<>().width(1)
                                .heightRel(1f)
                                .background(EditorWidgets.box(EditorWidgets.PANEL_BORDER, EditorWidgets.PANEL_BORDER)))
                        .child(buildSelectedColumn())));
    }

    private Flow buildMenuColumn() {
        return Flow.column()
            .width(MENU_LIST_WIDTH - 1 - PADDING)
            .heightRel(1f)
            .childPadding(GAP)
            .child(
                Flow.row()
                    .widthRel(1f)
                    .height(EditorWidgets.ROW_HEIGHT)
                    .child(
                        IKey.lang("radialmenu.manager.menus")
                            .color(EditorWidgets.DETAIL_COLOR)
                            .alignment(Alignment.CenterLeft)
                            .asWidget()
                            .expanded()
                            .heightRel(1f))
                    .child(EditorWidgets.symbolButton("plus", "radialmenu.manager.new", this::createMenu)))
            .child(
                new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                    .showScrollShadows(false)
                    .widthRel(1f)
                    .expanded()
                    .childSeparator(
                        IDrawable.EMPTY.asIcon()
                            .size(ROW_GAP))
                    .children(store.getMenus(), this::createMenuButton));
    }

    /**
     * Lists the backups of menus.json, newest first. Picking one replaces the menus with it; the menus as they were
     * are backed up first, so that can be undone the same way.
     */
    private void openBackupPicker() {
        List<String> backups = store.getBackups();

        IPanelHandler.simple(
            panel,
            (parentPanel, player) -> new SearchListDialog<>(
                "backup_picker",
                I18n.format(backups.isEmpty() ? "radialmenu.manager.no_backups" : "radialmenu.manager.pick_backup"),
                backups,
                null,
                name -> name,
                null,
                name -> {
                    store.restoreBackup(name);
                    open(null);
                }),
            true)
            .openPanel();
    }

    /**
     * A menu in the list: name on the left, section count on the right. The selected menu gets a tinted fill and an
     * accent bar. Menus only reachable as submenus are marked with an arrow.
     */
    private ButtonWidget<?> createMenuButton(RadialMenu menu) {
        return new ButtonWidget<>().widthRelOffset(1f, -EditorWidgets.LIST_ROW_INSET)
            .height(MENU_ROW_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(
                (context, x, y, width, height, widgetTheme) -> GuiDraw
                    .drawRect(x, y, width, height, EditorWidgets.ROW_HOVER_FILL))
            .overlay((context, x, y, width, height, widgetTheme) -> {
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                String name = (menu.isInCycle() ? "" : SUBMENU_MARK) + menu.getName();
                String count = String.valueOf(menu.getSectionCount());
                int countWidth = font.getStringWidth(count);
                float textY = y + ((height - EditorWidgets.FONT_HEIGHT) / 2f);

                if (menu == selected) {
                    GuiDraw.drawRect(x, y, width, height, EditorWidgets.SELECTED_FILL);
                    GuiDraw.drawRect(x, y, ACCENT_WIDTH, height, EditorWidgets.ACCENT_COLOR);
                }
                GuiDraw.drawText(
                    EditorWidgets.fitToWidth(font, name, width - (3 * EditorWidgets.TEXT_INSET) - countWidth),
                    x + EditorWidgets.TEXT_INSET + ACCENT_WIDTH,
                    textY,
                    1f,
                    EditorWidgets.TEXT_COLOR,
                    EditorWidgets.TEXT_SHADOW);
                GuiDraw.drawText(
                    count,
                    x + width - EditorWidgets.TEXT_INSET - countWidth,
                    textY,
                    1f,
                    EditorWidgets.DETAIL_COLOR,
                    EditorWidgets.TEXT_SHADOW);
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
            .childPadding(GAP)
            .collapseDisabledChild()
            .child(buildNameRow())
            .child(buildSettingsRow())
            .child(
                IKey.dynamic(this::getHiddenSectionsWarning)
                    .color(WARNING_COLOR)
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .widthRel(1f)
                    .height(WARNING_HEIGHT)
                    .setEnabledIf(
                        warning -> !selected.getHiddenSectionsWithContent()
                            .isEmpty()))
            .child(
                new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                    .showScrollShadows(false)
                    .widthRel(1f)
                    .expanded()
                    .collapseDisabledChild()
                    .childSeparator(
                        IDrawable.EMPTY.asIcon()
                            .size(ROW_GAP))
                    .children(RadialMenu.MAX_SECTIONS, this::createSectionRow))
            .child(buildSelectionBar());
    }

    /**
     * The menu's name, buttons that move it up and down the list, and a delete button that asks for a second click.
     */
    private Flow buildNameRow() {
        return Flow.row()
            .widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT)
            .childPadding(2)
            .child(
                new TextFieldWidget().autoUpdateOnChange(true)
                    .value(new StringValue.Dynamic(selected::getName, value -> selected.setName(value)))
                    .expanded()
                    .heightRel(1f))
            .child(EditorWidgets.symbolButton("arrow-up", "radialmenu.manager.menu_up", () -> moveSelected(-1)))
            .child(EditorWidgets.symbolButton("arrow-down", "radialmenu.manager.menu_down", () -> moveSelected(1)))
            .child(
                new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                    .overlay(
                        (context, x, y, width, height, widgetTheme) -> EditorWidgets
                            .symbol("trash", deleteArmed ? EditorWidgets.DANGER_COLOR : EditorWidgets.DETAIL_COLOR)
                            .draw(context, x, y, width, height, widgetTheme))
                    .tooltipDynamic(
                        tooltip -> tooltip.addLine(
                            I18n.format(
                                store.getMenus()
                                    .size() <= 1
                                        ? "radialmenu.manager.delete_last"
                                        : deleteArmed ? "radialmenu.manager.delete_confirm"
                                            : "radialmenu.manager.delete")))
                    .onMousePressed(mouseButton -> {
                        deleteSelected();
                        return true;
                    }));
    }

    /**
     * Section count, whether the menu is only a submenu, and the ring colors, side by side.
     */
    private Flow buildSettingsRow() {
        return Flow.row()
            .widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT)
            .childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(EditorWidgets.inlineLabel("radialmenu.manager.sections"))
            .child(
                EditorWidgets
                    .symbolButton("minus", null, () -> selected.setSectionCount(selected.getSectionCount() - 1)))
            .child(
                IKey.dynamic(() -> String.valueOf(selected.getSectionCount()))
                    .alignment(Alignment.Center)
                    .asWidget()
                    .width(COUNT_WIDTH)
                    .heightRel(1f))
            .child(
                EditorWidgets
                    .symbolButton("plus", null, () -> selected.setSectionCount(selected.getSectionCount() + 1)))
            .child(new Widget<>().width(GROUP_GAP))
            .child(EditorWidgets.inlineLabel("radialmenu.manager.submenu_only"))
            .child(createSubmenuOnlyToggle())
            .child(new Widget<>().width(GROUP_GAP))
            .child(EditorWidgets.inlineLabel("radialmenu.manager.colors"))
            .child(createColorSwatch(RingColor.SECTION))
            .child(createColorSwatch(RingColor.HOVER))
            .child(createColorSwatch(RingColor.CENTER))
            .child(
                EditorWidgets.symbolButton(
                    "arrow-counter-clockwise",
                    "radialmenu.manager.reset_colors",
                    MenuManager::resetColors));
    }

    /**
     * An on/off switch for whether the menu only opens from a section in another menu, so the switch key and scroll
     * wheel skip it.
     */
    private ButtonWidget<?> createSubmenuOnlyToggle() {
        return new ButtonWidget<>().size(TOGGLE_WIDTH, EditorWidgets.ROW_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay((context, x, y, width, height, widgetTheme) -> {
                boolean on = !selected.isInCycle();
                int top = y + ((height - TOGGLE_HEIGHT) / 2);
                int knobX = on ? (x + width - TOGGLE_KNOB - 1) : (x + 1);

                GuiDraw.drawRoundedRect(
                    x,
                    top,
                    width,
                    TOGGLE_HEIGHT,
                    on ? EditorWidgets.ACCENT_COLOR : TOGGLE_OFF_FILL,
                    TOGGLE_HEIGHT / 2,
                    6);
                GuiDraw
                    .drawRoundedRect(knobX, top + 1, TOGGLE_KNOB, TOGGLE_KNOB, TOGGLE_KNOB_COLOR, TOGGLE_KNOB / 2, 6);
            })
            .tooltip(tooltip -> tooltip.addLine(I18n.format("radialmenu.manager.submenu_only_tooltip")))
            .onMousePressed(mouseButton -> {
                selected.setInCycle(!selected.isInCycle());
                return true;
            });
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
     * One section: a checkbox, the section itself (clicking it edits it), and arrows that move it around the ring. The
     * checkbox and arrows only show while the mouse is over the row, or the checkbox while it is ticked.
     */
    private Flow createSectionRow(int index) {
        Flow row = Flow.row();
        BooleanSupplier rowHovered = row::isBelowMouse;

        return row.widthRelOffset(1f, -EditorWidgets.LIST_ROW_INSET)
            .height(SECTION_ROW_HEIGHT)
            .background(
                (context, x, y, width, height, widgetTheme) -> GuiDraw.drawRect(
                    x,
                    y,
                    width,
                    height,
                    rowHovered.getAsBoolean() ? EditorWidgets.ROW_HOVER_FILL : EditorWidgets.ROW_FILL))
            .setEnabledIf(shown -> index < selected.getSectionCount())
            .child(createCheckbox(index, rowHovered))
            .child(
                new ButtonWidget<>().expanded()
                    .heightRel(1f)
                    .background(IDrawable.EMPTY)
                    .hoverBackground(IDrawable.EMPTY)
                    .overlay(sectionRowDrawable(index, rowHovered))
                    .onMousePressed(mouseButton -> {
                        store.save();
                        SectionEditor.open(selected, index, true);
                        return true;
                    }))
            .child(createMoveButton(index, -1, "caret-up", rowHovered))
            .child(createMoveButton(index, 1, "caret-down", rowHovered));
    }

    /**
     * A box that ticks the section for moving to another menu.
     */
    private ButtonWidget<?> createCheckbox(int index, BooleanSupplier rowHovered) {
        return new ButtonWidget<>().width(CHECKBOX_WIDTH)
            .heightRel(1f)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay((context, x, y, width, height, widgetTheme) -> {
                int boxX = x + ((width - CHECKBOX_SIZE) / 2);
                int boxY = y + ((height - CHECKBOX_SIZE) / 2);
                boolean isTicked = ticked.contains(index);

                if (!isTicked && !rowHovered.getAsBoolean()) {
                    return;
                }
                GuiDraw.drawRect(
                    boxX,
                    boxY,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    isTicked ? EditorWidgets.ACCENT_COLOR : CHECKBOX_BORDER);
                GuiDraw.drawRect(
                    boxX + 1,
                    boxY + 1,
                    CHECKBOX_SIZE - 2,
                    CHECKBOX_SIZE - 2,
                    isTicked ? EditorWidgets.ACCENT_COLOR : EditorWidgets.FIELD_FILL);
            })
            .onMousePressed(mouseButton -> {
                if (!ticked.remove(index)) {
                    ticked.add(index);
                }
                moveResult = "";
                return true;
            });
    }

    private ButtonWidget<?> createMoveButton(int index, int offset, String symbol, BooleanSupplier rowHovered) {
        IDrawable arrow = EditorWidgets.symbol(symbol, EditorWidgets.DETAIL_COLOR);

        return new ButtonWidget<>().width(ROW_BUTTON_WIDTH)
            .heightRel(1f)
            .background(IDrawable.EMPTY)
            .hoverBackground(
                (context, x, y, width, height, widgetTheme) -> GuiDraw.drawRect(x, y, width, height, ROW_BUTTON_HOVER))
            .overlay((context, x, y, width, height, widgetTheme) -> {
                if (rowHovered.getAsBoolean()) {
                    arrow.draw(context, x, y, width, height, widgetTheme);
                }
            })
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
        if (ticked.contains(index) != ticked.contains(target)) {
            if (!ticked.remove(index)) {
                ticked.add(index);
            }
            if (!ticked.remove(target)) {
                ticked.add(target);
            }
        }
        store.save();
    }

    /**
     * Number, icon and name of a section, with what it does in grey on the right. The right side is left free while
     * the mouse is over the row, since the arrows show there.
     */
    private IDrawable sectionRowDrawable(int index, BooleanSupplier rowHovered) {
        return (context, x, y, width, height, widgetTheme) -> {
            MenuSection section = selected.getSection(index);
            SectionIcon icon = section.getIcon();
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            float textY = y + ((height - EditorWidgets.FONT_HEIGHT) / 2f);
            float iconX = x + NUMBER_WIDTH;
            int textX = Math.round(iconX + ICON_SIZE + EditorWidgets.TEXT_INSET);
            int right = x + width - EditorWidgets.TEXT_INSET;
            String detail = getSectionDetail(section);
            int detailWidth = 0;

            GuiDraw.drawText((index + 1) + ".", x, textY, 1f, EditorWidgets.HINT_COLOR, EditorWidgets.TEXT_SHADOW);
            if (icon != null) {
                icon.draw(iconX + (ICON_SIZE / 2f), y + (height / 2f), ICON_SIZE);
            }
            if (!detail.isEmpty()) {
                detail = EditorWidgets.fitToWidth(font, detail, (right - textX) / 2);
                detailWidth = font.getStringWidth(detail) + EditorWidgets.TEXT_INSET;
                GuiDraw.drawText(
                    detail,
                    right - font.getStringWidth(detail),
                    textY,
                    1f,
                    EditorWidgets.DETAIL_COLOR,
                    EditorWidgets.TEXT_SHADOW);
            }
            GuiDraw.drawText(
                EditorWidgets.fitToWidth(font, getSectionName(section), right - textX - detailWidth),
                textX,
                textY,
                1f,
                section.isEmpty() ? EditorWidgets.HINT_COLOR : EditorWidgets.TEXT_COLOR,
                EditorWidgets.TEXT_SHADOW);
        };
    }

    private static String getSectionName(MenuSection section) {
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

    /**
     * @return what the section does, shown beside its name, or nothing if its name already says it
     */
    private static String getSectionDetail(MenuSection section) {
        SectionAction action = section.getAction();

        if ((action == null) || section.getLabel()
            .isEmpty()) {
            return "";
        }
        return action.describe();
    }

    /**
     * The bottom bar: how many sections are ticked with buttons to move them, or a hint when none are.
     */
    private Flow buildSelectionBar() {
        Flow bar = Flow.row();
        String clear = I18n.format("radialmenu.manager.clear_ticks");

        return bar.widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT + 4)
            .padding(2)
            .childPadding(GAP)
            .collapseDisabledChild()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .background((context, x, y, width, height, widgetTheme) -> {
                if (!getTickedSections().isEmpty()) {
                    GuiDraw.drawRect(x, y, width, height, EditorWidgets.SELECTED_FILL);
                }
            })
            .child(
                IKey.dynamic(() -> I18n.format("radialmenu.manager.ticked", getTickedSections().size()))
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .width(EditorWidgets.textWidth(I18n.format("radialmenu.manager.ticked", RadialMenu.MAX_SECTIONS)))
                    .heightRel(1f)
                    .setEnabledIf(text -> !getTickedSections().isEmpty()))
            .child(
                EditorWidgets.accentButton("radialmenu.manager.move_to", this::openMoveTargetPicker)
                    .width(EditorWidgets.textWidth(I18n.format("radialmenu.manager.move_to")) + TEXT_BUTTON_PADDING)
                    .overlay(
                        IKey.dynamic(
                            () -> I18n.format(
                                Interactable.hasShiftDown() ? "radialmenu.manager.copy_to"
                                    : "radialmenu.manager.move_to")))
                    .setEnabledIf(button -> !getTickedSections().isEmpty()))
            .child(
                new ButtonWidget<>()
                    .size(EditorWidgets.textWidth(clear) + TEXT_BUTTON_PADDING, EditorWidgets.ROW_HEIGHT)
                    .overlay(IKey.str(clear))
                    .onMousePressed(mouseButton -> {
                        ticked.clear();
                        return true;
                    })
                    .setEnabledIf(button -> !getTickedSections().isEmpty()))
            .child(
                IKey.dynamic(this::getSelectionHint)
                    .color(EditorWidgets.HINT_COLOR)
                    .alignment(Alignment.CenterRight)
                    .asWidget()
                    .expanded()
                    .heightRel(1f));
    }

    private String getSelectionHint() {
        if (!getTickedSections().isEmpty()) {
            return I18n.format("radialmenu.manager.shift_copies");
        }
        return moveResult.isEmpty() ? I18n.format("radialmenu.manager.tick_hint") : moveResult;
    }

    /**
     * @return the ticked sections that are shown and not empty, in ring order
     */
    private List<Integer> getTickedSections() {
        List<Integer> sections = new ArrayList<>();

        for (int index : ticked) {
            if ((index < selected.getSectionCount()) && !selected.getSection(index)
                .isEmpty()) {
                sections.add(index);
            }
        }
        return sections;
    }

    private void openMoveTargetPicker() {
        List<MoveTarget> targets = new ArrayList<>();

        targets.add(new MoveTarget(null, I18n.format("radialmenu.manager.new_menu_target")));
        for (RadialMenu menu : store.getMenus()) {
            if (menu != selected) {
                targets.add(new MoveTarget(menu, menu.getName()));
            }
        }
        IPanelHandler
            .simple(
                panel,
                (parentPanel, player) -> new SearchListDialog<>(
                    "move_target_picker",
                    I18n.format("radialmenu.manager.pick_move_target"),
                    targets,
                    null,
                    target -> target.label,
                    null,
                    target -> moveTickedSections(target.menu)),
                true)
            .openPanel();
    }

    /**
     * Puts the ticked sections into the first empty slots of the target, or of a new submenu if the target is null.
     * Holding Shift copies them; otherwise they are emptied here. Sections that don't fit stay where they are.
     */
    private void moveTickedSections(@Nullable RadialMenu target) {
        List<Integer> indexes = getTickedSections();
        List<MenuSection> sections = new ArrayList<>();
        boolean copy = Interactable.hasShiftDown();
        RadialMenu destination = target != null ? target
            : store.createMenu(I18n.format("radialmenu.editor.default_submenu_name"), false);
        int added;

        for (int index : indexes) {
            sections.add(selected.getSection(index));
        }
        added = destination.addSections(sections);
        if (!copy) {
            for (int index : indexes.subList(0, added)) {
                selected.setSection(index, MenuSection.EMPTY);
            }
        }
        ticked.clear();
        store.save();
        moveResult = I18n.format(
            added < sections.size() ? "radialmenu.manager.moved_partly"
                : copy ? "radialmenu.manager.copied" : "radialmenu.manager.moved",
            added,
            destination.getName(),
            sections.size());
        if (target == null) {
            // The menu list on the left is built once, so it is rebuilt to show the new menu.
            open(selected);
        }
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
        if (store.getMenus()
            .size() <= 1) {
            return;
        }
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
