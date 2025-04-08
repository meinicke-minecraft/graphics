package dev.meinicke.mc.graphics.utils;

// Copyright (c) 2017 deanveloper (see LICENSE.md for more info)

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * A library for the Bukkit API to create player skulls
 * from names, base64 strings, and texture URLs.
 * <p>
 * Does not use any NMS code, and should work across all versions.
 *
 * @author deanveloper on 12/28/2016.
 */
public class SkullCreator {

    private SkullCreator() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

    // some reflection stuff to be used when setting a skull's profile
    private static @Nullable Field blockProfileField;
    private static @Nullable Method metaSetProfileMethod;
    private static @Nullable Field metaProfileField;

    /**
     * Creates a player skull, should work in both legacy and new Bukkit APIs.
     */
    private static @NotNull ItemStack createSkull() {
        checkLegacy();

        try {
            return new ItemStack(Material.valueOf("PLAYER_HEAD"));
        } catch (@NotNull IllegalArgumentException e) {
            return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
        }
    }

    /**
     * Creates a player skull item with the skin based on a player's name.
     *
     * @param name The Player's name.
     * @return The head of the Player.
     * @deprecated names don't make for good identifiers.
     */
    public static @NotNull ItemStack itemFromName(@NotNull String name) {
        return Objects.requireNonNull(itemWithName(createSkull(), name));
    }

    /**
     * Creates a player skull item with the skin based on a player's UUID.
     *
     * @param id The Player's UUID.
     * @return The head of the Player.
     */
    public static @NotNull ItemStack itemFromUuid(@NotNull UUID id) {
        return Objects.requireNonNull(itemWithUuid(createSkull(), id));
    }

    /**
     * Creates a player skull item with the skin at a Mojang URL.
     *
     * @param url The Mojang URL.
     * @return The head of the Player.
     */
    public static @NotNull ItemStack itemFromUrl(@NotNull String url) {
        return Objects.requireNonNull(itemWithUrl(createSkull(), url));
    }

    /**
     * Creates a player skull item with the skin based on a base64 string.
     *
     * @param base64 The Base64 string.
     * @return The head of the Player.
     */
    public static @NotNull ItemStack itemFromBase64(@NotNull String base64) {
        return Objects.requireNonNull(itemWithBase64(createSkull(), base64));
    }

    /**
     * Modifies a skull to use the skin of the player with a given name.
     *
     * @param item The item to apply the name to. Must be a player skull.
     * @param name The Player's name.
     * @return The head of the Player.
     * @deprecated names don't make for good identifiers.
     */
    @Deprecated
    public static @Nullable ItemStack itemWithName(@NotNull ItemStack item, @NotNull String name) {
        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return null;
        }

        @NotNull SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(name);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Modifies a skull to use the skin of the player with a given UUID.
     *
     * @param item The item to apply the name to. Must be a player skull.
     * @param id   The Player's UUID.
     * @return The head of the Player.
     */
    public static @Nullable ItemStack itemWithUuid(@NotNull ItemStack item, @NotNull UUID id) {
        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return null;
        }

        @NotNull SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(Bukkit.getOfflinePlayer(id).getName());
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Modifies a skull to use the skin at the given Mojang URL.
     *
     * @param item The item to apply the skin to. Must be a player skull.
     * @param url  The URL of the Mojang skin.
     * @return The head associated with the URL.
     */
    public static @Nullable ItemStack itemWithUrl(@NotNull ItemStack item, @NotNull String url) {
        return itemWithBase64(item, urlToBase64(url));
    }

    /**
     * Modifies a skull to use the skin based on the given base64 string.
     *
     * @param item   The ItemStack to put the base64 onto. Must be a player skull.
     * @param base64 The base64 string containing the texture.
     * @return The head with a custom texture.
     */
    public static @Nullable ItemStack itemWithBase64(@NotNull ItemStack item, @NotNull String base64) {
        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return null;
        }

        @NotNull SkullMeta meta = (SkullMeta) item.getItemMeta();
        mutateItemMeta(meta, base64);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Sets the block to a skull with the given name.
     *
     * @param block The block to set.
     * @param name  The player to set it to.
     * @deprecated names don't make for good identifiers.
     */
    @Deprecated
    public static void blockWithName(@NotNull Block block, @NotNull String name) {
        // Change block skull state
        @NotNull Skull state = (Skull) block.getState();
        state.setOwner(name);
        state.update(false, false);
    }

    /**
     * Sets the block to a skull with the given UUID.
     *
     * @param block The block to set.
     * @param id    The player to set it to.
     */
    public static void blockWithUuid(@NotNull Block block, @NotNull UUID id) {
        // Set block as a valid skull item
        setToSkull(block);

        // Change skull state
        @NotNull Skull state = (Skull) block.getState();
        state.setOwner(Bukkit.getOfflinePlayer(id).getName());
        state.update(false, false);
    }

