package com.cultivate.radialmenu.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import com.cultivate.radialmenu.client.KeyBindings;
import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.cultivate.radialmenu.config.RingColor;
import com.cultivate.radialmenu.menu.MenuSection;
import com.cultivate.radialmenu.menu.RadialMenu;
import com.cultivate.radialmenu.menu.action.SectionAction;
import com.cultivate.radialmenu.menu.icon.SectionIcon;
import com.cultivate.radialmenu.menu.icon.TextIcon;

/**
 * Draws the ring, icons and labels of a {@link RadialScreen}. Input is handled by the screen.
 *
 * <p>
 * Everything is drawn through the Tessellator and plain GL state calls, which Angelica handles like vanilla GUI
 * drawing.
 */
final class RadialWidget extends Widget<RadialWidget> {

    // Segments used for a full circle; arcs use their share of it.
    private static final int CIRCLE_SEGMENTS = 96;
    private static final int MIN_ARC_SEGMENTS = 4;
    // Gap between neighbouring sections, in GUI pixels at the middle of the ring.
    private static final float SECTION_GAP = 1.5f;
    // Width of the highlight drawn along the outside of the hovered section.
    private static final float HOVER_RIM_WIDTH = 2f;
    private static final float HOVER_ICON_GROWTH = 1.15f;
    private static final float LABEL_GAP = 6f;
    private static final int FONT_HEIGHT = 8;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int HINT_COLOR = 0xAAAAAA;
    // Rim and hint color while Shift is held, marking that a click edits instead of runs.
    private static final int EDIT_COLOR = 0xFFD040;
    private static final int PLACEHOLDER_COLOR = 0x808080;
    private static final String EMPTY_SECTION_MARK = "+";
    private static final String BACK_MARK = "←";
    private static final String PATH_SEPARATOR = " › ";
    // Placeholder text length when a section has a label but no icon.
    private static final int LABEL_INITIALS = 2;

    private final RadialScreen screen;

    RadialWidget(RadialScreen screen) {
        this.screen = screen;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        RadialLayout layout = screen.createLayout(screen.getOpenScale());
        int hovered = screen.getHoveredSection();
        int inlineHovered = screen.getHoveredInlineSection();
        boolean editing = Interactable.hasShiftDown();
        RadialMenu inlineMenu = screen.getInlineMenu();

        drawRing(layout, hovered, editing);
        drawIcons(layout, hovered);
        if (inlineMenu != null) {
            drawInlineRing(layout, inlineMenu, inlineHovered, editing);
        }
        drawLabels(layout, hovered, inlineHovered, editing);
    }

