// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuMachineStrandCaster;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {0},
        units = 1L << 2,
        components = false)
public class BlockEntityMachineStrandCaster extends BlockEntityFoundryCastingBase
        implements FluidTankEndpoint, MenuProvider {

    public static final int SLOT_MOLD = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int SLOT_OUTPUT_END = 6;
    public static final int SLOT_COUNT = 7;

    public static final int TANK_CAPACITY = 64_000;

    public static final int BATCH = 9;
    public static final int IDLE_FLUSH = 200;

    public static final int MOLDLESS_CAPACITY = 50_000;

    private static final int[] ACCESSIBLE_SLOTS = {1, 2, 3, 4, 5, 6};

    @SyncField(units = 1L << 3)
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SPENTSTEAM, TANK_CAPACITY);

    private long lastProgressTick;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineStrandCaster(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRAND_CASTER.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {water};
        sending = new FluidTankNTM[] {steam};
    }

    @Override
    public void tickServer() {

        if (amount > getCapacity() && type != null) {
            ItemStack scrap =
                    ItemScraps.create(new MaterialStack(type, Math.max(amount - getCapacity(), 0)));
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 2,
                            worldPosition.getZ() + 0.5,
                            scrap));
            this.amount = getCapacity();
        }

        if (this.amount == 0) this.type = null;

        int moldsToCast = maxProcessable();

        if (moldsToCast > 0
                && (moldsToCast >= BATCH || level.getGameTime() >= lastProgressTick + IDLE_FLUSH)) {
            Mold mold = this.getInstalledMold();
            this.amount -= moldsToCast * mold.getCost();

            ItemStack out = mold.getOutput(type);
            int remaining = out.getCount() * moldsToCast;
            final int maxStackSize = out.getMaxStackSize();

            for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                if (remaining <= 0) break;

                ItemStack slot = inventory.get(i);
                if (slot.isEmpty()) {
                    int toDeposit = Math.min(remaining, maxStackSize);
                    inventory.set(i, out.copyWithCount(toDeposit));
                    remaining -= toDeposit;
                } else if (ItemStack.isSameItemSameComponents(slot, out)) {
                    int toDeposit = Math.min(remaining, maxStackSize - slot.getCount());
                    slot.grow(toDeposit);
                    remaining -= toDeposit;
                }
            }

            setChanged();

            water.setFill(water.getFill() - getWaterRequired() * moldsToCast);
            steam.setFill(steam.getFill() + getWaterRequired() * moldsToCast);

            lastProgressTick = level.getGameTime();
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    private int maxProcessable() {
        Mold mold = this.getInstalledMold();
        if (type == null || mold == null || mold.getOutput(type) == null) return 0;

        ItemStack out = mold.getOutput(type);
        int freeSlots = 0;
        final int stackLimit = out.getMaxStackSize();

        for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) freeSlots += stackLimit;
            else if (ItemStack.isSameItemSameComponents(slot, out))
                freeSlots += stackLimit - slot.getCount();
        }

        int moldsToCast = amount / mold.getCost();
        moldsToCast = Math.min(moldsToCast, freeSlots / out.getCount());
        moldsToCast = Math.min(moldsToCast, water.getFill() / getWaterRequired());
        moldsToCast =
                Math.min(moldsToCast, (steam.getMaxFill() - steam.getFill()) / getWaterRequired());

        return moldsToCast;
    }

    public BlockPos[] getMetalPourPos() {
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        Direction rot = facing.getClockWise();
        BlockPos back = worldPosition.relative(facing.getOpposite()).above(2);
        BlockPos front = worldPosition.above(2);

        return new BlockPos[] {back.relative(rot), back, front.relative(rot), front};
    }

    @Override
    public @Nullable Mold getInstalledMold() {
        return inventory.get(SLOT_MOLD).getItem() instanceof ItemMold item ? item.mold : null;
    }

    @Override
    public int getMoldSize() {
        Mold mold = getInstalledMold();
        return mold == null ? 0 : mold.size;
    }

    @Override
    public boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        for (BlockPos port : getMetalPourPos()) {
            if (port.equals(pos)) return this.standardCheck(level, pos, side, stack);
        }
        return false;
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return stack;
    }

    @Override
    public boolean standardCheck(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (this.type != null && this.type != stack.material) return false;
        Mold mold = this.getInstalledMold();
        if (mold == null) return false;
        return this.amount < mold.getCost() * BATCH;
    }

    @Override
    public @Nullable MaterialStack standardAdd(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.material;
        Mold mold = this.getInstalledMold();
        int limit = mold != null ? mold.getCost() * BATCH : this.getCapacity();
        if (stack.amount + this.amount <= limit) {
            this.amount += stack.amount;
            return null;
        }

        int required = limit - this.amount;
        this.amount = limit;
        stack.amount -= required;

        lastProgressTick = level.getGameTime();

        return stack;
    }

    @Override
    public int getCapacity() {
        Mold mold = this.getInstalledMold();
        return mold == null ? MOLDLESS_CAPACITY : mold.getCost() * 10;
    }

    public int getWaterRequired() {
        Mold mold = this.getInstalledMold();
        return mold != null ? 5 * mold.getCost() : 50;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_MOLD && stack.getItem() instanceof ItemMold;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !canPlaceItem(slot, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineStrandCaster");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineStrandCaster(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(water::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
        lastProgressTick = input.getLongOr("lastProgress", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
        output.putLong("lastProgress", lastProgressTick);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x18L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> serializeMold(output);
            case 3 -> this.water.packetSerialize(output);
            case 4 -> this.steam.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> deserializeMold(input);
            case 3 -> this.water.packetDeserialize(input);
            case 4 -> this.steam.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