    /**
     * Sets the block to a skull with the skin found at the provided mojang URL.
     *
     * @param block The block to set.
     * @param url   The mojang URL to set it to use.
     */
    public static void blockWithUrl(@NotNull Block block, @NotNull String url) {
        blockWithBase64(block, urlToBase64(url));
    }

    /**
     * Sets the block to a skull with the skin for the base64 string.
     *
     * @param block  The block to set.
     * @param base64 The base64 to set it to use.
     */
    public static void blockWithBase64(@NotNull Block block, @NotNull String base64) {
        setToSkull(block);
        @NotNull Skull state = (Skull) block.getState();
        mutateBlockState(state, base64);
        state.update(false, false);
    }

    private static void setToSkull(@NotNull Block block) {
        checkLegacy();

        try {
            block.setType(Material.valueOf("PLAYER_HEAD"), false);
        } catch (IllegalArgumentException e) {
            block.setType(Material.valueOf("SKULL"), false);

            @NotNull Skull state = (Skull) block.getState();
            state.setSkullType(SkullType.PLAYER);
            state.update(false, false);
        }
    }

    private static @NotNull String urlToBase64(@NotNull String url) {
        @NotNull URI actualUrl;
        
        try {
            actualUrl = new URI(url);
        } catch (@NotNull URISyntaxException e) {
            throw new RuntimeException("invalid skull url: " + url, e);
        }
        
        @NotNull String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    private static @NotNull GameProfile makeProfile(@NotNull String base64) {
        // random uuid based on the base64 string
        @NotNull UUID uuid = new UUID(
                base64.substring(base64.length() - 20).hashCode(),
                base64.substring(base64.length() - 10).hashCode()
        );
        @NotNull GameProfile profile = new GameProfile(uuid, "Player");

        profile.getProperties().put("textures", new Property("textures", base64));
        return profile;
    }

    private static void mutateBlockState(@NotNull Skull block, @NotNull String base64) {
        try {
            if (blockProfileField == null) {
                blockProfileField = block.getClass().getDeclaredField("profile");
                blockProfileField.setAccessible(true);
            }

            blockProfileField.set(block, makeProfile(base64));
        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot find profile field at skull block class", e);
        }
    }

    private static void mutateItemMeta(@NotNull SkullMeta meta, @NotNull String base64) {
        try {
            if (metaSetProfileMethod == null) {
                metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                metaSetProfileMethod.setAccessible(true);
            }

            metaSetProfileMethod.invoke(meta, makeProfile(base64));
        } catch (@NotNull NoSuchMethodException | @NotNull IllegalAccessException | @NotNull InvocationTargetException ex) {
            // if in an older API where there is no setProfile method,
            // we set the profile field directly.
            try {
                if (metaProfileField == null) {
                    metaProfileField = meta.getClass().getDeclaredField("profile");
                    metaProfileField.setAccessible(true);
                }

                @NotNull Object object = makeProfile(base64);

                if (metaProfileField.getType().getName().equals("net.minecraft.world.item.component.ResolvableProfile")) {
                    try {
                        // Variables
                        @NotNull GameProfile profile = (GameProfile) object;
                        @NotNull Constructor<?> constructor = metaProfileField.getType().getDeclaredConstructor(GameProfile.class);
                        constructor.setAccessible(true);

                        object = constructor.newInstance(profile);

                        // Change profile field
                        metaProfileField.set(meta, object);
                    } catch (@NotNull InvocationTargetException e) {
                        throw new RuntimeException("cannot invoke " + metaProfileField.getType().getName() + " constructor", e);
                    } catch (@NotNull NoSuchMethodException e) {
                        throw new RuntimeException("cannot find " + metaProfileField.getType().getName() + " constructor", e);
                    } catch (@NotNull InstantiationException e) {
                        throw new RuntimeException("cannot generate " + metaProfileField.getType().getName() + " instance", e);
                    }
                } else {
                    metaProfileField.set(meta, object);
                }
            } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException ex2) {
                throw new RuntimeException("cannot find profile field at skull meta class", ex2);
            }
        }
    }

    // suppress warning since PLAYER_HEAD doesn't exist in 1.12.2,
    // but we expect this and catch the error at runtime.
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void checkLegacy() {
        try {
            // if both of these succeed, then we are running
            // in a legacy api, but on a modern (1.13+) server.
            Material.class.getDeclaredField("PLAYER_HEAD");
            Material.valueOf("SKULL");
        } catch (@NotNull NoSuchFieldException | @NotNull IllegalArgumentException ignored) {}
    }
}