// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineHephaestus extends BlockEntityMachineBase
        implements AudioLoop, FluidTankEndpoint, IFluidCopiable, SyncUnitSchema {

    public static final int TANK_CAPACITY = 24_000;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM input;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM output;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 2)
    private final int[] heat = new int[10];

    public int bufferedHeat;
    public float rot, prevRot;

    @SyncField(units = 1L << 2)
    private long fissureScanTime;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineHephaestus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEPHAESTUS.get(), pos, state, 0);
        this.input = new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY);
        this.output = new FluidTankNTM(NTMFluids.HOTOIL, TANK_CAPACITY);
        receiving = new FluidTankNTM[] {input};
        sending = new FluidTankNTM[] {output};
    }

    @Override
    public void tickServer() {
        setupTanks();

        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        int height = (int) (level.getGameTime() % 10);
        int range = 7;
        int fromY = y - 1 - height;

        heat[height] = 0;
        if (fromY >= level.getMinY()) {
            for (int offsetX = -range; offsetX <= range; offsetX++) {
                for (int offsetZ = -range; offsetZ <= range; offsetZ++) {
                    heat[height] += heatFromBlock(x + offsetX, fromY, z + offsetZ);
                }
            }
        }

        heatFluid();

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.HEPHAESTUS_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY() + 5F,
                worldPosition.getZ(),
                0.75F,
                10F,
                1.0F);
    }

    @Override
    public void tickClient() {
        this.prevRot = this.rot;

        if (this.bufferedHeat > 0) {
            this.rot += 0.5F;

            if (level.getRandom().nextInt(7) == 0) {
                double px = level.getRandom().nextGaussian() * 2;
                double py = level.getRandom().nextGaussian() * 3;
                double pz = level.getRandom().nextGaussian() * 2;
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX() + 0.5 + px,
                        worldPosition.getY() + 6 + py,
                        worldPosition.getZ() + 0.5 + pz,
                        0,
                        0,
                        0);
            }
        }

        audioLoop(this.bufferedHeat > 0, 0.75F);

        if (this.rot >= 360F) {
            this.prevRot -= 360F;
            this.rot -= 360F;
        }
    }

    private void heatFluid() {
        FT_Heatable trait = NTMFluidProperties.getTrait(input.getTankType(), FT_Heatable.class);
        if (trait == null) return;

        HeatingStep step = trait.getFirstStep();
        if (step.amountReq <= 0 || step.heatReq <= 0 || step.amountProduced <= 0) return;

        int totalHeat = getTotalHeat();
        int inputOps = input.getFill() / step.amountReq;
        int outputOps = (output.getMaxFill() - output.getFill()) / step.amountProduced;
        int heatOps = totalHeat / step.heatReq;
        int ops = Math.min(Math.min(inputOps, outputOps), heatOps);
        if (ops <= 0) return;

        input.setFill(input.getFill() - step.amountReq * ops);
        output.setFill(output.getFill() + step.amountProduced * ops);
        setChanged();
    }

    private void setupTanks() {
        FT_Heatable trait = NTMFluidProperties.getTrait(input.getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.HEATEXCHANGER) > 0) {
            output.setTankType(trait.getFirstStep().typeProduced());
            return;
        }
        input.setTankType(NTMFluids.NONE);
        output.setTankType(NTMFluids.NONE);
    }

    private int heatFromBlock(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        if (state.is(Blocks.LAVA)) return 5;
        if (state.is(ModBlocks.VOLCANIC_LAVA_BLOCK.get())) return 150;

        if (state.is(ModBlocks.ORE_VOLCANO.get())) {
            this.fissureScanTime = level.getGameTime();
            return 300;
        }

        return 0;
    }

    public int getTotalHeat() {
        boolean fissure = level.getGameTime() - this.fissureScanTime < 20;
        int total = 0;
        for (int h : this.heat) total += h;
        if (fissure) total *= 3;
        return total;
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
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {input, output};
    }

    private void writeHeat(ByteBuf output) {
        output.writeInt(getTotalHeat());
    }

    private void readHeat(ByteBuf input) {
        bufferedHeat = input.readInt();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("0").ifPresent(input::deserialize);
        in.child("1").ifPresent(output::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        input.serialize(out.child("0"));
        output.serialize(out.child("1"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.input.packetSerialize(output);
            case 1 -> this.output.packetSerialize(output);
            case 2 -> writeHeat(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.input.packetDeserialize(input);
            case 1 -> this.output.packetDeserialize(input);
            case 2 -> readHeat(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
