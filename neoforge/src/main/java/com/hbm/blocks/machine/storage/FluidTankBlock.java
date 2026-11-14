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

package com.hbm.blocks.machine.storage;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blockentity.machine.storage.FluidTankBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.client.RepairLookOverlay;
import com.hbm.entity.projectile.BombletZetaEntity;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.RepairHandler;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FluidTankBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                ToolInteractionHandler,
                LookOverlayProvider,
                PersistentItemTooltipProvider {

    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    public FluidTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DAMAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DAMAGED);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean cellsHaveAnalogOutput() {
        return true;
    }

    @Override
    protected boolean comparatorCellAt(int localX, int localY, int localZ) {
        return localY == 0 && Math.abs(localX) == 1 && Math.abs(localZ) == 1;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(DAMAGED)) return InteractionResult.PASS;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (state.getValue(DAMAGED)) return InteractionResult.PASS;
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var identifier =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        var fluid = identifier.primary();
        if (fluid == null || fluid == Fluids.EMPTY) return InteractionResult.PASS;

        var tank = (FluidTankBlockEntity) level.getBlockEntity(core);
        tank.tank.setTankTypeByIdentifier(fluid);
        tank.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                                "desc.fluidtank.changed_type",
                                NTMFluidProperties.getDisplayName(fluid))
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        var tank = (FluidTankBlockEntity) level.getBlockEntity(core);
        if (tank.lastHitBy == explosion) return;
        tank.lastHitBy = explosion;
        if (tank.isDamaged()) {
            level.removeBlock(core, false);
            return;
        }

        tank.explode();
        if (explosion.getDirectSourceEntity() instanceof BombletZetaEntity
                && NTMFluidProperties.hasTrait(tank.tank.getTankType(), FlammableTrait.class)) {
            AwardRegions.within(
                    level,
                    core.getX() + 0.5,
                    core.getY() + 0.5,
                    core.getZ() + 0.5,
                    100,
                    player -> HbmCriteria.detonation(player, DetonationTrigger.Kind.FLUID_TANK));
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.TORCH || player == null || player.isSpectator()) return false;
        return RepairHandler.tryRepair(level, pos, player);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        RepairLookOverlay.build(level, pos, info);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        var content = stack.get(ModDataComponents.FLUID_TANK_CONTENTS.get());
        if (content == null) return;
        adder.accept(PersistentItemTooltipProvider.tankLine(content.tank(), content.capacity()));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FLUID_TANK).fluidIn().fluidOut().items();
    }
}
