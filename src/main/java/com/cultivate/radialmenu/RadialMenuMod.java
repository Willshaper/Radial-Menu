package com.cultivate.radialmenu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cultivate.radialmenu.client.RadialMenuClient;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = RadialMenuMod.MODID,
    name = "Radial Menu",
    version = Tags.VERSION,
    dependencies = "required-after:gtnhlib;required-after:modularui2",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    guiFactory = "com.cultivate.radialmenu.client.gui.RadialMenuGuiFactory")
public class RadialMenuMod {

    public static final String MODID = "radialmenu";
    public static final Logger LOG = LogManager.getLogger("RadialMenu");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            ConfigurationManager.registerConfig(RadialMenuConfig.class);
        } catch (ConfigException e) {
            LOG.error("failed to register the config, using defaults", e);
        }
        if (event.getSide()
            .isClient()) {
            RadialMenuClient.preInit(event.getModConfigurationDirectory());
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide()
            .isClient()) {
            RadialMenuClient.init();
        }
    }
}
