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

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blockentity.machine.oil.RefineryBlockEntity;
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
import com.hbm.items.ModDataComponents;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RefineryBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                ToolInteractionHandler,
                LookOverlayProvider,
                PersistentItemTooltipProvider {

    public static final BooleanProperty EXPLODED = BooleanProperty.create("exploded");

    public RefineryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(EXPLODED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXPLODED);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    public RefineryBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefineryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (state.getValue(EXPLODED)) return InteractionResult.SUCCESS;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        if (!(level.getBlockEntity(core) instanceof RefineryBlockEntity refinery)) {
            super.onExplosionHit(state, level, core, explosion, onHit);
            return;
        }
        if (!refinery.firstHitBy(explosion)) return;
        if (refinery.hasExploded()) {
            level.removeBlock(core, false);
            return;
        }

        refinery.explode(level, core);
        if (explosion.getDirectSourceEntity() instanceof BombletZetaEntity) {
            AwardRegions.within(
                    level,
                    core.getX() + 0.5,
                    core.getY() + 0.5,
                    core.getZ() + 0.5,
                    100,
                    player -> HbmCriteria.detonation(player, DetonationTrigger.Kind.REFINERY));
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
        var contents = stack.get(ModDataComponents.REFINERY_CONTENTS.get());
        if (contents == null) return;

        var tanks = contents.tanks();
        for (int index = 0; index < tanks.size(); index++) {
            int capacity = RefineryBlockEntity.TANK_CAPACITY_OUTPUT;
            if (index == 0) capacity = RefineryBlockEntity.TANK_CAPACITY_INPUT;
            adder.accept(PersistentItemTooltipProvider.tankLine(tanks.get(index), capacity));
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.REFINERY)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe()
                .fluidFaces(
                        RefineryBlockEntity.class,
                        (_, face) -> face.side() != Direction.DOWN);
    }
}
