// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.BlockEntityReactorZirnox;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemDyatlov extends Item {

    public ItemDyatlov(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return InteractionResult.PASS;

        BlockEntity te = level.getBlockEntity(core);
        if (te instanceof BlockEntityRBMKBase rbmk) {
            rbmk.meltdown();
            return InteractionResult.SUCCESS;
        }
        if (te instanceof BlockEntityReactorZirnox zirnox) {
            zirnox.heat = 200000;
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
