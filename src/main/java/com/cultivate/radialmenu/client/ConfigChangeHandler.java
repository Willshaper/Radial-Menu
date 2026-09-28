package com.cultivate.radialmenu.client;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.config.RadialMenuConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Re-reads the config after it is edited in the in-game config screen. GTNHLib only re-syncs its own config, so
 * without this the edits would not apply until a restart.
 */
public class ConfigChangeHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!RadialMenuMod.MODID.equals(event.modID)) {
            return;
        }
        try {
            RadialMenuConfig.load();
        } catch (ConfigException e) {
            RadialMenuMod.LOG.error("failed to reload the config", e);
        }
    }
}
