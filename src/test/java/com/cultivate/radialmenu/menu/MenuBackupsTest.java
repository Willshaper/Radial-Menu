package com.cultivate.radialmenu.menu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuBackupsTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 18, 0, 0);

    @TempDir
    Path folder;
    private File source;
    private MenuBackups backups;

    @BeforeEach
    void setUp() throws IOException {
        source = folder.resolve("menus.json")
            .toFile();
        backups = new MenuBackups(
            folder.resolve("backups")
                .toFile());
        write("first");
    }

    private void write(String content) throws IOException {
        Files.write(source.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(String backupName) throws IOException {
        File file = backups.find(backupName);

        assertNotNull(file);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void namesBackupsAfterTheirTime() throws IOException {
        assertEquals("menus-2026-10-10_18-00-00.json", backups.create(source, START));
        assertEquals("first", read("menus-2026-10-10_18-00-00.json"));
    }

    @Test
    void keepsOnlyTheNewestBackupsNewestFirst() throws IOException {
        int made = MenuBackups.KEPT_BACKUPS + 2;
        List<String> names;

        for (int i = 0; i < made; i++) {
            write("version " + i);
            backups.create(source, START.plusMinutes(i));
        }
        names = backups.list();
        assertEquals(MenuBackups.KEPT_BACKUPS, names.size());
        assertEquals("menus-2026-10-10_18-11-00.json", names.get(0));
        assertEquals("menus-2026-10-10_18-02-00.json", names.get(names.size() - 1));
        assertEquals("version 11", read(names.get(0)));
    }

    @Test
    void sortsByTimeAcrossDays() throws IOException {
        backups.create(source, START.plusDays(1));
        backups.create(source, START);
        assertEquals(
            "menus-2026-10-11_18-00-00.json",
            backups.list()
                .get(0));
    }

    @Test
    void backupInTheSameSecondReplacesTheEarlierOne() throws IOException {
        backups.create(source, START);
        write("second");
        backups.create(source, START);
        assertEquals(
            1,
            backups.list()
                .size());
        assertEquals("second", read("menus-2026-10-10_18-00-00.json"));
    }

    @Test
    void ignoresOtherFilesInTheFolder() throws IOException {
        File unrelated = folder.resolve("backups/notes.txt")
            .toFile();

        backups.create(source, START);
        Files.write(unrelated.toPath(), new byte[] { 1 });
        assertEquals(
            1,
            backups.list()
                .size());
        assertArrayEquals(new byte[] { 1 }, Files.readAllBytes(unrelated.toPath()));
    }

    @Test
    void onlyFindsListedBackups() throws IOException {
        backups.create(source, START);
        assertNull(backups.find("menus-2000-01-01_00-00-00.json"));
        assertNull(backups.find("../menus.json"));
        assertNull(backups.find("notes.txt"));
    }

    @Test
    void listIsEmptyBeforeTheFirstBackup() {
        assertEquals(
            0,
            backups.list()
                .size());
    }
}
