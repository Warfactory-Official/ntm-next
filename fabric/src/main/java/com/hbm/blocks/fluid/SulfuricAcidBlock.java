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

package com.hbm.blocks.fluid;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.HbmCriteria;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.EntitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SulfuricAcidBlock extends ClassicFluidBlock {
    private static final float DAMAGE = 5.0F;

    public SulfuricAcidBlock(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity.tickCount % 20 == 0 && level instanceof ServerLevel server) {
                ItemStack stack = item.getItem().copy();
                entity.hurt(level.damageSources().source(ModDamageTypes.ACID), DAMAGE * 0.1F);
                if (entity.isRemoved()) {
                    AwardRegions.nearby(
                            server,
                            entity.getBoundingBox().inflate(10),
                            player -> HbmCriteria.itemDissolved(player, stack));
                }
            }
            if (entity.tickCount % 5 == 0 && level.isClientSide()) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        0D,
                        0D,
                        0D);
            }
        } else {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < -0.2D) entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
            if (!level.isClientSide()) {
                entity.hurt(level.damageSources().source(ModDamageTypes.ACID), DAMAGE);
            }
        }

        if (entity.tickCount % 5 == 0 && !level.isClientSide()) {
            EntitySounds.play(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.2F,
                    1.0F);
        }
    }
}
