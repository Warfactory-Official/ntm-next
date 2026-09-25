// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.capability.port.ItemPort;
import com.hbm.tileentity.BlockEntityMachineBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineItemHandler implements ResourceHandler<ItemResource> {

    private static final Map<BlockEntityMachineBase, PerMachine> HANDLERS = new WeakHashMap<>();

    private static final int NULL_SIDE = 6;

    private static final int MAX_PORTS_PER_SIDE = 8;

    private final WeakReference<BlockEntityMachineBase> machineRef;
    private final ResourceHandler<ItemResource> core;
    private final @Nullable Direction side;
    private final boolean[] accessible;

    private final @Nullable ItemPort access;

    public MachineItemHandler(BlockEntityMachineBase machine, @Nullable Direction side) {
        this(machine, side, side == null ? null : machine.getSlotsForFace(side), null);
    }

    public MachineItemHandler(
            BlockEntityMachineBase machine, @Nullable Direction side, ItemPort access) {
        this(machine, side, access.slots(), access);
    }

    public MachineItemHandler(
            BlockEntityMachineBase machine, @Nullable Direction side, int @Nullable [] slots) {
        this(machine, side, slots, null);
    }

    private MachineItemHandler(
            BlockEntityMachineBase machine,
            @Nullable Direction side,
            int @Nullable [] slots,
            @Nullable ItemPort access) {
        this.access = access;
        this.machineRef = new WeakReference<>(machine);
        this.core = VanillaContainerWrapper.of(machine);
        this.side = side;
        int n = machine.inventory.size();
        this.accessible = new boolean[n];
        if (slots == null) {
            Arrays.fill(accessible, true);
        } else {
            for (int slot : slots) {
                if (slot >= 0 && slot < n) accessible[slot] = true;
            }
        }
    }

    public static MachineItemHandler of(BlockEntityMachineBase machine, @Nullable Direction side) {
        return handlerFor(machine, side, null);
    }

    public static MachineItemHandler of(
            BlockEntityMachineBase machine, @Nullable Direction side, ItemPort access) {
        return handlerFor(machine, side, access);
    }

    private static synchronized MachineItemHandler handlerFor(
            BlockEntityMachineBase machine, @Nullable Direction side, @Nullable ItemPort access) {
        if (machine.isRemoved()) {
            return access == null
                    ? new MachineItemHandler(machine, side)
                    : new MachineItemHandler(machine, side, access);
        }
        PerMachine perMachine = HANDLERS.computeIfAbsent(machine, m -> new PerMachine());
        int index = side == null ? NULL_SIDE : side.ordinal();
        Int2ObjectOpenHashMap<WeakReference<MachineItemHandler>> byAccess =
                perMachine.bySide[index];
        if (byAccess == null) {
            byAccess = new Int2ObjectOpenHashMap<>();
            perMachine.bySide[index] = byAccess;
        }
        int key = access == null ? 0 : System.identityHashCode(access);
        WeakReference<MachineItemHandler> ref = byAccess.get(key);
        MachineItemHandler handler = ref == null ? null : ref.get();

        if (handler != null && handler.access == access) return handler;
        handler =
                access == null
                        ? new MachineItemHandler(machine, side)
                        : new MachineItemHandler(machine, side, access);
        if (access == null || byAccess.size() < MAX_PORTS_PER_SIDE)
            byAccess.put(key, new WeakReference<>(handler));
        return handler;
    }

    private @Nullable BlockEntityMachineBase live() {
        BlockEntityMachineBase machine = machineRef.get();
        return machine != null && !machine.isRemoved() ? machine : null;
    }

    @Override
    public int size() {
        return core.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return live() == null ? ItemResource.EMPTY : core.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return live() == null ? 0 : core.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return live() == null ? 0 : core.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return core.isValid(index, resource);
    }

    @Override
    public int insert(
            int index, ItemResource resource, int amount, TransactionContext transaction) {
        BlockEntityMachineBase machine = live();
        if (machine == null || !accessible[index]) return 0;
        ItemStack incoming = resource.toStack(1);

        boolean allowed =
                access != null
                        ? access.canInsert(index, incoming)
                        : machine.canPlaceItemThroughFace(index, incoming, side);
        return allowed ? core.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(
            int index, ItemResource resource, int amount, TransactionContext transaction) {
        BlockEntityMachineBase machine = live();
        if (machine == null || !accessible[index]) return 0;
        boolean allowed =
                access != null
                        ? access.canExtract(index, machine.inventory.get(index))
                        : side == null
                                || machine.canTakeItemThroughFace(
                                        index, machine.inventory.get(index), side);
        return allowed ? core.extract(index, resource, amount, transaction) : 0;
    }

    private static final class PerMachine {
        @SuppressWarnings("unchecked")
        final Int2ObjectOpenHashMap<WeakReference<MachineItemHandler>>[] bySide =
                new Int2ObjectOpenHashMap[NULL_SIDE + 1];
    }
}
