// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blockentity.BlockEntityPlushie;
import com.hbm.blocks.ModBlocks;
import com.hbm.registration.OpenRegistry;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockPlushie extends Block implements EntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final OpenRegistry<Plushie> TYPES = new OpenRegistry<>("plushie", Plushie::id);

    static {
        for (PlushieType type : PlushieType.values()) TYPES.add(type);
    }

    public BlockPlushie(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPlushie(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(ROTATION, RotationSegment.convertToSegment(ctx.getRotation() + 180.0F));
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BlockEntityPlushie plushie) {
            plushie.type = BlockEntityPlushie.typeOf(stack);
            plushie.setChanged();
            if (!level.isClientSide()) plushie.networkPackNTTracking();
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof BlockEntityPlushie plushie) {
            return BlockEntityPlushie.stackOf(plushie.type);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getAbilities().instabuild
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityPlushie plushie) {
            ItemEntity item =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            BlockEntityPlushie.stackOf(plushie.type));
            item.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPlushie plushie)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return plushie.type.useItemOn(stack, plushie, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPlushie plushie))
            return InteractionResult.SUCCESS;
        if (level.isClientSide()) {
            plushie.squish();
        } else {
            plushie.type.squeak(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public interface Plushie {

        String name();

        String label();

        @Nullable String inscription();

        ItemStack stack();

        void squeak(Level level, BlockPos pos);

        default InteractionResult useItemOn(
                ItemStack held, BlockEntityPlushie plushie, Player player) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        default String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum PlushieType implements Plushie {
        NONE("NONE", null),
        YOMI("Yomi", "Hi! Can I be your rabbit friend?"),
        NUMBERNINE("Number Nine", "None of y'all deserve coal."),
        HUNDUN("Hundun", "混沌"),
        DERG("Dragon", "Squeeze him.");

        public final String label;
        public final @Nullable String inscription;

        PlushieType(String label, @Nullable String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public @Nullable String inscription() {
            return inscription;
        }

        @Override
        public ItemStack stack() {
            return this == NONE ? new ItemStack(ModBlocks.PLUSHIE) : ModBlocks.PLUSHIES.stack(this);
        }

        @Override
        public void squeak(Level level, BlockPos pos) {
            if (this == HUNDUN) {
                level.playSound(
                        null,
                        pos,
                        ModSounds.BLOCK_HUNDUNS_MAGNIFICENT_HOWL.get(),
                        SoundSource.BLOCKS,
                        100.0F,
                        1.0F);
            } else {
                level.playSound(
                        null, pos, ModSounds.BLOCK_PLUSHY.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            }
        }
    }
}
