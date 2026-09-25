// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityMachinePumpBase extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                FluidFlushSender,
                IFluidCopiable,
                SyncUnitSchema {

    public static @Nullable Consumer<BlockEntityMachinePumpBase> CLIENT_SOUND;

    @SyncField(units = 1L << 2)
    public final FluidTankNTM water;

    @SyncField(units = 1L << 0)
    public boolean isOn = false;

    @SyncField(units = 1L << 1)
    public boolean onGround = false;

    public float rotor;
    public float lastRotor;

    public int groundCheckDelay = 0;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    protected BlockEntityMachinePumpBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int waterCapacity) {
        super(type, pos, state);
        this.water = new FluidTankNTM(NTMFluids.WATER, waterCapacity);
        this.sending = new FluidTankNTM[] {water};
    }

    private static boolean isValidGround(Block b) {
        return b == Blocks.GRASS_BLOCK
                || b == Blocks.DIRT
                || b == Blocks.SAND
                || b == Blocks.MYCELIUM
                || b == ModBlocks.WASTE_EARTH.get()
                || b == ModBlocks.DIRT_DEAD.get()
                || b == ModBlocks.DIRT_OILY.get()
                || b == ModBlocks.SAND_DIRTY.get()
                || b == ModBlocks.SAND_DIRTY_RED.get();
    }

    public void tickServer() {

        flush.provide((ServerLevel) level, this);

        if (groundCheckDelay > 0) {
            groundCheckDelay--;
        } else {
            onGround = checkGround();
        }

        isOn = false;
        if (canOperate()
                && worldPosition.getY() <= MachineData.PUMP_GROUND_HEIGHT.get()
                && onGround) {
            isOn = true;
            operate();
        }

        networkPackNT(150);
    }

    public void tickClient() {
        lastRotor = rotor;
        if (isOn) rotor += 10F;

        if (rotor >= 360F) {
            rotor -= 360F;
            lastRotor -= 360F;

            if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
        }
    }

    protected abstract boolean canOperate();

    protected abstract void operate();

    protected boolean checkGround() {
        if (!level.dimensionType().hasSkyLight()) return false;

        int valid = 0, invalid = 0;
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
        int depth = MachineData.PUMP_GROUND_DEPTH.get();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -depth; y--) {
                for (int z = -1; z <= 1; z++) {
                    scratch.set(
                            worldPosition.getX() + x,
                            worldPosition.getY() + y,
                            worldPosition.getZ() + z);
                    BlockState state = level.getBlockState(scratch);

                    if (y == -1 && !(state.isSolidRender() && !state.isSignalSource()))
                        return false;
                    if (isValidGround(state.getBlock())) valid++;
                    else invalid++;
                }
            }
        }
        return valid >= invalid;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (water.provides(type) && pressure == water.getPressure()) return water.getFill();
        return 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!water.provides(type) || pressure != water.getPressure()) return;
        int drained = (int) Math.min(amount, Integer.MAX_VALUE);
        if (water.drain(drained, true) > 0) setChanged();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {water};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(water::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeBoolean(this.onGround);
            case 2 -> this.water.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isOn = input.readBoolean();
            case 1 -> this.onGround = input.readBoolean();
            case 2 -> this.water.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
