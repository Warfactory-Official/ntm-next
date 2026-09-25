// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableBoxBlock extends CableBlock {

    public final int size;

    private final VoxelShape[] selectionShapes;
    private final VoxelShape[] collisionShapes;

    public CableBoxBlock(BlockBehaviour.Properties props, int size) {
        super(props);
        this.size = size;
        this.selectionShapes = BoxConduitShapes.selection(size, BoxConduitShapes.CABLE_HUB);
        this.collisionShapes = BoxConduitShapes.collision(size, BoxConduitShapes.CABLE_HUB);
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
