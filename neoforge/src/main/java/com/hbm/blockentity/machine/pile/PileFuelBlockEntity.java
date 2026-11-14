/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.machine.pile.PileBaseBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.DrilledGraphiteBlock;
import com.hbm.config.BalanceConfig;
import com.hbm.handler.neutron.PileNeutronReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PileFuelBlockEntity extends PileBaseBlockEntity implements PileNeutronReceiver {

    public static final int maxHeat = 1000;
    public int heat;
    public int neutrons;
    public int lastNeutrons;
    public int progress;

    public PileFuelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_FUEL.get(), pos, state);
    }

    public static int maxProgress() {
        return BalanceConfig.enable528 ? 75000 : 50000;
    }

    @Override
    public void tickServer() {

        dissipateHeat();
        checkRedstone(react());
        transmute();

        if (this.heat >= maxHeat) {
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    4F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
            level.setBlockAndUpdate(
                    worldPosition, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }

        if (level.getRandom().nextFloat() * 2F <= this.heat / (float) maxHeat) {
            ((ServerLevel) level)
                    .sendParticles(
                            ParticleTypes.SMOKE,
                            worldPosition.getX() + 0.25 + level.getRandom().nextDouble() * 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.25 + level.getRandom().nextDouble() * 0.5,
                            0,
                            0D,
                            0.05D,
                            0D,
                            1D);
        }

        if (this.progress >= maxProgress()) {
            level.setBlockAndUpdate(
                    worldPosition,
                    DrilledGraphiteBlock.copyShape(
                            getBlockState(),
                            ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState()));
        }
    }

    private void dissipateHeat() {
        if (this.heat != 0) level.blockEntityChanged(worldPosition);
        this.heat -=
                getBlockState().getValue(DrilledGraphiteBlock.SHROUDED)
                        ? heat * 0.065
                        : heat * 0.05;
    }

    private int react() {
        int reaction =
                (int) (this.neutrons * (1D - ((double) this.heat / (double) maxHeat) * 0.5D));

        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;
        if (this.lastNeutrons != 0) level.blockEntityChanged(worldPosition);

        int lastProgress = this.progress;

        this.progress += reaction;

        if (reaction <= 0) return lastProgress;

        this.heat += reaction;

        for (int i = 0; i < 12; i++) this.castRay((int) Math.max(reaction * 0.25, 1));

        return lastProgress;
    }

    private void checkRedstone(int lastProgress) {
        int lastLevel = Mth.clamp((lastProgress * 16) / maxProgress(), 0, 15);
        int newLevel = Mth.clamp((progress * 16) / maxProgress(), 0, 15);
        if (lastLevel != newLevel)
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private void transmute() {

        BlockState state = getBlockState();

        if (state.getValue(DrilledGraphiteBlock.PU239)) {
            if (this.progress < maxProgress() - 1000) this.progress = maxProgress() - 1000;
        } else if (this.progress >= maxProgress() - 1000) {
            level.setBlock(worldPosition, state.setValue(DrilledGraphiteBlock.PU239, true), 3);
        }
    }

    @Override
    public void absorbNeutrons(int n) {
        this.neutrons += n;
        if (n != 0) level.blockEntityChanged(worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.heat = input.getIntOr("heat", 0);
        this.progress = input.getIntOr("progress", 0);
        this.neutrons = input.getIntOr("neutrons", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", this.heat);
        output.putInt("progress", this.progress);
        output.putInt("neutrons", this.neutrons);
    }
}
