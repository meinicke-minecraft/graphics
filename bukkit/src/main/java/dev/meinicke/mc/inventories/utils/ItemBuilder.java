package dev.meinicke.mc.inventories.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class ItemBuilder {

    // Static initializers

    public static @NotNull ItemBuilder skullFromUrl(@NotNull String url) {
        return new ItemBuilder(SkullCreator.itemFromUrl(url));
    }
    public static @NotNull ItemBuilder skullFromBase64(@NotNull String base64) {
        return new ItemBuilder(SkullCreator.itemFromBase64(base64));
    }
    public static @NotNull ItemBuilder skullFromUuid(@NotNull UUID uuid) {
        return new ItemBuilder(SkullCreator.itemFromUuid(uuid));
    }

    // Object

    private @NotNull Material material;
    private @Nullable ItemMeta meta;

    private int amount = 1;
    private short durability = 0;

    private @NotNull BaseComponent @Nullable [] name = null;
    private @NotNull BaseComponent @Nullable [] lore = null;

    private final @NotNull Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

    public ItemBuilder(@NotNull Material material) {
        this.material = material;
    }
    public ItemBuilder(@NotNull ItemStack itemStack) {
        this.material = itemStack.getType();
        this.amount = itemStack.getAmount();
        this.durability = itemStack.getDurability();

        // Retrieve name and lore
        this.meta = itemStack.getItemMeta();

        if (meta != null) {
            name = meta.getDisplayName() == null ? null : TextComponent.fromLegacyText(meta.getDisplayName());
            lore = meta.getLore() == null ? null : meta.getLore().stream().map(TextComponent::new).toArray(BaseComponent[]::new);
            enchantments.putAll(meta.getEnchants());
        }
    }

    // Getters

    @Contract("_->this")
    public @NotNull ItemBuilder material(@NotNull Material material) {
        this.material = material;
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    @Contract("_->this")
    public @NotNull ItemBuilder name(@Nullable String name) {
        this.name = name != null ? TextComponent.fromLegacyText(name) : null;
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder name(@Nullable TextComponent name) {
        this.name = name != null ? new BaseComponent[] { name } : null;
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder name(@NotNull BaseComponent @Nullable ... name) {
        this.name = name;
        return this;
    }

    @Contract("_->this")
    public @NotNull ItemBuilder lore(@NotNull String @Nullable ... lore) {
        this.lore = lore == null ? null : Arrays.stream(lore).map(TextComponent::new).toArray(BaseComponent[]::new);
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder loreWithString(@Nullable Collection<String> lore) {
        this.lore = lore == null ? null : lore.stream().map(TextComponent::new).toArray(BaseComponent[]::new);
        return this;
    }

    @Contract("_->this")
    public @NotNull ItemBuilder lore(@Nullable BaseComponent lore) {
        this.lore = lore == null ? null : new BaseComponent[] { lore };
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder lore(@NotNull BaseComponent @NotNull ... lore) {
        this.lore = lore;
        return this;
    }
    @Contract("_->this")
    public @NotNull ItemBuilder loreWithComponent(@NotNull Collection<BaseComponent[]> lore) {
        @NotNull BaseComponent[] result = new BaseComponent[lore.stream().mapToInt(c -> c.length).sum() + (lore.size() - 1)];

        int row = 0;
        for (@NotNull BaseComponent[] components : lore) {
            // Add components
            for (@NotNull BaseComponent component : components) {
                result[row] = component;
                row++;
            }

            // Append new line
            components[row++] = new TextComponent("\n");
        }

        this.lore = result;
        return this;
    }

    @Contract("_,_->this")
    public @NotNull ItemBuilder enchantment(@NotNull Enchantment enchantment, int level) {
        this.enchantments.put(enchantment, level);
        return this;
    }

    @Contract("_->this")
    public @NotNull ItemBuilder durability(short damage) {
        this.durability = damage;
        return this;
    }

    @Contract("_->this")
    public @NotNull ItemBuilder meta(@NotNull ItemMeta meta) {
        this.meta = meta;
        return this;
    }

    // Builder

    public @NotNull ItemStack build() {
        @NotNull ItemStack itemStack = new ItemStack(material, amount, durability);
        @Nullable ItemMeta meta = this.meta != null ? this.meta : itemStack.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(getText(name));
            } if (lore != null) {
                meta.setLore(Arrays.stream(lore).map(ItemBuilder::getText).collect(Collectors.toList()));
            }

            for (@NotNull Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                @NotNull Enchantment enchantment = entry.getKey();
                int i = entry.getValue();

                // Add enchantment
                meta.addEnchant(enchantment, i, true);
            }
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    // Utilities

    private static @NotNull String getText(@NotNull BaseComponent... components) {
        @NotNull StringBuilder str = new StringBuilder();

        for (@NotNull BaseComponent component : components) {
            str.append(component.toLegacyText());
        }

        if (str.toString().startsWith("§f")) {
            return str.toString().replaceFirst("§f", "");
        }

        return str.toString();
    }

}
