package com.cultivate.radialmenu.client;

import java.io.File;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.menu.MenuStore;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Client-side setup and the shared menu store.
 */
public final class RadialMenuClient {

    private static final String MENU_FILE = RadialMenuMod.MODID + "/menus.json";

    private static MenuStore menuStore;
    private static boolean menusLoaded;

    private RadialMenuClient() {}

    public static void preInit(File configDirectory) {
        menuStore = new MenuStore(new File(configDirectory, MENU_FILE));
    }

    public static void init() {
        KeyBindings.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientTickHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new ConfigChangeHandler());
    }

    /**
     * Returns the menus, loading them on first use. Loading waits until then because item icons look their items up
     * in the registry, which is only complete once every mod has finished loading.
     */
    public static MenuStore getMenuStore() {
        if (!menusLoaded) {
            menusLoaded = true;
            menuStore.load();
        }
        return menuStore;
    }
}
