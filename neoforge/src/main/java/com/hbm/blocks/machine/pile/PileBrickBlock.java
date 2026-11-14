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

package com.hbm.blocks.machine.pile;

import com.hbm.blockentity.machine.pile.PileCoreBlockEntity.PileOrientation;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.PileBlock.Role;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.blocks.machine.pile.PileError;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PileBrickBlock extends Block implements ToolInteractionHandler {

    public static final int MIN_V_SIZE = 5;
    public static final int MIN_H_SIZE = 5;
    public static final int MAX_V_SIZE = 15;
    public static final int MAX_H_SIZE = 15;

    public PileBrickBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    private static BlockPos cell(
            BlockPos origin, Direction dir, Direction dirLeft, int h, int v, int d) {
        return origin.relative(dirLeft, -v).relative(dir, d).above(h);
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool == ToolType.HAND_DRILL) {

            if (side == null || side.getAxis().isVertical()) return false;
            if (level.isClientSide()) return true;

            Direction dir = side.getOpposite();
            Direction dirLeft = dir.getCounterClockWise();

            int negHeight = 0;
            int posHeight = 0;
            int left = 0;
            int right = 0;
            int depth = 0;

            for (int i = 1; i <= MAX_V_SIZE - 1; i++) {
                if (!level.getBlockState(pos.above(i)).is(this)) break;
                posHeight = i;
            }
            for (int i = 1; i <= MAX_V_SIZE - posHeight - 1; i++) {
                if (!level.getBlockState(pos.below(i)).is(this)) break;
                negHeight = i;
            }
            for (int i = 1; i <= MAX_H_SIZE - 1; i++) {
                if (!level.getBlockState(pos.relative(dirLeft, i)).is(this)) break;
                left = i;
            }
            for (int i = 1; i <= MAX_H_SIZE - left - 1; i++) {
                if (!level.getBlockState(pos.relative(dirLeft, -i)).is(this)) break;
                right = i;
            }
            for (int i = 1; i <= MAX_H_SIZE; i++) {
                if (!level.getBlockState(pos.relative(dir, i)).is(this)) break;
                depth = i;
            }

            if (posHeight + negHeight + 1 < MIN_V_SIZE) {
                PileError report = new PileError();
                Component message =
                        Component.translatable("marker.hbm.pile.height_low", MIN_V_SIZE);
                report.add(pos.above(posHeight), message);
                report.add(pos.below(negHeight), message);
                report.send(player);
                return true;
            }

            if (left + right + 1 < MIN_H_SIZE) {
                PileError report = new PileError();
                Component message = Component.translatable("marker.hbm.pile.width_low", MIN_H_SIZE);
                report.add(pos.relative(dirLeft, left), message);
                report.add(pos.relative(dirLeft, -right), message);
                report.send(player);
                return true;
            }

            if (depth + 1 < MIN_H_SIZE) {
                PileError.send(
                        pos.relative(dir, depth),
                        Component.translatable("marker.hbm.pile.depth_low", MIN_H_SIZE),
                        player);
                return true;
            }

            if (posHeight == 0 || negHeight == 0 || left == 0 || right == 0) {
                PileError.send(pos, Component.translatable("marker.hbm.pile.core_on_edge"), player);
                return true;
            }

            for (int h = -negHeight; h <= posHeight; h++) {
                for (int v = -left; v <= right; v++) {
                    for (int d = 0; d <= depth; d++) {
                        BlockPos probe = cell(pos, dir, dirLeft, h, v, d);
                        if (!level.getBlockState(probe).is(this)) {
                            PileError.send(
                                    probe,
                                    Component.translatable("marker.hbm.pile.graphite_missing"),
                                    player);
                            return true;
                        }
                    }
                }
            }

            List<BlockPos> cells = new ArrayList<>();
            for (int h = -negHeight; h <= posHeight; h++) {
                for (int v = -left; v <= right; v++) {
                    for (int d = 0; d <= depth; d++) {
                        BlockPos cell = cell(pos, dir, dirLeft, h, v, d);

                        if (cell.equals(pos)) {
                            level.setBlock(
                                    cell,
                                    ModBlocks.PILE_BLOCK
                                            .get()
                                            .defaultBlockState()
                                            .setValue(PileBlock.ROLE, Role.CORE),
                                    3);
                        } else {
                            int edgeCount = 0;
                            if (h == -negHeight || h == posHeight) edgeCount++;
                            if (v == -left || v == right) edgeCount++;
                            if (d == 0 || d == depth) edgeCount++;
                            boolean isEdge = edgeCount > 1;
                            level.setBlock(
                                    cell,
                                    ModBlocks.PILE_BLOCK
                                            .get()
                                            .defaultBlockState()
                                            .setValue(
                                                    PileBlock.ROLE,
                                                    isEdge ? Role.EDGE : Role.DUMMY),
                                    3);
                            cells.add(cell);
                        }
                    }
                }
            }
            AssembledMembers.assemble((ServerLevel) level, pos, cells);
            PileCoreBlockEntity core = (PileCoreBlockEntity) level.getBlockEntity(pos);
            core.orientation = PileOrientation.getOrientation(dir);
            core.setupSize(
                    posHeight,
                    negHeight,
                    left,
                    right,
                    depth + 1,
                    dir,
                    BoundingBox.encapsulatingPositions(cells).orElseThrow());
            core.setChanged();

            return true;
        }

        return false;
    }
}
