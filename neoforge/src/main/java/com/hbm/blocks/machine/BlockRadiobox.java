// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityRadiobox;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockRadiobox extends BlockMachineHorizontal implements ICapabilityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape NORTH = Block.box(4, 1, 11, 12, 15, 16);
    private static final VoxelShape SOUTH = Block.box(4, 1, 0, 12, 15, 5);
    private static final VoxelShape WEST = Block.box(11, 1, 4, 16, 15, 12);
    private static final VoxelShape EAST = Block.box(0, 1, 4, 5, 15, 12);

    public BlockRadiobox(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(ACTIVE) ? super.getTicker(level, state, type) : null;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRadiobox(pos, state);
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
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntityRadiobox radiobox = (BlockEntityRadiobox) level.getBlockEntity(pos);
        if (stack.is(ModItems.BATTERY_SPARK.get()) && !radiobox.isInfinite()) {
            stack.consume(1, player);
            level.playSound(
                    null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
            radiobox.installInfinite();
            return InteractionResult.SUCCESS;
        }

        toggle(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) toggle(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void toggle(BlockState state, Level level, BlockPos pos) {
        boolean active = !state.getValue(ACTIVE);
        level.setBlock(pos, state.setValue(ACTIVE, active), UPDATE_CLIENTS);
        level.playSound(
                null,
                pos,
                ModSounds.REACTOR_START.get(),
                SoundSource.BLOCKS,
                1.0F,
                active ? 1.0F : 0.85F);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADIOBOX).powerIn().fe();
    }
}
