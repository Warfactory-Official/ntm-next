// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class EntityItemBuoyant extends ItemEntity {

    public EntityItemBuoyant(EntityType<? extends EntityItemBuoyant> type, Level level) {
        super(type, level);
    }

    public EntityItemBuoyant(Level level, double x, double y, double z, ItemStack stack) {
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
