package com.cultivate.radialmenu.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Copies of menus.json in a folder of their own, named after the time they were made. Only the newest few are kept.
 */
final class MenuBackups {

    static final int KEPT_BACKUPS = 3;

    private static final String PREFIX = "menus-";
    private static final String SUFFIX = ".json";
    // Sorts in time order as plain text, so the newest backup is always last by name.
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final File directory;

    MenuBackups(File directory) {
        this.directory = directory;
    }

    /**
     * Copies {@code source} into the backup folder, then deletes all but the newest {@link #KEPT_BACKUPS} backups. A
     * backup made in the same second as an earlier one replaces it.
     *
     * @return the backup's name
     */
    String create(File source, LocalDateTime time) throws IOException {
        String name = PREFIX + TIME_FORMAT.format(time) + SUFFIX;
        List<String> names;

        Files.createDirectories(directory.toPath());
        Files.copy(source.toPath(), new File(directory, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
        names = list();
        for (String old : names.subList(Math.min(KEPT_BACKUPS, names.size()), names.size())) {
            Files.deleteIfExists(new File(directory, old).toPath());
        }
        return name;
    }

    /**
     * @return the backup names, newest first
     */
    List<String> list() {
        String[] files = directory.list((dir, name) -> name.startsWith(PREFIX) && name.endsWith(SUFFIX));
        List<String> names = files == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(files));

        names.sort(Collections.reverseOrder());
        return names;
    }

    /**
     * @return the backup file with this name, or null if there is none. Only names from {@link #list()} are accepted,
     *         so a name can't point outside the backup folder.
     */
    @Nullable
    File find(String name) {
        return list().contains(name) ? new File(directory, name) : null;
    }
}
