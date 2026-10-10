package com.cultivate.radialmenu.client.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Util;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.AbstractScrollWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.menu.icon.EffectIcon;
import com.cultivate.radialmenu.menu.icon.HeadIcon;
import com.cultivate.radialmenu.menu.icon.ImageIcon;
import com.cultivate.radialmenu.menu.icon.ItemIcon;
import com.cultivate.radialmenu.menu.icon.SectionIcon;
import com.cultivate.radialmenu.menu.icon.SymbolIcon;
import com.cultivate.radialmenu.menu.icon.SymbolSheet;
import com.cultivate.radialmenu.menu.icon.TextIcon;

/**
 * Chooses an icon of any type: one tab per type, a search box, and a grid of icons. Clicking an icon picks it and
 * closes the dialog.
 *
 * <p>
 * The color swatch applies to symbols and text. Items can also be dragged in from NEI onto the slot in the Items tab.
 */
final class IconPicker extends Dialog<SectionIcon> {

    /**
     * The picker's tabs, one per icon type.
     */
    enum Tab {

        ITEMS("cube"),
        SYMBOLS("star"),
        EFFECTS("flask"),
        IMAGES("image"),
        HEADS("user"),
        TEXT("text-t");

        // The bundled symbol shown on the tab.
        private final String symbol;

        Tab(String symbol) {
            this.symbol = symbol;
        }

        String getDisplayName() {
            return I18n.format("radialmenu.picker.tab." + name().toLowerCase(Locale.ROOT));
        }

        /**
         * @return the tab for this kind of icon, or Items for none
         */
        static Tab of(@Nullable SectionIcon icon) {
            if (icon instanceof SymbolIcon) {
                return SYMBOLS;
            }
            if (icon instanceof EffectIcon) {
                return EFFECTS;
            }
            if (icon instanceof ImageIcon) {
                return IMAGES;
            }
            if (icon instanceof HeadIcon) {
                return HEADS;
            }
            if (icon instanceof TextIcon) {
                return TEXT;
            }
            return ITEMS;
        }
    }

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private static final int PADDING = 7;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_SYMBOL_SIZE = 9;
    private static final int TAB_SYMBOL_GAP = 3;
    private static final int SEARCH_HEIGHT = 14;
    private static final int SWATCH_WIDTH = 28;
    private static final int SWATCH_INSET = 3;
    private static final int BUTTON_WIDTH = 70;
    private static final int TAB_ACCENT_HEIGHT = 2;
    // Room for a few characters plus Angelica's color codes, such as &#FF8800TP.
    private static final int TEXT_ICON_MAX_LENGTH = 32;
    // Minecraft account names: letters, digits and underscores, at most 16 long.
    private static final Pattern PLAYER_NAME = Pattern.compile("\\w{1,16}");
    private static final String VANILLA_MOD_ID = "minecraft";

    // Every item and its variants, built the first time the Items tab is shown, since it takes a moment in big packs.
    @Nullable
    private static List<Choice> itemChoices;

    private final StringValue search = new StringValue("");
    private final IconGrid grid = new IconGrid();
    private Tab tab;
    private int color;
    private String text;
    @Nullable
    private List<Choice> tabChoices;
    @Nullable
    private Tab tabChoicesTab;
    // The search the Heads choices were built for, since a typed name is offered as a choice of its own.
    private String tabChoicesSearch = "";

    /**
     * One icon to choose from: how it looks in the grid, what to call it, and the icon picking it gives.
     */
    private static final class Choice {

        private final String name;
        private final String searchText;
        private final SectionIcon preview;
        private final Supplier<SectionIcon> result;

        /**
         * @param searchText extra words the icon is found by, besides its name
         */
        private Choice(String name, String searchText, SectionIcon preview, Supplier<SectionIcon> result) {
            this.name = name;
            this.searchText = (name + " " + searchText).toLowerCase(Locale.ROOT);
            this.preview = preview;
            this.result = result;
        }
    }

