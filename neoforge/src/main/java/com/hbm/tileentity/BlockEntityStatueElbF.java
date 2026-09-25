// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BlockEntityStatueElbF extends BlockEntity {

    private static final double SEARCH_RADIUS = 9.0D;
    private static final double FEET_RADIUS_SQ = 16.0D;
    private static final double EYE_RADIUS_SQ = 64.0D;

    public BlockEntityStatueElbF(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATUE_ELB_F.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        AABB search =
                new AABB(
                        x - SEARCH_RADIUS,
                        y - SEARCH_RADIUS,
                        z - SEARCH_RADIUS,
                        x + SEARCH_RADIUS,
                        y + SEARCH_RADIUS,
                        z + SEARCH_RADIUS);
        for (Player player : server.getEntitiesOfClass(Player.class, search)) {
            if (player.distanceToSqr(x, y, z) > FEET_RADIUS_SQ) continue;
            double dx = player.getX() - x;
            double dy = player.getEyeY() - y;
            double dz = player.getZ() - z;
            if (dx * dx + dy * dy + dz * dz >= EYE_RADIUS_SQ) continue;
            player.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 5, 99));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 99));
        }
    }
}
