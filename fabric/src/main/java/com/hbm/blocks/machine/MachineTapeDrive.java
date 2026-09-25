// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.tileentity.machine.BlockEntityMachineTapeDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MachineTapeDrive extends BlockMachineHorizontal {
    private static final VoxelShape EAST = Shapes.box(0, 0, 0, 0.75, 1, 1);
    private static final VoxelShape SOUTH = Shapes.box(0, 0, 0, 1, 1, 0.75);
    private static final VoxelShape WEST = Shapes.box(0.25, 0, 0, 1, 1, 1);
    private static final VoxelShape NORTH = Shapes.box(0, 0, 0.25, 1, 1, 1);

    public MachineTapeDrive(Properties properties) {
        super(properties);
    }

    private static VoxelShape shape(Direction facing) {
        return switch (facing) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineTapeDrive(pos, state);
    }
}
