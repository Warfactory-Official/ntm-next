// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFoundryMold extends BlockEntityFoundryCastingBase
        implements IRenderFoundry {

    public BlockEntityFoundryMold(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 0;
    }

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.25D / this.getCapacity();
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
        return 0.25D;
    }
}
