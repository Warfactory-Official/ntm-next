// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.sound.ModSounds;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CableSwitchBlock extends CableConductorBlockBase {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public CableSwitchBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected int nodeMask(BlockState state) {
        return state.getValue(OPEN) ? OPEN_ALL : 0;
    }

    @Override
    public boolean canConnectCable(BlockState state, Direction side) {

        return true;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return toggle(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(state, level, pos, player);
    }

    protected InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        setOpen(level, pos, state, !state.getValue(OPEN));
        return InteractionResult.SUCCESS;
    }

    protected void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
        level.playSound(
                null,
                pos,
                ModSounds.REACTOR_START.get(),
                SoundSource.BLOCKS,
                1.0F,
                open ? 1.0F : 0.85F);
        if (level instanceof ServerLevel sl) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(sl);
            if (graph.containsCell(pos.asLong())) {
                graph.updateConnections(pos.asLong(), open ? OPEN_ALL : 0);
            }
        }
    }
}
