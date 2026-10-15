package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.BlockEntitySmeltingFurnace;
import com.hbm.capability.MachineItemHandler;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
    public final int size() {
        return machine().getContainerSize();
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, Object>> list() {
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
    public final @Nullable Map<String, Object> getItemDetail(int slot) throws LuaException {
        T furnace = machine();
        int index = index(slot, furnace.getContainerSize());
        ItemStack stack = furnace.getItem(index);
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(furnace.getLevel().registryAccess(), stack);
    }

    @LuaFunction(mainThread = true)
    public final int getItemLimit(int slot) throws LuaException {
        T furnace = machine();
        ItemStack stack = furnace.getItem(index(slot, furnace.getContainerSize()));
        return stack.isEmpty() ? furnace.getMaxStackSize() : stack.getMaxStackSize();
    }

    @LuaFunction(mainThread = true)
    public final int pushItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        ResourceHandler<ItemResource> from = MachineItemHandler.of(furnace, Direction.NORTH);
        ResourceHandler<ItemResource> to = connectedInventory(computer, args.getString(0));
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    @LuaFunction(mainThread = true)
    public final int pullItems(IComputerAccess computer, IArguments args) throws LuaException {
        T furnace = machine();
        ResourceHandler<ItemResource> from = connectedInventory(computer, args.getString(0));
        ResourceHandler<ItemResource> to = MachineItemHandler.of(furnace, Direction.NORTH);
        return transfer(from, to, args.getInt(1), args.optInt(2, 64), args.optInt(3, 0));
    }

    private static int index(int slot, int size) throws LuaException {
        if (slot < 1 || slot > size) throw new LuaException("Slot out of range (" + slot + ")");
        return slot - 1;
    }

    private static ResourceHandler<ItemResource> connectedInventory(IComputerAccess computer, String name) throws LuaException {
        IPeripheral peripheral = computer.getAvailablePeripheral(name);
        if (peripheral == null || !"inventory".equals(peripheral.getType()) && !peripheral.getAdditionalTypes().contains("inventory")) {
            throw new LuaException("Target is not an inventory");
        }
        Object target = peripheral.getTarget();
        if (target instanceof BlockEntitySmeltingFurnace furnace)
            return MachineItemHandler.of(furnace, Direction.NORTH);
        if (target instanceof BlockEntity be && be.getLevel() != null) {
            ResourceHandler<ItemResource> handler = be.getLevel().getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), null);
            if (handler != null) return handler;
            for (Direction side : Direction.values()) {
                handler = be.getLevel().getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), side);
                if (handler != null) return handler;
            }
        }
        if (target instanceof Container container) return VanillaContainerWrapper.of(container);
        throw new LuaException("Target has no accessible item storage");
    }

    private static int transfer(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, int fromSlot, int limit, int toSlot) throws LuaException {
        int source = index(fromSlot, from.size());
        if (toSlot != 0) index(toSlot, to.size());
        if (limit < 0) throw new LuaException("Limit must be non-negative");
        if (limit == 0 || from == to) return 0;

        ItemResource resource = from.getResource(source);
        int remaining = (int) Math.min(limit, from.getAmountAsLong(source));
        int moved = 0;
        for (int destination = toSlot == 0 ? 0 : toSlot - 1; destination < (toSlot == 0 ? to.size() : toSlot) && remaining > 0; destination++) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = to.insert(destination, resource, remaining, transaction);
                if (inserted == 0) continue;
                int extracted = from.extract(source, resource, inserted, transaction);
                if (extracted != inserted) continue;
                transaction.commit();
                moved += extracted;
                remaining -= extracted;
            }
        }
        return moved;
    }
}
