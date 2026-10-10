package com.cultivate.radialmenu.menu;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.cultivate.radialmenu.RadialMenuMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * All menus, loaded from and saved to {@code config/radialmenu/menus.json}.
 *
 * <p>
 * The first save of each game session first copies the file into {@code config/radialmenu/backups}, so the edits made
 * in one session can be undone together.
 */
public class MenuStore {

    private static final String DEFAULT_MENU_ID = "main";
    private static final String DEFAULT_MENU_NAME = "Main";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private static final String BACKUP_DIRECTORY = "backups";

    private final File file;
    private final MenuBackups backups;
    private final List<RadialMenu> menus = new ArrayList<>();
    @Nullable
    private String lastMenuId;
    private boolean backedUpThisSession;

    public MenuStore(File file) {
        this.file = file;
        this.backups = new MenuBackups(new File(file.getParentFile(), BACKUP_DIRECTORY));
    }

    /**
     * Loads the menus from disk. A file that can't be read is renamed to a backup, not overwritten, and the defaults
     * are used instead.
     */
    public void load() {
        menus.clear();
        lastMenuId = null;
        if (file.isFile()) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader)
                    .getAsJsonObject();

                menus.addAll(MenuJson.readMenus(root));
                lastMenuId = MenuJson.readLastMenuId(root);
            } catch (IOException | RuntimeException e) {
                RadialMenuMod.LOG.error("could not read {}, using the default menu", file, e);
                backUpBrokenFile();
                menus.clear();
            }
        }
        if (menus.isEmpty()) {
            menus.add(new RadialMenu(DEFAULT_MENU_ID, DEFAULT_MENU_NAME, RadialMenu.DEFAULT_SECTIONS, true));
            save();
        }
    }

    public void save() {
        File tempFile = new File(file.getPath() + ".tmp");

        if (!backedUpThisSession && file.isFile()) {
            backedUpThisSession = createBackup() != null;
        }
        try {
            Files.createDirectories(
                file.getParentFile()
                    .toPath());
            try (Writer writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(MenuJson.write(menus, lastMenuId), writer);
            }
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            RadialMenuMod.LOG.error("could not save {}", file, e);
        }
    }

    /**
     * Copies menus.json as it is on disk into the backup folder.
     *
     * @return the backup's name, or null if there was nothing to back up or the copy failed
     */
    @Nullable
    public String createBackup() {
        if (!file.isFile()) {
            return null;
        }
        try {
            return backups.create(file, LocalDateTime.now());
        } catch (IOException e) {
            RadialMenuMod.LOG.error("could not back up {}", file, e);
            return null;
        }
    }

    /**
     * @return the names of the backups, newest first
     */
    public List<String> getBackups() {
        return backups.list();
    }

    /**
     * Replaces the menus with a backup. The current file is backed up first, so a restore can be undone the same way.
     *
     * @return false if there is no backup with this name or it could not be copied
     */
    public boolean restoreBackup(String name) {
        File backup = backups.find(name);
        byte[] content;

        if (backup == null) {
            return false;
        }
        try {
            // Read before backing up the current file, since that can delete the oldest backup.
            content = Files.readAllBytes(backup.toPath());
            createBackup();
            Files.write(file.toPath(), content);
        } catch (IOException e) {
            RadialMenuMod.LOG.error("could not restore the menu backup {}", backup, e);
            return false;
        }
        load();
        return true;
    }

    private void backUpBrokenFile() {
        File backup = new File(file.getPath() + ".broken-" + System.currentTimeMillis());

        if (file.renameTo(backup)) {
            RadialMenuMod.LOG.warn("moved the unreadable menu file to {}", backup);
        }
    }

    public List<RadialMenu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    @Nullable
    public RadialMenu getMenu(String id) {
        for (RadialMenu menu : menus) {
            if (menu.getId()
                .equals(id)) {
                return menu;
            }
        }
        return null;
    }

    /**
     * @return the menus the switch key cycles through, or every menu if none is marked for cycling
     */
    public List<RadialMenu> getCycleMenus() {
        List<RadialMenu> cycle = new ArrayList<>();

        for (RadialMenu menu : menus) {
            if (menu.isInCycle()) {
                cycle.add(menu);
            }
        }
        return cycle.isEmpty() ? getMenus() : cycle;
    }

    @Nullable
    public String getLastMenuId() {
        return lastMenuId;
    }

    public void setLastMenuId(String lastMenuId) {
        this.lastMenuId = lastMenuId;
    }

    /**
     * Adds a new empty menu at the end and returns it. The caller saves.
     */
    public RadialMenu createMenu(String name, boolean inCycle) {
        RadialMenu menu = new RadialMenu(createUniqueId(name), name, RadialMenu.DEFAULT_SECTIONS, inCycle);

        menus.add(menu);
        return menu;
    }

    /**
     * Removes a menu. The last remaining menu can't be removed. Submenu sections pointing at it are left alone and show
     * as missing in the editor. The caller saves.
     *
     * @return true if the menu was removed
     */
    public boolean deleteMenu(RadialMenu menu) {
        if (menus.size() <= 1) {
            return false;
        }
        return menus.remove(menu);
    }

    /**
     * Moves a menu up (negative offset) or down (positive offset) in the list, which is also the cycle order. The
     * caller saves.
     */
    public void moveMenu(RadialMenu menu, int offset) {
        int index = menus.indexOf(menu);
        int target = index + offset;

        if ((index < 0) || (target < 0) || (target >= menus.size())) {
            return;
        }
        menus.remove(index);
        menus.add(target, menu);
    }

    private String createUniqueId(String name) {
        String base = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        String id;
        int suffix = 2;

        if (base.isEmpty()) {
            base = "menu";
        }
        id = base;
        while (getMenu(id) != null) {
            id = base + "_" + suffix;
            suffix++;
        }
        return id;
    }
}
