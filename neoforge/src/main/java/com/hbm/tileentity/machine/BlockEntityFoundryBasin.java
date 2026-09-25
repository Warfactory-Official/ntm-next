// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFoundryBasin extends BlockEntityFoundryCastingBase
        implements IRenderFoundry {

    public BlockEntityFoundryBasin(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 1;
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

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.75D / this.getCapacity();
    }

    @Override
    public NTMMaterial getMat() {
        return this.type;
    }

    @Override
    public double minX() {
        return 0.125D;
    }

    @Override
    public double maxX() {
        return 0.875D;
    }

    @Override
    public double minZ() {
        return 0.125D;
    }

    @Override
    public double maxZ() {
        return 0.875D;
    }

    @Override
    public double moldHeight() {
        return 0.13D;
    }

    @Override
    public double outHeight() {
        return 0.875D;
    }
}
