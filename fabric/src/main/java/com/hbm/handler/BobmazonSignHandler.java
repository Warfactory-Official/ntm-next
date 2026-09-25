// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.items.ModItems;
import com.hbm.util.ShadyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public final class BobmazonSignHandler {

    private BobmazonSignHandler() {}

    public static boolean onSignUsed(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;

        SignText text = sign.getFrontText();
        String result =
                ShadyUtil.smoosh(
                        text.getMessage(0, false).getString(),
                        text.getMessage(1, false).getString(),
                        text.getMessage(2, false).getString(),
                        text.getMessage(3, false).getString());

        if (!ShadyUtil.HASHES.contains(result)) return false;

        level.destroyBlock(pos, false);

        ItemEntity drop =
                new ItemEntity(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        new ItemStack(ModItems.BOBMAZON_HIDDEN));
        drop.setPickUpDelay(10);
        level.addFreshEntity(drop);

        return true;
    }
}
