// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.network.BlockEntityPylonMedium;
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

public class PylonMediumBlock extends BlockMultiblockCore implements EntityBlock {

    private static final int[] DIMENSIONS = {6, 0, 0, 0, 0, 0};

    private final boolean transformer;
    private final MapCodec<PylonMediumBlock> ownCodec;

    public PylonMediumBlock(boolean transformer, Properties props) {
        super(props);
        this.transformer = transformer;
        this.ownCodec = simpleCodec(p -> new PylonMediumBlock(transformer, p));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return ownCodec;
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

    private int nodeMask(BlockState state) {
        return transformer ? 1 << state.getValue(FACING).getOpposite().ordinal() : 0;
    }

    public boolean canConnectCable(BlockState state, Direction side) {
        return (nodeMask(state) & (1 << side.ordinal())) != 0;
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
            CableConductorBlockBase.mintNode(server, pos, state, nodeMask(state));
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        CableConductorBlockBase.dropNode(level, pos);
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
        return new BlockEntityPylonMedium(pos, state);
    }
}
