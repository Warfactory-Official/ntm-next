// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityPylonLarge extends BlockEntityPylonBase {

    private static final double CROSSARM_HEIGHT = 11.5D;
    private static final double ARM_LIFT = 0.75D + 0.0625D;
    private static final double ARM_REACH = 3.375D;

    public BlockEntityPylonLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYLON_LARGE.get(), pos, state);
    }

    private static float armYaw(Direction facing) {
        return (float)
                (Math.PI
                        * switch (facing) {
                            case NORTH -> 0D;
                            case WEST -> 0.25D;
                            case SOUTH -> 0.5D;
                            default -> 0.75D;
                        });
    }

    @Override
    public ConnectionType connectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] mounts() {
        Vec3 arm =
                new Vec3(ARM_REACH, 0, 0)
                        .yRot(armYaw(getBlockState().getValue(BlockMultiblockCore.FACING)));
        return new Vec3[] {
            new Vec3(0.5 + arm.x, CROSSARM_HEIGHT + ARM_LIFT, 0.5 + arm.z),
            new Vec3(0.5 + arm.x, CROSSARM_HEIGHT - ARM_LIFT, 0.5 + arm.z),
            new Vec3(0.5 - arm.x, CROSSARM_HEIGHT + ARM_LIFT, 0.5 - arm.z),
            new Vec3(0.5 - arm.x, CROSSARM_HEIGHT - ARM_LIFT, 0.5 - arm.z),
        };
    }

    @Override
    public double maxWireLength() {
        return 100;
    }
}
