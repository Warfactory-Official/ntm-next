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

package com.hbm.blockentity.machine;

import com.hbm.api.block.MoltenMetalHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FoundryTankBlockEntity extends FoundryBaseBlockEntity {

    public int nextUpdate;

    public FoundryTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_TANK.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (this.type == null && this.amount != 0) {
            this.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
            markChanged();
        }

        nextUpdate--;

        if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {

            boolean hasOp = false;
            nextUpdate = level.getRandom().nextInt(6) + 5;

            if (level.getBlockEntity(worldPosition.below())
                    instanceof FoundryTankBlockEntity tank) {
                if ((tank.type == null || tank.type == this.type)
                        && tank.amount < tank.getCapacity()) {
                    tank.hbm$syncSet$FoundryBaseBlockEntity$type(this.type);
                    int toFill = Math.min(this.amount, tank.getCapacity() - tank.amount);
                    this.hbm$syncSet$FoundryBaseBlockEntity$amount((int) (this.amount - (toFill)));
                    tank.hbm$syncSet$FoundryBaseBlockEntity$amount((int) (tank.amount + (toFill)));
                    markChanged();
                    tank.markChanged();
                    hasOp = true;
                }
            }

            List<Direction> dirs =
                    new ArrayList<>(
                            List.of(
                                    Direction.NORTH,
                                    Direction.SOUTH,
                                    Direction.WEST,
                                    Direction.EAST));
            Collections.shuffle(dirs);

            if (!hasOp) {
                for (Direction dir : dirs) {
                    BlockPos target = worldPosition.relative(dir);
                    Block b = level.getBlockState(target).getBlock();
                    MoltenMetalHandler acc = acceptorAt(dir, target);

                    if (acc != null && b != ModBlocks.FOUNDRY_CHANNEL.get()) {
                        if (acc.canAcceptFlow(
                                level,
                                target,
                                dir.getOpposite(),
                                new MaterialStack(this.type, this.amount))) {
                            MaterialStack left =
                                    acc.acceptFlow(
                                            level,
                                            target,
                                            dir.getOpposite(),
                                            new MaterialStack(this.type, this.amount));
                            if (left == null) {
                                this.hbm$syncSet$FoundryBaseBlockEntity$type(null);
                                this.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
                            } else {
                                this.hbm$syncSet$FoundryBaseBlockEntity$amount(left.amount);
                            }
                            markChanged();
                            hasOp = true;
                            break;
                        }
                    }
                }
            }

            if (!hasOp) {
                for (Direction dir : dirs) {
                    BlockEntity b = level.getBlockEntity(worldPosition.relative(dir));

                    if (b instanceof FoundryTankBlockEntity acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            if (acc.type != this.type) acc.markChanged();
                            acc.hbm$syncSet$FoundryBaseBlockEntity$type(this.type);
                            int before = this.amount;
                            if (level.getRandom().nextInt(5) == 0) {
                                int buf = this.amount;
                                this.hbm$syncSet$FoundryBaseBlockEntity$amount(acc.amount);
                                acc.hbm$syncSet$FoundryBaseBlockEntity$amount(buf);
                            } else {
                                int diff = this.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    this.hbm$syncSet$FoundryBaseBlockEntity$amount(
                                            (int) (this.amount - (diff)));
                                    acc.hbm$syncSet$FoundryBaseBlockEntity$amount(
                                            (int) (acc.amount + (diff)));
                                }
                            }
                            if (this.amount != before) {
                                markChanged();
                                acc.markChanged();
                            }
                        }
                    }
                }
            }
        }

        super.tickServer();
    }

    @Override
    public int getCapacity() {
        return MaterialShape.BLOCK.amount(4);
    }

    @Override
    public boolean canAcceptFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack acceptFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return stack;
    }
}
