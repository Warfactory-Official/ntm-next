// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineIntake extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                AudioLoop,
                IEnergyHandlerMK2,
                IFluidHandlerMK2,
                SyncUnitSchema {

    public static final long MAX_POWER = 2_000;
    private static final long POWER_PER_TICK = MAX_POWER / SharedConstants.TICKS_PER_SECOND;
    private static final int AIR_TANK_CAPACITY = 1_000;
    private static final float FAN_SPEED = 45F;
    private static final int TARGET_COUNT = 8;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM compair = new FluidTankNTM(NTMFluids.AIR, AIR_TANK_CAPACITY);

    @SyncField(units = 1L << 0)
    public long power;

    public float fan;
    public float prevFan;

    private @Nullable Direction boundFacing;
    private BlockLookupCache<IFluidHandlerMK2> @Nullable [] airTargets;
    private BlockLookupCache<IEnergyHandlerMK2> @Nullable [] powerTargets;

    public BlockEntityMachineIntake(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTAKE.get(), pos, state);
    }

    public void tickServer() {
        if (power >= POWER_PER_TICK) {
            compair.setFill(compair.getMaxFill());
            power -= POWER_PER_TICK;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        bindTargets(serverLevel);

        for (int i = 0; i < TARGET_COUNT; i++) poll(i);

        networkPackNT(50);
    }

    public void tickClient() {
        prevFan = fan;
        boolean running = power >= POWER_PER_TICK;
        if (running) {
            fan += FAN_SPEED;
            if (fan >= 360F) {
                fan -= 360F;
                prevFan -= 360F;
            }
        }
        audioLoop(running, 0.25F);
    }

    private Direction facing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    @SuppressWarnings("unchecked")
    private void bindTargets(ServerLevel level) {
        Direction dir = facing();
        if (boundFacing == dir && airTargets != null) return;
        boundFacing = dir;
        Direction rot = dir.getClockWise();
        int[] df = {1, 1, -2, -2, 0, -1, 0, -1};
        int[] dr = {0, 1, 0, 1, 2, 2, -1, -1};
        Direction[] from = {
            dir,
            dir,
            dir.getOpposite(),
            dir.getOpposite(),
            rot,
            rot,
            rot.getOpposite(),
            rot.getOpposite()
        };
        airTargets = new BlockLookupCache[TARGET_COUNT];
        powerTargets = new BlockLookupCache[TARGET_COUNT];
        for (int i = 0; i < TARGET_COUNT; i++) {
            BlockPos target = at(dir, rot, df[i], dr[i]);
            Direction side = from[i].getOpposite();
            airTargets[i] =
                    Services.CAPS.createCache(
                            FluidCaps.RECEIVER,
                            level,
                            target,
                            FluidFace.of(side, compair.getTankType()));
            powerTargets[i] = Services.CAPS.createCache(EnergyCaps.PROVIDER, level, target, side);
        }
    }

    private BlockPos at(Direction dir, Direction rot, int df, int dr) {
        return worldPosition.offset(
                dir.getStepX() * df + rot.getStepX() * dr,
                0,
                dir.getStepZ() * df + rot.getStepZ() * dr);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.MOTOR_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.25F,
                10F,
                1.0F,
                20);
    }

    private void poll(int index) {
        if (compair.getFill() > 0) {
            IFluidHandlerMK2 receiver = airTargets[index].find();
            if (receiver != null) {
                long demand =
                        Math.min(
                                receiver.getDemand(compair.getFluid(), compair.getPressure()),
                                receiver.getReceiverSpeed(
                                        compair.getFluid(), compair.getPressure()));
                if (demand > 0) {
                    long push = Math.min(demand, compair.getFill());
                    long refused =
                            receiver.transferFluid(compair.getFluid(), compair.getPressure(), push);
                    long accepted = push - refused;
                    if (accepted > 0) {
                        compair.drain((int) accepted, true);
                        setChanged();
                    }
                }
            }
        }

        IEnergyHandlerMK2 provider = powerTargets[index].find();
        if (provider != null) {
            long room = MAX_POWER - power;
            long available = Math.min(provider.getPower(), provider.getProviderSpeed());
            if (room > 0 && available > 0) {
                long pull = Math.min(room, available);
                provider.usePower(pull);
                power += pull;
                setChanged();
            }
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        return compair.provides(type) && pressure == compair.getPressure() ? compair.getFill() : 0;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (compair.provides(type) && pressure == compair.getPressure())
            compair.setFill((int) Math.max(0, compair.getFill() - amount));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", power);
        input.child("compair").ifPresent(compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        compair.serialize(output.child("compair"));
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> this.compair.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.compair.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
