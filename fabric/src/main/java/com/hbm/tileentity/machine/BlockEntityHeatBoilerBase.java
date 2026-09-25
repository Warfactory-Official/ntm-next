// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.TomSaveData;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.NeighborDerived;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityHeatBoilerBase extends BlockEntityMachineBase
        implements AudioLoop, FluidFlushSender, IFluidCopiable, IRORValueProvider, SyncUnitSchema {

    public static final String[] ROR =
            new String[] {PREFIX_VALUE + "input", PREFIX_VALUE + "output"};
    public static @Nullable Consumer<BlockEntityHeatBoilerBase> CLIENT_SOUND;

    @SyncField(units = (1L << 3) | (1L << 4))
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    @SyncField(value = 2, units = 0)
    public int heat;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    @SyncField(units = 1L << 1)
    public boolean hasExploded;

    public int audioTime;

    @SyncField(units = 1L)
    private int syncHeat;

    @SyncField(units = 1L << 3)
    private int syncInputFill;

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    protected BlockEntityHeatBoilerBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int waterCap) {
        super(type, pos, state, 0);
        this.tanks[0] = new FluidTankNTM(NTMFluids.WATER, waterCap);
        this.tanks[1] = new FluidTankNTM(NTMFluids.STEAM, waterCap * 100);
        sending = new FluidTankNTM[] {tanks[1]};
    }

    protected abstract int maxHeat();

    protected abstract double diffusion();

    protected abstract boolean canExplode();

    @Override
    public void tickServer() {

        if (hasExploded) {
            networkPackNT(25);
            return;
        }

        setupTanks();
        tryPullHeat();
        syncHeat = heat;

        ServerLevel serverLevel = (ServerLevel) level;
        if (level.getBrightness(LightLayer.SKY, worldPosition) > 7
                && TomSaveData.get(serverLevel).fire > 1e-5F) {
            heat += (int) ((maxHeat() - heat) * 0.000005D);
        }

        isOn = false;
        syncInputFill = tanks[0].getFill();
        tryConvert();

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    protected void tryPullHeat() {
        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int diff = source.getHeatStored(level, heatPos) - this.heat;
            if (diff == 0) return;

            if (diff > 0) {
                int maxHeat = maxHeat();
                diff = (int) Math.ceil(diff * diffusion());
                diff = Math.min(diff, maxHeat - this.heat);
                source.useUpHeat(level, heatPos, diff);
                this.heat += diff;
                if (this.heat > maxHeat) this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    protected void setupTanks() {
        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.BOILER) > 0) {
            HeatingStep entry = trait.getFirstStep();
            tanks[1].setTankType(entry.typeProduced());
            tanks[1].changeTankSize(tanks[0].getMaxFill() * entry.amountProduced / entry.amountReq);
            return;
        }

        tanks[0].setTankType(NTMFluids.NONE);
        tanks[1].setTankType(NTMFluids.NONE);
    }

    protected void tryConvert() {
        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.BOILER) <= 0) return;

        HeatingStep entry = trait.getFirstStep();
        int heatReq = (int) Math.max(entry.heatReq / trait.getEfficiency(HeatingType.BOILER), 1);
        int inputOps = tanks[0].getFill() / entry.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / entry.amountProduced;
        int heatOps = heat / heatReq;

        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));

        tanks[0].setFill(tanks[0].getFill() - entry.amountReq * ops);
        tanks[1].setFill(tanks[1].getFill() + entry.amountProduced * ops);
        heat -= heatReq * ops;

        if (ops > 0 && level.getRandom().nextInt(400) == 0) {
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5,
                    ModSounds.BOILER_GROAN.get(),
                    SoundSource.BLOCKS,
                    0.5F,
                    1.0F);
        }

        if (ops > 0) isOn = true;

        if (outputOps == 0 && canExplode()) explode();
    }

    private void explode() {
        hasExploded = true;

        BlockMultiblockCore.setBusy(true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 2; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.removeBlock(worldPosition.offset(dx, dy, dz), false);
                }
            }
        }
        level.removeBlock(worldPosition.above(), false);

        new ExplosionVNT(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 2,
                        worldPosition.getZ() + 0.5,
                        5F)
                .setEntityProcessor(new EntityProcessorStandard().withRangeMod(3F))
                .setPlayerProcessor(new PlayerProcessorStandard())
                .setSFX(new ExplosionEffectStandard())
                .explode();
        BlockMultiblockCore.setBusy(false);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.BOILER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.125F,
                10F,
                1.0F,
                20);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (tanks[0].getTankType() == NTMFluids.NONE || !tanks[0].accepts(type)) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure()) return amount;
        if (tanks[0].getTankType() == NTMFluids.NONE || !tanks[0].accepts(type)) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return;
        if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if (hasExploded) {
            if ((PREFIX_VALUE + "input").equals(name)) return "0";
            if ((PREFIX_VALUE + "output").equals(name)) return "0";
            return null;
        }
        if ((PREFIX_VALUE + "input").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + tanks[1].getFill();
        return null;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0b1_1111;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(syncHeat);
            case 1 -> output.writeBoolean(hasExploded);
            case 2 -> output.writeBoolean(isOn);
            case 3 -> tanks[0].packetSerialize(output, syncInputFill);
            case 4 -> tanks[1].packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> heat = input.readInt();
            case 1 -> hasExploded = input.readBoolean();
            case 2 -> isOn = input.readBoolean();
            case 3 -> tanks[0].packetDeserialize(input);
            case 4 -> tanks[1].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return false;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(heat);
            case 3 -> tanks[0].packetSerialize(output);
            default -> writeSyncUnit(unit, output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = input.getIntOr("heat", heat);
        hasExploded = input.getBooleanOr("exploded", hasExploded);
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("steam").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putBoolean("exploded", hasExploded);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("steam"));
    }
}
