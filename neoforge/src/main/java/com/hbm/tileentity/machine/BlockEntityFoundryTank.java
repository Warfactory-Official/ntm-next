// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFoundryTank extends BlockEntityFoundryBase {

    public int nextUpdate;

    public BlockEntityFoundryTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_TANK.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (this.type == null && this.amount != 0) {
            this.amount = 0;
        }

        nextUpdate--;

        if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {

            boolean hasOp = false;
            nextUpdate = level.getRandom().nextInt(6) + 5;

            if (level.getBlockEntity(worldPosition.below())
                    instanceof BlockEntityFoundryTank tank) {
                if ((tank.type == null || tank.type == this.type)
                        && tank.amount < tank.getCapacity()) {
                    tank.type = this.type;
                    int toFill = Math.min(this.amount, tank.getCapacity() - tank.amount);
                    this.amount -= toFill;
                    tank.amount += toFill;
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
                    ICrucibleAcceptor acc = acceptorAt(dir, target);

                    if (acc != null && b != ModBlocks.FOUNDRY_CHANNEL.get()) {
                        if (acc.canAcceptPartialFlow(
                                level,
                                target,
                                dir.getOpposite(),
                                new MaterialStack(this.type, this.amount))) {
                            MaterialStack left =
                                    acc.flow(
                                            level,
                                            target,
                                            dir.getOpposite(),
                                            new MaterialStack(this.type, this.amount));
                            if (left == null) {
                                this.type = null;
                                this.amount = 0;
                            } else {
                                this.amount = left.amount;
                            }
                            hasOp = true;
                            break;
                        }
                    }
                }
            }

            if (!hasOp) {
                for (Direction dir : dirs) {
                    BlockEntity b = level.getBlockEntity(worldPosition.relative(dir));

                    if (b instanceof BlockEntityFoundryTank acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            acc.type = this.type;

                            if (level.getRandom().nextInt(5) == 0) {
                                int buf = this.amount;
                                this.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                int diff = this.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    this.amount -= diff;
                                    acc.amount += diff;
                                }
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
        return MaterialShapes.BLOCK.q(4);
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return stack;
    }
}
