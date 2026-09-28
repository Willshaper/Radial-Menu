package com.cultivate.radialmenu.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

/**
 * Adds a Config button for Radial Menu to the mod list.
 */
public class RadialMenuGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigScreen.class;
    }

    public static class ConfigScreen extends SimpleGuiConfig {

        public ConfigScreen(GuiScreen parent) throws ConfigException {
            super(parent, RadialMenuConfig.class, RadialMenuMod.MODID, "Radial Menu");
        }
    }
}
