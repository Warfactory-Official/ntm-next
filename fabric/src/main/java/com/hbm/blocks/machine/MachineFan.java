// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.IToolable;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityFan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MachineFan extends Block implements ITickingBlock, IToolable {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int ID_FAN_MODE = 10;

    public MachineFan(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(POWERED) ? ITickingBlock.super.getTicker(level, state, type) : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
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
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return AxialSupportShape.forAxis(state.getValue(FACING).getAxis());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFan(pos, state);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool == ToolType.SCREWDRIVER) {

            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()), 3);
            return true;
        }

        if (tool != ToolType.HAND_DRILL && tool != ToolType.DEFUSER) return false;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFan fan)) return true;

        String suffix;
        if (tool == ToolType.HAND_DRILL) {
            fan.falloff = !fan.falloff;
            suffix = fan.falloff ? ".falloffOn" : ".falloffOff";
        } else {
            fan.suck = !fan.suck;
            suffix = fan.suck ? ".suckOn" : ".suckOff";
        }
        fan.setChanged();

        if (!level.isClientSide()) {
            fan.networkPackNT(150);
            if (player instanceof ServerPlayer serverPlayer) {
                Services.NETWORK.sendTo(
                        new PlayerInformPayload(
                                Component.translatable(getDescriptionId() + suffix)
                                        .withStyle(ChatFormatting.GOLD),
                                ID_FAN_MODE,
                                PlayerInformPayload.DEFAULT_MILLIS),
                        serverPlayer);
            }

            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.5F,
                    0.5F);
        }

        return true;
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
        if (powered != state.getValue(POWERED))
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
    }
}
