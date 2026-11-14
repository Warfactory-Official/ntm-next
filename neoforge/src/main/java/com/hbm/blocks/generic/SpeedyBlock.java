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

package com.hbm.blocks.generic;

import com.hbm.blocks.PlayerStepListener;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class SpeedyBlock extends Block implements PlayerStepListener {

    public final double speed;

    public SpeedyBlock(double speed, Properties props) {
        super(props);
        this.speed = speed;
    }

    @Override
    public void onPlayerStep(Level level, BlockPos pos, Player player) {
        boost(level, player, speed);
    }

    static void boost(Level level, Player player, double speed) {
        if (!level.isClientSide()) return;

        if (player.zza != 0F || player.xxa != 0F) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * speed, motion.y, motion.z * speed);
        }
    }

    public Component tooltip() {
        return Component.translatable(
                        "desc.block.speedy.increasesSpeed", Mth.floor((speed - 1) * 100))
                .withStyle(ChatFormatting.BLUE);
    }
}
