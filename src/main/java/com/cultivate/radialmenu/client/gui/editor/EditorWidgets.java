package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

/**
 * Small widget builders and colors shared by the editor screens.
 *
 * <p>
 * The screens' panels, buttons and text fields are drawn by the {@code radialmenu:dark} MUI2 theme in
 * {@code assets/radialmenu/themes/dark.json}; the colors here match it for what the mod draws itself.
 */
final class EditorWidgets {

    static final int ROW_HEIGHT = 16;
    static final int LABEL_WIDTH = 58;
    static final int HINT_COLOR = 0xFF6B6F78;
    // Height of a line of text in the vanilla font, without the gap below it.
    static final int FONT_HEIGHT = 8;
    static final int TEXT_COLOR = 0xFFE6E7EA;
    // The theme draws text without a shadow; text the mod draws itself matches it.
    static final boolean TEXT_SHADOW = false;
    static final int ACCENT_COLOR = 0xFF4A86E8;
    static final int SELECTED_FILL = 0xFF263552;
    static final int FIELD_FILL = 0xFF141518;
    static final int FIELD_BORDER = 0xFF33363C;
    static final int TITLE_HEIGHT = 12;
    // Space between a list row's edge and its text.
    static final int TEXT_INSET = 4;
    static final int DETAIL_COLOR = 0xFF9A9EA6;
    // List scrollbars get a fixed width, and list rows leave this much room so the scrollbar never covers them.
    static final int SCROLLBAR_THICKNESS = 4;
    static final int LIST_ROW_INSET = SCROLLBAR_THICKNESS + 4;

    private static final int RIGHT_BUTTON = 1;
    private static final String ELLIPSIS = "...";
    private static final String CLOSE_MARK = "×";

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
                    .color(DETAIL_COLOR)
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
     * A window's title bar: the title on the left and an × that closes the window on the right.
     */
    static Flow titleBar(IKey title, Runnable onClose) {
        return Flow.row()
            .widthRel(1f)
            .height(TITLE_HEIGHT)
            .child(
                title.alignment(Alignment.CenterLeft)
                    .asWidget()
                    .expanded()
                    .heightRel(1f))
            .child(
                new ButtonWidget<>().size(TITLE_HEIGHT)
                    .background(IDrawable.EMPTY)
                    .hoverBackground(IDrawable.EMPTY)
                    .overlay(
                        IKey.str(CLOSE_MARK)
                            .color(DETAIL_COLOR))
                    .hoverOverlay(
                        IKey.str(CLOSE_MARK)
                            .color(TEXT_COLOR))
                    .onMousePressed(mouseButton -> {
                        onClose.run();
                        return true;
                    }));
    }

    /**
     * @return a dark box with a thin border, like the theme's text fields, for slots and similar wells
     */
    static IDrawable wellBackground() {
        return new DrawableStack(
            new Rectangle().color(FIELD_FILL),
            new Rectangle().color(FIELD_BORDER)
                .hollow());
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

    static <E extends Enum<E>> E cycle(E value, int offset) {
        E[] values = value.getDeclaringClass()
            .getEnumConstants();

        return values[Math.floorMod(value.ordinal() + offset, values.length)];
    }
}
