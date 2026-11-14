/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FloodlightBlock extends Block
        implements TickingEntityBlock, ToolInteractionHandler, CapabilityBlock {

    public static final IntegerProperty FACING = IntegerProperty.create("facing", 0, 7);

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, 1));
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
        return new FloodlightBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().get3DDataValue());
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer != null) {
            setAngle(level, pos, placer, true);
        }
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || player == null || player.isSpectator()) {
            return false;
        }
        if (!level.isClientSide()) {
            setAngle(level, pos, player, false);
        }
        return true;
    }

    private static void setAngle(
            Level level, BlockPos pos, LivingEntity actor, boolean updateMeta) {
        int quadrant = Mth.floor(actor.getYRot() * 4F / 360F + 0.5D) & 3;
        float pitch = actor.getXRot();
        if (!(level.getBlockEntity(pos) instanceof FloodlightBlockEntity floodlight)) {
            return;
        }
        var state = level.getBlockState(pos);
        int mount = state.getValue(FACING) % 6;
        var updatedState = state;
        if (updateMeta && (mount == 0 || mount == 1) && (quadrant == 0 || quadrant == 2)) {
            updatedState = state.setValue(FACING, mount + 6);
        }
        if ((mount == 1 && (quadrant == 0 || quadrant == 1))
                || (mount == 0 && (quadrant == 0 || quadrant == 3))) {
            pitch = 180F - pitch;
        }
        floodlight.hbm$syncSet$FloodlightBlockEntity$rotation(-Math.round(pitch / 5F) * 5F);
        if (floodlight.isOn) {
            floodlight.destroyLights();
        }
        floodlight.setChanged();
        if (updatedState != state) {
            level.setBlock(pos, updatedState, UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        int facing = state.getValue(FACING);
        var direction = Direction.from3DDataValue(facing % 6);
        int rotated = rotation.rotate(direction).get3DDataValue();
        if (facing >= 6) {
            rotated += 6;
        }
        return state.setValue(FACING, rotated);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        var direction = Direction.from3DDataValue(state.getValue(FACING) % 6);
        return state.rotate(mirror.getRotation(direction));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FLOODLIGHT).fe().powerIn().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pending.add(
                new Caps.Registration<>(
                        EnergyCaps.RECEIVER,
                        (Supplier<? extends BlockEntityType<? extends FloodlightBlockEntity>>)
                                ModBlockEntities.FLOODLIGHT,
                        (floodlight, side) ->
                                side == floodlight.inputDirection() ? floodlight : null));
    }
}
