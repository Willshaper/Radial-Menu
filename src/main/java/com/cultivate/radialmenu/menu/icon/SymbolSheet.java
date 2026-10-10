package com.cultivate.radialmenu.menu.icon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.cultivate.radialmenu.RadialMenuMod;

/**
 * The bundled symbols: the Phosphor icon set (MIT license, see {@code symbols/LICENSE-phosphor.txt}), drawn white on
 * one sheet so they can be tinted any color.
 *
 * <p>
 * {@code symbols/symbols.txt} has one line per symbol, in sheet order: the name, then search words. The sheet and the
 * list are made by {@code tools/SymbolSheet.java}.
 */
public final class SymbolSheet {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        RadialMenuMod.MODID,
        "textures/gui/symbols.png");
    private static final String INDEX_PATH = "/assets/" + RadialMenuMod.MODID + "/symbols/symbols.txt";
    // Must match the sheet: square cells of this many pixels, filling rows of this width.
    private static final int CELL = 48;
    private static final int SHEET_WIDTH = 2016;
    private static final int SHEET_HEIGHT = 2048;
    private static final int COLUMNS = SHEET_WIDTH / CELL;

    private static final Map<String, Symbol> BY_NAME = new HashMap<>();
    @Nullable
    private static List<Symbol> symbols;

    /**
     * One symbol: its name, the words it is found by, and its place on the sheet.
     */
    public static final class Symbol {

        private final String name;
        private final String searchText;
        private final int index;

        private Symbol(String name, String searchText, int index) {
            this.name = name;
            this.searchText = searchText;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        /**
         * @return the name and the search words, separated by spaces
         */
        public String getSearchText() {
            return searchText;
        }
    }

    private SymbolSheet() {}

    /**
     * @return every symbol, in sheet order
     */
    public static List<Symbol> getSymbols() {
        load();
        return symbols;
    }

    @Nullable
    public static Symbol find(String name) {
        load();
        return BY_NAME.get(name);
    }

    static void draw(Symbol symbol, float centerX, float centerY, float size, int rgb) {
        float minU = ((symbol.index % COLUMNS) * CELL) / (float) SHEET_WIDTH;
        float minV = ((symbol.index / COLUMNS) * CELL) / (float) SHEET_HEIGHT;

        IconTextures.bindSmoothed(TEXTURE);
        IconTextures.drawBound(
            centerX,
            centerY,
            size,
            minU,
            minV,
            minU + (CELL / (float) SHEET_WIDTH),
            minV + (CELL / (float) SHEET_HEIGHT),
            rgb);
    }

    private static void load() {
        List<Symbol> loaded = new ArrayList<>();
        InputStream stream;
        String line;

        if (symbols != null) {
            return;
        }
        symbols = Collections.unmodifiableList(loaded);
        stream = SymbolSheet.class.getResourceAsStream(INDEX_PATH);
        if (stream == null) {
            RadialMenuMod.LOG.error("the symbol list {} is missing", INDEX_PATH);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                int nameEnd = line.indexOf(' ');
                String name = nameEnd < 0 ? line : line.substring(0, nameEnd);
                Symbol symbol = new Symbol(name, line, loaded.size());

                loaded.add(symbol);
                BY_NAME.put(name, symbol);
            }
        } catch (IOException e) {
            RadialMenuMod.LOG.error("could not read the symbol list", e);
        }
    }
}