    /**
     * @param current the section's icon now, which picks the starting tab, color and text
     */
    IconPicker(@Nullable SectionIcon current, Consumer<SectionIcon> onPick) {
        super("icon_picker", onPick);
        this.tab = Tab.of(current);
        this.color = current instanceof SymbolIcon symbolIcon ? symbolIcon.getColor()
            : current instanceof TextIcon textIcon ? textIcon.getColor() : TextIcon.DEFAULT_COLOR;
        this.text = current instanceof TextIcon textIcon ? textIcon.getText() : "";
        size(WIDTH, HEIGHT);
        padding(PADDING);
        // Not closed by clicks outside it: dragging an item from NEI starts with one.
        child(
            Flow.column()
                .sizeRel(1f)
                .childPadding(3)
                .collapseDisabledChild()
                .child(EditorWidgets.titleBar(IKey.lang("radialmenu.picker.title"), this::closeIfOpen))
                .child(buildTabRow())
                .child(buildSearchRow())
                .child(buildItemRow())
                .child(buildImageRow())
                .child(
                    // Always shown, so its expanded height can be worked out; the grid and the Text tab take turns
                    // inside it.
                    Flow.column()
                        .widthRel(1f)
                        .expanded()
                        .collapseDisabledChild()
                        .child(
                            new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                                .showScrollShadows(false)
                                .sizeRel(1f)
                                .padding(2)
                                .background(EditorWidgets.wellBackground())
                                .child(grid)
                                .setEnabledIf(list -> tab != Tab.TEXT))
                        .child(buildTextTab()))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(EditorWidgets.ROW_HEIGHT)
                        .childPadding(4)
                        .child(
                            IKey.dynamic(grid::getHoveredName)
                                .color(EditorWidgets.DETAIL_COLOR)
                                .alignment(Alignment.CenterLeft)
                                .asWidget()
                                .expanded()
                                .heightRel(1f))));
    }

    /**
     * The tabs, as a strip of segments in a dark well: a symbol and the type's name, which is left out if it doesn't
     * fit. The current tab is tinted and underlined.
     */
    private Flow buildTabRow() {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(TAB_HEIGHT)
            .padding(1)
            .childPadding(1)
            .background(EditorWidgets.wellBackground());

        for (Tab rowTab : Tab.values()) {
            row.child(
                new ButtonWidget<>().expanded()
                    .heightRel(1f)
                    .background(IDrawable.EMPTY)
                    .hoverBackground(
                        (context, x, y, width, height, widgetTheme) -> GuiDraw
                            .drawRect(x, y, width, height, EditorWidgets.ROW_HOVER_FILL))
                    .overlay((context, x, y, width, height, widgetTheme) -> drawTab(rowTab, x, y, width, height))
                    .tooltip(tooltip -> tooltip.addLine(rowTab.getDisplayName()))
                    .onMousePressed(mouseButton -> {
                        tab = rowTab;
                        return true;
                    }));
        }
        return row;
    }

    private void drawTab(Tab drawn, int x, int y, int width, int height) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String name = drawn.getDisplayName();
        boolean current = drawn == tab;
        int color = current ? EditorWidgets.TEXT_COLOR : EditorWidgets.DETAIL_COLOR;
        int contentWidth = TAB_SYMBOL_SIZE + TAB_SYMBOL_GAP + font.getStringWidth(name);
        boolean showName = contentWidth <= (width - 4);
        float left = x + ((width - (showName ? contentWidth : TAB_SYMBOL_SIZE)) / 2f);

        if (current) {
            GuiDraw.drawRect(x, y, width, height, EditorWidgets.SELECTED_FILL);
            GuiDraw.drawRect(x, y + height - TAB_ACCENT_HEIGHT, width, TAB_ACCENT_HEIGHT, EditorWidgets.ACCENT_COLOR);
        }
        new SymbolIcon(drawn.symbol, color).draw(left + (TAB_SYMBOL_SIZE / 2f), y + (height / 2f), TAB_SYMBOL_SIZE);
        if (showName) {
            GuiDraw.drawText(
                name,
                left + TAB_SYMBOL_SIZE + TAB_SYMBOL_GAP,
                y + ((height - EditorWidgets.FONT_HEIGHT) / 2f),
                1f,
                color,
                EditorWidgets.TEXT_SHADOW);
        }
    }

    private Flow buildSearchRow() {
        return Flow.row()
            .widthRel(1f)
            .height(SEARCH_HEIGHT)
            .childPadding(4)
            .collapseDisabledChild()
            .setEnabledIf(row -> tab != Tab.TEXT)
            .child(
                new TextFieldWidget().value(search)
                    .autoUpdateOnChange(true)
                    .hintText(I18n.format("radialmenu.editor.search"))
                    .expanded()
                    .heightRel(1f))
            .child(
                createColorSwatch().setEnabledIf(swatch -> tab == Tab.SYMBOLS)
                    .heightRel(1f));
    }

    private ButtonWidget<?> createColorSwatch() {
        IPanelHandler colorPicker = IPanelHandler.simple(
            this,
            (parentPanel,
                player) -> new ColorPickerDialog(picked -> color = picked & 0xFFFFFF, 0xFF000000 | color, false)
                    .relative(parentPanel)
                    .top(0)
                    .leftRel(1f),
            true);

        return new ButtonWidget<>().width(SWATCH_WIDTH)
            .overlay(
                (context, x, y, width, height, widgetTheme) -> GuiDraw.drawRect(
                    x + SWATCH_INSET,
                    y + SWATCH_INSET,
                    width - (2 * SWATCH_INSET),
                    height - (2 * SWATCH_INSET),
                    0xFF000000 | color))
            .tooltip(tooltip -> tooltip.addLine(I18n.format("radialmenu.picker.color")))
            .onMousePressed(mouseButton -> {
                colorPicker.openPanel();
                return true;
            });
    }

    /**
     * The Items tab's slot for dragging an item in from NEI, and a button for the held item.
     */
    private Flow buildItemRow() {
        return Flow.row()
            .widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT + 2)
            .childPadding(4)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .setEnabledIf(row -> tab == Tab.ITEMS)
            .child(new IconSlot(() -> null, stack -> closeWith(new ItemIcon(stack)), null))
            .child(
                EditorWidgets.button("radialmenu.editor.use_held", this::pickHeldItem)
                    .width(BUTTON_WIDTH))
            .child(
                IKey.lang("radialmenu.picker.drag_hint")
                    .color(EditorWidgets.HINT_COLOR)
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .expanded()
                    .heightRel(1f));
    }

    private void pickHeldItem() {
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();

        if (held != null) {
            ItemStack copy = held.copy();

            copy.stackSize = 1;
            closeWith(new ItemIcon(copy));
        }
    }

    private Flow buildImageRow() {
        return Flow.row()
            .widthRel(1f)
            .height(EditorWidgets.ROW_HEIGHT)
            .childPadding(4)
            .setEnabledIf(row -> tab == Tab.IMAGES)
            .child(
                EditorWidgets.button("radialmenu.picker.open_folder", IconPicker::openImageFolder)
                    .width(BUTTON_WIDTH))
            .child(EditorWidgets.button("radialmenu.picker.reload", () -> {
                ImageIcon.reloadAll();
                tabChoicesTab = null;
            })
                .width(BUTTON_WIDTH))
            .child(
                IKey.lang("radialmenu.picker.image_hint")
                    .color(EditorWidgets.HINT_COLOR)
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .expanded()
                    .heightRel(1f));
    }

    /**
     * Opens the image folder in the system's file manager.
     */
    private static void openImageFolder() {
        File folder = ImageIcon.getDirectory();
        String path;
        String[] command;

        if (folder == null) {
            return;
        }
        path = folder.getAbsolutePath();
        command = switch (Util.getOSType()) {
            case WINDOWS -> new String[] { "explorer", path };
            case OSX -> new String[] { "open", path };
            default -> new String[] { "xdg-open", path };
        };
        try {
            Runtime.getRuntime()
                .exec(command);
        } catch (IOException e) {
            RadialMenuMod.LOG.warn("could not open the icon folder {}", path, e);
        }
    }

    /**
     * The Text tab: the text, its color, a preview, and a button to use it.
     */
    private Flow buildTextTab() {
        return Flow.column()
            .widthRel(1f)
            .coverChildrenHeight()
            .childPadding(4)
            .setEnabledIf(column -> tab == Tab.TEXT)
            .child(
                EditorWidgets.labeledRow(
                    "radialmenu.editor.text",
                    new TextFieldWidget().autoUpdateOnChange(true)
                        .value(new StringValue.Dynamic(() -> text, value -> text = value))
                        .setMaxLength(TEXT_ICON_MAX_LENGTH)
                        .expanded()
                        .height(EditorWidgets.ROW_HEIGHT),
                    createColorSwatch().height(EditorWidgets.ROW_HEIGHT),
                    new IconSlot(this::createTextIcon, stack -> closeWith(new ItemIcon(stack)), null))
                    .height(IconSlot.SIZE))
            .child(
                Flow.row()
                    .widthRel(1f)
                    .height(EditorWidgets.ROW_HEIGHT)
                    .childPadding(4)
                    .child(
                        IKey.lang("radialmenu.picker.text_hint")
                            .color(EditorWidgets.HINT_COLOR)
                            .alignment(Alignment.CenterLeft)
                            .asWidget()
                            .expanded()
                            .heightRel(1f))
                    .child(EditorWidgets.accentButton("radialmenu.picker.use_text", () -> {
                        TextIcon icon = createTextIcon();

                        if (icon != null) {
                            closeWith(icon);
                        }
                    })
                        .width(BUTTON_WIDTH)));
    }

    @Nullable
    private TextIcon createTextIcon() {
        return text.trim()
            .isEmpty() ? null : new TextIcon(text.trim(), color);
    }

    /**
     * @return the choices of the current tab, built when the tab is shown or the search changes what it offers
     */
    private List<Choice> getChoices() {
        String query = search.getStringValue();

        if ((tabChoices == null) || (tabChoicesTab != tab) || ((tab == Tab.HEADS) && !query.equals(tabChoicesSearch))) {
            tabChoices = switch (tab) {
                case ITEMS -> getItemChoices();
                case SYMBOLS -> createSymbolChoices();
                case EFFECTS -> createEffectChoices();
                case IMAGES -> createImageChoices();
                case HEADS -> createHeadChoices();
                case TEXT -> Collections.emptyList();
            };
            tabChoicesTab = tab;
            tabChoicesSearch = query;
        }
        return tabChoices;
    }

    private static List<Choice> getItemChoices() {
        if (itemChoices == null) {
            itemChoices = createItemChoices();
        }
        return itemChoices;
    }

    /**
     * @return every registered item and each of its variants, vanilla first, then by mod, each mod in the order it
     *         registered its items
     */
    private static List<Choice> createItemChoices() {
        List<Item> items = new ArrayList<>();
        List<Choice> choices = new ArrayList<>();

        for (Object item : Item.itemRegistry) {
            items.add((Item) item);
        }
        items.sort(Comparator.comparing(item -> {
            String modId = getModId(ItemIcon.getRegistryName(item));

            return VANILLA_MOD_ID.equals(modId) ? "" : modId;
        }));
        for (Item item : items) {
            String registryName = ItemIcon.getRegistryName(item);

            for (ItemStack stack : getVariants(item)) {
                ItemIcon icon = new ItemIcon(stack);

                choices.add(
                    new Choice(
                        getDisplayName(stack, String.valueOf(registryName)),
                        String.valueOf(registryName),
                        icon,
                        () -> icon));
            }
        }
        return choices;
    }

    private static String getModId(@Nullable String registryName) {
        int colon = registryName == null ? -1 : registryName.indexOf(':');

        return colon < 0 ? VANILLA_MOD_ID : registryName.substring(0, colon);
    }

    /**
     * @return the item's variants as the creative menu lists them, or just meta 0 if it lists none. Items are asked for
     *         every creative tab they are in, since some give different variants per tab.
     */
    private static List<ItemStack> getVariants(Item item) {
        List<ItemStack> variants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<ItemStack> listed = new ArrayList<>();
        CreativeTabs[] creativeTabs;

        try {
            creativeTabs = item.getCreativeTabs();
        } catch (RuntimeException | LinkageError e) {
            creativeTabs = null;
        }
        if ((creativeTabs == null) || (creativeTabs.length == 0)) {
            creativeTabs = new CreativeTabs[] { null };
        }
        for (CreativeTabs creativeTab : creativeTabs) {
            try {
                item.getSubItems(item, creativeTab, listed);
            } catch (RuntimeException | LinkageError e) {
                // Some items assume a real creative tab or a world; they still get their plain variant below.
            }
        }
        for (Object stack : listed) {
            if ((stack instanceof ItemStack itemStack) && (itemStack.getItem() == item)
                && seen.add(itemStack.getItemDamage() + "|" + itemStack.getTagCompound())) {
                variants.add(itemStack);
            }
        }
        if (variants.isEmpty()) {
            variants.add(new ItemStack(item));
        }
        return variants;
    }

    private static String getDisplayName(ItemStack stack, String fallback) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    private List<Choice> createSymbolChoices() {
        List<Choice> choices = new ArrayList<>();

        for (SymbolSheet.Symbol symbol : SymbolSheet.getSymbols()) {
            String name = symbol.getName();

            choices.add(
                new Choice(
                    name,
                    symbol.getSearchText(),
                    (x, y, size) -> new SymbolIcon(name, color).draw(x, y, size),
                    () -> new SymbolIcon(name, color)));
        }
        return choices;
    }

    private static List<Choice> createEffectChoices() {
        List<Choice> choices = new ArrayList<>();

        for (Potion potion : EffectIcon.getEffectsWithIcons()) {
            EffectIcon icon = new EffectIcon(potion.getName());

            choices.add(new Choice(I18n.format(potion.getName()), potion.getName(), icon, () -> icon));
        }
        return choices;
    }

    private static List<Choice> createImageChoices() {
        List<Choice> choices = new ArrayList<>();

        for (String file : ImageIcon.listFiles()) {
            ImageIcon icon = new ImageIcon(file);

            choices.add(new Choice(file, "", icon, () -> icon));
        }
        return choices;
    }

    /**
     * @return the players on the server and nearby, plus the name typed in the search box if it could be a player
     */
    private List<Choice> createHeadChoices() {
        Minecraft mc = Minecraft.getMinecraft();
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String typed = search.getStringValue()
            .trim();
        List<Choice> choices = new ArrayList<>();

        if (mc.thePlayer != null) {
            for (Object info : mc.thePlayer.sendQueue.playerInfoList) {
                addPlayerName(names, ((GuiPlayerInfo) info).name);
            }
        }
        if (mc.theWorld != null) {
            for (Object player : mc.theWorld.playerEntities) {
                addPlayerName(names, ((EntityPlayer) player).getCommandSenderName());
            }
        }
        if (PLAYER_NAME.matcher(typed)
            .matches() && !names.contains(typed)) {
            choices.add(createHeadChoice(typed));
        }
        for (String name : names) {
            choices.add(createHeadChoice(name));
        }
        return choices;
    }

    /**
     * Adds a name from the tab list or the world. Servers can put color codes or ranks into tab list names, so the
     * codes are removed, and what isn't a valid account name is left out since no skin could be found for it.
     */
    private static void addPlayerName(Set<String> names, @Nullable String listed) {
        String name = listed == null ? "" : EnumChatFormatting.getTextWithoutFormattingCodes(listed);

        if (PLAYER_NAME.matcher(name)
            .matches()) {
            names.add(name);
        }
    }

    private static Choice createHeadChoice(String name) {
        HeadIcon icon = new HeadIcon(name);

        return new Choice(name, "", icon, () -> icon);
    }

    private String getEmptyText() {
        return switch (tab) {
            case IMAGES -> I18n.format("radialmenu.picker.no_images");
            case HEADS -> I18n.format("radialmenu.picker.no_players");
            default -> I18n.format("radialmenu.picker.no_match");
        };
    }

    /**
     * The choices of the current tab that match the search, in rows that fill the list's width. Draws only the rows in
     * view itself, since the Items tab can hold tens of thousands of icons.
     */
    private final class IconGrid extends Widget<IconGrid> implements Interactable {

        private static final int CELL = 20;
        private static final int ICON_SIZE = 16;
        private static final int HOVER_FILL = 0xFF2C3A57;
        private static final int TEXT_INSET = 4;

        private final List<Choice> shown = new ArrayList<>();
        @Nullable
        private List<Choice> shownChoices;
        @Nullable
        private String shownSearch;
        private int shownColumns;
        @Nullable
        private Choice hovered;

        private IconGrid() {
            widthRelOffset(1f, -EditorWidgets.LIST_ROW_INSET);
            height(CELL);
        }

        String getHoveredName() {
            return hovered == null ? "" : hovered.name;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            refilter();
        }

        /**
         * Recomputes the shown icons when the tab, the search or the width changed, and grows or shrinks to fit them.
         */
        private void refilter() {
            List<Choice> choices = getChoices();
            String query = search.getStringValue()
                .trim()
                .toLowerCase(Locale.ROOT);
            int rows;

            if ((choices == shownChoices) && query.equals(shownSearch) && (getColumns() == shownColumns)) {
                return;
            }
            shownChoices = choices;
            shownSearch = query;
            shownColumns = getColumns();
            shown.clear();
            for (Choice choice : choices) {
                if (choice.searchText.contains(query)) {
                    shown.add(choice);
                }
            }
            rows = Math.max(1, (shown.size() + shownColumns - 1) / shownColumns);
            height(rows * CELL);
            scheduleResize();
        }

        private int getColumns() {
            return Math.max(1, getArea().width / CELL);
        }

        @Nullable
        private Choice choiceAt(int x, int y) {
            int index;

            if ((x < 0) || (y < 0) || (x >= (getColumns() * CELL))) {
                return null;
            }
            index = ((y / CELL) * getColumns()) + (x / CELL);
            return index < shown.size() ? shown.get(index) : null;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            int columns = getColumns();
            int firstRow = 0;
            int lastRow = Integer.MAX_VALUE;

            refilter();
            if (shown.isEmpty()) {
                drawEmptyText();
                return;
            }
            if (getParent() instanceof AbstractScrollWidget<?, ?>scroll) {
                firstRow = scroll.getScrollY() / CELL;
                lastRow = (scroll.getScrollY() + scroll.getArea().height) / CELL;
            }
            hovered = isHovering() ? choiceAt(context.getMouseX(), context.getMouseY()) : null;
            for (int i = firstRow * columns; i < Math.min(shown.size(), (lastRow + 1) * columns); i++) {
                Choice choice = shown.get(i);
                int x = (i % columns) * CELL;
                int y = (i / columns) * CELL;

                if (choice == hovered) {
                    GuiDraw.drawRect(x, y, CELL, CELL, HOVER_FILL);
                }
                choice.preview.draw(x + (CELL / 2f), y + (CELL / 2f), ICON_SIZE);
            }
        }

        private void drawEmptyText() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int y = TEXT_INSET;

            hovered = null;
            for (Object line : font.listFormattedStringToWidth(getEmptyText(), getArea().width - (2 * TEXT_INSET))) {
                GuiDraw
                    .drawText((String) line, TEXT_INSET, y, 1f, EditorWidgets.DETAIL_COLOR, EditorWidgets.TEXT_SHADOW);
                y += EditorWidgets.FONT_HEIGHT + 2;
            }
        }

        @Override
        public Result onMousePressed(int mouseButton) {
            Choice choice = choiceAt(getContext().getMouseX(), getContext().getMouseY());

            if (choice == null) {
                return Result.IGNORE;
            }
            closeWith(choice.result.get());
            return Result.SUCCESS;
        }
    }
}
