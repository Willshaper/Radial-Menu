package com.cultivate.radialmenu.client;

import java.io.File;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.menu.MenuStore;
import com.cultivate.radialmenu.menu.icon.HeadIcon;
import com.cultivate.radialmenu.menu.icon.ImageIcon;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Client-side setup and the shared menu store.
 */
public final class RadialMenuClient {

    private static final String MENU_FILE = RadialMenuMod.MODID + "/menus.json";
    private static final String ICON_DIRECTORY = RadialMenuMod.MODID + "/icons";
    private static final String HEAD_CACHE_FILE = RadialMenuMod.MODID + "/heads.json";

    private static MenuStore menuStore;
    private static boolean menusLoaded;

    private RadialMenuClient() {}

    public static void preInit(File configDirectory) {
        menuStore = new MenuStore(new File(configDirectory, MENU_FILE));
        ImageIcon.setDirectory(new File(configDirectory, ICON_DIRECTORY));
        HeadIcon.setCacheFile(new File(configDirectory, HEAD_CACHE_FILE));
    }

    public static void init() {
        KeyPresser keyPresser = new KeyPresser();

        KeyBindings.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientTickHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new ConfigChangeHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(keyPresser);
        MinecraftForge.EVENT_BUS.register(keyPresser);
        ClientCommandHandler.instance.registerCommand(new RadialMenuCommand());
    }

    /**
     * Reads menus.json and the icon images again, and looks up player heads again.
     */
    public static void reload() {
        getMenuStore().load();
        ImageIcon.reloadAll();
        HeadIcon.reloadAll();
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