    private void drawRing(RadialLayout layout, int hovered, boolean editing) {
        RadialMenu menu = screen.getMenu();
        int sectionColor = RingColor.SECTION.getArgb();
        int hoverColor = RingColor.HOVER.getArgb();
        double gapAngle = SECTION_GAP / layout.middleRadius();

        Platform.setupDrawColor();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < layout.sectionCount; i++) {
            double start = (layout.angleOf(i) - (layout.sectionAngle / 2)) + (gapAngle / 2);
            double end = (layout.angleOf(i) + (layout.sectionAngle / 2)) - (gapAngle / 2);
            int color = (i == hovered) ? hoverColor : sectionColor;

            if ((i != hovered) && (i == screen.getInlineParent())) {
                // Marks which section the inline ring belongs to.
                color = halveAlpha(hoverColor);
            } else if (menu.getSection(i)
                .isEmpty()) {
                    color = halveAlpha(color);
                }
            drawArc(layout, start, end, layout.innerRadius, layout.outerRadius, color);
            if (i == hovered) {
                drawHoverRim(layout, start, end, layout.outerRadius, editing);
            }
        }
        if (layout.centerRadius > 0) {
            boolean centerActive = (hovered == RadialLayout.CENTER) && screen.canGoBack();

            drawArc(
                layout,
                0,
                RadialLayout.FULL_CIRCLE,
                0,
                layout.centerRadius,
                centerActive ? hoverColor : RingColor.CENTER.getArgb());
        }
        Platform.endDrawColor();
    }

    private static void drawHoverRim(RadialLayout layout, double start, double end, float radius, boolean editing) {
        drawArc(
            layout,
            start,
            end,
            radius,
            radius + HOVER_RIM_WIDTH,
            (editing ? EDIT_COLOR : RingColor.HOVER.getArgb()) | 0xFF000000);
    }

    /**
     * Draws the inline ring: the sections of a submenu, in an arc outside the main ring centered on the section that
     * opens it.
     */
    private void drawInlineRing(RadialLayout layout, RadialMenu inlineMenu, int hovered, boolean editing) {
        int count = inlineMenu.getSectionCount();
        double sectionAngle = layout.inlineSectionAngle(count);
        double startAngle = layout.inlineStartAngle(screen.getInlineParent(), count);
        float middleRadius = layout.inlineMiddleRadius();
        double gapAngle = SECTION_GAP / middleRadius;
        float iconSize = layout.inlineIconSize(count);

        Platform.setupDrawColor();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < count; i++) {
            double start = startAngle + (i * sectionAngle) + (gapAngle / 2);
            double end = (startAngle + ((i + 1) * sectionAngle)) - (gapAngle / 2);
            int color = (i == hovered) ? RingColor.HOVER.getArgb() : RingColor.SECTION.getArgb();

            if (inlineMenu.getSection(i)
                .isEmpty()) {
                color = halveAlpha(color);
            }
            drawArc(layout, start, end, layout.inlineInnerRadius, layout.inlineOuterRadius, color);
            if (i == hovered) {
                drawHoverRim(layout, start, end, layout.inlineOuterRadius, editing);
            }
        }
        Platform.endDrawColor();
        for (int i = 0; i < count; i++) {
            double angle = startAngle + ((i + 0.5) * sectionAngle);

            drawSectionIcon(
                inlineMenu.getSection(i),
                layout.xAt(angle, middleRadius),
                layout.yAt(angle, middleRadius),
                (i == hovered) ? iconSize * HOVER_ICON_GROWTH : iconSize);
        }
    }

    /**
     * Draws a ring segment between two angles and two radii as one triangle strip.
     */
    private static void drawArc(RadialLayout layout, double startAngle, double endAngle, float innerRadius,
        float outerRadius, int argb) {
        Tessellator tessellator = Tessellator.instance;
        int segments = Math.max(
            MIN_ARC_SEGMENTS,
            (int) Math.ceil((CIRCLE_SEGMENTS * (endAngle - startAngle)) / RadialLayout.FULL_CIRCLE));

        tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tessellator.setColorRGBA_I(argb & 0xFFFFFF, argb >>> 24);
        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (((endAngle - startAngle) * i) / segments);

            tessellator.addVertex(layout.xAt(angle, outerRadius), layout.yAt(angle, outerRadius), 0);
            tessellator.addVertex(layout.xAt(angle, innerRadius), layout.yAt(angle, innerRadius), 0);
        }
        tessellator.draw();
    }

    private static int halveAlpha(int argb) {
        return (((argb >>> 24) / 2) << 24) | (argb & 0xFFFFFF);
    }

    private void drawIcons(RadialLayout layout, int hovered) {
        RadialMenu menu = screen.getMenu();
        float radius = layout.middleRadius();

        for (int i = 0; i < layout.sectionCount; i++) {
            MenuSection section = menu.getSection(i);
            double angle = layout.angleOf(i);
            float x = layout.xAt(angle, radius);
            float y = layout.yAt(angle, radius);
            float size = (i == hovered) ? layout.iconSize * HOVER_ICON_GROWTH : layout.iconSize;

            drawSectionIcon(section, x, y, size);
        }
        if (screen.canGoBack() && (layout.centerRadius > 0)) {
            TextIcon.drawCenteredText(BACK_MARK, TEXT_COLOR, layout.centerX, layout.centerY, layout.centerRadius);
        }
    }

    private static void drawSectionIcon(MenuSection section, float x, float y, float size) {
        SectionIcon icon = section.getIcon();

        if (icon != null) {
            icon.draw(x, y, size);
        } else if (section.isEmpty()) {
            TextIcon.drawCenteredText(EMPTY_SECTION_MARK, PLACEHOLDER_COLOR, x, y, size);
        } else {
            TextIcon.drawCenteredText(getInitials(section.getLabel()), TEXT_COLOR, x, y, size);
        }
    }

    private static String getInitials(String label) {
        String trimmed = label.trim();

        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, Math.min(LABEL_INITIALS, trimmed.length()));
    }

    /**
     * Draws the menu name above the ring and a line about the hovered section below it. While Shift is held the line
     * below always says what a click will edit, even with labels turned off.
     */
    private void drawLabels(RadialLayout layout, int hovered, int inlineHovered, boolean editing) {
        float titleY = layout.centerY - layout.extentRadius - HOVER_RIM_WIDTH - LABEL_GAP - FONT_HEIGHT;
        float hintY = layout.centerY + layout.extentRadius + HOVER_RIM_WIDTH + LABEL_GAP;
        RadialMenu inlineMenu = screen.getInlineMenu();

        if (RadialMenuConfig.showLabels) {
            drawCenteredString(getTitle(), layout.centerX, titleY, TEXT_COLOR);
        }
        if (editing) {
            drawCenteredString(
                I18n.format(
                    hovered == RadialLayout.CENTER ? "radialmenu.hint.shift_center" : "radialmenu.hint.shift_section"),
                layout.centerX,
                hintY,
                EDIT_COLOR);
        } else if (RadialMenuConfig.showLabels) {
            if ((inlineHovered != RadialLayout.NONE) && (inlineMenu != null)) {
                drawSectionLabel(inlineMenu.getSection(inlineHovered), layout.centerX, hintY);
            } else if (hovered == RadialLayout.CENTER) {
                drawCenteredString(getCenterHint(), layout.centerX, hintY, HINT_COLOR);
            } else if (hovered != RadialLayout.NONE) {
                drawSectionLabel(
                    screen.getMenu()
                        .getSection(hovered),
                    layout.centerX,
                    hintY);
            }
        }
    }

    private String getTitle() {
        String title = String.join(PATH_SEPARATOR, screen.getMenuPath());
        int cycleSize = RadialMenuClient.getMenuStore()
            .getCycleMenus()
            .size();
        int position = screen.getCyclePosition();

        if ((cycleSize > 1) && (position > 0)) {
            title += "  (" + position + "/" + cycleSize + ")";
        }
        return title;
    }

    private String getCenterHint() {
        int switchKey = KeyBindings.SWITCH.getKeyCode();

        if (screen.canGoBack()) {
            return I18n.format("radialmenu.hint.back");
        }
        if (RadialMenuClient.getMenuStore()
            .getCycleMenus()
            .size() > 1) {
            return I18n.format("radialmenu.hint.switch", GameSettings.getKeyDisplayString(switchKey));
        }
        return I18n.format("radialmenu.hint.edit");
    }

    private void drawSectionLabel(MenuSection section, float x, float y) {
        SectionAction action = section.getAction();

        if (section.isEmpty()) {
            drawCenteredString(I18n.format("radialmenu.hint.empty"), x, y, HINT_COLOR);
            return;
        }
        if (!section.getLabel()
            .isEmpty()) {
            drawCenteredString(section.getLabel(), x, y, TEXT_COLOR);
            return;
        }
        if (action != null) {
            drawCenteredString(action.describe(), x, y, TEXT_COLOR);
        }
    }

    private static void drawCenteredString(String text, float x, float y, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.drawStringWithShadow(text, Math.round(x - (font.getStringWidth(text) / 2f)), Math.round(y), color);
    }
}
