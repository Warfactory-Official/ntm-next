// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.interfaces.injected.UnstableFuseSchedule;
import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemUnstable;
import com.hbm.items.special.UnstableItemData;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import com.hbm.util.ChunkUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class UnstableFuses {
    public static final int RECOVERY_PERIOD = 20;

    private UnstableFuses() {}

    public static void init() {
        Services.SERVER.onServerTickPost(UnstableFuses::tick);
        Services.SERVER.onChunkLoad(UnstableFuses::onChunkLoad);
        Services.SERVER.onPlayerJoin(UnstableFuses::onEntityLoad);
        Services.SERVER.onPlayerRespawn(UnstableFuses::onEntityLoad);
        Services.SERVER.onPlayerChangeLevel((player, origin, destination) -> onEntityLoad(player));
    }

    public static long nextDeadline(MinecraftServer server) {
        return ((UnstableFuseSchedule) (Object) server).hbm$nextUnstableFuse();
    }

    public static void arm(ServerLevel level, long deadline) {
        UnstableFuseSchedule schedule = (UnstableFuseSchedule) (Object) level.getServer();
        if (deadline < schedule.hbm$nextUnstableFuse()) schedule.hbm$nextUnstableFuse(deadline);
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now < nextDeadline(server) && !auditing(now)) return;
        ((UnstableFuseSchedule) (Object) server).hbm$nextUnstableFuse(Long.MAX_VALUE);
        Sweep sweep = new Sweep(now, true);
        List<BlockEntity> blocks = new ArrayList<>();
        List<Entity> entities = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            ChunkUtil.forEachLoadedChunk(
                    level,
                    chunk -> {
                        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                            if (blockEntity instanceof StoredItems
                                    || blockEntity instanceof Container
                                            && nativeOwner(blockEntity)) {
                                blocks.add(blockEntity);
                            }
                        }
                    });
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ServerPlayer)) entities.add(entity);
            }
        }
        entities.addAll(server.getPlayerList().getPlayers());
        for (BlockEntity blockEntity : blocks) sweep.block(blockEntity);
        for (Entity entity : entities) {
            sweep.level = (ServerLevel) entity.level();
            sweep.entity(entity);
        }
        arm(server.overworld(), sweep.nextDeadline);
        sweep.publishChanges();
        sweep.detonate();
    }

    private static boolean auditing(long now) {
        return now % RECOVERY_PERIOD == 0 && Services.CONFIG.runtime().unstableRecoverySweep();
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (chunk.getBlockEntities().isEmpty()) return;
        Sweep sweep = new Sweep(level.getGameTime(), false);
        sweep.level = level;
        sweep.chunk(chunk);
        arm(level, sweep.nextDeadline);
    }

    public static void onEntityLoad(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof LivingEntity
                || entity instanceof Container
                || entity instanceof StoredItems
                || entity instanceof InventoryCarrier
                || entity instanceof ItemEntity
                || entity instanceof ItemFrame
                || entity instanceof Display.ItemDisplay)) return;
        Sweep sweep = new Sweep(level.getGameTime(), false);
        sweep.level = level;
        sweep.entity(entity);
        arm(level, sweep.nextDeadline);
    }

    public static long deadline(ItemInstance stack) {
        if (stack instanceof ItemStack live) return deadline(live);
        if (stack instanceof ItemStackTemplate template) return deadline(template);
        return readDeadline(stack);
    }

    public static long deadline(ItemStack stack) {
        if (stack.isEmpty()) return Long.MAX_VALUE;
        PatchedDataComponentMap components = (PatchedDataComponentMap) stack.getComponents();
        long deadline = UnstableItemData.cached(components);
        if (deadline != UnstableItemData.UNRESOLVED) return deadline;
        deadline = readDeadline(stack);
        UnstableItemData.cache(components, deadline);
        return deadline;
    }

    public static long deadline(ItemStackTemplate stack) {
        long deadline = UnstableItemData.cached(stack);
        if (deadline != UnstableItemData.UNRESOLVED) return deadline;
        deadline = readDeadline(stack);
        UnstableItemData.cache(stack, deadline);
        return deadline;
    }

    private static long readDeadline(ItemInstance stack) {
        if (stack.count() == 0) return Long.MAX_VALUE;
        long deadline = Long.MAX_VALUE;
        if (stack.typeHolder().value() instanceof ItemUnstable) {
            Long armed = stack.get(ModDataComponents.UNSTABLE_AT.get());
            if (armed != null) deadline = armed;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            boolean emptyMassStorage =
                    isMassStorage(stack)
                            && stack.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0) <= 0;
            for (int slot = 0; slot < contents.items.size(); slot++) {
                if (emptyMassStorage && slot == BlockEntityMassStorage.SLOT_TYPE) continue;
                Optional<ItemStackTemplate> item = contents.items.get(slot);
                if (item.isPresent()) deadline = Math.min(deadline, deadline(item.get()));
            }
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStackTemplate item : bundle.items())
                deadline = Math.min(deadline, deadline(item));
        }
        ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (projectiles != null) {
            for (ItemStackTemplate item : projectiles.items())
                deadline = Math.min(deadline, deadline(item));
        }
        ItemStackTemplate cargo = stack.get(ModDataComponents.ARTY_CARGO.get());
        if (cargo != null) deadline = Math.min(deadline, deadline(cargo));
        List<ItemStackTemplate> kit = stack.get(ModDataComponents.KIT_CONTENTS.get());
        if (kit != null) {
            for (ItemStackTemplate item : kit) deadline = Math.min(deadline, deadline(item));
        }
        return deadline;
    }

    private static boolean isMassStorage(ItemInstance stack) {
        return stack.typeHolder().value() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof BlockMassStorage;
    }

    public record MenuWatcher(ServerPlayer player) implements ContainerListener {
        @Override
        public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
            Slot slot = menu.getSlot(slotIndex);
            if (physicalSlot(slot.container, slot.getContainerSlot())) {
                arm(player.level(), deadline(stack));
            }
        }

        @Override
        public void dataChanged(AbstractContainerMenu menu, int id, int value) {}
    }

    private static boolean physicalSlot(Container container, int slot) {
        while (container instanceof CompoundContainer compound) {
            int first = compound.container1.getContainerSize();
            if (slot < first) container = compound.container1;
            else {
                slot -= first;
                container = compound.container2;
            }
        }
        if (container instanceof BlockEntity blockEntity && blockEntity.isRemoved()) return false;
        if (container instanceof Entity entity && entity.isRemoved()) return false;
        if (container instanceof ResultContainer) return false;
        if (container instanceof IControlReceiverFilter filter) {
            int[] range = filter.getFilterSlots();
            if (slot >= range[0] && slot < range[1]) return false;
        }
        return !(container instanceof BlockEntityMachineBase machine) || machine.dropsSlot(slot);
    }

    private static boolean menuOwnedSlot(Slot slot, ServerPlayer player) {
        if (!nativeOwner(player.containerMenu)) return false;
        Container container = slot.container;
        int index = slot.getContainerSlot();
        while (container instanceof CompoundContainer compound) {
            int first = compound.container1.getContainerSize();
            if (index < first) container = compound.container1;
            else {
                index -= first;
                container = compound.container2;
            }
        }
        return !(container instanceof BlockEntity || container instanceof Entity)
                && container != player.getInventory()
                && container != player.getEnderChestInventory()
                && physicalSlot(container, index);
    }

    private static boolean nativeOwner(Object owner) {
        String name = owner.getClass().getName();
        return name.startsWith("net.minecraft.") || name.startsWith("com.hbm.");
    }

    private static final class Sweep implements StoredItems.Visitor {
        private final long now;
        private final boolean expire;
        private ServerLevel level;
        private Entity carrier;
        private BlockEntity blockOwner;
        private double x;
        private double y;
        private double z;
        private long nextDeadline = Long.MAX_VALUE;
        private List<Blast> blasts;
        private List<Runnable> changes;
        private int mutations;

        private Sweep(long now, boolean expire) {
            this.now = now;
            this.expire = expire;
        }

        private void chunk(LevelChunk chunk) {
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                block(blockEntity);
            }
        }

        private void block(BlockEntity blockEntity) {
            if (blockEntity.isRemoved()
                    || !(blockEntity.getLevel() instanceof ServerLevel ownerLevel)) return;
            level = ownerLevel;
            carrier = null;
            blockOwner = blockEntity;
            x = blockEntity.getBlockPos().getX() + 0.5D;
            y = blockEntity.getBlockPos().getY() + 0.5D;
            z = blockEntity.getBlockPos().getZ() + 0.5D;
            LevelChunk chunk =
                    level.getChunkSource()
                            .getChunkNow(
                                    blockEntity.getBlockPos().getX() >> 4,
                                    blockEntity.getBlockPos().getZ() >> 4);
            int before = mutations;
            if (blockEntity instanceof Container container && nativeOwner(blockEntity))
                inventory(container);
            if (blockEntity instanceof StoredItems stored) stored.visitStoredItems(this);
            if (mutations != before) chunk.markUnsaved();
        }

        private boolean ownerPresent() {
            if (carrier != null) return !carrier.isRemoved() && carrier.level() == level;
            if (blockOwner == null) return true;
            LevelChunk chunk =
                    level.getChunkSource()
                            .getChunkNow(
                                    blockOwner.getBlockPos().getX() >> 4,
                                    blockOwner.getBlockPos().getZ() >> 4);
            return !blockOwner.isRemoved()
                    && chunk != null
                    && chunk.getBlockEntities().get(blockOwner.getBlockPos()) == blockOwner;
        }

        @Override
        public boolean visit(Container container) {
            return inventory(container);
        }

        @Override
        public boolean visit(BlockEntity blockEntity) {

            if (!nativeOwner(blockEntity) && blockEntity.getLevel() == null) return false;
            int before = mutations;
            if (blockEntity instanceof Container container && nativeOwner(blockEntity))
                inventory(container);
            if (blockEntity instanceof StoredItems stored) stored.visitStoredItems(this);
            return mutations != before;
        }

        @Override
        public boolean visit(StoredItemSlots slots) {
            if (!ownerPresent()) return false;
            int before = mutations;
            for (int slot = 0, size = slots.storedSlotCount(); slot < size; slot++) {
                ItemStack item = slots.storedItem(slot);
                if (!due(item)) continue;
                int previousMutations = mutations;
                int previousBlasts = blasts == null ? 0 : blasts.size();
                ItemStack replacement = item.copy();
                if (stack(replacement)
                        && !slots.replaceStoredItem(
                                slot,
                                item,
                                replacement.isEmpty() ? ItemStack.EMPTY : replacement)) {
                    refuse(previousMutations, previousBlasts);
                }
                if (!ownerPresent()) break;
                size = Math.min(size, slots.storedSlotCount());
            }
            return mutations != before;
        }

        @Override
        public boolean visitOwned(Container container) {
            if (!ownerPresent()) return false;
            int before = mutations;
            for (int slot = 0, size = container.getContainerSize(); slot < size; slot++) {
                ItemStack item = container.getItem(slot);
                if (!due(item)) continue;
                ItemStack replacement = item.copy();
                if (stack(replacement))
                    container.setItem(slot, replacement.isEmpty() ? ItemStack.EMPTY : replacement);
                if (!ownerPresent()) break;
                size = Math.min(size, container.getContainerSize());
            }
            if (mutations != before && ownerPresent()) container.setChanged();
            return mutations != before;
        }

        private boolean due(ItemStack stack) {
            long deadline = deadline(stack);
            if (!expire || now < deadline) {
                nextDeadline = Math.min(nextDeadline, deadline);
                return false;
            }
            return true;
        }

        private void refuse(int previousMutations, int previousBlasts) {
            mutations = previousMutations;
            if (blasts != null) blasts.subList(previousBlasts, blasts.size()).clear();
            nextDeadline = Math.min(nextDeadline, now + 1);
        }

        private boolean inventory(Container container) {

            if (container instanceof RandomizableContainer loot && loot.getLootTable() != null)
                return false;
            if (container instanceof ContainerEntity loot && loot.getContainerLootTable() != null)
                return false;
            int firstFilter = 0;
            int endFilter = 0;
            if (container instanceof IControlReceiverFilter filter) {
                int[] range = filter.getFilterSlots();
                firstFilter = range[0];
                endFilter = range[1];
            }
            boolean changed = false;
            for (int slot = 0, size = container.getContainerSize(); slot < size; slot++) {
                if (slot >= firstFilter && slot < endFilter) continue;
                if (container instanceof BlockEntityMachineBase machine && !machine.dropsSlot(slot))
                    continue;
                ItemStack item = container.getItem(slot);
                if (stack(item)) {
                    changed = true;
                }
            }
            if (changed)
                afterChanges(
                        () -> {
                            if (container instanceof BlockEntity blockEntity
                                    && blockEntity.isRemoved()) return;
                            if (container instanceof Entity entity && entity.isRemoved()) return;
                            container.setChanged();
                        });
            return changed;
        }

        private void entity(Entity entity) {
            if (entity.isRemoved()) return;
            carrier = entity;
            blockOwner = null;
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            if (entity instanceof ServerPlayer player) {
                inventory(player.getInventory());
                inventory(player.getEnderChestInventory());
                AbstractContainerMenu menu = player.containerMenu;
                for (Slot slot : menu.slots) {
                    if (!menuOwnedSlot(slot, player)) continue;
                    ItemStack item = slot.getItem();
                    if (stack(item))
                        afterChanges(
                                () -> {
                                    if (player.containerMenu == menu && slot.getItem() == item) {
                                        slot.setByPlayer(item.isEmpty() ? ItemStack.EMPTY : item);
                                    }
                                });
                }
                ItemStack carried = menu.getCarried();
                if (stack(carried))
                    afterChanges(
                            () -> {
                                if (player.containerMenu == menu && menu.getCarried() == carried) {
                                    menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                                }
                            });
            } else {
                if (entity instanceof Container container && nativeOwner(entity))
                    inventory(container);
                if (entity instanceof InventoryCarrier container)
                    inventory(container.getInventory());
                if (entity instanceof AbstractHorse horse) inventory(horse.inventory);
                if (entity instanceof LivingEntity living) {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        ItemStack item = living.getItemBySlot(slot);
                        if (stack(item))
                            afterChanges(
                                    () -> {
                                        if (!living.isRemoved()
                                                && living.getItemBySlot(slot) == item) {
                                            living.setItemSlot(
                                                    slot, item.isEmpty() ? ItemStack.EMPTY : item);
                                        }
                                    });
                    }
                }
            }
            if (entity instanceof StoredItems stored) stored.visitStoredItems(this);
            if (entity instanceof ItemEntity item) {
                ItemStack contents = item.getItem();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!item.isRemoved() && item.getItem() == contents)
                                    item.setItem(contents.copy());
                            });
            } else if (entity instanceof ItemFrame frame) {
                ItemStack contents = frame.getItem();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!frame.isRemoved() && frame.getItem() == contents)
                                    frame.setItem(contents.copy());
                            });
            } else if (entity instanceof Display.ItemDisplay display) {
                ItemStack contents = display.getItemStack();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!display.isRemoved() && display.getItemStack() == contents)
                                    display.setItemStack(contents.copy());
                            });
            }
        }

        @Override
        public void afterChanges(Runnable notification) {
            if (changes == null) changes = new ArrayList<>();
            changes.add(notification);
        }

        private void publishChanges() {

            if (changes != null) for (Runnable notification : changes) notification.run();
        }

        @Override
        public boolean visit(ItemStack stack) {
            return stack(stack);
        }

        private boolean stack(ItemStack stack) {
            if (stack.isEmpty()) return false;
            long deadline = deadline(stack);
            if (!expire || now < deadline) {
                if (deadline < nextDeadline) nextDeadline = deadline;
                return false;
            }
            if (ItemUnstable.hasSpentFuse(stack, now)) {
                stack.setCount(0);
                mutations++;
                if (blasts == null) blasts = new ArrayList<>();
                blasts.add(new Blast(level, x, y, z, carrier));
                return true;
            }
            boolean changed = false;
            ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
            if (contents != null) {
                boolean massStorage = isMassStorage(stack);
                boolean emptyMassStorage =
                        massStorage
                                && stack.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0)
                                        <= 0;
                List<Optional<ItemStackTemplate>> replacement = null;
                for (int i = 0; i < contents.items.size(); i++) {
                    if (emptyMassStorage && i == BlockEntityMassStorage.SLOT_TYPE) continue;
                    Optional<ItemStackTemplate> item = contents.items.get(i);
                    if (item.isEmpty() || deadline(item.get()) > now) continue;
                    ItemStack child = item.get().create();
                    if (!stack(child)) continue;
                    if (replacement == null) replacement = new ArrayList<>(contents.items);
                    replacement.set(
                            i,
                            child.isEmpty()
                                    ? Optional.empty()
                                    : Optional.of(ItemStackTemplate.fromNonEmptyStack(child)));
                    if (massStorage && i == BlockEntityMassStorage.SLOT_TYPE && child.isEmpty()) {
                        stack.remove(ModDataComponents.MASS_STOCKPILE.get());
                    }
                }
                if (replacement != null) {
                    stack.set(DataComponents.CONTAINER, new ItemContainerContents(replacement));
                    changed = true;
                }
            }
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                TemplateChanges replacement =
                        templates(bundle.items(), bundle.getSelectedItemIndex());
                if (replacement != null) {
                    stack.set(
                            DataComponents.BUNDLE_CONTENTS,
                            new BundleContents(replacement.items, replacement.selected));
                    changed = true;
                }
            }
            ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (projectiles != null) {
                TemplateChanges replacement = templates(projectiles.items(), -1);
                if (replacement != null) {
                    stack.set(
                            DataComponents.CHARGED_PROJECTILES,
                            new ChargedProjectiles(replacement.items));
                    changed = true;
                }
            }
            ItemStackTemplate cargo = stack.get(ModDataComponents.ARTY_CARGO.get());
            if (cargo != null && deadline(cargo) <= now) {
                ItemStack copy = cargo.create();
                if (stack(copy)) {
                    if (copy.isEmpty()) stack.remove(ModDataComponents.ARTY_CARGO.get());
                    else
                        stack.set(
                                ModDataComponents.ARTY_CARGO.get(),
                                ItemStackTemplate.fromNonEmptyStack(copy));
                    changed = true;
                }
            }
            List<ItemStackTemplate> kit = stack.get(ModDataComponents.KIT_CONTENTS.get());
            if (kit != null) {
                TemplateChanges replacement = templates(kit, -1);
                if (replacement != null) {
                    stack.set(ModDataComponents.KIT_CONTENTS.get(), List.copyOf(replacement.items));
                    changed = true;
                }
            }
            long next = deadline(stack);
            if (next < nextDeadline) nextDeadline = next;
            if (changed) mutations++;
            return changed;
        }

        private TemplateChanges templates(List<ItemStackTemplate> items, int selected) {
            List<ItemStackTemplate> replacement = null;
            int newSelection = selected;
            for (int i = 0; i < items.size(); i++) {
                ItemStackTemplate item = items.get(i);
                if (deadline(item) > now) continue;
                ItemStack child = item.create();
                if (!stack(child)) continue;
                if (replacement == null) replacement = new ArrayList<>(items);
                replacement.set(
                        i, child.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(child));
                if (child.isEmpty()) {
                    if (i < selected) newSelection--;
                    else if (i == selected) newSelection = -1;
                }
            }
            if (replacement == null) return null;
            replacement.removeIf(item -> item == null);
            return new TemplateChanges(replacement, newSelection);
        }

        private void detonate() {

            if (blasts == null) return;
            for (Blast blast : blasts) {
                ItemUnstable.detonate(blast.level, blast.x, blast.y, blast.z);
                if (blast.carrier != null && !blast.carrier.isRemoved()) {
                    ServerLevel carrierLevel = (ServerLevel) blast.carrier.level();
                    blast.carrier.hurtServer(
                            carrierLevel,
                            carrierLevel.damageSources().source(ModDamageTypes.NUCLEAR_BLAST),
                            10_000.0F);
                }
            }
        }
    }

    private record Blast(ServerLevel level, double x, double y, double z, Entity carrier) {}

    private record TemplateChanges(List<ItemStackTemplate> items, int selected) {}
}
