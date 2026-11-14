/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.tank.TankConversion;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.TomSaveData;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class FluidBoilerBlockEntity extends MachineBlockEntity
        implements ThermalFluidHandler,
                FluidSettingsHandler,
                RadioValueProvider,
                AudioLoop,
                SoundEmitter,
                SyncUnitSchema {
    public static final String[] ROR = {"VAL:input", "VAL:output"};
    public static Consumer<FluidBoilerBlockEntity> CLIENT_SOUND = _ -> {};
    public int heat;
    public boolean hasExploded;
    public boolean isOn;
    public final FluidTankNTM[] tanks;
    private int syncHeat;
    private int syncInputFill;
    private final int waterCapacity;
    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;
    private @Nullable Fluid heatingFluid;
    private @Nullable HeatableTrait heating;
    public int audioTime;

    protected FluidBoilerBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, 0);
        waterCapacity = capacity;
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(Fluids.WATER, capacity),
                    new FluidTankNTM(NTMFluids.STEAM, 100 * capacity)
                };
        sending = new FluidTankNTM[] {tanks[1]};
        below = pos.below();
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
        boolean changed = false;
        var fluid = tanks[0].getTankType();
        if (fluid != heatingFluid) {
            heatingFluid = fluid;
            heating = HeatableTrait.forMode(fluid, HeatableTrait.Mode.BOILER);
        }
        if (heating == null) {
            changed = tanks[0].getTankType() != null || tanks[1].getTankType() != null;
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
        } else {
            var step = heating.first();
            int capacity = waterCapacity * step.produced() / step.consumed();
            changed = tanks[1].getTankType() != step.product() || tanks[1].getMaxFill() != capacity;
            tanks[1].setTankType(step.product());
            tanks[1].changeTankSize(capacity);
        }
        int previousHeat = heat;
        int ceiling = maxHeat();
        hbm$syncSet$FluidBoilerBlockEntity$heat(
                HeatIntake.GAP_TO_ROOM.pull(
                        heatBelow.get(level, below), heat, ceiling, diffusion()));
        hbm$syncSet$FluidBoilerBlockEntity$syncHeat(heat);
        var server = (ServerLevel) level;
        if (level.getBrightness(LightLayer.SKY, worldPosition) > 7
                && TomSaveData.get(server).fire > 1E-5F) {
            hbm$syncSet$FluidBoilerBlockEntity$heat((int) (heat + 0.000005D * (ceiling - heat)));
        }
        hbm$syncSet$FluidBoilerBlockEntity$isOn(false);
        hbm$syncSet$FluidBoilerBlockEntity$syncInputFill(tanks[0].getFill());
        if (heating != null) {
            var step = heating.first();
            int room = TankConversion.room(tanks[1], step.produced());
            int cost =
                    (int) Math.max(step.heat() / heating.efficiency(HeatableTrait.Mode.BOILER), 1D);
            int operations =
                    TankConversion.run(
                            tanks[0], step.consumed(), tanks[1], step.produced(), heat / cost);
            if (operations > 0) {
                hbm$syncSet$FluidBoilerBlockEntity$heat((int) (heat - (operations * cost)));
                hbm$syncSet$FluidBoilerBlockEntity$isOn(true);
                changed = true;
                if (level.getRandom().nextInt(400) == 0) {
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 2,
                            worldPosition.getZ() + 0.5,
                            ModSounds.BOILER_GROAN.get(),
                            SoundSource.BLOCKS,
                            0.5F,
                            1F);
                }
            }
            if (room == 0 && canExplode()) burst();
        }
        if (changed || heat != previousHeat) markChanged();
        flush.provide(server, this);
        networkPackNT(25);
    }

    private void burst() {
        hbm$syncSet$FluidBoilerBlockEntity$hasExploded(true);
        markChanged();
        MultiblockCoreBlock.withoutTeardown(
                () -> {
                    for (int dy = 2; dy <= 3; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++)
                                level.removeBlock(worldPosition.offset(dx, dy, dz), false);
                        }
                    }
                    level.removeBlock(worldPosition.above(), false);
                });
        new ComposableExplosion(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 2,
                        worldPosition.getZ() + 0.5,
                        5F)
                .entityProcessor(new SinglePointExplosionEntityProcessor().range(3F))
                .effects(new ExplosionEffectStandard())
                .explode();
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public boolean tickSound() {
        if (isOn) audioTime = 20;
        if (audioTime > 0) {
            audioTime--;
            return true;
        }
        return false;
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.BOILER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                10F,
                1F,
                20);
    }

    @Override
    public FluidTankNTM receivingTank() {
        return tanks[0];
    }

    @Override
    public FluidTankNTM deliveryTank() {
        return tanks[1];
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[0];
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:input" -> Integer.toString(hasExploded ? 0 : tanks[0].getFill());
            case "VAL:output" -> Integer.toString(hasExploded ? 0 : tanks[1].getFill());
            default -> null;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FluidBoilerBlockEntity$heat(input.getIntOr("heat", heat));
        hbm$syncSet$FluidBoilerBlockEntity$hasExploded(input.getBooleanOr("exploded", hasExploded));
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("steam").ifPresent(tanks[1]::deserialize);

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putBoolean("exploded", hasExploded);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("steam"));

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31L;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) output.writeInt(heat);
        else if (unit == 3) tanks[0].packetSerialize(output);
        else writeSyncUnit(unit, output);
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
            case 0 -> hbm$syncSet$FluidBoilerBlockEntity$heat(input.readInt());
            case 1 -> hbm$syncSet$FluidBoilerBlockEntity$hasExploded(input.readBoolean());
            case 2 -> hbm$syncSet$FluidBoilerBlockEntity$isOn(input.readBoolean());
            case 3 -> tanks[0].packetDeserialize(input);
            case 4 -> tanks[1].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$FluidBoilerBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$FluidBoilerBlockEntity$hasExploded(boolean next) {
        if (this.hasExploded != next) {
            this.hasExploded = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$FluidBoilerBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$FluidBoilerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 24L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$FluidBoilerBlockEntity$syncHeat(int next) {
        if (this.syncHeat != next) {
            this.syncHeat = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$FluidBoilerBlockEntity$syncInputFill(int next) {
        if (this.syncInputFill != next) {
            this.syncInputFill = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FluidBoilerBlockEntity.class, "tanks", 3, 24L);
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
