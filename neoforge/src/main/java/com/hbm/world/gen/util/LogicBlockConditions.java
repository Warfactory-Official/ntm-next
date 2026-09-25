// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.util;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityLogicBlock;
import com.hbm.tileentity.BlockEntityPedestal;
import com.hbm.tileentity.bomb.BlockEntityCharge;
import com.hbm.util.TickPhase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class LogicBlockConditions {

    public static final Map<String, Predicate<BlockEntityLogicBlock>> CONDITIONS =
            new LinkedHashMap<>();

    static {
        CONDITIONS.put("EMPTY", tile -> false);
        CONDITIONS.put("PLAYER_CUBE_3", playerCube(3));
        CONDITIONS.put("PLAYER_CUBE_5", playerCube(5));
        CONDITIONS.put("PLAYER_CUBE_25", playerCube(25));
        CONDITIONS.put("BOMB_CRANE", LogicBlockConditions::bombCrane);
        CONDITIONS.put("ABERRATOR", LogicBlockConditions::aberrator);
        CONDITIONS.put("REDSTONE", BlockEntityLogicBlock::isPowered);
        CONDITIONS.put("PUZZLE_TEST", puzzleTest());
    }

    private LogicBlockConditions() {}

    private static Predicate<BlockEntityLogicBlock> puzzleTest() {
        return tile -> {
            Level level = tile.getLevel();
            if (level == null) return false;
            BlockPos pos = tile.getBlockPos();
            if (tile.phase == 0 && tile.isPowered()) {
                Player player =
                        level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0, false);
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
                    && level.getBlockEntity(pos.above()) instanceof BlockEntityPedestal pedestal
                    && pedestal.item.is(ModItems.BIG_SWORD.get());
        };
    }

    private static Predicate<BlockEntityLogicBlock> playerCube(int n) {
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

    private static boolean aberrator(BlockEntityLogicBlock tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || level.getDifficulty().getId() == 0)
            return false;

        BlockPos pos = tile.getBlockPos();
        boolean playerNearby =
                !level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 10)).isEmpty();
        if (tile.phase == 0) return TickPhase.every(tile, 20) && playerNearby;
        if (tile.phase < 3) {
            return TickPhase.every(tile, 20)
                    && tile.timer >= 60
                    && level.getEntitiesOfClass(EntityUndeadSoldier.class, legacySoldierBox(pos))
                            .isEmpty()
                    && playerNearby;
        }
        return false;
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

    private static boolean bombCrane(BlockEntityLogicBlock tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return false;

        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 0) {
            BlockPos chargePos = pos.above();
            level.setBlockAndUpdate(
                    chargePos,
                    ModBlocks.CHARGE_C4
                            .get()
                            .defaultBlockState()
                            .setValue(BlockChargeBase.FACING, Direction.UP));
            if (level.getBlockEntity(chargePos) instanceof BlockEntityCharge charge) {
                charge.timer = 200;
                charge.changed();
            }
        }

        return !level.getEntitiesOfClass(Player.class, legacyPlayerBox(pos, 10)).isEmpty();
    }
}
