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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.StirlingBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.CogEntity;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class StirlingGeneratorBlockEntity extends MachineBlockEntity
        implements EnergyHandler, PersistentDrop, SyncUnitSchema {
    public long powerBuffer;
    public int heat;
    private int syncHeat;
    public boolean hasCog;

    {
        hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(true);
    }

    public float spin;
    public float lastSpin;

    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;
    private int overspeed;
    private int warningCooldown;

    public StirlingGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STIRLING.get(), pos, state, 0);
        below = pos.below();
    }

    private StirlingBlock.Tier tier() {
        return ((StirlingBlock) getBlockState().getBlock()).tier();
    }

    public int maxHeat() {
        return tier().maxHeat;
    }

    public boolean isCreative() {
        return tier().creative;
    }

    public int gearVariant() {
        return tier().gear;
    }

    @Override
    public void tickServer() {
        long oldPower = powerBuffer;
        int oldOverspeed = overspeed;
        if (hasCog) {
            hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(0);
            heat =
                    HeatIntake.STORED.pull(
                            heatBelow.get(level, below),
                            heat,
                            0,
                            MachineData.STIRLING_DIFFUSION.get());
            hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(
                    (long) (heat * (isCreative() ? 1D : MachineData.STIRLING_EFFICIENCY.get())));
            if (warningCooldown > 0) warningCooldown--;
            if (!isCreative() && heat > maxHeat()) {
                overspeed++;
                if (overspeed > 60 && warningCooldown == 0) {
                    warningCooldown = 100;
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1D,
                            worldPosition.getZ() + 0.5,
                            ModSounds.WARN_OVERSPEED.get(),
                            SoundSource.BLOCKS,
                            2F,
                            1F);
                }
                if (overspeed > MachineData.STIRLING_OVERSPEED_LIMIT.get()) shedGear();
            } else {
                overspeed = 0;
            }
        } else {
            overspeed = 0;
            warningCooldown = 0;
        }
        hbm$syncSet$StirlingGeneratorBlockEntity$syncHeat(heat);
        networkPackNT(150);
        if (!hasCog && powerBuffer > 0)
            hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer((long) (powerBuffer - 1));
        if (oldPower != powerBuffer || oldOverspeed != overspeed) setChanged();
        heat = 0;
    }

    private void shedGear() {
        hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(false);
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1D;
        double z = worldPosition.getZ() + 0.5;
        level.explode(null, x, y, z, 5F, false, Level.ExplosionInteraction.NONE);
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var sideways = facing.getCounterClockWise();
        var cog =
                new CogEntity(level, x + facing.getStepX(), y, z + facing.getStepZ())
                        .setOrientation(facing.get3DDataValue())
                        .setMeta(gearVariant());
        cog.setDeltaMovement(
                sideways.getStepX(), 1D + 0.0001D * (heat - maxHeat()), sideways.getStepZ());
        level.addFreshEntity(cog);
        setChanged();
    }

    @Override
    public void tickClient() {
        float momentum = 50F * powerBuffer / maxHeat();
        if (isCreative()) momentum = Math.min(momentum, 45F);
        lastSpin = spin;
        spin += momentum;
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(stored);
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
    }

    @Override
    public long getSendRate() {
        return hasCog ? powerBuffer : 0;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (!hasCog) components.set(ModDataComponents.HAS_COG.get(), false);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(
                components.getOrDefault(ModDataComponents.HAS_COG.get(), true));
    }

    @Override
    public String[] persistentKeys() {
        return new String[] {"hasCog"};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(
                input.getLongOr("powerBuffer", powerBuffer));
        hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(input.getBooleanOr("hasCog", hasCog));
        overspeed = input.getIntOr("overspeed", overspeed);

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerBuffer", powerBuffer);
        output.putBoolean("hasCog", hasCog);
        output.putInt("overspeed", overspeed);

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return 7L;
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return false;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        if (unit == 1) output.writeInt(heat);
        else writeSyncUnit(unit, output);
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(powerBuffer);
            case 1 -> output.writeInt(syncHeat);
            case 2 -> output.writeBoolean(hasCog);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(input.readLong());
            case 1 -> {
                heat = input.readInt();
                hbm$syncSet$StirlingGeneratorBlockEntity$syncHeat(heat);
            }
            case 2 -> hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$StirlingGeneratorBlockEntity$powerBuffer(long next) {
        if (this.powerBuffer != next) {
            this.powerBuffer = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$StirlingGeneratorBlockEntity$syncHeat(int next) {
        if (this.syncHeat != next) {
            this.syncHeat = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$StirlingGeneratorBlockEntity$hasCog(boolean next) {
        if (this.hasCog != next) {
            this.hasCog = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
