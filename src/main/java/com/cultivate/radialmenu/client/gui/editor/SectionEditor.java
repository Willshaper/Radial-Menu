package com.cultivate.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RecipeViewerSettingsImpl;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.client.ClientTickHandler;
import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.menu.MenuSection;
import com.cultivate.radialmenu.menu.MenuStore;
import com.cultivate.radialmenu.menu.RadialMenu;
import com.cultivate.radialmenu.menu.action.ActionType;
import com.cultivate.radialmenu.menu.action.ChatAction;
import com.cultivate.radialmenu.menu.action.CommandAction;
import com.cultivate.radialmenu.menu.action.HotbarAction;
import com.cultivate.radialmenu.menu.action.KeybindAction;
import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.action.SubmenuAction;
import com.cultivate.radialmenu.menu.icon.ItemIcon;
import com.cultivate.radialmenu.menu.icon.SectionIcon;
import com.cultivate.radialmenu.menu.icon.TextIcon;

/**
 * Edits one section: its name, icon and action.
 *
 * <p>
 * The screen opens as a client-only container so NEI shows its item list next to it; item icons are set by dragging an
 * item from NEI onto the icon slot.
 */
public final class SectionEditor {

    private enum IconKind {
        NONE,
        ITEM,
        TEXT
    }

    private static final String PANEL_NAME = "section_editor";
    private static final int PANEL_WIDTH = 260;
    private static final int PADDING = 7;
    // Room for a few characters plus Angelica's color codes, such as &#FF8800TP.
    private static final int TEXT_ICON_MAX_LENGTH = 32;
    private static final int COLOR_SWATCH_INSET = 3;

    private final RadialMenu menu;
    private final int index;
    private final boolean returnToManager;
    private final MenuStore store = RadialMenuClient.getMenuStore();
    private ModularPanel panel;
    private boolean clearArmed;

    private String label;
    private IconKind iconKind = IconKind.NONE;
    @Nullable
    private ItemIcon itemIcon;
    private String iconText = "";
    private int iconColor = TextIcon.DEFAULT_COLOR;

    private ActionType actionType = ActionType.NONE;
    private String command = "";
    private String chatMessage = "";
    private String keyDescription = "";
    private KeybindAction.Mode keyMode = KeybindAction.Mode.TAP;
    private int hotbarSlot = HotbarAction.FIRST_SLOT;
    @Nullable
    private String submenuId;

    private SectionEditor(RadialMenu menu, int index, boolean returnToManager) {
        MenuSection section = menu.getSection(index);

        this.menu = menu;
        this.index = index;
        this.returnToManager = returnToManager;
        this.label = section.getLabel();
        loadIcon(section.getIcon());
        loadAction(section.getAction());
    }

    public static void open(RadialMenu menu, int index) {
        open(menu, index, false);
    }

    /**
     * @param returnToManager reopen the menu manager when done, for sections edited from there
     */
    public static void open(RadialMenu menu, int index, boolean returnToManager) {
        ClientTickHandler.openScreenNextTick(() -> {
            SectionEditor editor = new SectionEditor(menu, index, returnToManager);
            RecipeViewerSettingsImpl recipeViewer = new RecipeViewerSettingsImpl();
            UISettings settings;

            // Client-only screens hide the recipe viewer by default, and NEI only draws on container screens.
            recipeViewer.enable();
            settings = new UISettings(recipeViewer);
            settings.customContainer(ModularContainer::new);
            ClientGUI.open(new ModularScreen(RadialMenuMod.MODID, editor.buildPanel()), settings);
        });
    }

    private void loadIcon(@Nullable SectionIcon icon) {
        if (icon instanceof ItemIcon loadedItemIcon) {
            iconKind = IconKind.ITEM;
            itemIcon = loadedItemIcon;
        } else if (icon instanceof TextIcon textIcon) {
            iconKind = IconKind.TEXT;
            iconText = textIcon.getText();
            iconColor = textIcon.getColor();
        }
    }

