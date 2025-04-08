package dev.meinicke.mc.graphics.paged;

import dev.meinicke.mc.graphics.InventoryGraphic.AbstractAction;
import dev.meinicke.mc.graphics.InventoryGraphic.Action;
import dev.meinicke.mc.graphics.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class BlankItem {

    // Static initializers

    private static final @NotNull Material DEFAULT_MATERIAL;

    static {
        @NotNull Material material;
        try {
            material = Material.valueOf("COBWEB");
        } catch (@NotNull IllegalArgumentException ignore) {
            material = Material.WEB;
        }

        DEFAULT_MATERIAL = material;
    }

    public static @NotNull BlankItem create(
            @Range(from = 0, to = 53) int slot
    ) {
        return new BlankItem(slot);
    }
    public static @NotNull BlankItem create(
            @Range(from = 0, to = 53) int slot,
            @NotNull ItemStack itemStack
    ) {
        return new BlankItem(slot, itemStack);
    }

    // Object

    @Range(from = 0, to = 53)
    private final int slot;

    private final @NotNull ItemStack itemStack;
    private final @NotNull Collection<@NotNull Action<?>> actions = new LinkedList<>();

    public BlankItem(
            @Range(from = 0, to = 53)
            int slot
    ) {
        this(slot, new ItemBuilder(DEFAULT_MATERIAL).name("§cEmpty!").lore("§7There's nothing here to explore.").build());
    }
    public BlankItem(
            @Range(from = 0, to = 53)
            int slot,

            @NotNull ItemStack itemStack
    ) {
        this.slot = slot;
        this.itemStack = itemStack;

        // Add default action
        this.actions.add(new AbstractAction<InventoryClickEvent>(InventoryClickEvent.class) {
            @Override
            public void accept(@NotNull InventoryClickEvent e) {
                e.setCancelled(true);
                e.getWhoClicked().sendMessage("§c✘ §7There's nothing here to explore, noob!");
            }
        });
    }

    // Getters

    @Range(from = 0, to = 53)
    public int getSlot() {
        return slot;
    }
    public @NotNull ItemStack getItemStack() {
        return itemStack;
    }

    public @NotNull Collection<Action<?>> getActions() {
        return actions;
    }

    // Natives

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof BlankItem)) return false;
        @NotNull BlankItem that = (BlankItem) object;
        return getSlot() == that.getSlot() && Objects.equals(getItemStack(), that.getItemStack());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getSlot(), getItemStack());
    }

    @Override
    public @NotNull String toString() {
        return "BlankItem{" +
                "slot=" + slot +
                ", itemStack=" + itemStack +
                '}';
    }

}
