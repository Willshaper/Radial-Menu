package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cultivate.radialmenu.menu.icon.SymbolIcon;

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
    static final int TITLE_HEIGHT = 12;
    static final int CAPTION_HEIGHT = 10;
    // Height of a line of text in the vanilla font, without the gap below it.
    static final int FONT_HEIGHT = 8;
    // Space between a list row's edge and its text.
    static final int TEXT_INSET = 4;
    // List scrollbars get a fixed width, and list rows leave this much room so the scrollbar never covers them.
    static final int SCROLLBAR_THICKNESS = 4;
    static final int LIST_ROW_INSET = SCROLLBAR_THICKNESS + 4;

    static final int TEXT_COLOR = 0xFFE6E7EA;
    static final int DETAIL_COLOR = 0xFF9A9EA6;
    static final int HINT_COLOR = 0xFF6B6F78;
    // The theme draws text without a shadow; text the mod draws itself matches it.
    static final boolean TEXT_SHADOW = false;
    static final int ACCENT_COLOR = 0xFF4A86E8;
    static final int SELECTED_FILL = 0xFF263552;
    static final int ROW_FILL = 0xFF24262A;
    static final int ROW_HOVER_FILL = 0xFF2E3137;
    static final int FIELD_FILL = 0xFF141518;
    static final int FIELD_BORDER = 0xFF33363C;
    static final int PANEL_BORDER = 0xFF34373D;
    static final int DANGER_COLOR = 0xFFF08A8A;

    private static final int ACCENT_FILL = 0xFF3C6FD0;
    private static final int ACCENT_HOVER_FILL = 0xFF4A7DDC;
    private static final int ACCENT_BORDER = 0xFF5A93F0;
    private static final int DANGER_FILL = 0xFF2E2224;
    private static final int DANGER_HOVER_FILL = 0xFF46282B;
    private static final int DANGER_BORDER = 0xFF5A3434;
    private static final int SYMBOL_SIZE = 10;
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
     * A grey label exactly as wide as its translated text, for rows that pack several settings side by side.
     */
    static IWidget inlineLabel(String langKey) {
        String text = I18n.format(langKey);

        return IKey.str(text)
            .color(DETAIL_COLOR)
            .alignment(Alignment.CenterLeft)
            .asWidget()
            .width(textWidth(text))
            .heightRel(1f);
    }

    static int textWidth(String text) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
    }

    /**
     * A window's title bar: the title on the left, then any extra controls, then an × that closes the window.
     */
    static Flow titleBar(IKey title, Runnable onClose, IWidget... extras) {
        Flow bar = Flow.row()
            .widthRel(1f)
            .height(TITLE_HEIGHT)
            .childPadding(4)
            .child(
                title.alignment(Alignment.CenterLeft)
                    .asWidget()
                    .expanded()
                    .heightRel(1f));

        for (IWidget extra : extras) {
            bar.child(extra);
        }
        return bar.child(
            symbolButton("x", null, onClose).background(IDrawable.EMPTY)
                .hoverBackground(IDrawable.EMPTY)
                .size(TITLE_HEIGHT));
    }

    /**
     * A small grey heading above a group of rows.
     */
    static IWidget caption(String langKey) {
        return IKey.lang(langKey)
            .color(HINT_COLOR)
            .alignment(Alignment.BottomLeft)
            .asWidget()
            .widthRel(1f)
            .height(CAPTION_HEIGHT);
    }

    /**
     * A thin line across the window, between groups of rows.
     */
    static IWidget divider() {
        return new Widget<>().widthRel(1f)
            .height(1)
            .background(new Rectangle().color(PANEL_BORDER));
    }

    /**
     * @return a drawable of one of the bundled symbols, centered in whatever area it is drawn in
     */
    static IDrawable symbol(String name, int color) {
        SymbolIcon icon = new SymbolIcon(name, color);

        return (context, x, y, width, height, widgetTheme) -> icon
            .draw(x + (width / 2f), y + (height / 2f), SYMBOL_SIZE);
    }

    /**
     * A square button showing a symbol, grey until the mouse is over it.
     *
     * @param tooltipKey the tooltip, or null for none
     */
    static ButtonWidget<?> symbolButton(String symbolName, @Nullable String tooltipKey, Runnable onClick) {
        ButtonWidget<?> button = new ButtonWidget<>().size(ROW_HEIGHT)
            .overlay(symbol(symbolName, DETAIL_COLOR))
            .hoverOverlay(symbol(symbolName, TEXT_COLOR))
            .onMousePressed(mouseButton -> {
                onClick.run();
                return true;
            });

        if (tooltipKey != null) {
            button.tooltip(tooltip -> tooltip.addLine(I18n.format(tooltipKey)));
        }
        return button;
    }

    /**
     * @return a flat box with a thin border
     */
    static IDrawable box(int fill, int border) {
        return new DrawableStack(
            new Rectangle().color(fill),
            new Rectangle().color(border)
                .hollow());
    }

    /**
     * @return a dark box with a thin border, like the theme's text fields, for slots and similar wells
     */
    static IDrawable wellBackground() {
        return box(FIELD_FILL, FIELD_BORDER);
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
     * The window's main button, such as Save, filled with the accent color.
     */
    static ButtonWidget<?> accentButton(String langKey, Runnable onClick) {
        return button(langKey, onClick).background(box(ACCENT_FILL, ACCENT_BORDER))
            .hoverBackground(box(ACCENT_HOVER_FILL, ACCENT_BORDER));
    }

    /**
     * A button that removes something, in red.
     */
    static ButtonWidget<?> dangerButton(Supplier<String> text, Runnable onClick) {
        return new ButtonWidget<>().height(ROW_HEIGHT)
            .background(box(DANGER_FILL, DANGER_BORDER))
            .hoverBackground(box(DANGER_HOVER_FILL, DANGER_BORDER))
            .overlay(
                IKey.dynamic(text)
                    .color(DANGER_COLOR))
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
