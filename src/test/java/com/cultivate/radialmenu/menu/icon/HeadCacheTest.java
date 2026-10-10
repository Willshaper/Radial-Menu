package com.cultivate.radialmenu.menu.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class HeadCacheTest {

    private static final String SKIN_URL = "http://textures.minecraft.net/texture/abc123";
    private static final long NOW = 1_000_000_000_000L;

    @TempDir
    Path folder;
    private File file;

    @BeforeEach
    void setUp() {
        file = folder.resolve("radialmenu/heads.json")
            .toFile();
    }

    @Test
    void keepsSkinsAcrossLaunches() {
        new HeadCache(file).put("Steve", SKIN_URL, NOW);
        HeadCache.Entry entry = new HeadCache(file).get("Steve");

        assertNotNull(entry);
        assertEquals(SKIN_URL, entry.skinUrl);
        assertEquals(NOW, entry.checkedAt);
    }

    @Test
    void namesAreCaseInsensitive() {
        HeadCache cache = new HeadCache(file);

        cache.put("Steve", SKIN_URL, NOW);
        assertNotNull(cache.get("STEVE"));
    }

    @Test
    void unknownPlayersAreNotCached() {
        assertNull(new HeadCache(file).get("Alex"));
    }

    @Test
    void entriesGoStaleAfterADay() {
        HeadCache.Entry entry = new HeadCache.Entry(SKIN_URL, NOW);

        assertFalse(entry.isStale(NOW + HeadCache.REFRESH_MILLIS - 1));
        assertTrue(entry.isStale(NOW + HeadCache.REFRESH_MILLIS));
    }

    @Test
    void markingStaleKeepsTheSkin() {
        HeadCache cache = new HeadCache(file);

        cache.put("Steve", SKIN_URL, NOW);
        cache.markAllStale();
        assertEquals(SKIN_URL, cache.get("Steve").skinUrl);
        assertTrue(
            cache.get("Steve")
                .isStale(NOW));
    }

    @Test
    void unreadableFileStartsAnEmptyCache() throws IOException {
        Files.createDirectories(
            file.getParentFile()
                .toPath());
        Files.write(file.toPath(), "not json".getBytes(StandardCharsets.UTF_8));

        assertNull(new HeadCache(file).get("Steve"));
    }
}
