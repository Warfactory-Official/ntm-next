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

package com.hbm.blockentity.effect;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.GeysirBlock;
import com.hbm.entity.particle.OrangeFXEntity;
import com.hbm.entity.projectile.ShrapnelEntity;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GasFlameLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class GeyserBlockEntity extends BlockEntity implements BlockEntityTicks {
    private int remaining;

    public GeyserBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.GEYSIR.get(), pos, state);
    }

    protected GeyserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tickServer() {
        var state = getBlockState();
        if (!level.getBlockState(worldPosition.above()).isAir()) return;
        boolean active = state.getValue(GeysirBlock.ACTIVE);
        var random = level.getRandom();
        if (--this.remaining <= 0) {
            if (state.is(ModBlocks.GEYSIR_CHLORINE.get())) {
                this.remaining = active ? 400 + random.nextInt(100) : 20;
            } else if (state.is(ModBlocks.GEYSIR_NETHER.get())) {
                this.remaining =
                        active ? 80 + random.nextInt(60) : random.nextBoolean() ? 300 : 450;
            }
            level.setBlock(worldPosition, state.setValue(GeysirBlock.ACTIVE, !active), 3);
        }
        if (!active) return;
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1.5D;
        double z = worldPosition.getZ() + 0.5D;
        if (state.is(ModBlocks.GEYSIR_CHLORINE.get())) {
            for (int i = 0; i < 3; i++) {
                double motionX = random.nextGaussian() * 0.45D;
                double motionZ = random.nextGaussian() * 0.45D;
                var cloud = new OrangeFXEntity(level, x, y, z, 0D, 0D, 0D);
                cloud.setDeltaMovement(motionX, this.remaining * 0.3D, motionZ);
                level.addFreshEntity(cloud);
            }
        } else if (state.is(ModBlocks.GEYSIR_NETHER.get())) {
            double centerY = worldPosition.getY() + 0.5D;
            if (level.getEntitiesOfClass(
                            Player.class, new AABB(x, centerY, z, x, centerY, z).inflate(32D))
                    .isEmpty()) return;
            if (random.nextInt(3) == 0) {
                var shard = new ShrapnelEntity(level, x, y, z);
                double motionX = random.nextGaussian() * 0.05D;
                double motionZ = random.nextGaussian() * 0.05D;
                double motionY = 0.5D + random.nextDouble() * this.remaining * 0.01D;
                shard.setDeltaMovement(motionX, motionY, motionZ);
                level.addFreshEntity(shard);
            }
            if ((this.remaining & 1) == 0) {
                double motionX = random.nextGaussian() * 0.05D;
                double motionZ = random.nextGaussian() * 0.05D;
                double flameY = worldPosition.getY() + 1.1D;
                var payload = GasFlameLayout.encode(x, flameY, z, motionX, 0.2D, motionZ, 6.5F);
                PacketWire.sendToAllAround(
                        payload, new TargetPoint((ServerLevel) level, x, flameY, z, 75D));
            }
        }
    }
}
