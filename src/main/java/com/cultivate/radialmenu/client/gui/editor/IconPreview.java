package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.Supplier;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cultivate.radialmenu.menu.icon.SectionIcon;

/**
 * Shows an icon in a slot, the way the ring will draw it.
 */
final class IconPreview extends Widget<IconPreview> {

    private static final int SIZE = 18;
    private static final int ICON_SIZE = 16;

    private final Supplier<SectionIcon> icon;

    /**
     * @param icon the icon to show, or null for an empty slot
     */
    IconPreview(Supplier<SectionIcon> icon) {
        this.icon = icon;
        size(SIZE);
        background(GuiTextures.SLOT_ITEM);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        SectionIcon shown = icon.get();

        if (shown != null) {
            shown.draw(SIZE / 2f, SIZE / 2f, ICON_SIZE);
        }
    }
}
