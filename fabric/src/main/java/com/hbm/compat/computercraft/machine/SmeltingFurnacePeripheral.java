package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.BlockEntitySmeltingFurnace;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SmeltingFurnacePeripheral<T extends BlockEntitySmeltingFurnace> extends SnapshotPeripheral<T> {
    public SmeltingFurnacePeripheral(T machine) {
        super(machine, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType()).toString(), 0, 0);
    }

    @Override
    protected void capture(T machine) {
    }

    @Override
    public @NonNull Set<String> getAdditionalTypes() {
        return Set.of("inventory");
    }

    @LuaFunction(mainThread = true)
    public int size() {
        return machine().getContainerSize();
    }

    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, Object>> list() {
        T furnace = machine();
        Map<Integer, Map<String, Object>> items = new HashMap<>();
        for (int slot = 0; slot < furnace.getContainerSize(); slot++) {
            ItemStack stack = furnace.getItem(slot);
            if (!stack.isEmpty()) {
                items.put(slot + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(furnace.getLevel().registryAccess(), stack));
            }
        }
        return items;
    }

    @LuaFunction(mainThread = true)
    public @Nullable Map<String, Object> getItemDetail(int slot) throws LuaException {
        T furnace = machine();
        int index = index(slot, furnace.getContainerSize());
        ItemStack stack = furnace.getItem(index);
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(furnace.getLevel().registryAccess(), stack);
    }

    @LuaFunction(mainThread = true)
    public int getItemLimit(int slot) throws LuaException {
        T furnace = machine();
        ItemStack stack = furnace.getItem(index(slot, furnace.getContainerSize()));
        return stack.isEmpty() ? furnace.getMaxStackSize() : stack.getMaxStackSize();
    }

    @LuaFunction(mainThread = true)
    public int pushItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        SlottedStorage<ItemVariant> from = ContainerStorage.of(furnace, null);
        SlottedStorage<ItemVariant> to = connectedInventory(computer, args.getString(0));
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    @LuaFunction(mainThread = true)
    public int pullItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        SlottedStorage<ItemVariant> from = connectedInventory(computer, args.getString(0));
        SlottedStorage<ItemVariant> to = ContainerStorage.of(furnace, null);
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    private static int index(int slot, int size) throws LuaException {
        if (slot < 1 || slot > size) throw new LuaException("Slot out of range (" + slot + ")");
        return slot - 1;
    }

    private static SlottedStorage<ItemVariant> connectedInventory(IComputerAccess computer, String name) throws LuaException {
        IPeripheral peripheral = computer.getAvailablePeripheral(name);
        if (peripheral == null || !"inventory".equals(peripheral.getType()) && !peripheral.getAdditionalTypes().contains("inventory")) {
            throw new LuaException("Target is not an inventory");
        }
        Object target = peripheral.getTarget();
        if (target instanceof Container container) return ContainerStorage.of(container, null);
        if (target instanceof BlockEntity be && be.getLevel() != null) {
            SlottedStorage<ItemVariant> handler = slotted(ItemStorage.SIDED.find(be.getLevel(), be.getBlockPos(), null));
            if (handler != null) return handler;
            for (Direction side : Direction.values()) {
                handler = slotted(ItemStorage.SIDED.find(be.getLevel(), be.getBlockPos(), side));
                if (handler != null) return handler;
            }
        }
        throw new LuaException("Target has no accessible item storage");
    }

    private static @Nullable SlottedStorage<ItemVariant> slotted(@Nullable Storage<ItemVariant> storage) {
        return storage instanceof SlottedStorage<?> ? (SlottedStorage<ItemVariant>) storage : null;
    }

    private static int transfer(SlottedStorage<ItemVariant> from, SlottedStorage<ItemVariant> to, int fromSlot, int limit, int toSlot) throws LuaException {
        int source = index(fromSlot, from.getSlotCount());
        if (toSlot != 0) index(toSlot, to.getSlotCount());
        if (limit < 0) throw new LuaException("Limit must be non-negative");
        if (limit == 0 || from == to) return 0;

        SingleSlotStorage<ItemVariant> sourceSlot = from.getSlot(source);
        if (sourceSlot.isResourceBlank()) return 0;
        ItemVariant resource = sourceSlot.getResource();
        int remaining = (int) Math.min(limit, sourceSlot.getAmount());
        int moved = 0;
        for (int destination = toSlot == 0 ? 0 : toSlot - 1; destination < (toSlot == 0 ? to.getSlotCount() : toSlot) && remaining > 0; destination++) {
            SingleSlotStorage<ItemVariant> destinationSlot = to.getSlot(destination);
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = destinationSlot.insert(resource, remaining, transaction);
                if (inserted == 0) continue;
                long extracted = sourceSlot.extract(resource, inserted, transaction);
                if (extracted != inserted) continue;
                transaction.commit();
                moved += (int) extracted;
                remaining -= (int) extracted;
            }
        }
        return moved;
    }
}
