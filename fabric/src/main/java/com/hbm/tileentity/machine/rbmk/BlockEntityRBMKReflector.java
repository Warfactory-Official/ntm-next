// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.packet.SyncUnitSchema;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityRBMKReflector extends BlockEntityRBMKBase implements SyncUnitSchema {

    public BlockEntityRBMKReflector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_REFLECTOR.get(), pos, state, 0);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.BLANK);
        super.onMelt(reduce);
    }

    @Override
    public RBMKType getRBMKType() {
        return RBMKType.REFLECTOR;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.REFLECTOR;
    }
}
