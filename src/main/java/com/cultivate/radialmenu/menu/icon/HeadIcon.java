package com.cultivate.radialmenu.menu.icon;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.cultivate.radialmenu.RadialMenuMod;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

/**
 * A player's face, from their skin.
 *
 * <p>
 * If the player is in the world, their loaded skin is used. Otherwise the skin is looked up by name once, in the
 * background, through the same Mojang services the game uses for skins, and kept until {@link #reloadAll()}. Until
 * then, and for names Mojang doesn't know (such as players on offline-mode servers who aren't nearby), Steve's face is
 * drawn.
 */
public class HeadIcon implements SectionIcon {

    // The face and the hat layer over it, in pixels on a 64 x 32 skin.
    private static final float SKIN_WIDTH = 64f;
    private static final float SKIN_HEIGHT = 32f;
    private static final int FACE_SIZE = 8;
    private static final int FACE_X = 8;
    private static final int FACE_Y = 8;
    private static final int HAT_X = 40;
    private static final int WHITE = 0xFFFFFF;

    private static final Map<String, ResourceLocation> SKINS = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final ExecutorService LOOKUPS = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Radial Menu skin lookup");

        thread.setDaemon(true);
        return thread;
    });

    private final String playerName;

    public HeadIcon(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * Forgets every looked-up skin, so the next draw looks them up again.
     */
    public static void reloadAll() {
        SKINS.clear();
        REQUESTED.clear();
    }

    @Override
    public void draw(float centerX, float centerY, float size) {
        IconTextures.bind(findSkin(playerName));
        drawLayer(FACE_X, centerX, centerY, size);
        drawLayer(HAT_X, centerX, centerY, size);
    }

    private static void drawLayer(int skinX, float centerX, float centerY, float size) {
        IconTextures.drawBound(
            centerX,
            centerY,
            size,
            skinX / SKIN_WIDTH,
            FACE_Y / SKIN_HEIGHT,
            (skinX + FACE_SIZE) / SKIN_WIDTH,
            (FACE_Y + FACE_SIZE) / SKIN_HEIGHT,
            WHITE);
    }

    private static ResourceLocation findSkin(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer loaded = mc.theWorld == null ? null : mc.theWorld.getPlayerEntityByName(name);
        String key = name.toLowerCase(Locale.ROOT);
        ResourceLocation skin;

        if (loaded instanceof AbstractClientPlayer player) {
            return player.getLocationSkin();
        }
        skin = SKINS.get(key);
        if (skin != null) {
            return skin;
        }
        if (!name.isEmpty() && REQUESTED.add(key)) {
            LOOKUPS.execute(() -> lookUpSkin(name, key));
        }
        return SkinManager.field_152793_a;
    }

    /**
     * Finds the player's profile by name, then their skin, then loads the skin on the client thread. Runs in the
     * background; a name that can't be found keeps Steve's face.
     */
    private static void lookUpSkin(String name, String key) {
        Minecraft mc = Minecraft.getMinecraft();
        MinecraftSessionService sessions = mc.func_152347_ac();
        GameProfile profile;
        MinecraftProfileTexture skin;

        try {
            profile = findProfile(mc, name);
            if (profile == null) {
                return;
            }
            skin = sessions.getTextures(sessions.fillProfileProperties(profile, false), false)
                .get(MinecraftProfileTexture.Type.SKIN);
        } catch (RuntimeException e) {
            RadialMenuMod.LOG.warn("could not look up the skin of {}", name, e);
            return;
        }
        if (skin != null) {
            mc.func_152344_a(
                () -> SKINS.put(
                    key,
                    mc.func_152342_ad()
                        .func_152792_a(skin, MinecraftProfileTexture.Type.SKIN)));
        }
    }

    @Nullable
    private static GameProfile findProfile(Minecraft mc, String name) {
        GameProfile[] found = new GameProfile[1];

        new YggdrasilAuthenticationService(
            mc.getProxy(),
            UUID.randomUUID()
                .toString()).createProfileRepository()
                    .findProfilesByNames(new String[] { name }, Agent.MINECRAFT, new ProfileLookupCallback() {

                        @Override
                        public void onProfileLookupSucceeded(GameProfile profile) {
                            found[0] = profile;
                        }

                        @Override
                        public void onProfileLookupFailed(GameProfile profile, Exception exception) {}
                    });
        return found[0];
    }
}
