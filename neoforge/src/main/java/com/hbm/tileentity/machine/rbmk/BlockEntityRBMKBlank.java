// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.packet.SyncUnitSchema;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityRBMKBlank extends BlockEntityRBMKBase implements SyncUnitSchema {

    public BlockEntityRBMKBlank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_BLANK.get(), pos, state, 0);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.BLANK);
        super.onMelt(reduce);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.BLANK;
    }
}