    private void loadAction(@Nullable SectionAction action) {
        if (action == null) {
            return;
        }
        actionType = action.getType();
        switch (actionType) {
            case COMMAND -> command = ((CommandAction) action).getCommand();
            case CHAT -> chatMessage = ((ChatAction) action).getMessage();
            case KEYBIND -> {
                keyDescription = ((KeybindAction) action).getKeyDescription();
                keyMode = ((KeybindAction) action).getMode();
            }
            case HOTBAR -> hotbarSlot = ((HotbarAction) action).getSlot();
            case SUBMENU -> submenuId = ((SubmenuAction) action).getMenuId();
            default -> {}
        }
    }

    private ModularPanel buildPanel() {
        panel = new ModularPanel(PANEL_NAME).width(PANEL_WIDTH)
            .coverChildrenHeight()
            .padding(PADDING);
        panel.child(
            Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4)
                .collapseDisabledChild()
                .child(
                    IKey.str(I18n.format("radialmenu.editor.section_title", index + 1, menu.getName()))
                        .asWidget()
                        .widthRel(1f)
                        .height(12))
                .child(
                    EditorWidgets.labeledRow(
                        "radialmenu.editor.name",
                        new TextFieldWidget().autoUpdateOnChange(true)
                            .value(new StringValue.Dynamic(() -> label, value -> label = value))
                            .expanded()
                            .height(EditorWidgets.ROW_HEIGHT)))
                .child(buildIconRows())
                .child(buildActionRows())
                .child(buildButtonRow()));
        return panel;
    }

    private Flow buildIconRows() {
        IPanelHandler colorPicker = IPanelHandler.simple(
            panel,
            (parentPanel,
                player) -> new ColorPickerDialog(color -> iconColor = color & 0xFFFFFF, 0xFF000000 | iconColor, false)
                    .relative(parentPanel)
                    .top(0)
                    .leftRel(1f),
            true);

        return Flow.column()
            .widthRel(1f)
            .coverChildrenHeight()
            .childPadding(4)
            .collapseDisabledChild()
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.icon",
                    EditorWidgets.cycleButton(
                        () -> I18n.format(
                            "radialmenu.editor.icon." + iconKind.name()
                                .toLowerCase()),
                        offset -> iconKind = EditorWidgets.cycle(iconKind, offset))
                        .expanded()))
            .child(
                EditorWidgets
                    .labeledRow(
                        "radialmenu.editor.item",
                        new ItemDropSlot(this::getIconStack, this::setIconStack),
                        EditorWidgets.button("radialmenu.editor.use_held", this::useHeldItem)
                            .width(70),
                        IKey.lang("radialmenu.editor.drag_hint")
                            .color(EditorWidgets.HINT_COLOR)
                            .alignment(Alignment.CenterLeft)
                            .asWidget()
                            .expanded()
                            .heightRel(1f))
                    .setEnabledIf(row -> iconKind == IconKind.ITEM))
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.text",
                    new TextFieldWidget().autoUpdateOnChange(true)
                        .value(new StringValue.Dynamic(() -> iconText, value -> iconText = value))
                        .setMaxLength(TEXT_ICON_MAX_LENGTH)
                        .expanded()
                        .height(EditorWidgets.ROW_HEIGHT),
                    new ButtonWidget<>().size(EditorWidgets.ROW_HEIGHT)
                        .overlay(
                            (context, x, y, width, height, widgetTheme) -> GuiDraw.drawRect(
                                x + COLOR_SWATCH_INSET,
                                y + COLOR_SWATCH_INSET,
                                width - (2 * COLOR_SWATCH_INSET),
                                height - (2 * COLOR_SWATCH_INSET),
                                0xFF000000 | iconColor))
                        .onMousePressed(mouseButton -> {
                            colorPicker.openPanel();
                            return true;
                        }))
                    .setEnabledIf(row -> iconKind == IconKind.TEXT));
    }

    @Nullable
    private ItemStack getIconStack() {
        return itemIcon == null ? null : itemIcon.getStack();
    }

    private void setIconStack(@Nullable ItemStack stack) {
        itemIcon = stack == null ? null : new ItemIcon(stack);
    }

    private void useHeldItem() {
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();

        if (held != null) {
            setIconStack(held);
        }
    }

    private Flow buildActionRows() {
        return Flow.column()
            .widthRel(1f)
            .coverChildrenHeight()
            .childPadding(4)
            .collapseDisabledChild()
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.action",
                    EditorWidgets
                        .cycleButton(
                            () -> actionType.getDisplayName(),
                            offset -> actionType = EditorWidgets.cycle(actionType, offset))
                        .expanded()))
            .child(
                actionRow(
                    ActionType.COMMAND,
                    "radialmenu.editor.command",
                    new TextFieldWidget().autoUpdateOnChange(true)
                        .value(new StringValue.Dynamic(() -> command, value -> command = value))
                        .hintText("/home")
                        .expanded()
                        .height(EditorWidgets.ROW_HEIGHT)))
            .child(
                actionRow(
                    ActionType.CHAT,
                    "radialmenu.editor.message",
                    new TextFieldWidget().autoUpdateOnChange(true)
                        .value(new StringValue.Dynamic(() -> chatMessage, value -> chatMessage = value))
                        .expanded()
                        .height(EditorWidgets.ROW_HEIGHT)))
            .child(
                actionRow(
                    ActionType.KEYBIND,
                    "radialmenu.editor.keybind",
                    new ButtonWidget<>().expanded()
                        .height(EditorWidgets.ROW_HEIGHT)
                        .overlay(IKey.dynamic(this::getKeybindButtonText))
                        .onMousePressed(mouseButton -> {
                            openKeybindPicker();
                            return true;
                        }),
                    EditorWidgets
                        .cycleButton(
                            () -> keyMode.getDisplayName(),
                            offset -> keyMode = EditorWidgets.cycle(keyMode, offset))
                        .width(50)))
            .child(
                actionRow(
                    ActionType.HOTBAR,
                    "radialmenu.editor.slot",
                    EditorWidgets
                        .cycleButton(
                            () -> String.valueOf(hotbarSlot),
                            offset -> hotbarSlot = HotbarAction.FIRST_SLOT + Math
                                .floorMod((hotbarSlot - HotbarAction.FIRST_SLOT) + offset, HotbarAction.LAST_SLOT))
                        .expanded()))
            .child(
                actionRow(
                    ActionType.SUBMENU,
                    "radialmenu.editor.submenu",
                    new ButtonWidget<>().expanded()
                        .height(EditorWidgets.ROW_HEIGHT)
                        .overlay(IKey.dynamic(this::getSubmenuButtonText))
                        .onMousePressed(mouseButton -> {
                            openSubmenuPicker();
                            return true;
                        }),
                    EditorWidgets.button("radialmenu.editor.new_submenu", this::createSubmenu)
                        .width(40)));
    }

    private Flow actionRow(ActionType type, String labelKey, IWidget... widgets) {
        return EditorWidgets.labeledRow(labelKey, widgets)
            .setEnabledIf(row -> actionType == type);
    }

    private String getKeybindButtonText() {
        KeyBinding binding = KeybindAction.findBinding(keyDescription);

        if (keyDescription.isEmpty()) {
            return I18n.format("radialmenu.editor.choose");
        }
        if (binding == null) {
            return I18n.format("radialmenu.editor.missing", keyDescription);
        }
        return I18n.format(keyDescription);
    }

    private void openKeybindPicker() {
        List<KeyBinding> bindings = new ArrayList<>(Arrays.asList(Minecraft.getMinecraft().gameSettings.keyBindings));

        bindings.sort(
            Comparator.comparing((KeyBinding binding) -> I18n.format(binding.getKeyCategory()))
                .thenComparing(binding -> I18n.format(binding.getKeyDescription())));
        IPanelHandler
            .simple(
                panel,
                (parentPanel, player) -> new SearchListDialog<>(
                    "keybind_picker",
                    I18n.format("radialmenu.editor.pick_keybind"),
                    bindings,
                    binding -> I18n.format(binding.getKeyCategory()),
                    binding -> I18n.format(binding.getKeyDescription()),
                    binding -> GameSettings.getKeyDisplayString(binding.getKeyCode()),
                    binding -> keyDescription = binding.getKeyDescription()),
                true)
            .openPanel();
    }

    private String getSubmenuButtonText() {
        RadialMenu submenu = submenuId == null ? null : store.getMenu(submenuId);

        if (submenuId == null) {
            return I18n.format("radialmenu.editor.choose");
        }
        if (submenu == null) {
            return I18n.format("radialmenu.editor.missing", submenuId);
        }
        return submenu.getName();
    }

    private void openSubmenuPicker() {
        List<RadialMenu> menus = new ArrayList<>(store.getMenus());

        menus.remove(menu);
        IPanelHandler
            .simple(
                panel,
                (parentPanel, player) -> new SearchListDialog<>(
                    "submenu_picker",
                    I18n.format("radialmenu.editor.pick_submenu"),
                    menus,
                    null,
                    RadialMenu::getName,
                    null,
                    picked -> submenuId = picked.getId()),
                true)
            .openPanel();
    }

    private void createSubmenu() {
        String name = label.trim()
            .isEmpty() ? I18n.format("radialmenu.editor.default_submenu_name") : label.trim();

        submenuId = store.createMenu(name, false)
            .getId();
        store.save();
    }

    private Flow buildButtonRow() {
        return Flow.row()
            .widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT)
            .childPadding(4)
            .child(
                EditorWidgets.button("radialmenu.editor.save", this::save)
                    .expanded())
            .child(
                new ButtonWidget<>().height(EditorWidgets.ROW_HEIGHT)
                    .overlay(
                        IKey.dynamic(
                            () -> I18n
                                .format(clearArmed ? "radialmenu.editor.clear_confirm" : "radialmenu.editor.clear")))
                    .onMousePressed(mouseButton -> {
                        clear();
                        return true;
                    })
                    .expanded())
            .child(
                EditorWidgets.button("radialmenu.editor.cancel", this::finish)
                    .expanded());
    }

    private void save() {
        menu.setSection(index, new MenuSection(label.trim(), createIcon(), createAction()));
        store.save();
        finish();
    }

    /**
     * Empties the section on the second click; the first only arms the button, so a stray click loses nothing.
     */
    private void clear() {
        if (!clearArmed) {
            clearArmed = true;
            return;
        }
        menu.setSection(index, MenuSection.EMPTY);
        store.save();
        finish();
    }

    private void finish() {
        if (returnToManager) {
            MenuManager.open(menu);
        } else {
            panel.closeIfOpen();
        }
    }

    @Nullable
    private SectionIcon createIcon() {
        return switch (iconKind) {
            case ITEM -> itemIcon;
            case TEXT -> iconText.trim()
                .isEmpty() ? null : new TextIcon(iconText.trim(), iconColor);
            case NONE -> null;
        };
    }

    @Nullable
    private SectionAction createAction() {
        return switch (actionType) {
            case COMMAND -> command.trim()
                .isEmpty() ? null : new CommandAction(command.trim());
            case CHAT -> chatMessage.isEmpty() ? null : new ChatAction(chatMessage);
            case KEYBIND -> keyDescription.isEmpty() ? null : new KeybindAction(keyDescription, keyMode);
            case HOTBAR -> new HotbarAction(hotbarSlot);
            case SUBMENU -> submenuId == null ? null : new SubmenuAction(submenuId);
            case NONE -> null;
        };
    }
}
