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

package com.hbm.world.gen;

import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.LootBlock.BlockEntityLoot;
import com.hbm.blocks.generic.RedBrickBlock;
import com.hbm.blocks.generic.RedBrickKeyholeBlock;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolIds;
import com.hbm.items.ModItems;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class RedRoom {

    private static final int HALF = 4;
    private static final int TOP = 4;
    private static final int PLAIN = 6;
    private static final Direction[] WALLS = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private RedRoom() {}

    public static void open(
            ServerLevel level,
            BlockPos keyhole,
            Direction side,
            boolean black,
            RandomSource random) {
        var room = new Room(level, keyhole.relative(side, -HALF).below(2));
        if (black) {
            room.shell(_ -> brick(PLAIN), side, null);
            blackRewards(room, random);
        } else {
            boolean keyed = random.nextInt(1) == 0;
            Direction lock = keyed ? WALLS[random.nextInt(4)] : null;
            room.shell(inward -> brick(inward.get3DDataValue()), null, lock);
            dress(room, random);
            redReward(room, random);
        }
        room.clearItems();
        door(level, keyhole.below(), side.getOpposite());
    }

    private static void dress(Room room, RandomSource random) {
        for (int a = 2; a <= 3; a++) {
            int b = 5 - a;
            room.mirrored(a, 2, b, RedRoom::torch);
        }
        if (random.nextInt(4) == 0) {
            for (int i = -HALF + 1; i < HALF; i++)
                for (int j = -HALF + 1; j < HALF; j++)
                    if (random.nextBoolean())
                        room.set(i, TOP - 1, j, Blocks.COBWEB.defaultBlockState());
        }
        if (random.nextInt(4) == 0) {
            for (int dy = 1; dy < TOP; dy++)
                room.mirrored(2, dy, 2, ModBlocks.CONCRETE_RED.get().defaultBlockState());
        }
        if (random.nextInt(4) == 0) {
            room.mirrored(3, 0, 3, Blocks.NETHERRACK.defaultBlockState());
            room.mirrored(3, 1, 3, Blocks.FIRE.defaultBlockState());
        }
        if (random.nextInt(4) == 0) {
            for (var ring : Direction.Plane.HORIZONTAL) {
                var corner = ring.getClockWise();
                room.set(
                        ring.getStepX(),
                        0,
                        ring.getStepZ(),
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
                room.set(
                        ring.getStepX() + corner.getStepX(),
                        0,
                        ring.getStepZ() + corner.getStepZ(),
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
            }
        }
        if (random.nextInt(4) == 0) {
            for (int a = 1; a <= 2; a++) {
                room.mirrored(a, 0, 3, Blocks.LAVA.defaultBlockState());
                room.mirrored(3, 0, a, Blocks.LAVA.defaultBlockState());
            }
        }
    }

    private static void redReward(Room room, RandomSource random) {
        if (random.nextInt(20) != 0) {
            room.pedestal(
                    0,
                    0,
                    ItemPool.getUnit(
                            ItemPoolIds.POOL_RED_PEDESTAL, ItemPool.context(room.level, random)));
            return;
        }
        room.set(0, 1, 0, ModBlocks.LOOT.get().defaultBlockState());
        var pile = (BlockEntityLoot) room.level.getBlockEntity(room.at(0, 1, 0));
        var set =
                random.nextInt(5) == 0
                        ? new Item[] {
                            ModItems.NCRPA_HELMET.get(),
                            ModItems.NCRPA_PLATE.get(),
                            ModItems.NCRPA_LEGS.get(),
                            ModItems.NCRPA_BOOTS.get()
                        }
                        : new Item[] {
                            ModItems.TRENCHMASTER_HELMET.get(),
                            ModItems.TRENCHMASTER_PLATE.get(),
                            ModItems.TRENCHMASTER_LEGS.get(),
                            ModItems.TRENCHMASTER_BOOTS.get()
                        };
        for (var piece : set) pile.addItem(new ItemStack(piece), 0, 0, 0);
    }

    private static void blackRewards(Room room, RandomSource random) {
        var loot = ItemPool.context(room.level, random);
        room.pedestal(0, 0, ItemPool.getStack(ItemPoolIds.POOL_BLACK_SLAB, loot));
        for (var wall : WALLS) {
            if (random.nextBoolean())
                room.pedestal(
                        wall.getStepX() * 2,
                        wall.getStepZ() * 2,
                        ItemPool.getStack(ItemPoolIds.POOL_BLACK_PART, loot));
        }
    }

    private static BlockState brick(int face) {
        return ModBlocks.BRICK_RED.get().defaultBlockState().setValue(RedBrickBlock.FACE, face);
    }

    private static BlockState torch(ServerLevel level, BlockPos pos) {
        return Blocks.WALL_TORCH.getStateForPlacement(
                new DirectionalPlaceContext(
                        level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
    }

    private static void door(ServerLevel level, BlockPos pos, Direction facing) {
        DoorBlock block = ModBlocks.DOOR_RED.get();
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        BlockState state =
                block.getStateForPlacement(
                        new DirectionalPlaceContext(
                                level, pos, facing, ItemStack.EMPTY, Direction.UP));
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
        level.updateNeighborsAt(pos, block);
    }

    private static final class Room {
        final ServerLevel level;
        final BlockPos floor;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        Room(ServerLevel level, BlockPos floor) {
            this.level = level;
            this.floor = floor;
        }

        BlockPos.MutableBlockPos at(int dx, int dy, int dz) {
            return cursor.setWithOffset(floor, dx, dy, dz);
        }

        void set(int dx, int dy, int dz, BlockState state) {
            level.setBlock(at(dx, dy, dz), state, Block.UPDATE_ALL);
        }

        void mirrored(int a, int dy, int b, BlockState state) {
            mirrored(a, dy, b, (_, _) -> state);
        }

        void mirrored(int a, int dy, int b, BiFunction<ServerLevel, BlockPos, BlockState> state) {
            for (int sx = 1; sx >= -1; sx -= 2)
                for (int sz = 1; sz >= -1; sz -= 2) {
                    var pos = at(sx * a, dy, sz * b);
                    level.setBlock(pos, state.apply(level, pos), Block.UPDATE_ALL);
                }
        }

        void shell(
                Function<Direction, BlockState> face,
                @Nullable Direction open,
                @Nullable Direction lock) {
            for (int dy = 0; dy <= TOP; dy++)
                for (int dx = -HALF; dx <= HALF; dx++)
                    for (int dz = -HALF; dz <= HALF; dz++) {
                        var state = cell(face, open, lock, dx, dy, dz);
                        if (state != null) set(dx, dy, dz, state);
                    }
        }

        private static @Nullable BlockState cell(
                Function<Direction, BlockState> face,
                @Nullable Direction open,
                @Nullable Direction lock,
                int dx,
                int dy,
                int dz) {
            boolean onX = Math.abs(dx) == HALF,
                    onZ = Math.abs(dz) == HALF,
                    onY = dy == 0 || dy == TOP;
            int bounds = (onX ? 1 : 0) + (onZ ? 1 : 0) + (onY ? 1 : 0);
            if (bounds == 0) return Blocks.AIR.defaultBlockState();
            if (bounds > 1) return brick(PLAIN);
            if (onY) return face.apply(dy == 0 ? Direction.UP : Direction.DOWN);
            var outward =
                    onX
                            ? (dx > 0 ? Direction.EAST : Direction.WEST)
                            : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            if (outward == open) return null;
            if (outward == lock && dy == 2 && (onX ? dz : dx) == 0)
                return ModBlocks.STONE_KEYHOLE_META
                        .get()
                        .defaultBlockState()
                        .setValue(RedBrickKeyholeBlock.FACING, outward.getOpposite());
            return face.apply(outward.getOpposite());
        }

        void pedestal(int dx, int dz, ItemStack stack) {
            var pos = at(dx, 1, dz).immutable();
            BlockState state = ModBlocks.PEDESTAL.get().defaultBlockState();
            level.setBlock(pos, state, Block.UPDATE_ALL);
            PedestalBlockEntity pedestal = (PedestalBlockEntity) level.getBlockEntity(pos);
            pedestal.hbm$syncSet$PedestalBlockEntity$display(stack);
            pedestal.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        void clearItems() {
            int x = floor.getX(), y = floor.getY(), z = floor.getZ();
            for (ItemEntity item :
                    level.getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(
                                    x - HALF,
                                    y,
                                    z - HALF,
                                    x + HALF + 1,
                                    y + TOP + 1,
                                    z + HALF + 1))) item.discard();
        }
    }
}
