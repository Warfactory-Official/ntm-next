// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockBarrelExplosive extends Block implements IFuckingExplode {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    private final int popFuse;

    private final boolean flammable;

    protected BlockBarrelExplosive(
            BlockBehaviour.Properties props, int popFuse, boolean flammable) {
        super(props);
        this.popFuse = popFuse;
        this.flammable = flammable;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        primeFromBlast(level, pos, explosion);
    }

    protected void primeFromBlast(Level level, BlockPos pos, @Nullable Explosion explosion) {
        ChainDetonation.spawn(
                level,
                pos,
                explosion != null ? explosion.getIndirectSourceEntity() : null,
                defaultBlockState(),
                popFuse);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!flammable || !TntRule.explodes(level)) return;
        for (Direction dir : Direction.VALUES) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) {
                level.removeBlock(pos, false);

                primeFromBlast(level, pos, null);
                return;
            }
        }
    }
}
