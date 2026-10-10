package com.cultivate.radialmenu.menu.icon;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import com.cultivate.radialmenu.RadialMenuMod;

/**
 * A PNG image of the player's own, from {@code config/radialmenu/icons}.
 *
 * <p>
 * Images are loaded the first time they are drawn and kept until {@link #reloadAll()}. A missing or unreadable file
 * draws as a question mark.
 */
public class ImageIcon implements SectionIcon {

    // Larger images are refused: an icon is drawn at most a few dozen pixels wide, and each image is a GPU texture.
    private static final int MAX_IMAGE_SIZE = 512;
    private static final String EXTENSION = ".png";
    private static final String MISSING_TEXT = "?";
    private static final int MISSING_COLOR = 0xFF5555;
    private static final int WHITE = 0xFFFFFF;

    // File name to loaded image, or empty if the file could not be loaded.
    private static final Map<String, Optional<LoadedImage>> IMAGES = new HashMap<>();
    @Nullable
    private static File directory;

    private final String fileName;

    /**
     * An image as a texture, with its size so it can be drawn without stretching.
     */
    private static final class LoadedImage {

        private final ResourceLocation texture;
        private final int width;
        private final int height;

        private LoadedImage(ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    public ImageIcon(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the folder images are read from. Called once while the mod loads.
     */
    public static void setDirectory(File iconDirectory) {
        directory = iconDirectory;
    }

    /**
     * @return the folder images are read from, created if it doesn't exist yet
     */
    @Nullable
    public static File getDirectory() {
        if ((directory != null) && !directory.isDirectory() && !directory.mkdirs()) {
            RadialMenuMod.LOG.warn("could not create the icon folder {}", directory);
        }
        return directory;
    }

    /**
     * @return the names of the PNG files in the icon folder, sorted
     */
    public static List<String> listFiles() {
        File folder = getDirectory();
        String[] names = folder == null ? null
            : folder.list(
                (dir, name) -> name.toLowerCase(Locale.ROOT)
                    .endsWith(EXTENSION));
        List<String> files = names == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(names));

        files.sort(String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    /**
     * Forgets every loaded image, so changed files are read again the next time they are drawn.
     */
    public static void reloadAll() {
        TextureManager textureManager = Minecraft.getMinecraft()
            .getTextureManager();

        IMAGES.values()
            .forEach(image -> image.ifPresent(loaded -> textureManager.deleteTexture(loaded.texture)));
        IMAGES.clear();
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        Optional<LoadedImage> image = IMAGES.computeIfAbsent(fileName, ImageIcon::load);
        float scale;

        if (!image.isPresent()) {
            TextIcon.drawCenteredText(MISSING_TEXT, MISSING_COLOR, centerX, centerY, size);
            return;
        }
        // Fits the longer side to the icon size.
        scale = size / Math.max(image.get().width, image.get().height);
        IconTextures.bindSmoothed(image.get().texture);
        IconTextures
            .drawBound(centerX, centerY, image.get().width * scale, image.get().height * scale, 0, 0, 1, 1, WHITE);
    }

    private static Optional<LoadedImage> load(String fileName) {
        File folder = getDirectory();
        File file = folder == null ? null : new File(folder, fileName);
        BufferedImage image;

        // Names come from menus.json, which can be edited by hand; only files directly in the icon folder are read.
        if ((file == null) || !file.getName()
            .equals(fileName) || !file.isFile()) {
            return Optional.empty();
        }
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            RadialMenuMod.LOG.warn("could not read the icon image {}", file, e);
            return Optional.empty();
        }
        if (image == null) {
            RadialMenuMod.LOG.warn("{} is not an image", file);
            return Optional.empty();
        }
        if ((image.getWidth() > MAX_IMAGE_SIZE) || (image.getHeight() > MAX_IMAGE_SIZE)) {
            RadialMenuMod.LOG.warn("{} is larger than {} pixels, so it is not used", file, MAX_IMAGE_SIZE);
            return Optional.empty();
        }
        return Optional.of(
            new LoadedImage(
                Minecraft.getMinecraft()
                    .getTextureManager()
                    .getDynamicTextureLocation(RadialMenuMod.MODID + "_icon", new DynamicTexture(image)),
                image.getWidth(),
                image.getHeight()));
    }
}
