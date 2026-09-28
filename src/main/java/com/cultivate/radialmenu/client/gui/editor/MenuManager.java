package com.cultivate.radialmenu.client.gui.editor;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
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
 * Lists every menu and edits the selected one: name, section count, whether the switch key reaches it, and its
 * sections.
 *
 * <p>
 * Adding, removing or reordering menus reopens the screen so the menu list is rebuilt.
 */
public final class MenuManager {

    private static final String PANEL_NAME = "menu_manager";
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 250;
    private static final int PADDING = 7;
    private static final int MENU_LIST_WIDTH = 110;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int ICON_SIZE = 16;
    private static final int NUMBER_WIDTH = 18;
    private static final int FONT_HEIGHT = 8;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_COLOR = 0x603C78D8;
    private static final int SWATCH_WIDTH = 30;
    private static final int SWATCH_INSET = 3;
    // Drawn under each swatch so see-through colors look see-through.
    private static final int SWATCH_BACKDROP = 0xFF8B8B8B;

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
                new ListWidget<>().widthRel(1f)
                    .expanded()
                    .children(menus, this::createMenuButton))
            .child(
                Flow.row()
                    .widthRel(1f)
                    .height(EditorWidgets.ROW_HEIGHT)
                    .childPadding(2)
                    .child(
                        EditorWidgets.button("radialmenu.manager.new", this::createMenu)
                            .expanded())
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
                    .setEnabledIf(
                        button -> store.getMenus()
                            .size() > 1)
                    .onMousePressed(mouseButton -> {
                        deleteSelected();
                        return true;
                    }));
    }

    private ButtonWidget<?> createMenuButton(RadialMenu menu) {
        ButtonWidget<?> button = new ButtonWidget<>().widthRel(1f)
            .height(LIST_ROW_HEIGHT)
            .overlay(
                IKey.dynamic(menu::getName)
                    .alignment(Alignment.CenterLeft))
            .onMousePressed(mouseButton -> {
                if (menu != selected) {
                    open(menu);
                }
                return true;
            });

        if (menu == selected) {
            button.background(
                (context, x, y, width, height, widgetTheme) -> GuiDraw.drawRect(x, y, width, height, SELECTED_COLOR));
        }
        return button;
    }

    private Flow buildSelectedColumn() {
        return Flow.column()
            .expanded()
            .heightRel(1f)
            .childPadding(3)
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.name",
                    new TextFieldWidget().autoUpdateOnChange(true)
                        .value(new StringValue.Dynamic(selected::getName, value -> selected.setName(value)))
                        .expanded()
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
                        .expanded()))
            .child(
                new ListWidget<>().widthRel(1f)
                    .expanded()
                    .collapseDisabledChild()
                    .children(RadialMenu.MAX_SECTIONS, this::createSectionButton));
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

    private ButtonWidget<?> createSectionButton(int index) {
        return new ButtonWidget<>().widthRel(1f)
            .height(LIST_ROW_HEIGHT)
            .overlay(sectionRowDrawable(index))
            .setEnabledIf(button -> index < selected.getSectionCount())
            .onMousePressed(mouseButton -> {
                store.save();
                SectionEditor.open(selected, index, true);
                return true;
            });
    }

    private IDrawable sectionRowDrawable(int index) {
        return (context, x, y, width, height, widgetTheme) -> {
            MenuSection section = selected.getSection(index);
            SectionIcon icon = section.getIcon();
            float textY = y + ((height - FONT_HEIGHT) / 2f);
            float iconX = x + NUMBER_WIDTH;

            GuiDraw.drawText((index + 1) + ".", x + 3, textY, 1f, TEXT_COLOR, true);
            if (icon != null) {
                icon.draw(iconX + (ICON_SIZE / 2f), y + (height / 2f), ICON_SIZE);
            }
            GuiDraw.drawText(describeSection(section), iconX + ICON_SIZE + 4, textY, 1f, TEXT_COLOR, true);
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
