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

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.extprop.LivingHazards;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class DeconBlockEntity extends BlockEntity implements BlockEntityTicks {

    private static final float RAD_REMOVE = -0.5F;

    private static final float NEUTRON_REMOVE = -0.005F;
    private static final float NEUTRON_DECAY = 0.9998074776F;

    public DeconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECON.get(), pos, state);
    }

    @Override
    public void tickServer() {
        AABB box =
                new AABB(
                        worldPosition.getX() - 0.5D,
                        worldPosition.getY(),
                        worldPosition.getZ() - 0.5D,
                        worldPosition.getX() + 1.5D,
                        worldPosition.getY() + 2D,
                        worldPosition.getZ() + 1.5D);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            LivingHazards.addRadiation(entity, RAD_REMOVE);
            entity.removeEffect(HbmPotion.radiation());
            LivingHazards.getOrCreate(entity).clearDoses();
            if (entity instanceof Player player) {
                ContaminationUtil.neutronActivateInventory(player, NEUTRON_REMOVE, NEUTRON_DECAY);
            }
        }
    }

    @Override
    public void tickClient() {
        RandomSource rand = level.getRandom();
        level.addParticle(
                ParticleTypes.MYCELIUM,
                worldPosition.getX() + 0.125D + rand.nextDouble() * 0.75D,
                worldPosition.getY() + 1.1D,
                worldPosition.getZ() + 0.125D + rand.nextDouble() * 0.75D,
                0D,
                0.04D,
                0D);
    }
}
