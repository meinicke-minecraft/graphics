package dev.meinicke.mc.inventories.paged;

import dev.meinicke.mc.inventories.InventoryGraphic;
import dev.meinicke.mc.inventories.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PagedInventoryGraphic extends InventoryGraphic {

    // Object

    private int[] bounds = new int[0];
    private volatile int page = 0;

    private final @NotNull Collection<@NotNull Element> elements = new LinkedList<>();

    private @NotNull Supplier<@Nullable ItemStack> previousItem = () -> ItemBuilder.skullFromUrl("https://textures.minecraft.net/texture/5f133e91919db0acefdc272d67fd87b4be88dc44a958958824474e21e06d53e6").name("§9Previous Page").lore("§7Click here to navigate to the previous page.", "§7The actual page is §f" + (getPage() + 1) + "§9/§f" + getAvailablePages() + "§7.").build();
    private @NotNull Supplier<@Nullable ItemStack> nextItem = () -> ItemBuilder.skullFromUrl("https://textures.minecraft.net/texture/e3fc52264d8ad9e654f415bef01a23947edbccccf649373289bea4d149541f70").name("§9Next Page").lore("§7Click here to navigate to the next page.", "§7The actual page is §f" + (getPage() + 1) + "§9/§f" + getAvailablePages() + "§7.").build();

    private int previousItemSlot;
    private int nextItemSlot;

    private @Nullable BlankItem blankItem;

    // Cache (for performance)

    private @Nullable Integer blankItemSlotCache = null;
    private final @NotNull Collection<Action<?>> blankItemActionsCache = new LinkedList<>();

    // Object

    public PagedInventoryGraphic(@NotNull Plugin plugin, @Nullable String title, @NotNull Rows rows, int previousItemSlot, int nextItemSlot) {
        this(plugin, title, rows.getSlots(), previousItemSlot, nextItemSlot);
    }
    public PagedInventoryGraphic(@NotNull Plugin plugin, @Nullable String title, int size, int previousItemSlot, int nextItemSlot) {
        super(plugin, title, size);

        // Variables
        this.previousItemSlot = previousItemSlot;
        this.nextItemSlot = nextItemSlot;
    }

    // Getters

    public @NotNull Collection<@NotNull Element> getElements() {
        return elements;
    }
    @Unmodifiable
    public @NotNull Collection<@NotNull Element> getElements(int page) {
        int[] availableBounds = getAvailableBounds();
        return getElements().stream().skip(((long) getPage() * availableBounds.length)).limit(availableBounds.length).collect(Collectors.toList());
    }

    public int getAvailablePages() {
        int bounds = getAvailableBounds().length;
        if (bounds == 0) return 1;

        return Math.max(1, (int) Math.ceil((double) getElements().size() / bounds));
    }

    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        if (this.page == page) {
            return;
        }

        this.page = page;
        sync();
    }

    private int @NotNull [] getAvailableBounds(int size) {
        return IntStream.of(getBounds()).filter(i -> i < size).toArray();
    }
    /**
     * The bounds that is available for this inventory size
     * @return
     */
    public int @NotNull [] getAvailableBounds() {
        return getAvailableBounds(getSize());
    }
    @Range(from = 0, to = 53)
    public final int @NotNull [] getBounds() {
        return bounds;
    }
    public final void setBounds(@NotNull Integer @NotNull [] slots) {
        this.bounds = Arrays.stream(slots).mapToInt(i -> i).toArray();
        sync();

        if (slots.length > 0) {
            setPage(Math.min(getPage(), getAvailablePages() - 1));
        }
    }
    public final void setBounds(int @NotNull [] slots) {
        this.bounds = slots;
        sync();

        if (slots.length > 0) {
            setPage(Math.min(getPage(), getAvailablePages() - 1));
        }
    }
    public final void setBounds(@Range(from = 1, to = 54) int bounds) {
        this.setBounds(IntStream.range(0, bounds).boxed().toArray(Integer[]::new));
    }
    public final void setBounds(@Range(from = 0, to = 53) int startInclusive, @Range(from = 0, to = 54) int endExclusive) {
        if (startInclusive > endExclusive) {
            throw new IllegalStateException("the start cannot be higher than to end");
        }

        this.setBounds(IntStream.range(startInclusive, endExclusive).boxed().toArray(Integer[]::new));
    }

    public int getIndex(int slot) {
        int[] bounds = getAvailableBounds();

        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] == slot) {
                return (getPage() * bounds.length) + i;
            }
        }

        return -1;
    }

    public @NotNull Supplier<@Nullable ItemStack> getPreviousItem() {
        return previousItem;
    }
    public void setPreviousItem(@NotNull Supplier<@Nullable ItemStack> previousItem) {
        this.previousItem = previousItem;
    }

    public @NotNull Supplier<@Nullable ItemStack> getNextItem() {
        return nextItem;
    }
    public void setNextItem(@NotNull Supplier<@Nullable ItemStack> nextItem) {
        this.nextItem = nextItem;
    }

    public int getPreviousItemSlot() {
        return previousItemSlot;
    }
    public void setPreviousItemSlot(int previousItemSlot) {
        // Remove old action and item
        if (getActions().containsKey(getPreviousItemSlot())) {
            getActions().get(getPreviousItemSlot()).removeIf(action -> action instanceof PreviousPageAction);
        }
        getHandle().setItem(getPreviousItemSlot(), new ItemStack(Material.AIR));

        // Change slot
        this.previousItemSlot = previousItemSlot;
        sync();
    }

    public int getNextItemSlot() {
        return nextItemSlot;
    }
    public void setNextItemSlot(int nextItemSlot) {
        // Remove old action and item
        if (getActions().containsKey(getNextItemSlot())) {
            getActions().get(getNextItemSlot()).removeIf(action -> action instanceof NextPageAction);
        }
        getHandle().setItem(getNextItemSlot(), new ItemStack(Material.AIR));

        // Change slot
        this.nextItemSlot = nextItemSlot;
        sync();
    }

    public @Nullable BlankItem getBlankItem() {
        return blankItem;
    }
    public void setBlankItem(@Nullable BlankItem blankItem) {
        // Change blank item
        this.blankItem = blankItem;

        // Sync to apply possible changes
        sync();
    }
    public void setBlankItem(int slot) {
        setBlankItem(new BlankItem(slot));
    }
    public void setBlankItem(int slot, @NotNull ItemStack itemStack) {
        setBlankItem(new BlankItem(slot, itemStack));
    }

    // Super inventory graphic methods

    @Override
    public void setSize(int size) {
        int old = getSize();
        super.setSize(size);

        if (old != getSize()) { // Changed!
            // Remove all old actions
            for (int slot : getAvailableBounds(old)) {
                if (getActions().containsKey(slot)) {
                    getActions().get(slot).removeIf(action -> action instanceof ElementAction);
                }
            }

            sync();
        }
    }

    // Utilities

    public void sync() {
        // Verify page
        this.page = Math.min(getAvailablePages() - 1, getPage());

        // Remove old actions
        for (int slot = 0; slot < getSize(); slot++) {
            if (getActions().containsKey(slot)) {
                getActions().get(slot).removeIf(a -> a instanceof PagedAction);
            }
        }

        // Redefine page items
        @Nullable ItemStack previous = getPreviousItem().get();
        if (previous != null && getPreviousItemSlot() <= getSize()) {
            setItem(previous, new PreviousPageAction(), getPreviousItemSlot());
        }

        @Nullable ItemStack next = getNextItem().get();
        if (next != null && getNextItemSlot() <= getSize()) {
            setItem(next, new NextPageAction(), getNextItemSlot());
        }

        // Variables
        @NotNull Element[] elements = getElements(getPage()).toArray(new Element[0]);
        int[] availableBounds = getAvailableBounds();

        // Check if there's elements (add blank item if not)
        @NotNull Runnable deleteOldBlankItem = () -> {
            setItem(new ItemStack(Material.AIR), blankItemSlotCache);
            getActions(blankItemSlotCache).removeAll(blankItemActionsCache);

            blankItemSlotCache = null;
            blankItemActionsCache.clear();
        };

        if (elements.length == 0) {
            @Nullable BlankItem item = getBlankItem();

            if (blankItemSlotCache != null && (item == null || !Objects.equals(item.getSlot(), blankItemSlotCache))) {
                deleteOldBlankItem.run();
            }

            if (item != null && blankItemSlotCache == null) {
                // Clear element bounds
                setItem(new ItemStack(Material.AIR), availableBounds);

                // Add blank item
                blankItemSlotCache = item.getSlot();
                blankItemActionsCache.addAll(item.getActions());

                setItem(item.getItemStack(), blankItemSlotCache);
                getActions(blankItemSlotCache).addAll(blankItemActionsCache);
            }

            return;
        } else if (blankItemSlotCache != null) {
            deleteOldBlankItem.run();
        }

        // Fill items, there's elements available.
        for (int row = 0; row < availableBounds.length; row++) {
            // Index and slot
            int slot = availableBounds[row];

            // Retrieve element
            @Nullable Element element = row < elements.length ? elements[row] : null;

            // Add item to the inventory
            if (element == null) { // Empty bound
                setItem(new ItemStack(Material.AIR), slot);
            } else { // Valid bound with an element
                setItem(element.getItemStack(), new ElementAction(element), slot);
            }
        }
    }

    // Classes

    public interface Element {

        // Static initializers

        static @NotNull Element create(@NotNull ItemStack itemStack, @NotNull Collection<Action<? extends InventoryEvent>> actions) {
            // Variables
            @NotNull Element element = new ElementImpl(itemStack);

            // Add actions
            element.getActions().addAll(actions);

            // Finish
            return element;
        }
        @SafeVarargs
        static @NotNull Element create(@NotNull ItemStack itemStack, @NotNull Action<? extends InventoryEvent> @NotNull ... actions) {
            // Variables
            @NotNull Element element = new ElementImpl(itemStack);

            // Add actions
            element.getActions().addAll(Arrays.asList(actions));

            // Finish
            return element;
        }
        static @NotNull Element create(@NotNull ItemStack itemStack) {
            return new ElementImpl(itemStack);
        }

        // Object

        @NotNull ItemStack getItemStack();
        @NotNull Collection<Action<? extends InventoryEvent>> getActions();

    }
    private static final class ElementImpl implements Element {

        private final @NotNull ItemStack itemStack;
        private final @NotNull Collection<Action<? extends InventoryEvent>> actions;

        private ElementImpl(@NotNull ItemStack itemStack) {
            this.itemStack = itemStack;
            this.actions = new LinkedList<>();
        }

        // Getters

        @Override
        public @NotNull ItemStack getItemStack() {
            return itemStack;
        }
        @Override
        public @NotNull Collection<Action<? extends InventoryEvent>> getActions() {
            return actions;
        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Element)) return false;
            @NotNull Element element = (Element) object;
            return Objects.equals(getItemStack(), element.getItemStack()) && Objects.equals(getActions(), element.getActions());
        }
        @Override
        public int hashCode() {
            return Objects.hash(getItemStack(), getActions());
        }

        @Override
        public @NotNull String toString() {
            return "Element{" +
                    "itemStack=" + getItemStack() +
                    ", actions=" + getActions() +
                    '}';
        }

    }

    private static abstract class PagedAction extends AbstractAction<InventoryClickEvent> {
        private PagedAction() {
            super(InventoryClickEvent.class);
        }
    }

    public final class PreviousPageAction extends PagedAction {
        @Override
        public void accept(@NotNull InventoryClickEvent e) {
            // Cancel event
            e.setCancelled(true);

            // Proceed with previous page action
            setPage(Math.max(0, getPage() - 1));
        }
    }
    public final class NextPageAction extends PagedAction {
        @Override
        public void accept(@NotNull InventoryClickEvent e) {
            // Cancel event
            e.setCancelled(true);

            // Proceed with next page action
            if (getPage() + 1 < getAvailablePages()) {
                setPage(getPage() + 1);
            } else {
                e.getWhoClicked().sendMessage("§c✘ §7There are no next pages.");
            }
        }
    }

    public static final class ElementAction extends PagedAction {

        // Object

        private final @NotNull Element element;

        private ElementAction(@NotNull Element element) {
            this.element = element;
        }

        // Getters

        public @NotNull Element getElement() {
            return element;
        }

        // Action

        @SuppressWarnings("unchecked")
        @Override
        public void accept(@NotNull InventoryClickEvent e) {
            // Cancel event
            e.setCancelled(true);

            // Call actions
            //noinspection rawtypes
            for (@NotNull Action action : getElement().getActions()) {
                if (action.getReference().isAssignableFrom(e.getClass())) {
                    action.accept(e);
                }
            }
        }

    }

}
