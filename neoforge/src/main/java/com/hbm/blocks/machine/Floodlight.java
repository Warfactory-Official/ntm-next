// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IToolable;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.BlockEntityFloodlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Floodlight extends Block implements ITickingBlock, IToolable, ICapabilityBlock {

    public static final IntegerProperty FACING = IntegerProperty.create("facing", 0, 7);

    public Floodlight(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, 1));
    }

    private static void setAngle(
            Level level, BlockPos pos, LivingEntity player, boolean updateMeta) {
        int yawQuadrant = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        float rotation = player.getXRot();

        if (!(level.getBlockEntity(pos) instanceof BlockEntityFloodlight floodlight)) return;

        BlockState state = level.getBlockState(pos);
        int meta = state.getValue(FACING) % 6;
        BlockState updatedState = state;
        if (meta == 0 || meta == 1) {
            if ((yawQuadrant == 0 || yawQuadrant == 2) && updateMeta) {
                updatedState = state.setValue(FACING, meta + 6);
            }
            if (meta == 1 && (yawQuadrant == 0 || yawQuadrant == 1)) rotation = 180F - rotation;
            if (meta == 0 && (yawQuadrant == 0 || yawQuadrant == 3)) rotation = 180F - rotation;
        }

        floodlight.rotation = -Math.round(rotation / 5F) * 5F;
        if (floodlight.isOn) floodlight.destroyLights();
        floodlight.setChanged();
        if (updatedState != state) {
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static Direction toDirection(int facing) {
        return Direction.from3DDataValue(facing % 6);
    }

    private static int fromDirection(Direction dir, boolean flip) {
        return dir.get3DDataValue() + (flip ? 6 : 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFloodlight(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().get3DDataValue());
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer != null) setAngle(level, pos, placer, true);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!level.isClientSide()) setAngle(level, pos, player, false);
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        int facing = state.getValue(FACING);
        return state.setValue(
                FACING, fromDirection(rotation.rotate(toDirection(facing)), facing >= 6));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(toDirection(state.getValue(FACING))));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FLOODLIGHT).fe().powerIn().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Services.CAPS.registerProvider(
                EnergyCaps.RECEIVER,
                ModBlockEntities.FLOODLIGHT,
                (be, side) -> side == be.inputDirection() ? be : null);
    }
}
