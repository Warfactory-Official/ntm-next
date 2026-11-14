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

package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.DynamicSlagBlock.BlockEntitySlag;
import com.hbm.blocks.generic.DynamicSlagBlock;
import com.hbm.inventory.material.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public final class SlagDischarge extends FoundryDischarge {
    public SlagDischarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_SLAGTAP.get(), pos, state);
    }

    @Override
    protected int dropRange() {
        return 15;
    }

    @Override
    public boolean canAcceptFlow(
            Level world, BlockPos cell, Direction side, MaterialStack offered) {
        return admitsSideAndMaterial(side, offered.material)
                && trace(world, cell).getType() == HitResult.Type.BLOCK;
    }

    @Override
    public @Nullable MaterialStack acceptFlow(
            Level world, BlockPos cell, Direction side, MaterialStack offered) {
        if (offered.amount <= 0) return null;
        var hit = trace(world, cell);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        var landing = hit.getBlockPos();
        int original = offered.amount;
        if (world.getBlockEntity(landing) instanceof BlockEntitySlag pile) {
            if (pile.mat == offered.material && pile.amount < BlockEntitySlag.MAX_AMOUNT) {
                offered.hbm$syncSet$MaterialStack$amount(
                        (int) (offered.amount - (pile.absorb(offered.amount))));
                world.scheduleTick(landing, ModBlocks.SLAG.get(), 1);
            }
        } else if (world.getBlockState(landing).canBeReplaced()) {
            deposit(world, landing, offered);
        }
        if (offered.amount > 0 && world.getBlockState(landing.above()).canBeReplaced()) {
            deposit(world, landing.above(), offered);
        }
        if (offered.amount != original) {
            publishStream(
                    offered.material, Math.max(1F, worldPosition.getY() - (float) landing.getY()));
        }
        return offered.amount == 0 ? null : offered;
    }

    private static void deposit(Level world, BlockPos pos, MaterialStack offered) {
        int moved = Math.min(offered.amount, BlockEntitySlag.MAX_AMOUNT);
        if (DynamicSlagBlock.spill(world, pos, offered.material, moved))
            offered.hbm$syncSet$MaterialStack$amount((int) (offered.amount - (moved)));
    }
}
