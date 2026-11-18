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

package com.hbm.blocks.network;

import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorItem;
import com.hbm.api.conveyor.ConveyorPackage;
import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.entity.item.MovingPackageEntity;
import com.hbm.inventory.BlockMenus;
import com.hbm.items.tool.ConveyorWandItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CraneRouterBlock extends Block implements TickingEntityBlock, ConveyorInputHandler {
    public static final int[] FACE_COLOURS = TransportBlockData.ROUTER_FACE_COLOURS;

    public CraneRouterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraneRouterBlockEntity(pos, state);
    }

    public static List<List<ItemStack>> sort(Level level, BlockPos pos, List<ItemStack> stacks) {
        var buckets = new ArrayList<List<ItemStack>>(CraneRouterBlockEntity.SIDES + 1);
        for (int bucket = 0; bucket <= CraneRouterBlockEntity.SIDES; bucket++) {
            buckets.add(new ArrayList<>());
        }
        if (!(level.getBlockEntity(pos) instanceof CraneRouterBlockEntity router)) {
            return buckets;
        }
        for (var stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            var output = getOutputDir(router, stack);
            int bucket = CraneRouterBlockEntity.SIDES;
            if (output != null) {
                bucket = output.ordinal();
            }
            buckets.get(bucket).add(stack);
        }
        return buckets;
    }

    public static @Nullable Direction getOutputDir(CraneRouterBlockEntity router, ItemStack stack) {
        int candidates = 0;
        for (int face = 0; face < CraneRouterBlockEntity.SIDES; face++) {
            int mode = router.modes[face];
            if (mode == CraneRouterBlockEntity.MODE_NONE
                    || mode == CraneRouterBlockEntity.MODE_WILDCARD) {
                continue;
            }

            boolean matched = false;
            int firstSlot = face * CraneRouterBlockEntity.FILTERS_PER_SIDE;
            for (int filter = 0; filter < CraneRouterBlockEntity.FILTERS_PER_SIDE; filter++) {
                var pattern = router.getItem(firstSlot + filter);
                if (pattern.isEmpty()) {
                    continue;
                }
                if (router.patterns[face].isValidForFilter(pattern, filter, stack)) {
                    matched = true;
                    break;
                }
            }

            boolean accepted =
                    mode == CraneRouterBlockEntity.MODE_WHITELIST && matched
                            || mode == CraneRouterBlockEntity.MODE_BLACKLIST && !matched;
            if (accepted) {
                candidates |= 1 << face;
            }
        }

        if (candidates == 0) {
            for (int face = 0; face < CraneRouterBlockEntity.SIDES; face++) {
                if (router.modes[face] == CraneRouterBlockEntity.MODE_WILDCARD) {
                    candidates |= 1 << face;
                }
            }
        }
        if (candidates == 0) {
            return null;
        }

        int candidateCount = Integer.bitCount(candidates);
        int selected = 0;
        var level = router.getLevel();
        if (level != null) {
            selected = level.getRandom().nextInt(candidateCount);
        }
        while (selected > 0) {
            candidates &= candidates - 1;
            selected--;
        }
        return Direction.VALUES[Integer.numberOfTrailingZeros(candidates)];
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof ConveyorWandItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockMenus.open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        return true;
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorItem rider) {
        var stack = rider.getItemStack();
        if (stack.isEmpty()
                || !(level.getBlockEntity(pos) instanceof CraneRouterBlockEntity router)) {
            return;
        }

        var output = getOutputDir(router, stack);
        if (output != null) {
            sendOnRoute(level, pos, stack, output);
        } else {
            CraneBlock.spill(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        }
    }

    protected void sendOnRoute(Level level, BlockPos pos, ItemStack item, Direction direction) {
        var destination = pos.relative(direction);
        double mouthX = pos.getX() + 0.5 + direction.getStepX() * 0.55;
        double mouthY = pos.getY() + 0.5 + direction.getStepY() * 0.55;
        double mouthZ = pos.getZ() + 0.5 + direction.getStepZ() * 0.55;
        var belt = NtmContracts.CONVEYOR_BELT.at(level, destination);
        if (belt == null) {
            CraneBlock.spill(level, mouthX, mouthY, mouthZ, item.copy());
            return;
        }

        var moving = new MovingItemEntity(level);
        moving.setItemStack(item.copy());
        var snapped =
                belt.getSnappingPosition(level, destination, new Vec3(mouthX, mouthY, mouthZ));
        moving.snapTo(snapped.x, snapped.y, snapped.z, 0, 0);
        level.addFreshEntity(moving);
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorPackage rider) {
        return true;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction side, ConveyorPackage rider) {
        var buckets = sort(level, pos, rider.getItemStacks());
        releasePackages(level, pos, buckets);
    }

    private static void releasePackages(Level level, BlockPos pos, List<List<ItemStack>> buckets) {
        for (int face = 0; face < CraneRouterBlockEntity.SIDES; face++) {
            var bucket = buckets.get(face);
            if (bucket.isEmpty()) {
                continue;
            }

            var output = Direction.VALUES[face];
            var destination = pos.relative(output);
            double mouthX = pos.getX() + 0.5 + output.getStepX() * 0.55;
            double mouthY = pos.getY() + 0.5 + output.getStepY() * 0.55;
            double mouthZ = pos.getZ() + 0.5 + output.getStepZ() * 0.55;
            var belt = NtmContracts.CONVEYOR_BELT.at(level, destination);
            if (belt == null) {
                spillBucket(level, mouthX, mouthY, mouthZ, bucket);
            } else {
                var parcel = new MovingPackageEntity(level);
                parcel.setItemStacks(bucket);
                var snapped =
                        belt.getSnappingPosition(
                                level, destination, new Vec3(mouthX, mouthY, mouthZ));
                parcel.snapTo(snapped.x, snapped.y, snapped.z, 0, 0);
                level.addFreshEntity(parcel);
            }
        }

        spillBucket(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                buckets.get(CraneRouterBlockEntity.SIDES));
    }

    private static void spillBucket(
            Level level, double x, double y, double z, List<ItemStack> stacks) {
        for (var stack : stacks) {
            CraneBlock.spill(level, x, y, z, stack.copy());
        }
    }
}
