// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IToolable;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.bomb.BlockEntityCharge;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockChargeBase extends Block
        implements EntityBlock, IBomb, IToolable, IFuckingExplode {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final VoxelShape UP_SHAPE = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape DOWN_SHAPE = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape WEST_SHAPE = Block.box(10, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 6, 16, 16);

    private static final ThreadLocal<Boolean> SAFE_REMOVAL = ThreadLocal.withInitial(() -> false);

    protected BlockChargeBase(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    private static void removeBlockSafely(Level level, BlockPos pos) {
        boolean previous = SAFE_REMOVAL.get();
        SAFE_REMOVAL.set(true);
        try {
            level.removeBlock(pos, false);
        } finally {
            SAFE_REMOVAL.set(previous);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCharge(pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && !canSurvive(state, level, pos)) {

            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BlockEntityCharge charge)
            charge.fuseTick(level, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!SAFE_REMOVAL.get()) {
            explode(level, pos, null);
        }
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {

        if (!state.isAir()
                && explosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            removeBlockSafely(level, pos);
            wasExploded(level, pos, explosion);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {

        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), 0);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        explode(level, BlockPos.containing(x, y, z), entity);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.DEFUSER
                || !(level.getBlockEntity(pos) instanceof BlockEntityCharge charge)) return false;
        if (charge.started) {
            charge.disarm();
            level.playSound(
                    null, pos, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (!level.isClientSide()) {
            removeBlockSafely(level, pos);
            Block.popResource(level, pos, new ItemStack(this));
        }
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

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCharge charge) || charge.started)
            return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (charge.timer > 0) {
                charge.arm((ServerLevel) level);
                level.playSound(
                        null, pos, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            charge.timer =
                    switch (charge.timer) {
                        case 0 -> 100;
                        case 100 -> 200;
                        case 200 -> 300;
                        case 300 -> 600;
                        case 600 -> 1200;
                        case 1200 -> 3600;
                        case 3600 -> 6000;
                        default -> 0;
                    };
            level.playSound(null, pos, ModSounds.TECH_BOOP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        charge.changed();
        return InteractionResult.SUCCESS;
    }

    protected final void removeSafely(Level level, BlockPos pos) {
        removeBlockSafely(level, pos);
    }

    public void addChargeTooltip(Consumer<Component> tooltip) {
        tooltip.accept(
                Component.translatable("desc.block.charge.rightClickToChange")
                        .withStyle(ChatFormatting.YELLOW));
        tooltip.accept(
                Component.translatable("desc.block.charge.sneakClickToArm")
                        .withStyle(ChatFormatting.YELLOW));
        tooltip.accept(
                Component.translatable("desc.block.charge.canOnlyBeDisarmed")
                        .withStyle(ChatFormatting.RED));
    }
}
