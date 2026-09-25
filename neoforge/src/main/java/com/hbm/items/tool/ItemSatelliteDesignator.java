// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.machine.ItemSatelliteChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ItemSatelliteDesignator extends ItemSatelliteChip {

    private static final double RANGE = 300D;

    public ItemSatelliteDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.CONSUME;

        ItemStack stack = player.getItemInHand(hand);
        Satellite sat = SatelliteSavedData.get(server).getSatFromFreq(getFreq(stack));
        if (sat == null) return InteractionResult.CONSUME;

        BlockPos target = trace(server, player);
        sat.onCoordAction(server, player, target.getX(), target.getY(), target.getZ());
        return InteractionResult.CONSUME;
    }

    private static BlockPos trace(ServerLevel level, Player player) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player));
        return hit.getBlockPos().relative(hit.getDirection());
    }
}
