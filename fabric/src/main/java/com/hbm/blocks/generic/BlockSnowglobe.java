// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.tileentity.BlockEntitySnowglobe;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockSnowglobe extends Block implements EntityBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 5, 12);

    public static Consumer<BlockEntitySnowglobe> OPEN_GUI = be -> {};

    public BlockSnowglobe(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySnowglobe(pos, state);
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
        if (level.getBlockEntity(pos) instanceof BlockEntitySnowglobe globe) {
            globe.type = BlockEntitySnowglobe.typeOf(stack);
            globe.setChanged();
            if (!level.isClientSide()) globe.networkPackNTTracking();
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
        if (level.getBlockEntity(pos) instanceof BlockEntitySnowglobe globe) {
            return BlockEntitySnowglobe.stackOf(globe.type);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getAbilities().instabuild
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntitySnowglobe globe) {
            ItemEntity item =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            BlockEntitySnowglobe.stackOf(globe.type));

            item.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntitySnowglobe globe) {
            OPEN_GUI.accept(globe);
        }
        return InteractionResult.SUCCESS;
    }

    public enum SnowglobeType {
        NONE("NONE", null),
        RIVETCITY("Rivet City", "Welcome to Rivet City. Please wait while the bridge extends."),
        TENPENNYTOWER(
                "Tenpenny Tower",
                "Tenpenny Tower is the brainchild of Allistair Tenpenny, a British refugee who came to the "
                        + "Capital Wasteland seeking his fortune."),
        LUCKY38(
                "Lucky 38",
                "My guess? Leads to a big cashout at some casino - and if the \"38\" on it is any indication... "
                        + "well... Lucky 38 it is."),
        SIERRAMADRE(
                "Sierra Madre",
                "It's the moment you've been waiting for, the reason we're all here - the Gala Event, the Grand "
                        + "Opening of the Sierra Madre Casino."),
        PRYDWEN(
                "The Prydwen",
                "People of the Commonwealth. Do not interfere. Our intentions are peaceful. We are the "
                        + "Brotherhood of Steel.");

        private static final SnowglobeType[] VALUES = values();
        public final String label;
        public final @Nullable String inscription;

        SnowglobeType(String label, @Nullable String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        public static SnowglobeType byOrdinal(int raw) {
            return VALUES[Math.abs(raw) % VALUES.length];
        }

        public static int count() {
            return VALUES.length;
        }
    }
}
