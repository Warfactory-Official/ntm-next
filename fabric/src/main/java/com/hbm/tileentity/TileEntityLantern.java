// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TileEntityLantern extends BlockEntity {

    public TileEntityLantern(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % 20 != 0) return;

        BlockPos pos = getBlockPos();
        AABB aabb =
                new AABB(
                                pos.getX() + 0.5,
                                pos.getY() + 5.5,
                                pos.getZ() + 0.5,
                                pos.getX() + 0.5,
                                pos.getY() + 5.5,
                                pos.getZ() + 0.5)
                        .inflate(7.5);

        List<EntityGlyphid> glyphids = serverLevel.getEntitiesOfClass(EntityGlyphid.class, aabb);
        for (EntityGlyphid glyphid : glyphids) {
            glyphid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }
}
