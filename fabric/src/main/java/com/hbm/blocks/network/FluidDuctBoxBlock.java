// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FluidDuctBoxBlock extends FluidPipeBlock {

    public final int size;
    public final boolean tinted;

    private final VoxelShape[] selectionShapes;
    private final VoxelShape[] collisionShapes;

    public FluidDuctBoxBlock(BlockBehaviour.Properties props, int size, boolean tinted) {
        super(props);
        this.size = size;
        this.tinted = tinted;
        this.selectionShapes = BoxConduitShapes.selection(size, BoxConduitShapes.DUCT_HUB);
        this.collisionShapes = BoxConduitShapes.collision(size, BoxConduitShapes.DUCT_HUB);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return selectionShapes[BoxConduitShapes.maskOf(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collisionShapes[BoxConduitShapes.maskOf(state)];
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }
}
