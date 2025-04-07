package dev.meinicke.mc.inventories;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Collection;

public interface Graphic extends Closeable, Listener {

    // Static initializers

    static void initialize(@NotNull Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new InventoryGraphic.AbstractAction.ListenerImpl(), plugin);
    }

    // Getters

    default @Nullable Graphic recreate(@NotNull HumanEntity human) {
        return null;
    }

    // Modules

    void open(@NotNull HumanEntity @NotNull ... humans);
    void open(@NotNull Collection<HumanEntity> humans);
    void close(@NotNull HumanEntity @NotNull ... humans);
    void close(@NotNull Collection<HumanEntity> humans);

    @Override
    void close();

}
