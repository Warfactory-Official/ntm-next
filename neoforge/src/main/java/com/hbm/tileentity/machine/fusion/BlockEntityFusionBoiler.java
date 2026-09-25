// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityFusionBoiler extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                FluidTankEndpoint,
                IFusionPowerReceiver,
                SyncUnitSchema {
    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    public long plasmaEnergy;

    @SyncField(units = 1L << 0)
    public long plasmaEnergySync;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityFusionBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BOILER.get(), pos, state);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.WATER, 32_000),
                    new FluidTankNTM(NTMFluids.SUPERHOTSTEAM, 32_000)
                };
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4),
                        dir));
    }

    public void tickServer() {
        plasmaEnergySync = plasmaEnergy;
        plasmaEnergy = 0;

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(
            long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;

        FT_Heatable heatable =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (heatable == null) return;

        int waterCycles = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());

        int steamCycles =
                (int) Math.min(fusionPower / heatable.getFirstStep().heatReq, waterCycles);

        if (steamCycles <= 0) return;

        tanks[0].setFill(tanks[0].getFill() - steamCycles);
        tanks[1].setFill(tanks[1].getFill() + steamCycles);

        if (level.getRandom().nextInt(200) == 0) {
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5,
                    ModSounds.BOILER_GROAN.get(),
                    SoundSource.BLOCKS,
                    2.5F,
                    1.0F);
        }
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    private void readPlasmaEnergy(ByteBuf input) {
        plasmaEnergy = input.readLong();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.plasmaEnergySync);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPlasmaEnergy(input);
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
