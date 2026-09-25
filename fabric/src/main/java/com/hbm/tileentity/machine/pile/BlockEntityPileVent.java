// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.machine.pile.BlockEntityPileCore.PileChannel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPileVent extends BlockEntityPileDeviceBase
        implements IFluidHandlerMK2, SyncUnitSchema {
    @SyncField(units = 1L)
    public final FluidTankNTM compair = new FluidTankNTM(NTMFluids.AIR, 4_000).withPressure(1);

    @SyncField(units = 1L << 1)
    public boolean active;

    public float fan;
    public float lastFan;

    public BlockEntityPileVent(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_VENT.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        active = false;
        PileChannel channel = attachedChannel(BlockPile.Role.AIR_IN);
        if (channel != null) {
            int fill = Math.min(compair.getFill(), PileChannel.MAX_AIR - channel.air);
            channel.air += fill;
            compair.setFill(compair.getFill() - fill);
            active = fill > 0;
            if (active) setChanged();
        }
        networkPackNT(35);
    }

    @Override
    public void tickClient() {
        lastFan = fan;
        if (active) {
            fan += 45F;
            if (level.getRandom().nextInt(20) == 0)
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D,
                        0D,
                        0.05D,
                        0D);
        }
        if (fan >= 360F) {
            lastFan -= 360F;
            fan -= 360F;
        }
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!compair.accepts(type) || pressure != compair.getPressure()) return 0L;
        return compair.getMaxFill() - compair.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!compair.accepts(type) || pressure != compair.getPressure()) return amount;
        int accepted = compair.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid type) {
        return new int[] {compair.getPressure(), compair.getPressure()};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {compair};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        compair.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> compair.packetSerialize(output);
            case 1 -> output.writeBoolean(active);
            case 2 -> output.writeInt(channelNumber);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> compair.packetDeserialize(input);
            case 1 -> active = input.readBoolean();
            case 2 -> channelNumber = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
