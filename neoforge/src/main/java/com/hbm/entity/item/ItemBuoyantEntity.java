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

package com.hbm.entity.item;

import com.hbm.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

public class ItemBuoyantEntity extends ItemEntity {

    public ItemBuoyantEntity(EntityType<? extends ItemBuoyantEntity> type, Level level) {
        super(type, level);
    }

    public ItemBuoyantEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.ITEM_BUOYANT.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setDeltaMovement(random.nextDouble() * 0.2 - 0.1, 0.2, random.nextDouble() * 0.2 - 0.1);
    }

    @Override
    public void tick() {
        BlockPos below =
                new BlockPos(Mth.floor(getX()), Mth.floor(getY() - 0.0625), Mth.floor(getZ()));
        BlockState state = level().getBlockState(below);
        FluidState fluid = state.getFluidState();

        if (state.getBlock() instanceof LiquidBlock
                && fluid.is(FluidTags.WATER)
                && !fluid.getValue(FlowingFluid.FALLING)) {
            setDeltaMovement(getDeltaMovement().add(0, 0.045D, 0));
        }

        super.tick();
    }
}
