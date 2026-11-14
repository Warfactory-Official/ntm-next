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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.inventory.BlockMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ForceFieldBlock extends Block implements TickingEntityBlock, CapabilityBlock {
    private static final float EMITTER_HEIGHT = 2F;
    private static final int MOTES = 4;

    public ForceFieldBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RepulsionFieldBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && !player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            BlockMenus.open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof RepulsionFieldBlockEntity field)) return;

        ParticleOptions mote;
        if (field.requestedOn && field.recoveryTicks == 0 && field.storedEnergy > 0) {
            mote =
                    field.fieldColor == RepulsionFieldBlockEntity.COLOR_HIT
                            ? ParticleTypes.LAVA
                            : DustParticleOptions.REDSTONE;
        } else if (field.recoveryTicks > 0) {
            mote = ParticleTypes.SMOKE;
        } else {
            return;
        }

        for (int i = 0; i < MOTES; i++) {
            level.addParticle(
                    mote,
                    pos.getX() + random.nextFloat(),
                    pos.getY() + EMITTER_HEIGHT,
                    pos.getZ() + random.nextFloat(),
                    0D,
                    0D,
                    0D);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FORCEFIELD)
                .powerIn()
                .fe()
                .items()
                .roleFaces(
                        RepulsionFieldBlockEntity.class,
                        (_, side, role) ->
                                role != CapRole.POWER_IN || side != Direction.UP);
    }
}
