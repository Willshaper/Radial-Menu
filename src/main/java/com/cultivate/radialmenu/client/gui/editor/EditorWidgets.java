package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

/**
 * Small widget builders shared by the editor screens.
 */
final class EditorWidgets {

    static final int ROW_HEIGHT = 16;
    static final int LABEL_WIDTH = 58;
    static final int HINT_COLOR = 0xFF707070;
    // Height of a line of text in the vanilla font, without the gap below it.
    static final int FONT_HEIGHT = 8;
    static final int TEXT_COLOR = 0xFFFFFFFF;
    // Space between a list row's edge and its text.
    static final int TEXT_INSET = 4;
    static final int DETAIL_COLOR = 0xFFA0A0A0;
    // List scrollbars get a fixed width, and list rows leave this much room so the scrollbar never covers them.
    static final int SCROLLBAR_THICKNESS = 4;
    static final int LIST_ROW_INSET = SCROLLBAR_THICKNESS + 4;

    private static final int RIGHT_BUTTON = 1;
    private static final String ELLIPSIS = "...";

    private EditorWidgets() {}

    /**
     * A row with a translated label on the left and the given widgets after it.
     */
    static Flow labeledRow(String labelKey, IWidget... widgets) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(ROW_HEIGHT)
            .childPadding(4)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.lang(labelKey)
                    .alignment(Alignment.CenterLeft)
                    .asWidget()
                    .width(LABEL_WIDTH)
                    .heightRel(1f));

        for (IWidget widget : widgets) {
            row.child(widget);
        }
        return row;
    }

    /**
     * A button showing changing text. Left click steps forward, right click steps back.
     */
    static ButtonWidget<?> cycleButton(Supplier<String> text, IntConsumer step) {
        return new ButtonWidget<>().height(ROW_HEIGHT)
            .overlay(IKey.dynamic(text))
            .onMousePressed(mouseButton -> {
                step.accept(mouseButton == RIGHT_BUTTON ? -1 : 1);
                return true;
            });
    }

    static ButtonWidget<?> button(String langKey, Runnable onClick) {
        return new ButtonWidget<>().height(ROW_HEIGHT)
            .overlay(IKey.lang(langKey))
            .onMousePressed(mouseButton -> {
                onClick.run();
                return true;
            });
    }

    /**
     * @return a vertical scrollbar on the right with a fixed width, so list rows can be sized to stay clear of it
     */
    static ScrollData verticalScrollbar() {
        return ScrollData.of(GuiAxis.Y, false, SCROLLBAR_THICKNESS);
    }

    /**
     * @return the text, cut short with an ellipsis if it is wider than {@code maxWidth} pixels
     */
    static String fitToWidth(FontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return font.trimStringToWidth(text, Math.max(0, maxWidth - font.getStringWidth(ELLIPSIS))) + ELLIPSIS;
    }

    /**
     * Shows the widget only while the condition holds, for use with {@code onUpdateListener}. When the widget appears
     * or
     * disappears, the whole panel is laid out again: ModularUI2 2.2.18 only moves the widget's neighbours, so the
     * columns and panel around it would keep their old size and rows would overlap.
     */
    static <W extends IWidget> Consumer<W> showIf(BooleanSupplier condition) {
        return widget -> {
            boolean show = condition.getAsBoolean();

            if (widget.isEnabled() != show) {
                widget.setEnabled(show);
                widget.getPanel()
                    .scheduleResize();
            }
        };
    }

    static <E extends Enum<E>> E cycle(E value, int offset) {
        E[] values = value.getDeclaringClass()
            .getEnumConstants();

        return values[Math.floorMod(value.ordinal() + offset, values.length)];
    }
}
