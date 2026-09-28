package com.cultivate.radialmenu.client.gui.editor;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/**
 * A dialog listing options with a search box. Clicking an option picks it and closes the dialog.
 */
final class SearchListDialog<T> extends Dialog<T> {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 200;
    private static final int ROW_HEIGHT = 14;

    SearchListDialog(String name, String title, List<T> options, Function<T, String> displayName, Consumer<T> onPick) {
        super(name, onPick);
        StringValue search = new StringValue("");

        size(WIDTH, HEIGHT);
        padding(7);
        setCloseOnOutOfBoundsClick(true);
        child(
            Flow.column()
                .sizeRel(1f)
                .childPadding(3)
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
                    new ListWidget<>().widthRel(1f)
                        .expanded()
                        .collapseDisabledChild()
                        .children(options, option -> createOptionButton(option, displayName, search))));
    }

    private ButtonWidget<?> createOptionButton(T option, Function<T, String> displayName, StringValue search) {
        String text = displayName.apply(option);
        String searchText = text.toLowerCase(Locale.ROOT);

        return new ButtonWidget<>().widthRel(1f)
            .height(ROW_HEIGHT)
            .overlay(
                IKey.str(text)
                    .alignment(Alignment.CenterLeft))
            .setEnabledIf(
                button -> searchText.contains(
                    search.getStringValue()
                        .trim()
                        .toLowerCase(Locale.ROOT)))
            .onMousePressed(mouseButton -> {
                closeWith(option);
                return true;
            });
    }
}
