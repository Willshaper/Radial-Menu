package com.cultivate.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cultivate.radialmenu.menu.icon.SectionIcon;

/**
 * A dialog showing icons in a grid with a search box. Clicking an icon picks it and closes the dialog; the name of the
 * icon under the mouse is shown below the grid.
 */
final class IconPickerDialog extends Dialog<IconPickerDialog.Choice> {

    private static final int WIDTH = 280;
    private static final int HEIGHT = 220;
    private static final int PADDING = 7;

    private final StringValue search = new StringValue("");
    private final IconGrid grid;

    /**
     * One icon to choose from.
     */
    static final class Choice {

        private final String value;
        private final String name;
        private final String searchText;
        private final SectionIcon icon;

        /**
         * @param value      what is stored when this icon is picked, such as a symbol or file name
         * @param name       shown when the mouse is over the icon
         * @param searchText extra words the icon is found by, besides its name
         */
        Choice(String value, String name, String searchText, SectionIcon icon) {
            this.value = value;
            this.name = name;
            this.searchText = (name + " " + searchText).toLowerCase(Locale.ROOT);
            this.icon = icon;
        }

        String getValue() {
            return value;
        }
    }

    /**
     * @param emptyHint shown instead of the grid when there is nothing to choose from
     */
    IconPickerDialog(String name, String title, List<Choice> choices, String emptyHint, Consumer<Choice> onPick) {
        super(name, onPick);
        this.grid = new IconGrid(choices);
        size(WIDTH, HEIGHT);
        padding(PADDING);
        setCloseOnOutOfBoundsClick(true);
        child(
            Flow.column()
                .sizeRel(1f)
                .childPadding(3)
                .collapseDisabledChild()
                .child(
                    IKey.str(title)
                        .asWidget()
                        .widthRel(1f)
                        .height(10))
                .child(
                    new TextFieldWidget().value(search)
                        .autoUpdateOnChange(true)
                        .hintText(I18n.format("radialmenu.editor.search"))
                        .widthRel(1f)
                        .height(14))
                .child(
                    IKey.str(emptyHint)
                        .color(EditorWidgets.HINT_COLOR)
                        .alignment(Alignment.TopLeft)
                        .asWidget()
                        .widthRel(1f)
                        .expanded()
                        .setEnabledIf(hint -> choices.isEmpty()))
                .child(
                    new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                        .showScrollShadows(false)
                        .widthRel(1f)
                        .expanded()
                        .child(grid)
                        .setEnabledIf(list -> !choices.isEmpty()))
                .child(
                    IKey.dynamic(grid::getHoveredName)
                        .color(EditorWidgets.DETAIL_COLOR)
                        .alignment(Alignment.CenterLeft)
                        .asWidget()
                        .widthRel(1f)
                        .height(10)));
    }

    /**
     * The icons matching the search, in rows that fill the list's width. Draws them all itself, since a widget per icon
     * would mean over a thousand widgets for the symbols.
     */
    private final class IconGrid extends Widget<IconGrid> implements Interactable {

        private static final int CELL = 20;
        private static final int ICON_SIZE = 16;
        private static final int HOVER_FILL = 0x603C78D8;

        private final List<Choice> choices;
        private final List<Choice> shown = new ArrayList<>();
        @Nullable
        private String shownSearch;
        private int shownColumns;
        @Nullable
        private Choice hovered;

        private IconGrid(List<Choice> choices) {
            this.choices = choices;
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
         * Recomputes the shown icons when the search or the width changed, and grows or shrinks to fit them.
         */
        private void refilter() {
            String query = search.getStringValue()
                .trim()
                .toLowerCase(Locale.ROOT);
            int rows;

            if (query.equals(shownSearch) && (getColumns() == shownColumns)) {
                return;
            }
            shownSearch = query;
            shownColumns = getColumns();
            shown.clear();
            for (Choice choice : choices) {
                if (choice.searchText.contains(query)) {
                    shown.add(choice);
                }
            }
            rows = Math.max(1, (shown.size() + getColumns() - 1) / getColumns());
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

            refilter();
            hovered = isHovering() ? choiceAt(context.getMouseX(), context.getMouseY()) : null;
            for (int i = 0; i < shown.size(); i++) {
                Choice choice = shown.get(i);
                int x = (i % columns) * CELL;
                int y = (i / columns) * CELL;

                if (choice == hovered) {
                    GuiDraw.drawRect(x, y, CELL, CELL, HOVER_FILL);
                }
                choice.icon.draw(x + (CELL / 2f), y + (CELL / 2f), ICON_SIZE);
            }
        }

        @Override
        public Result onMousePressed(int mouseButton) {
            Choice choice = choiceAt(getContext().getMouseX(), getContext().getMouseY());

            if (choice == null) {
                return Result.IGNORE;
            }
            closeWith(choice);
            return Result.SUCCESS;
        }
    }
}
