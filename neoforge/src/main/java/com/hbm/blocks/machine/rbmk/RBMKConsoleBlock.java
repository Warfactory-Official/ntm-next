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

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import com.hbm.items.tool.GuideBookItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKConsoleBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, ToolInteractionHandler {
    private static final double BOOK_HALF_WIDTH = 0.1875D;

    private static final Vec3 BOOK_NORTH = bookOffset(90);
    private static final Vec3 BOOK_SOUTH = bookOffset(270);
    private static final Vec3 BOOK_WEST = bookOffset(180);
    private static final Vec3 BOOK_EAST = bookOffset(0);

    private static Vec3 bookOffset(int degrees) {
        return new Vec3(1.375D, 0, 0.75D).yRot((float) Math.toRadians(degrees));
    }

    private static Vec3 bookOffset(Direction facing) {
        return switch (facing) {
            case NORTH -> BOOK_NORTH;
            case SOUTH -> BOOK_SOUTH;
            case WEST -> BOOK_WEST;
            case EAST -> BOOK_EAST;
            default -> throw new IllegalArgumentException();
        };
    }

    public RBMKConsoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKConsoleBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide() || player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }

        BossSpawnHandler.markFBI(player);
        if (hit.getDirection() == Direction.UP) {
            var offset = bookOffset(state.getValue(FACING));
            var location = hit.getLocation();
            double bookX = core.getX() + 0.5 + offset.x;
            double bookZ = core.getZ() + 0.5 + offset.z;
            boolean withinBook =
                    Math.abs(location.x - bookX) < BOOK_HALF_WIDTH
                            && Math.abs(location.z - bookZ) < BOOK_HALF_WIDTH;
            if (withinBook) {
                var manual = ModItems.GUIDE_BOOK.get(GuideBookItem.BookType.RBMK);
                var inventory = player.getInventory();
                if (!inventory.contains(stack -> stack.is(manual))) {
                    inventory.placeItemBackInInventory(
                            new ItemStack(manual), Prediction.SERVER_ONLY);
                    player.inventoryMenu.broadcastChanges();
                    return InteractionResult.SUCCESS;
                }
            }
        }

        openCoreMenu(player, core, (RBMKConsoleBlockEntity) level.getBlockEntity(core));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) {
            return false;
        }
        if (!level.isClientSide()) {
            ((RBMKConsoleBlockEntity) level.getBlockEntity(pos)).rotate();
        }
        return true;
    }
}
