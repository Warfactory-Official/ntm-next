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

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class StatueElbFBlockEntity extends BlockEntity implements BlockEntityTicks {

    private static final double SEARCH_RADIUS = 9.0D;
    private static final double FEET_RADIUS_SQ = 16.0D;
    private static final double EYE_RADIUS_SQ = 64.0D;

    public StatueElbFBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATUE_ELB_F.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        AABB search =
                new AABB(
                        x - SEARCH_RADIUS,
                        y - SEARCH_RADIUS,
                        z - SEARCH_RADIUS,
                        x + SEARCH_RADIUS,
                        y + SEARCH_RADIUS,
                        z + SEARCH_RADIUS);
        for (Player player : server.getEntitiesOfClass(Player.class, search)) {
            if (player.distanceToSqr(x, y, z) > FEET_RADIUS_SQ) continue;
            double dx = player.getX() - x;
            double dy = player.getEyeY() - y;
            double dz = player.getZ() - z;
            if (dx * dx + dy * dy + dz * dz >= EYE_RADIUS_SQ) continue;
            player.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 5, 99));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 99));
        }
    }
}
