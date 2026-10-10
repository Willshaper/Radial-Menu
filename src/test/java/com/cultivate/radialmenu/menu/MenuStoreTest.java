package com.cultivate.radialmenu.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuStoreTest {

    @TempDir
    Path folder;
    private File file;

    @BeforeEach
    void setUp() {
        file = folder.resolve("radialmenu/menus.json")
            .toFile();
    }

    private MenuStore loadStore() {
        MenuStore store = new MenuStore(file);

        store.load();
        return store;
    }

    @Test
    void createsADefaultMenuWhenThereIsNoFile() {
        MenuStore store = loadStore();

        assertTrue(file.isFile());
        assertEquals(
            1,
            store.getMenus()
                .size());
        assertEquals(
            0,
            store.getBackups()
                .size());
    }

    @Test
    void savedMenusLoadAgain() {
        MenuStore store = loadStore();

        store.createMenu("Tools", false);
        store.setLastMenuId("tools");
        store.save();
        store = loadStore();

        assertEquals(
            2,
            store.getMenus()
                .size());
        assertEquals(
            "Tools",
            store.getMenu("tools")
                .getName());
        assertEquals("tools", store.getLastMenuId());
    }

    @Test
    void backsUpOnlyOnTheFirstSaveOfASession() {
        MenuStore store = loadStore();

        store.save();
        store.save();
        assertEquals(
            1,
            store.getBackups()
                .size());
    }

    @Test
    void restoringABackupBringsBackItsMenus() throws IOException {
        MenuStore store = loadStore();
        String backup;

        store.createMenu("Tools", true);
        store.save();
        backup = store.createBackup();
        store.deleteMenu(store.getMenu("tools"));
        store.save();
        assertNull(store.getMenu("tools"));

        assertTrue(store.restoreBackup(backup));
        assertEquals(
            "Tools",
            store.getMenu("tools")
                .getName());
        assertTrue(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains("\"Tools\""));
    }

    @Test
    void restoringAnUnknownBackupChangesNothing() {
        MenuStore store = loadStore();

        assertFalse(store.restoreBackup("menus-2000-01-01_00-00-00.json"));
        assertEquals(
            1,
            store.getMenus()
                .size());
    }

    @Test
    void unreadableFileIsSetAsideNotOverwritten() throws IOException {
        MenuStore store;
        File[] setAside;

        Files.createDirectories(
            file.getParentFile()
                .toPath());
        Files.write(file.toPath(), "{ not json".getBytes(StandardCharsets.UTF_8));
        store = loadStore();
        setAside = file.getParentFile()
            .listFiles((dir, name) -> name.startsWith("menus.json.broken-"));

        assertEquals(
            1,
            store.getMenus()
                .size());
        assertEquals(1, setAside.length);
        assertEquals("{ not json", new String(Files.readAllBytes(setAside[0].toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void newMenusGetUniqueIdsFromTheirNames() {
        MenuStore store = loadStore();
        RadialMenu first = store.createMenu("My Tools!", true);
        RadialMenu second = store.createMenu("My Tools!", true);

        assertEquals("my_tools", first.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void cycleFallsBackToEveryMenuWhenNoneIsSwitchable() {
        MenuStore store = loadStore();

        store.getMenus()
            .get(0)
            .setInCycle(false);
        store.createMenu("Hidden", false);
        assertEquals(
            2,
            store.getCycleMenus()
                .size());
    }
}
