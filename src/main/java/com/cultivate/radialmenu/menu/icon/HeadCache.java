package com.cultivate.radialmenu.menu.icon;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.cultivate.radialmenu.RadialMenuMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The skin address of each player a head was looked up for, kept in {@code config/radialmenu/heads.json} so heads show
 * at once on the next launch instead of after asking Mojang again. The skin images themselves are cached by the game.
 *
 * <p>
 * Each entry remembers when it was last checked, so it can be looked up again now and then to catch skin changes.
 */
final class HeadCache {

    // How long an entry is trusted before it is looked up again in the background.
    static final long REFRESH_MILLIS = 24L * 60 * 60 * 1000;

    private static final String KEY_URL = "skin";
    private static final String KEY_CHECKED = "checked";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private final File file;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private boolean loaded;

    /**
     * A player's skin address and when it was last confirmed.
     */
    static final class Entry {

        final String skinUrl;
        final long checkedAt;

        Entry(String skinUrl, long checkedAt) {
            this.skinUrl = skinUrl;
            this.checkedAt = checkedAt;
        }

        boolean isStale(long now) {
            return (now - checkedAt) >= REFRESH_MILLIS;
        }
    }

    HeadCache(File file) {
        this.file = file;
    }

    @Nullable
    Entry get(String playerName) {
        load();
        return entries.get(key(playerName));
    }

    /**
     * Stores a player's skin address, checked now, and writes the file.
     */
    void put(String playerName, String skinUrl, long now) {
        load();
        entries.put(key(playerName), new Entry(skinUrl, now));
        save();
    }

    /**
     * Marks every entry as due for a check, without forgetting it, so heads still show while they are looked up again.
     */
    void markAllStale() {
        load();
        entries.replaceAll((name, entry) -> new Entry(entry.skinUrl, 0));
    }

    private static String key(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!file.isFile()) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();

            for (Map.Entry<String, JsonElement> player : root.entrySet()) {
                JsonObject json = player.getValue()
                    .getAsJsonObject();

                entries.put(
                    key(player.getKey()),
                    new Entry(
                        json.get(KEY_URL)
                            .getAsString(),
                        json.has(KEY_CHECKED) ? json.get(KEY_CHECKED)
                            .getAsLong() : 0));
            }
        } catch (IOException | RuntimeException e) {
            // Only a cache: losing it means the heads are looked up again.
            RadialMenuMod.LOG.warn("could not read the head cache {}, starting a new one", file, e);
            entries.clear();
        }
    }

    private void save() {
        JsonObject root = new JsonObject();
        File tempFile = new File(file.getPath() + ".tmp");

        entries.forEach((name, entry) -> {
            JsonObject json = new JsonObject();

            json.addProperty(KEY_URL, entry.skinUrl);
            json.addProperty(KEY_CHECKED, entry.checkedAt);
            root.add(name, json);
        });
        try {
            Files.createDirectories(
                file.getParentFile()
                    .toPath());
            try (Writer writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            RadialMenuMod.LOG.warn("could not save the head cache {}", file, e);
        }
    }
}
