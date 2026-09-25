// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class BlockGraphiteRod extends BlockGraphiteDrilledBase {

    public BlockGraphiteRod(BlockBehaviour.Properties props) {
        super(props);
    }

    static void toggleRun(
            Level level, BlockPos pos, BlockState match, Direction dir, boolean withdrawn) {
        BlockPos.MutableBlockPos cursor = pos.mutable().move(dir);
        while (true) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof BlockGraphiteRod)) return;
            if (state.getValue(AXIS) != match.getValue(AXIS)) return;
            if (state.getValue(SHROUDED) != match.getValue(SHROUDED)) return;
            if (state.getValue(WITHDRAWN) != match.getValue(WITHDRAWN)) return;
            level.setBlock(cursor, state.setValue(WITHDRAWN, withdrawn), 3);
            cursor.move(dir);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Direction side = hit.getDirection();
        if (!isChannelFace(state, side)) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean withdrawn = state.getValue(WITHDRAWN);
        level.setBlock(pos, state.setValue(WITHDRAWN, !withdrawn), 3);

        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3F,
                withdrawn ? 0.65F : 0.75F);

        toggleRun(level, pos, state, side, !withdrawn);
        toggleRun(level, pos, state, side.getOpposite(), !withdrawn);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_BORON.get();
    }
}
