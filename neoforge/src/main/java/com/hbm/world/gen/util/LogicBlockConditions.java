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

package com.hbm.world.gen.util;

import com.hbm.blockentity.bomb.ChargeBlockEntity;
import com.hbm.blockentity.encounter.LogicBlockEntity;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.mob.UndeadSoldierEntity;
import com.hbm.items.ModItems;
import com.hbm.util.TickPhase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class LogicBlockConditions {

    public static final Map<String, Predicate<LogicBlockEntity>> CONDITIONS = new LinkedHashMap<>();

    static {
        CONDITIONS.put("EMPTY", tile -> false);
        CONDITIONS.put("PLAYER_CUBE_3", playerCube(3));
        CONDITIONS.put("PLAYER_CUBE_5", playerCube(5));
        CONDITIONS.put("PLAYER_CUBE_25", playerCube(25));
        CONDITIONS.put("BOMB_CRANE", LogicBlockConditions::bombCrane);
        CONDITIONS.put("ABERRATOR", LogicBlockConditions::aberrator);
        CONDITIONS.put("REDSTONE", LogicBlockEntity::isPowered);
        CONDITIONS.put("PUZZLE_TEST", puzzleTest());
    }

    private LogicBlockConditions() {}

    private static Predicate<LogicBlockEntity> puzzleTest() {
        return tile -> {
            Level level = tile.getLevel();
            if (level == null) return false;
            BlockPos pos = tile.getBlockPos();
            if (tile.phase == 0 && tile.isPowered()) {
                Player player =
                        level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0, false);
                if (player == null) return false;
                player.sendSystemMessage(
                        Component.translatable("chat.logicBlockConditions.findA")
                                .append(
                                        Component.translatable("chat.logicBlockConditions.great")
                                                .withStyle(ChatFormatting.GOLD))
                                .append(
                                        Component.translatable(
                                                "chat.logicBlockConditions.ancientWeaponOfQuestionable")));
                level.setBlockAndUpdate(pos.above(), ModBlocks.PEDESTAL.get().defaultBlockState());
                return true;
            }

            return tile.phase == 1
                    && level.getBlockEntity(pos.above()) instanceof PedestalBlockEntity pedestal
                    && pedestal.display.is(ModItems.BIG_SWORD.get());
        };
    }

    private static Predicate<LogicBlockEntity> playerCube(int n) {
        return tile -> {
            Level level = tile.getLevel();
            if (level == null) return false;
            return !level.getEntitiesOfClass(Player.class, legacyPlayerBox(tile.getBlockPos(), n))
                    .isEmpty();
        };
    }

    private static AABB legacyPlayerBox(BlockPos pos, int radius) {
        return new AABB(
                pos.getX() - radius,
                pos.getY() - radius,
                pos.getZ() - radius,
                pos.getX() + 1 + radius,
                pos.getY() - 2 + radius,
                pos.getZ() + 1 + radius);
    }

    private static boolean aberrator(LogicBlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || level.getDifficulty().getId() == 0)
            return false;

        BlockPos pos = tile.getBlockPos();
        if (tile.phase >= 3 || !TickPhase.every(tile, 20)) return false;
        if (tile.phase > 0
                && (tile.timer < 60
                        || !level.getEntitiesOfClass(
                                        UndeadSoldierEntity.class, legacySoldierBox(pos))
                                .isEmpty())) {
            return false;
        }
        return !level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 10)).isEmpty();
    }

    private static AABB legacySoldierBox(BlockPos pos) {
        return new AABB(
                pos.getX() - 50,
                pos.getY() - 20,
                pos.getZ() - 50,
                pos.getX() + 48,
                pos.getY() + 21,
                pos.getZ() + 51);
    }

    private static boolean bombCrane(LogicBlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return false;

        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 0) {
            ChargeBlockEntity charge = LogicBlockActions.craneCharge(level, pos.above(), 200);
            if (charge.timer != 200) {
                charge.hbm$syncSet$ChargeBlockEntity$timer(200);
                charge.changed();
            }
        }

        return !level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 10)).isEmpty();
    }
}
