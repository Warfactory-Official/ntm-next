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

package com.hbm.items.tool;

import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.items.machine.SatelliteChipItem;
import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SatelliteDesignatorItem extends SatelliteChipItem {

    private static final double RANGE = 300D;

    public SatelliteDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.CONSUME;

        ItemStack stack = player.getItemInHand(hand);
        var sat =
                SatelliteSavedData.at(server, player.getBlockX(), player.getBlockZ())
                        .getSatFromFreq(SatelliteFrequencyProvider.frequency(stack));
        if (sat == null) return InteractionResult.CONSUME;

        BlockPos target = trace(server, player);
        sat.designate(server, player, target.getX(), target.getY(), target.getZ());
        return InteractionResult.CONSUME;
    }

    private static BlockPos trace(ServerLevel level, Player player) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player));
        return hit.getBlockPos().relative(hit.getDirection());
    }
}
