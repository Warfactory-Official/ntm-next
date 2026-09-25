// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.network.BlockEntityPylon;
import com.hbm.uninos.graph.LevelNodeGraph;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PylonBlock extends BlockMultiblockCore implements EntityBlock {

    public static final MapCodec<PylonBlock> CODEC = simpleCodec(PylonBlock::new);
    private static final int[] DIMENSIONS = {4, 0, 0, 0, 0, 0};

    private static final int HORIZONTAL_MASK =
            (1 << Direction.NORTH.ordinal())
                    | (1 << Direction.SOUTH.ordinal())
                    | (1 << Direction.EAST.ordinal())
                    | (1 << Direction.WEST.ordinal());

    public PylonBlock(Properties props) {
        super(props);
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
        return 0;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    public boolean canConnectCable(BlockState state, Direction side) {
        return (HORIZONTAL_MASK & (1 << side.ordinal())) != 0;
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
            CableConductorBlockBase.mintNode(server, pos, state, HORIZONTAL_MASK);
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        CableConductorBlockBase.dropNode(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
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
        return new BlockEntityPylon(pos, state);
    }
}
