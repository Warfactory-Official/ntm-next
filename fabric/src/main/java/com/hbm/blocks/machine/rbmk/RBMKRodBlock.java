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

package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RBMKRodBlock extends RBMKBlock implements CapabilityBlock {
    public final boolean moderated;

    public RBMKRodBlock(Properties properties, boolean moderated) {
        super(properties);
        this.moderated = moderated;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        BossSpawnHandler.markFBI(player);
        if (held.getItem() instanceof RBMKRodItem) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            if (core != null
                    && level.getBlockEntity(core) instanceof RBMKRodBlockEntity rod
                    && rod.acceptsLoad(held)) {
                if (!level.isClientSide()) {
                    rod.takeLoad(held.copyWithCount(1));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                level.playSound(
                        player,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        ModSounds.UPGRADE_PLUG.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(held, state, level, pos, player, hand, hit);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RBMK_ROD).itemsAtCells();
    }

    @Override
    protected int topPassive() {
        return PASSIVE_ITEMS;
    }

    @Override
    protected BlockEntityType<? extends RBMKBaseBlockEntity> beType() {
        return ModBlockEntities.RBMK_ROD.get();
    }

    @Override
    protected RBMKBaseBlockEntity createCore(BlockPos pos, BlockState state) {
        return new RBMKRodBlockEntity(pos, state);
    }
}
