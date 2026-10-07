// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blockentity.machine.BlockEntityReactorZirnox;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
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

        BlockEntity be = level.getBlockEntity(core);
        if (be instanceof BlockEntityRBMKBase rbmk) {
            rbmk.meltdown();
            return InteractionResult.SUCCESS;
        }
        if (be instanceof BlockEntityReactorZirnox zirnox) {
            zirnox.heat = 200000;
            zirnox.markChanged();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
