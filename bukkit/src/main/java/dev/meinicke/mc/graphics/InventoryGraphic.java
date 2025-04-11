package dev.meinicke.mc.graphics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class InventoryGraphic implements Graphic {

    // Object

    private final @NotNull Plugin plugin;
    private @NotNull Listener listener = new ListenerImpl();

    private @NotNull Inventory handle;
    private @Nullable String title;

    private final @NotNull Map<@NotNull Integer, @NotNull Collection<@NotNull Action<?>>> actions = new HashMap<>();
    private volatile boolean closed = false;
    private volatile boolean activeListener = false;
    private volatile boolean listening = true;

    public InventoryGraphic(@NotNull Plugin plugin, @Nullable String title, @NotNull Rows rows) {
        this(plugin, title, rows.getSlots());
    }
    public InventoryGraphic(@NotNull Plugin plugin, @Nullable String title, int size) {
        this.plugin = plugin;
        this.handle = Bukkit.createInventory(null, size, title);
        this.title = title;
    }

    // Getters

    public final @NotNull Plugin getPlugin() {
        return plugin;
    }
    protected @NotNull Inventory getHandle() {
        return handle;
    }

    public @Nullable String getTitle() {
        return title;
    }
    public void setTitle(@Nullable String title) {
        // Verify if the title is the same
        if (Objects.equals(title, getTitle())) {
            return;
        }

        // Verify if it's on primary thread
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("this method should be called synchronously");
        }

        // Clone inventory
        @NotNull Inventory old = getHandle();
        @NotNull Inventory newly = Bukkit.createInventory(getHandle().getHolder(), getHandle().getSize(), title);

        for (int slot = 0; slot < getHandle().getSize(); slot++) {
            newly.setItem(slot, getHandle().getItem(slot));
        }

        // Open inventory to all viewers
        this.listening = false;
        for (@NotNull HumanEntity human : new ArrayList<>(old.getViewers())) {
            human.openInventory(newly);
        }

        // Change inventory handler
        this.listening = true;
        this.handle = newly;
        this.title = title;
    }

    public final @NotNull Rows getRows() {
        return Rows.getBySlots(getHandle().getSize());
    }
    public final int getSize() {
        return getRows().getSlots();
    }

    public void setSize(int size) {
        // Verify if the size is the same
        if (size == getSize()) {
            return;
        }

        // Verify if it's on primary thread
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("this method should be called synchronously");
        }

        // Clone inventory
        @NotNull Inventory old = getHandle();
        @NotNull Inventory newly = Bukkit.createInventory(getHandle().getHolder(), size, getTitle());

        for (int slot = 0; slot < Math.min(size, getHandle().getSize()); slot++) {
            newly.setItem(slot, getHandle().getItem(slot));
        }

        // Open inventory to all viewers
        this.listening = false;
        for (@NotNull HumanEntity human : new ArrayList<>(old.getViewers())) {
            human.openInventory(newly);
        }

        // Change inventory handler
        this.listening = true;
        this.handle = newly;
    }
    public void setSize(@NotNull Rows rows) {
        setSize(rows.getSlots());
    }

    // Actions

    public @NotNull Map<@Nullable Integer, @NotNull Collection<@NotNull Action<?>>> getActions() {
        return actions;
    }
    public @NotNull Collection<@NotNull Action<?>> getActions(@Nullable Integer slot) {
        return getActions().computeIfAbsent(slot, k -> new LinkedHashSet<>());
    }

    protected final void setListener(@NotNull Listener listener) {
        // Unregister old one
        HandlerList.unregisterAll(listener);

        // Change it and register if possible
        this.listener = listener;

        if (activeListener) {
            Bukkit.getPluginManager().registerEvents(listener, getPlugin());
        }
    }

    // Items

    public @Nullable ItemStack getItem(int slot) {
        // Variables
        @Nullable ItemStack item = getHandle().getItem(slot);

        // Check if it's air or null
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // Finish
        return item;
    }

    public <T extends InventoryEvent> void setItem(@Nullable ItemStack item, @Nullable Action<T> action, int @NotNull ... slots) {
        for (int slot : slots) {
            getHandle().setItem(slot, item != null ? item : new ItemStack(Material.AIR));

            if (action != null) {
                getActions().computeIfAbsent(slot, k -> new LinkedHashSet<>()).add(action);
            }
        }
    }
    public void setItem(@NotNull ItemStack item, int @NotNull ... slots) {
        setItem(item, null, slots);
    }

    // Helpers

    public void cancelOnClick() {
        @NotNull Collection<@NotNull Action<? extends InventoryEvent>> actions = getActions(null);

        if (actions.stream().noneMatch(a -> a instanceof CancelOnClickAction)) {
            actions.add(new CancelOnClickAction());
        }
    }
    public void cancelOnDrag() {
        @NotNull Collection<@NotNull Action<? extends InventoryEvent>> actions = getActions(null);

        if (actions.stream().noneMatch(a -> a instanceof CancelOnDragAction)) {
            actions.add(new CancelOnDragAction());
        }
    }

    // Modules

    @Override
    public void open(@NotNull HumanEntity @NotNull ... humans) {
        open(Arrays.asList(humans));
    }
    @Override
    public void open(@NotNull Collection<HumanEntity> humans) {
        // Verify if it's not closed
        if (closed) {
            throw new IllegalStateException("this inventory graphic is already closed, you need to recreate it.");
        }

        // Register events
        if (!humans.isEmpty() && !activeListener) {
            Bukkit.getPluginManager().registerEvents(listener, getPlugin());
            activeListener = true;
        }

        // Open inventory to humans
        for (@NotNull HumanEntity human : humans) {
            human.openInventory(getHandle());
        }
    }

    @Override
    public void close(@NotNull HumanEntity @NotNull ... humans) {
        for (@NotNull HumanEntity human : humans) {
            human.closeInventory();
        }
    }
    @Override
    public void close(@NotNull Collection<HumanEntity> humans) {
        for (@NotNull HumanEntity human : humans) {
            human.closeInventory();
        }
    }

    @Override
    public void close() {
        if (closed) return;
        else closed = true;

        // Close inventory to all entities
        close(getHandle().getViewers().toArray(new HumanEntity[0]));

        // Unregister listener
        HandlerList.unregisterAll(listener);
        activeListener = false;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof InventoryGraphic)) return false;
        @NotNull InventoryGraphic that = (InventoryGraphic) object;
        return Objects.equals(getHandle(), that.getHandle());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getHandle());
    }

    // Classes

    public enum Rows {

        // Static enums

        MINIMAL(9),
        SMALL(18),
        MEDIUM(27),
        LARGE(36),
        EXTRA_LARGE(45),
        FULL(54),
        ;

        // Static methods

        public static @NotNull Rows getBySlots(int slots) {
            for (@NotNull Rows rows : values()) {
                if (rows.getSlots() == slots) {
                    return rows;
                }
            }

            throw new IllegalArgumentException("there's no valid row with slots amount: " + slots);
        }

        // Object

        private final int slots;

        Rows(int slots) {
            this.slots = slots;
        }

        // Getters

        public int getSlots() {
            return slots;
        }

    }

    public interface Action<T extends InventoryEvent> extends Consumer<T> {
        @NotNull Class<T> getReference();

        @Override
        void accept(@NotNull T t);
    }
    public static abstract class AbstractAction<T extends InventoryEvent> implements Action<T> {

        // Object

        private final @NotNull Class<T> reference;

        public AbstractAction(@NotNull Class<T> reference) {
            this.reference = reference;
        }

        // Getters

        @Override
        public @NotNull Class<T> getReference() {
            return reference;
        }

        @Override
        public abstract void accept(@NotNull T t);

        // Classes

        static final class ListenerImpl implements Listener {
            @EventHandler
            private void click(@NotNull InventoryClickEvent e) {

            }
        }

    }
    public static abstract class ClickAction extends AbstractAction<InventoryClickEvent> {
        public ClickAction() {
            super(InventoryClickEvent.class);
        }
    }
    public static abstract class DragAction extends AbstractAction<InventoryDragEvent> {
        public DragAction() {
            super(InventoryDragEvent.class);
        }
    }

    private static final class CancelOnClickAction extends ClickAction {
        @Override
        public void accept(@NotNull InventoryClickEvent e) {
            e.setCancelled(true);
        }
    }
    private static final class CancelOnDragAction extends DragAction {
        @Override
        public void accept(@NotNull InventoryDragEvent e) {
            e.setCancelled(true);
        }
    }

    private final class ListenerImpl implements Listener {
        @EventHandler
        private void open(@NotNull InventoryOpenEvent e) {
            if (!listening || !e.getInventory().equals(getHandle())) {
                return;
            }

            call(null, e);
        }
        @EventHandler
        private void interact(@NotNull InventoryInteractEvent e) {
            if (!e.getInventory().equals(getHandle())) {
                return;
            }

            call(null, e);
        }
        @EventHandler
        private void creativeClick(@NotNull InventoryCreativeEvent e) {
            if (!Objects.equals(e.getClickedInventory(), getHandle())) {
                return;
            }

            call(e.getSlot(), e);
        }
        @EventHandler
        private void drag(@NotNull InventoryDragEvent e) {
            if (!e.getInventory().equals(getHandle())) {
                return;
            }

            call(null, e);
        }
        @EventHandler
        private void click(@NotNull InventoryClickEvent e) {
            if (!Objects.equals(e.getClickedInventory(), getHandle())) {
                return;
            }

            call(e.getSlot(), e);
        }
        @EventHandler
        private void close(@NotNull InventoryCloseEvent e) {
            if (!listening || !e.getInventory().equals(getHandle())) {
                return;
            } else if (!getHandle().getViewers().isEmpty() && activeListener) {
                HandlerList.unregisterAll(listener);
                activeListener = false;
            }

            call(null, e);
        }

        @SuppressWarnings("unchecked")
        private void call(@Nullable Integer slot, @NotNull InventoryEvent e) {
            for (@NotNull Entry<@Nullable Integer, @NotNull Collection<@NotNull Action<?>>> entry : new LinkedList<>(getActions().entrySet())) {
                @Nullable Integer actionSlot = entry.getKey();

                if (actionSlot != null && !Objects.equals(actionSlot, slot)) {
                    continue;
                }

                //noinspection rawtypes
                for (@NotNull Action action : entry.getValue()) {
                    if (action.getReference().isAssignableFrom(e.getClass())) {
                        try {
                            action.accept(e);
                        } catch (@NotNull Throwable throwable) {
                            if (e instanceof Cancellable) {
                                ((Cancellable) e).setCancelled(true);
                                throw new RuntimeException("cannot invoke action '" + entry + "' for event: " + e + ". The event has automatically cancelled to avoid issues.", throwable);
                            } else {
                                throw new RuntimeException("cannot invoke action '" + entry + "' for event: " + e, throwable);
                            }
                        }
                    }
                }
            }
        }

    }

}
