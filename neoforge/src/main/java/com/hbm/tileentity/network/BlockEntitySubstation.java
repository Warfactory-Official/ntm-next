// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntitySubstation extends BlockEntityPylonBase {

    private static final double WIRE_HEIGHT = 5.25D;

    public BlockEntitySubstation(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSTATION.get(), pos, state);
    }

    private static float armYaw(Direction facing) {
        return (float) (facing.getAxis() == Direction.Axis.Z ? 0D : Math.PI * 0.5D);
    }

    @Override
    public ConnectionType connectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] mounts() {
        Vec3 arm =
                new Vec3(1, 0, 0)
                        .yRot(armYaw(getBlockState().getValue(BlockMultiblockCore.FACING)));
        return new Vec3[] {
            new Vec3(0.5 + arm.x * 0.5, WIRE_HEIGHT, 0.5 + arm.z * 0.5),
            new Vec3(0.5 + arm.x * 1.5, WIRE_HEIGHT, 0.5 + arm.z * 1.5),
            new Vec3(0.5 - arm.x * 0.5, WIRE_HEIGHT, 0.5 - arm.z * 0.5),
            new Vec3(0.5 - arm.x * 1.5, WIRE_HEIGHT, 0.5 - arm.z * 1.5),
        };
    }

    @Override
    public Vec3 connectionPoint() {
        return new Vec3(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + WIRE_HEIGHT,
                worldPosition.getZ() + 0.5);
    }

    @Override
    public double maxWireLength() {
        return 20;
    }
}
