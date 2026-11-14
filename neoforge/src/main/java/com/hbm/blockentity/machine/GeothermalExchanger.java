/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class GeothermalExchanger extends MachineBlockEntity
        implements FluidTankHandler, FluidSettingsHandler, AudioLoop, SoundEmitter, SyncUnitSchema {
    public final FluidTankNTM inputTank = new FluidTankNTM(NTMFluids.OIL, 24_000);
    public final FluidTankNTM outputTank = new FluidTankNTM(NTMFluids.HOTOIL, 24_000);
    public int heatRate;
    public float rotation;
    public float previousRotation;

    private final FluidTankNTM[] inputs = {inputTank};
    private final FluidTankNTM[] outputs = {outputTank};
    private final FluidTankNTM[] configuredTanks = {inputTank, outputTank};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final int[] layerHeat = new int[10];
    private final Block volcanicLava = ModBlocks.VOLCANIC_LAVA_BLOCK.get();
    private final Block fissure = ModBlocks.ORE_VOLCANO.get();
    private final Predicate<BlockState> heatSources =
            state -> state.is(Blocks.LAVA) || state.is(volcanicLava) || state.is(fissure);
    private long lastFissureTick;

    public GeothermalExchanger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEPHAESTUS.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        var step = conformFluids();
        long now = server.getGameTime();
        int layer = (int) (now % layerHeat.length);
        layerHeat[layer] = scanLayer(server, worldPosition.getY() - 1 - layer, now);
        int total = 0;
        for (int heat : layerHeat) total += heat;
        hbm$syncSet$GeothermalExchanger$heatRate(now - lastFissureTick < 20 ? total * 3 : total);
        if (step != null) heatFluid(step);
        flush.provide(server, this);
        networkPackNT(150);
    }

    private HeatableTrait.@Nullable Step conformFluids() {
        var heating = HeatableTrait.forMode(inputTank.getFluid(), HeatableTrait.Mode.HEATEXCHANGER);
        if (heating == null) {
            boolean changed =
                    inputTank.getDeclaredFluid() != null || outputTank.getDeclaredFluid() != null;
            inputTank.setTankType(null);
            outputTank.setTankType(null);
            if (changed) markChanged();
            return null;
        }
        var step = heating.first();
        Fluid previous = outputTank.getTankType();
        outputTank.setTankType(step.product());
        if (outputTank.getTankType() != previous) markChanged();
        return step;
    }

    private int scanLayer(ServerLevel server, int y, long now) {
        if (y < server.getMinY()) return 0;
        int minX = worldPosition.getX() - 7;
        int maxX = worldPosition.getX() + 7;
        int minZ = worldPosition.getZ() - 7;
        int maxZ = worldPosition.getZ() + 7;
        int heat = 0;
        int localY = y & 15;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            int firstX = Math.max(minX, chunkX << 4);
            int lastX = Math.min(maxX, (chunkX << 4) + 15);
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                var chunk = server.getChunk(chunkX, chunkZ);
                var section = chunk.getSections()[server.getSectionIndex(y)];
                if (section.hasOnlyAir() || !section.maybeHas(heatSources)) continue;
                int firstZ = Math.max(minZ, chunkZ << 4);
                int lastZ = Math.min(maxZ, (chunkZ << 4) + 15);
                for (int x = firstX; x <= lastX; x++) {
                    for (int z = firstZ; z <= lastZ; z++) {
                        var state = section.getBlockState(x & 15, localY, z & 15);
                        if (state.is(fissure)) {
                            heat += 300;
                            lastFissureTick = now;
                        } else if (state.is(volcanicLava)) {
                            heat += 150;
                        } else if (state.is(Blocks.LAVA)) {
                            heat += 5;
                        }
                    }
                }
            }
        }
        return heat;
    }

    private void heatFluid(HeatableTrait.Step step) {
        int operations = Math.min(heatRate / step.heat(), inputTank.getFill() / step.consumed());
        int room = outputTank.getMaxFill() - outputTank.getFill();
        operations = Math.min(operations, room / step.produced());
        if (operations == 0) return;
        inputTank.setFill(inputTank.getFill() - operations * step.consumed());
        outputTank.setFill(outputTank.getFill() + operations * step.produced());
        markChanged();
    }

    public void selectInputFluid(Fluid fluid) {
        boolean changed = inputTank.getDeclaredFluid() != fluid || !inputTank.isStrict();
        inputTank.setTankTypeByIdentifier(fluid);
        if (changed) markChanged();
    }

    @Override
    public void tickClient() {
        previousRotation = rotation;
        boolean active = heatRate > 0;
        if (active) {
            rotation += 0.5F;
            if (rotation >= 360F) {
                rotation -= 360F;
                previousRotation -= 360F;
            }
            var random = level.getRandom();
            if (random.nextInt(7) == 0) {
                double x = worldPosition.getX() + 0.5 + random.nextGaussian() * 2;
                double y = worldPosition.getY() + 6 + random.nextGaussian() * 3;
                double z = worldPosition.getZ() + 0.5 + random.nextGaussian() * 2;
                level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
            }
        }
        audioLoop(active, 0.75F, 1F);
    }

    @Override
    public boolean tickSound() {
        return heatRate > 0;
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.HEPHAESTUS_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY() + 5F,
                worldPosition.getZ(),
                0.75F,
                10F,
                1F,
                20);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputs;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputs;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return configuredTanks;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> inputTank;
            case 1 -> outputTank;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public FluidTankNTM pasteTank() {
        return inputTank;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(outputTank, FlushFaces.own());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("0").ifPresent(inputTank::deserialize);
        input.child("1").ifPresent(outputTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("0"));
        outputTank.serialize(output.child("1"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> inputTank.packetSerialize(output);
            case 1 -> outputTank.packetSerialize(output);
            case 2 -> output.writeInt(heatRate);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> inputTank.packetDeserialize(input);
            case 1 -> outputTank.packetDeserialize(input);
            case 2 -> hbm$syncSet$GeothermalExchanger$heatRate(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$GeothermalExchanger$heatRate(int next) {
        if (this.heatRate != next) {
            this.heatRate = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(GeothermalExchanger.class, "inputTank", 3, 1L);
        com.hbm.packet.SyncBindings.field(GeothermalExchanger.class, "outputTank", 3, 2L);
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
