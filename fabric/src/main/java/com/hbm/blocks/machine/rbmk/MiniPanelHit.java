// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class MiniPanelHit {

    private MiniPanelHit() {}

    static int columnOf(BlockState state, BlockHitResult hit) {
        if (hit.getDirection().getAxis() == Direction.Axis.Y) return -1;

        BlockPos pos = hit.getBlockPos();
        Vec3 at = hit.getLocation();
        double hitX = at.x - pos.getX();
        double hitZ = at.z - pos.getZ();
        if (hitX == 0 || hitX == 1 || hitZ == 0 || hitZ == 1) return -1;

        return switch (state.getValue(RBMKMiniPanelBase.FACING)) {
            case NORTH -> hitX < 0.5 ? 1 : 0;
            case SOUTH -> hitX > 0.5 ? 1 : 0;
            case WEST -> hitZ > 0.5 ? 1 : 0;
            case EAST -> hitZ < 0.5 ? 1 : 0;
            default -> 0;
        };
    }

    static int rowOffset(BlockHitResult hit) {
        return hit.getLocation().y - hit.getBlockPos().getY() < 0.5 ? 2 : 0;
    }
}
