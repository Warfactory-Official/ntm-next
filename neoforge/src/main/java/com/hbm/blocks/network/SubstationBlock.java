// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.network.BlockEntitySubstation;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SubstationBlock extends BlockMultiblockCore implements EntityBlock {

    public static final MapCodec<SubstationBlock> CODEC = simpleCodec(SubstationBlock::new);

    private static final int[] DIMENSIONS = {4, 0, 1, 1, 2, 2};

    public SubstationBlock(Properties props) {
        super(props);
    }

    private static void visitRing(BlockPos core, RingVisitor visitor) {
        visitor.cell(core, MASK_HORIZONTAL);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            visitor.cell(core.relative(dir), MASK_HORIZONTAL & ~(1 << dir.ordinal()));
            visitor.cell(core.relative(dir).relative(dir.getClockWise()), MASK_HORIZONTAL);
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            visitRing(
                    pos,
                    (cell, mask) -> CableConductorBlockBase.mintNode(server, cell, state, mask));
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        visitRing(pos, (cell, mask) -> CableConductorBlockBase.dropNode(level, cell));
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return PylonBlockBase.dyeAt(level, core, held, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySubstation(pos, state);
    }

    private interface RingVisitor {
        void cell(BlockPos pos, int mask);
    }
}
