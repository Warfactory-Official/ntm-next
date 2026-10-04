// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.IControlReceiverFilter;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.interfaces.injected.UnstableFuseSchedule;
import com.hbm.inventory.BlockStorage;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventoryEntityAccess;
import com.hbm.inventory.InventoryLocation;
import com.hbm.inventory.InventoryOwnerAccess;
import com.hbm.inventory.InventoryPublications;
import com.hbm.inventory.InventorySlotAccess;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.ItemContents;
import com.hbm.inventory.ItemInteriorBackend;
import com.hbm.inventory.ItemInteriors;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.SlotDeltaBuffer;
import com.hbm.inventory.SlotSource;
import com.hbm.items.special.ItemUnstable;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.NeoForgeServerEvents;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class UnstableFuses {
    public static final int RECOVERY_PERIOD = 20;

    private UnstableFuses() {}

    public static void init() {
        NeoForgeServerEvents.onServerTickPost(UnstableFuses::tick);
        NeoForgeServerEvents.onChunkLoad(UnstableFuses::onChunkLoad);
        NeoForgeServerEvents.onPlayerJoin(UnstableFuses::onEntityLoad);
        NeoForgeServerEvents.onPlayerRespawn(UnstableFuses::onEntityLoad);
        NeoForgeServerEvents.onPlayerChangeLevel(
                (player, origin, destination) -> onEntityLoad(player));
    }

    public static long nextDeadline(MinecraftServer server) {
        UnstableFuseSchedule schedule = server;
        return schedule.hbm$unstableFuses().nextDeadline();
    }

    public static void registerInterior(
            MinecraftServer server, InventoryLocation owner, SlotSource[] sources) {
        UnstableFuseQueue queue = server.hbm$unstableFuses();
        SlotSource[] previous = queue.interiorSources(owner);
        boolean same = previous != null && previous.length == sources.length;
        if (same)
            for (int i = 0; i < sources.length; i++)
                if (previous[i] != sources[i]) {
                    same = false;
                    break;
                }
        if (same) {
            FuseSlots index = queue.slots(owner);
            queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
            return;
        }
        unregisterInterior(server, owner);
        queue.interiorSources(owner, sources.clone());
        for (SlotSource source : sources) {
            InventorySlotAccess.of(source).bind(source, owner);
            trackSource(queue, owner, source);
        }
        FuseSlots index = queue.slots(owner);
        queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
    }

    public static void unregisterInterior(MinecraftServer server, InventoryLocation owner) {
        UnstableFuseQueue queue = server.hbm$unstableFuses();
        SlotSource[] sources = queue.interiorSources(owner);
        queue.interiorSources(owner, null);
        queue.remove(owner);
        if (sources == null) return;
        for (SlotSource source : sources) {
            if (InventoryChanges.owner(source) == owner)
                InventorySlotAccess.of(source).bind(source, null);
        }
    }

    public static void arm(Entity owner, long deadline) {
        if (!isStorageOwner(owner)) return;
        if (!(owner.level() instanceof ServerLevel level)
                || level.getEntity(owner.getId()) != owner) return;
        UnstableFuseQueue queue = level.getServer().hbm$unstableFuses();
        if (deadline < queue.deadline(owner)) queue.put(owner, deadline);
    }

    public static void storedStackChanged(Entity owner, ItemStack stack) {
        if (!(owner instanceof ItemEntity
                        || owner instanceof ItemFrame
                        || owner instanceof Display.ItemDisplay)
                || !isStorageOwner(owner)) return;
        if (!(owner.level() instanceof ServerLevel level)) return;
        InventorySlotBinding.bind(stack, owner, 0);
        ItemInteriors.publish(owner, owner, 0, stack);
        if (deadline(stack) == Long.MAX_VALUE
                && level.getServer().hbm$unstableFuses().deadline(owner) == Long.MAX_VALUE) return;
        slotChanged(owner, owner, 0);
    }

    public static void slotChanged(Object owner, Object source, int slot) {
        InventoryOwnerAccess kind = InventoryOwnerAccess.of(owner);
        if (kind == InventoryOwnerAccess.BLOCK
                && InventoryChanges.sources((BlockEntity) owner) == null) return;
        if (kind == InventoryOwnerAccess.ENTITY
                && !InventoryEntityAccess.of((Entity) owner).indexed) {
            changed(owner);
            return;
        }
        ServerLevel level = kind.level(owner);
        if (level == null) return;
        level.getServer().hbm$unstableFuses().slotChanged(source, slot);
    }

    public static void changed(Object owner) {
        ServerLevel level;
        if (owner instanceof BlockEntity blockEntity) {
            if (isStorageOwner(blockEntity)) InventoryChanges.blockChanged(blockEntity);
            return;
        } else if (owner instanceof Entity entity) {
            if (!(entity.level() instanceof ServerLevel server)
                    || server.getEntity(entity.getId()) != entity
                    || !isStorageOwner(entity)) return;
            level = server;
        } else if (owner instanceof InventoryLocation) {
            level = InventoryOwnerAccess.of(owner).level(owner);
            if (level == null) return;
        } else {
            throw new IllegalArgumentException();
        }
        level.getServer().hbm$unstableFuses().changed(owner);
    }

    public static void removed(ServerLevel level, Object owner) {
        ItemInteriors.retire(level.getServer(), owner);
        level.getServer().hbm$unstableFuses().remove(owner);
        if (owner instanceof BlockEntity block) InventoryChanges.unbindBlock(block);
        if (owner instanceof Entity entity) bindContainers(entity, null);
    }

    private static void inspect(UnstableFuseQueue queue, Sweep sweep, Object owner) {
        sweep.nextDeadline = Long.MAX_VALUE;
        if (owner instanceof InventoryLocation location) {
            queue.remove(owner);
            SlotSource[] sources = queue.interiorSources(location);
            if (sources != null)
                for (SlotSource source : sources) trackSource(queue, owner, source);
            FuseSlots index = queue.slots(owner);
            queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
            return;
        }
        if (owner instanceof BlockEntity)
            throw new IllegalStateException("Block storage requires a slot index");
        ItemInteriors.retire(((ServerLevel) ((Entity) owner).level()).getServer(), owner);
        sweep.indexOwner = InventoryEntityAccess.of((Entity) owner).indexed ? owner : null;
        sweep.index = null;
        {
            Entity entity = (Entity) owner;
            sweep.level = (ServerLevel) entity.level();
            if (sweep.level.getEntity(entity.getId()) == entity) sweep.entity(entity);
        }
        queue.slots(owner, sweep.index);
        queue.wholeOwner(owner, sweep.indexOwner == null && sweep.nextDeadline != Long.MAX_VALUE);
        queue.put(owner, sweep.nextDeadline);
    }

    public static void rebuildBlock(BlockEntity owner) {
        InventoryOwnerAccess kind = InventoryOwnerAccess.BLOCK;
        if (!kind.live(owner)) return;
        UnstableFuseQueue queue = kind.level(owner).getServer().hbm$unstableFuses();
        queue.remove(owner);
        Object sources = InventoryChanges.sources(owner);
        ItemInteriors.replaceSources(owner, sources);
        if (sources instanceof Object[] multiple)
            for (Object source : multiple) trackSource(queue, owner, source);
        else trackSource(queue, owner, sources);
        FuseSlots index = queue.slots(owner);
        queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
    }

    private static void trackSource(UnstableFuseQueue queue, Object owner, Object source) {
        InventorySlotAccess access = InventorySlotAccess.of(source);
        FuseSlots index = queue.slots(owner);
        if (index != null) index.removeSource(source);
        ServerLevel level = InventoryOwnerAccess.of(owner).level(owner);
        if (level != null) {
            SlotSource canonical = ItemInteriors.canonicalSource(level.getServer(), source);
            ItemInteriors.suppressSource(level.getServer(), source);
            if (canonical != null && canonical != source) {
                queue.slots(owner, index);
                return;
            }
        }
        if (owner instanceof ItemInteriorBackend backend) {
            backend.slotChanged((SlotSource) source, InventoryChanges.LAYOUT);
            index = queue.slots(owner);
            ItemInteriors.publish(owner, source, InventoryChanges.LAYOUT, ItemStack.EMPTY);
        }
        for (int slot = 0, count = access.size(source); slot < count; slot++) {
            if (!access.physical(source, slot)) continue;
            ItemStack item = access.read(source, slot);
            ItemInteriors.publish(owner, source, slot, item);
            long deadline = deadline(item);
            if (deadline == Long.MAX_VALUE) continue;
            if (index == null) index = new FuseSlots();
            index.update(source, slot, deadline);
        }
        queue.slots(owner, index);
    }

    public static void reindexSource(MinecraftServer server, Object source) {
        Object owner = InventoryChanges.owner(source);
        if (owner == null || !InventoryOwnerAccess.of(owner).live(owner)) return;
        UnstableFuseQueue queue = server.hbm$unstableFuses();
        trackSource(queue, owner, source);
        FuseSlots index = queue.slots(owner);
        queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
    }

    public static void removeSource(Object owner, Object source) {
        ServerLevel level = InventoryOwnerAccess.of(owner).level(owner);
        if (level == null) return;
        var server = level.getServer();
        var queue = server.hbm$unstableFuses();
        FuseSlots index = queue.slots(owner);
        if (index != null) {
            index.removeSource(source);
            queue.slots(owner, index);
            queue.put(owner, index.minimum());
        }
        ItemInteriors.suppressSource(server, source);
    }

    public static void tick(MinecraftServer server) {
        ItemInteriors.flush(server);
        long now = server.overworld().getGameTime();
        boolean audit = auditing(now);
        UnstableFuseQueue queue = server.hbm$unstableFuses();
        SlotDeltaBuffer.Batch deltas = queue.takeSlots();
        if (deltas != null) {
            ReferenceOpenHashSet<Object> touched = null;
            for (int i = 0; i < deltas.size(); i++) {
                Object source = deltas.sources()[i];
                Object owner = InventoryChanges.owner(source);
                if (owner == null || queue.isChanged(owner)) continue;
                if (!InventoryOwnerAccess.of(owner).live(owner)) continue;
                int slot = deltas.slots()[i];
                if (slot != InventoryChanges.LAYOUT)
                    InventorySlotAccess.of(source).prepare(source, slot);
                SlotSource canonical = ItemInteriors.canonicalSource(server, source);
                if (canonical != null && canonical != source) {
                    source = canonical;
                    owner = canonical.inventoryOwner();
                }
                if (deltas.slots()[i] == InventoryChanges.LAYOUT) {
                    trackSource(queue, owner, source);
                    if (touched == null) touched = new ReferenceOpenHashSet<>();
                    touched.add(owner);
                    continue;
                }
                if (queue.wholeOwner(owner)) {
                    queue.changed(owner);
                    continue;
                }
                if (owner instanceof ItemInteriorBackend backend)
                    backend.slotChanged((SlotSource) source, slot);
                ItemStack item =
                        physicalSourceSlot(source, slot)
                                ? FuseSlots.read(source, slot)
                                : ItemStack.EMPTY;
                ItemInteriors.publish(owner, source, slot, item);
                long deadline = deadline(item);
                FuseSlots index = queue.slots(owner);
                if (index == null) {
                    if (deadline == Long.MAX_VALUE) continue;
                    index = new FuseSlots();
                }
                index.update(source, deltas.slots()[i], deadline);
                queue.slots(owner, index);
                if (touched == null) touched = new ReferenceOpenHashSet<>();
                touched.add(owner);
            }
            if (touched != null)
                for (Object owner : touched) {
                    FuseSlots index = queue.slots(owner);
                    queue.put(owner, index == null ? Long.MAX_VALUE : index.minimum());
                }
        }
        int changes = queue.takeChanges();
        if (changes != 0) {
            Sweep discovery = new Sweep(now, false);
            for (int i = 0; i < changes; i++) inspect(queue, discovery, queue.changedOwner(i));
        }
        ItemInteriors.flush(server);
        if (now < queue.nextDeadline() && !audit) return;
        Sweep sweep = new Sweep(now, true);
        List<Object> owners = new ArrayList<>();
        while (queue.nextDeadline() <= now) owners.add(queue.removeFirst());
        if (audit) {
            var visited = new ReferenceOpenHashSet<>(owners);
            for (ServerLevel level : server.getAllLevels()) {
                for (Object owner : level.hbm$storageEntities())
                    if (visited.add(owner)) owners.add(owner);
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (visited.add(player)) owners.add(player);
        }
        for (Object owner : owners) {
            FuseSlots index =
                    audit && InventoryOwnerAccess.of(owner) == InventoryOwnerAccess.ENTITY
                            ? null
                            : queue.slots(owner);
            if (index == null) inspect(queue, sweep, owner);
            else {
                sweep.indexOwner = null;
                sweep.index = null;
                sweep.beginOwner(owner);
                int due = index.due(now);
                for (int i = 0; i < due; i++) {
                    Object source = index.source(i);
                    int slot = index.slot(i);
                    long retry = sweep.indexedSlot(source, slot);
                    long next = Long.MAX_VALUE;
                    if (InventoryChanges.owner(source) == owner)
                        next =
                                retry != Long.MAX_VALUE
                                        ? retry
                                        : physicalSourceSlot(source, slot)
                                                ? deadline(FuseSlots.read(source, slot))
                                                : Long.MAX_VALUE;
                    index.update(source, slot, next);
                }
                if (InventoryOwnerAccess.of(owner).live(owner)) {
                    queue.put(owner, index.minimum());
                    queue.slots(owner, index);
                } else queue.remove(owner);
            }
        }
        sweep.publishChanges();
        ItemInteriors.flush(server);
        sweep.detonate();
    }

    private static boolean auditing(long now) {
        return now % RECOVERY_PERIOD == 0 && NeoForgeHbmConfig.runtime().unstableRecoverySweep();
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (chunk.getBlockEntities().isEmpty()) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            onBlockLoad(level, blockEntity);
        }
    }

    public static void onBlockLoad(ServerLevel level, BlockEntity owner) {
        if (!isStorageOwner(owner) || InventoryChanges.sources(owner) != null) return;
        InventoryChanges.bindBlock(owner);
    }

    public static void onEntityLoad(Entity entity) {
        if (!isStorageOwner(entity)) return;
        if (InventoryEntityAccess.of(entity).cargo) bindContainers(entity, entity);
        if (entity.level() instanceof ServerLevel level)
            inspect(
                    level.getServer().hbm$unstableFuses(),
                    new Sweep(level.getGameTime(), false),
                    entity);
    }

    private static boolean physicalSourceSlot(Object source, int slot) {
        InventorySlotAccess access = InventorySlotAccess.of(source);
        Object owner = access.owner(source);
        return owner != null
                && InventoryOwnerAccess.of(owner).live(owner)
                && access.physical(source, slot);
    }

    public static void menuSlotChanged(ServerPlayer player, Slot slot) {
        Container source = slot.container;
        int index = slot.getContainerSlot();
        while (source instanceof CompoundContainer compound) {
            int first = compound.container1.getContainerSize();
            if (index < first) source = compound.container1;
            else {
                index -= first;
                source = compound.container2;
            }
        }
        if (!physicalSlot(source, index)) return;
        Object owner = InventoryChanges.owner(source);
        if (owner == null) {
            if (menuOwnedSlot(slot, player)) changed(player);
            return;
        }
        if (InventoryOwnerAccess.of(owner) == InventoryOwnerAccess.BLOCK
                && !BlockStorage.supported((BlockEntity) owner)) return;
        slotChanged(owner, source, index);
    }

    private static void bindContainers(Entity entity, @Nullable Entity owner) {
        if (entity instanceof InventoryCarrier carrier) carrier.getInventory().hbm$fuseOwner(owner);
        if (entity instanceof AbstractHorse horse) horse.inventory.hbm$fuseOwner(owner);
        if (entity instanceof ServerPlayer player) {
            if (owner != null) InventoryChanges.bindPlayer(player);
            player.getEnderChestInventory().hbm$fuseOwner(owner);
            player.inventoryMenu.hbm$fuseOwner(owner);
            player.containerMenu.hbm$fuseOwner(owner);
        }
    }

    public static boolean isStorageOwner(BlockEntity blockEntity) {
        return BlockStorage.supported(blockEntity);
    }

    public static boolean isStorageOwner(Entity entity) {
        return InventoryEntityAccess.of(entity) != InventoryEntityAccess.NONE;
    }

    public static long deadline(ItemInstance stack) {
        return ItemContents.minimumDeadline(stack);
    }

    public static long deadline(ItemStack stack) {
        return ItemContents.minimumDeadline(stack);
    }

    public static long deadline(ItemStackTemplate stack) {
        return ItemContents.minimumDeadline(stack);
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
        if (player.containerMenu instanceof LecternMenu) return false;
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
        if (container instanceof SimpleContainer simple
                && simple.hbm$fuseOwner() != null
                && simple.hbm$fuseOwner() != player) return false;
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
        private Object scheduledOwner;
        private double x;
        private double y;
        private double z;
        private long nextDeadline = Long.MAX_VALUE;
        private List<Blast> blasts;
        private List<Runnable> changes;
        private int mutations;
        private Object indexOwner;
        private FuseSlots index;

        private Sweep(long now, boolean expire) {
            this.now = now;
            this.expire = expire;
        }

        private void noteSlot(Object source, int slot, ItemStack item) {
            InventorySlotAccess access = InventorySlotAccess.of(source);
            ItemInteriors.publish(scheduledOwner, source, slot, item);
            if (access != InventorySlotAccess.VIRTUAL && access != InventorySlotAccess.CONTAINER)
                InventorySlotBinding.bind(item, source, slot);
            if (indexOwner == null) return;
            if (access == InventorySlotAccess.VIRTUAL
                    || access == InventorySlotAccess.CONTAINER
                    || access.owner(source) != indexOwner) {
                if (deadline(item) != Long.MAX_VALUE) {
                    indexOwner = null;
                    index = null;
                }
                return;
            }
            long deadline = deadline(item);
            if (deadline == Long.MAX_VALUE) return;
            if (index == null) index = new FuseSlots();
            index.update(source, slot, deadline);
        }

        private void beginOwner(Object owner) {
            InventoryOwnerAccess access = InventoryOwnerAccess.of(owner);
            Object physical = access.carrier(owner);
            carrier = physical instanceof Entity entity ? entity : null;
            blockOwner = physical instanceof BlockEntity block ? block : null;
            scheduledOwner = owner;
            level = access.level(owner);
            if (physical == null) return;
            x = access.x(owner);
            y = access.y(owner);
            z = access.z(owner);
        }

        private long indexedSlot(Object source, int slot) {
            if (InventoryChanges.owner(source) != scheduledOwner
                    || !ownerPresent()
                    || !physicalSourceSlot(source, slot)) return Long.MAX_VALUE;
            InventorySlotAccess access = InventorySlotAccess.of(source);
            ItemStack item = access.read(source, slot);
            boolean direct = access.direct(source, item);
            ItemStack replacement = direct ? item : item.copy();
            int before = mutations;
            int beforeBlasts = blasts == null ? 0 : blasts.size();
            if (!stack(replacement)) return Long.MAX_VALUE;
            if (!access.replace(
                    source, slot, item, replacement.isEmpty() ? ItemStack.EMPTY : replacement)) {
                assert !direct;
                refuse(before, beforeBlasts);
                return now + 1;
            }
            ItemInteriors.publish(scheduledOwner, source, slot, replacement);
            if (blockOwner != null && ownerPresent())
                level.getChunkSource()
                        .getChunkNow(
                                blockOwner.getBlockPos().getX() >> 4,
                                blockOwner.getBlockPos().getZ() >> 4)
                        .markUnsaved();
            InventoryPublications.afterReplacement(
                    access,
                    source,
                    slot,
                    scheduledOwner,
                    carrier != null ? carrier : blockOwner,
                    this);
            return Long.MAX_VALUE;
        }

        private boolean ownerPresent() {
            if (scheduledOwner instanceof InventoryLocation
                    && !InventoryOwnerAccess.of(scheduledOwner).live(scheduledOwner)) return false;
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
            if (!BlockStorage.supported(blockEntity)) return false;
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
            if (container instanceof RandomizableContainer loot && loot.getLootTable() != null)
                return false;
            if (container instanceof ContainerEntity loot && loot.getContainerLootTable() != null)
                return false;
            int before = mutations;
            for (int slot = 0, size = container.getContainerSize(); slot < size; slot++) {
                if (!physicalSlot(container, slot)) continue;
                ItemStack item = container.getItem(slot);
                if (!due(item)) continue;
                int previousMutations = mutations;
                int previousBlasts = blasts == null ? 0 : blasts.size();
                ItemStack replacement = item.copy();
                if (stack(replacement)) {
                    ItemStack installed = replacement.isEmpty() ? ItemStack.EMPTY : replacement;
                    container.setItem(slot, installed);
                    if (ownerPresent() && !ItemStack.matches(container.getItem(slot), installed))
                        refuse(previousMutations, previousBlasts);
                }
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
                ItemStack replacement =
                        expire && deadline(item) <= now && ItemContents.copiedCarrier(item)
                                ? item.copy()
                                : item;
                if (stack(replacement)) {
                    if (replacement != item) {
                        container.setItem(slot, replacement);
                        if (!ownerPresent()) break;
                        item = container.getItem(slot);
                    }
                    changed = true;
                }
                noteSlot(container, slot, item);
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
            scheduledOwner = entity;
            carrier = entity;
            blockOwner = null;
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            if (entity instanceof ServerPlayer player) {
                int before = mutations;
                inventory(player.getInventory());
                inventory(player.getEnderChestInventory());
                AbstractContainerMenu menu = player.containerMenu;
                ItemStackContainer lastMirror = null;
                for (Slot slot : menu.slots) {
                    if (slot.container instanceof ItemStackContainer view) {
                        if (view.target().isEmpty()
                                || view.target().has(DataComponents.CONTAINER)) {
                            if (mutations != before && view != lastMirror)
                                afterChanges(
                                        () -> {
                                            if (player.containerMenu == menu)
                                                view.refreshFromTarget();
                                        });
                            lastMirror = view;
                            continue;
                        }
                        if (!view.stillValid(player)) continue;
                    }
                    if (!menuOwnedSlot(slot, player)) continue;
                    ItemStack item = slot.getItem();
                    if (stack(item))
                        afterChanges(
                                () -> {
                                    if (player.containerMenu == menu && slot.getItem() == item) {
                                        slot.setByPlayer(item.isEmpty() ? ItemStack.EMPTY : item);
                                    }
                                });
                    noteSlot(slot.container, slot.getContainerSlot(), item);
                }
                ItemStack carried = menu.getCarried();
                if (stack(carried))
                    afterChanges(
                            () -> {
                                if (player.containerMenu == menu && menu.getCarried() == carried) {
                                    menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                                }
                            });
                noteSlot(menu, -1, carried);
            } else {
                if (InventoryEntityAccess.of(entity).cargo) {
                    if (entity instanceof Container container && nativeOwner(entity))
                        inventory(container);
                    else if (entity instanceof Container container
                            && !(entity instanceof StoredItems)) visitOwned(container);
                    if (entity instanceof InventoryCarrier container)
                        inventory(container.getInventory());
                    if (entity instanceof AbstractHorse horse) inventory(horse.inventory);
                }
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
                        noteSlot(living, slot.ordinal(), item);
                    }
                }
            }
            if (entity instanceof StoredItems stored && InventoryEntityAccess.of(entity).cargo)
                stored.visitStoredItems(this);
            if (entity instanceof ItemEntity item) {
                ItemStack contents = item.getItem();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!item.isRemoved() && item.getItem() == contents)
                                    item.setItem(contents.copy());
                            });
                noteSlot(item, 0, contents);
            } else if (entity instanceof ItemFrame frame) {
                ItemStack contents = frame.getItem();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!frame.isRemoved() && frame.getItem() == contents)
                                    frame.setItem(contents.copy());
                            });
                noteSlot(frame, 0, contents);
            } else if (entity instanceof Display.ItemDisplay display) {
                ItemStack contents = display.getItemStack();
                if (stack(contents))
                    afterChanges(
                            () -> {
                                if (!display.isRemoved() && display.getItemStack() == contents)
                                    display.setItemStack(contents.copy());
                            });
                noteSlot(display, 0, contents);
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
            boolean changed = ItemContents.rewrite(stack, level, this);
            long next = deadline(stack);
            if (next < nextDeadline) nextDeadline = next;
            if (changed) mutations++;
            return changed;
        }

        @Override
        public boolean selects(ItemInstance item) {
            return deadline(item) <= now;
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
}
