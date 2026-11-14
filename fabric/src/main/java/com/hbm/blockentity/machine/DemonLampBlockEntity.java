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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DemonLampBlockEntity extends BlockEntity implements BlockEntityTicks {
    public static final float RAW_STRENGTH = 100000F;

    public DemonLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_LAMP.get(), pos, state);
    }

    @Override
    public void tickServer() {
        emitRadiation(level, worldPosition);
    }

    public static void emitRadiation(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        var bounds = new AABB(centerX, centerY, centerZ, centerX, centerY, centerZ).inflate(25D);
        var entities =
                level.getEntitiesOfClass(LivingEntity.class, bounds, EntitySelector.NO_SPECTATORS);
        var cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index < entities.size(); index++) {
            var entity = entities.get(index);
            double dx = entity.getX() - centerX;
            double dy = entity.getEyeY() - centerY;
            double dz = entity.getZ() - centerZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            double distance = Math.sqrt(distanceSquared);
            var direction = new Vec3(dx, dy, dz).normalize();
            float resistance = 0F;
            for (int sample = 1; sample < distance; sample++) {
                cursor.set(
                        centerX + direction.x * sample,
                        centerY + direction.y * sample,
                        centerZ + direction.z * sample);
                resistance += level.getBlockState(cursor).getBlock().getExplosionResistance();
            }
            float dose = RAW_STRENGTH / Math.max(resistance, 1F) / (float) (distance * distance);
            ContaminationUtil.contaminate(
                    entity,
                    ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE,
                    dose);
            if (distance < 2D)
                entity.hurtServer((ServerLevel) level, level.damageSources().inFire(), 100F);
        }
    }
}
