package com.cultivate.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/**
 * A dialog listing options with a search box. Clicking an option picks it and closes the dialog.
 *
 * <p>
 * Options can be grouped under headings, and can show a detail on the right, such as the key a keybind is bound to.
 * Every row is a single line; text that does not fit is cut short with an ellipsis instead of wrapping.
 */
final class SearchListDialog<T> extends Dialog<T> {

    private static final int WIDTH = 280;
    private static final int HEIGHT = 220;
    private static final int PADDING = 7;
    private static final int ROW_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 13;
    private static final int DETAIL_GAP = 8;
    private static final int HEADER_COLOR = 0xFF6EA0F0;
    private static final int BADGE_FILL = 0xFF2A2C31;
    private static final int BADGE_PADDING = 3;
    private static final int BADGE_MARGIN = 2;

    private final StringValue search = new StringValue("");

    /**
     * @param options options in display order; grouped options must already be next to each other
     * @param group   heading for an option, or null to show no headings
     * @param label   main text of an option
     * @param detail  text shown on the right of an option, or null for none
     */
    SearchListDialog(String name, String title, List<T> options, @Nullable Function<T, String> group,
        Function<T, String> label, @Nullable Function<T, String> detail, Consumer<T> onPick) {
        super(name, onPick);
        List<IWidget> rows = new ArrayList<>();
        String currentGroup = null;
        List<String> groupSearchTexts = new ArrayList<>();

        for (T option : options) {
            String optionGroup = group == null ? null : group.apply(option);
            String optionLabel = label.apply(option);
            String optionDetail = detail == null ? "" : detail.apply(option);
            String searchText = (optionLabel + " " + optionDetail + " " + (optionGroup == null ? "" : optionGroup))
                .toLowerCase(Locale.ROOT);

            if ((optionGroup != null) && !Objects.equals(optionGroup, currentGroup)) {
                currentGroup = optionGroup;
                groupSearchTexts = new ArrayList<>();
                rows.add(createHeader(optionGroup, groupSearchTexts));
            }
            groupSearchTexts.add(searchText);
            rows.add(createOptionButton(option, optionLabel, optionDetail, searchText));
        }
        size(WIDTH, HEIGHT);
        padding(PADDING);
        setCloseOnOutOfBoundsClick(true);
        child(
            Flow.column()
                .sizeRel(1f)
                .childPadding(3)
                .child(EditorWidgets.titleBar(IKey.str(title), this::closeIfOpen))
                .child(
                    new TextFieldWidget().value(search)
                        .autoUpdateOnChange(true)
                        .hintText(I18n.format("radialmenu.editor.search"))
                        .widthRel(1f)
                        .height(14))
                .child(
                    new ListWidget<>().scrollDirection(EditorWidgets.verticalScrollbar())
                        .showScrollShadows(false)
                        .widthRel(1f)
                        .expanded()
                        .collapseDisabledChild()
                        .children(rows)));
    }

    private boolean matchesSearch(String searchText) {
        return searchText.contains(
            search.getStringValue()
                .trim()
                .toLowerCase(Locale.ROOT));
    }

    /**
     * A heading that hides itself when none of the options under it match the search.
     */
    private IWidget createHeader(String text, List<String> optionSearchTexts) {
        return IKey.str(text)
            .color(HEADER_COLOR)
            .alignment(Alignment.BottomLeft)
            .asWidget()
            .widthRelOffset(1f, -EditorWidgets.LIST_ROW_INSET)
            .height(HEADER_HEIGHT)
            .setEnabledIf(
                header -> optionSearchTexts.stream()
                    .anyMatch(this::matchesSearch));
    }

    private ButtonWidget<?> createOptionButton(T option, String label, String detail, String searchText) {
        return new ButtonWidget<>().widthRelOffset(1f, -EditorWidgets.LIST_ROW_INSET)
            .height(ROW_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(
                (context, x, y, width, height, widgetTheme) -> GuiDraw
                    .drawRect(x, y, width, height, EditorWidgets.ROW_HOVER_FILL))
            .overlay((context, x, y, width, height, widgetTheme) -> {
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                int detailWidth = font.getStringWidth(detail);
                int labelWidth = width - (2 * EditorWidgets.TEXT_INSET)
                    - (detail.isEmpty() ? 0 : detailWidth + DETAIL_GAP + (2 * BADGE_PADDING));
                float textY = y + ((height - EditorWidgets.FONT_HEIGHT) / 2f);

                GuiDraw.drawText(
                    EditorWidgets.fitToWidth(font, label, labelWidth),
                    x + EditorWidgets.TEXT_INSET,
                    textY,
                    1f,
                    EditorWidgets.TEXT_COLOR,
                    EditorWidgets.TEXT_SHADOW);
                if (!detail.isEmpty()) {
                    // The detail, such as the key a keybind is on, sits in a small badge.
                    GuiDraw.drawRect(
                        x + width - EditorWidgets.TEXT_INSET - detailWidth - BADGE_PADDING,
                        y + BADGE_MARGIN,
                        detailWidth + (2 * BADGE_PADDING),
                        height - (2 * BADGE_MARGIN),
                        BADGE_FILL);
                    GuiDraw.drawText(
                        detail,
                        x + width - EditorWidgets.TEXT_INSET - detailWidth,
                        textY,
                        1f,
                        EditorWidgets.DETAIL_COLOR,
                        EditorWidgets.TEXT_SHADOW);
                }
            })
            .setEnabledIf(button -> matchesSearch(searchText))
            .onMousePressed(mouseButton -> {
                closeWith(option);
                return true;
            });
    }
}
