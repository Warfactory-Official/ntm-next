// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ITickingBlock;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.BlockEntityFireworks;
import com.hbm.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockFireworks extends Block implements ITickingBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int GUNPOWDER_CHARGES = 3;

    public BlockFireworks(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFireworks(pos, state);
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFireworks battery)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        int dyed = ColorUtil.getColorFromDye(stack);
        if (stack.is(Items.GUNPOWDER)) {
            battery.charges += stack.getCount() * GUNPOWDER_CHARGES;
            stack.setCount(0);
        } else if (stack.is(ModItems.SULFUR.get())) {
            battery.charges += stack.getCount();
            stack.setCount(0);
        } else if (dyed != 0) {
            battery.color = dyed;
            stack.shrink(1);
        } else if (stack.is(Items.NAME_TAG)) {
            battery.message = stack.getHoverName().getString();
            stack.shrink(1);
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        battery.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFireworks battery)) {
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(getName().copy().withStyle(ChatFormatting.GOLD));
        say(player, "charges", Component.literal(Integer.toString(battery.charges)));
        say(player, "color", Component.literal(Integer.toHexString(battery.color)));
        say(player, "message", Component.literal(battery.message));
        return InteractionResult.SUCCESS;
    }

    private static void say(Player player, String row, Component value) {
        player.sendSystemMessage(
                Component.translatable("desc.block.fireworks." + row, value)
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(POWERED) ? ITickingBlock.super.getTicker(level, state, type) : null;
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
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_CLIENTS);
        if (!powered && level.getBlockEntity(pos) instanceof BlockEntityFireworks battery)
            battery.resetSequence();
    }
}
