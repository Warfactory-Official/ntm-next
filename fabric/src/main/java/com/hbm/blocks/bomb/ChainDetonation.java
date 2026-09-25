// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityTntNtm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class ChainDetonation {

    private ChainDetonation() {}

    public static void spawn(
            Level level,
            BlockPos pos,
            @Nullable LivingEntity igniter,
            BlockState bomb,
            int popFuse) {
        if (!TntRule.explodes(level)) return;
        EntityTntNtm tnt =
                new EntityTntNtm(
                        ModEntities.TNT_NTM.get(),
                        level,
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D,
                        igniter,
                        bomb);
        tnt.setFuse(popFuse <= 0 ? 0 : level.getRandom().nextInt(popFuse) + popFuse / 2);
        level.addFreshEntity(tnt);
    }
}
