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

package com.hbm.blocks.generic;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HEVBatteryBlock extends Block {
    private static final long CHARGE = 150_000L;

    private static final VoxelShape SHAPE = Shapes.box(0.375, 0.0, 0.375, 0.625, 0.375, 0.625);

    public HEVBatteryBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ModArmorItem helmet)
                || !helmet.isPowered()
                || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem()
                        instanceof ModArmorItem plate)
                || !ArmorSuitEffects.hasFullSet(player, plate.suit(), false)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.getItem() instanceof ModArmorItem armor) {
                    armor.setCharge(worn, armor.getCharge(worn) + CHARGE);
                } else if (worn.getItem() instanceof EnergyItem battery) {
                    battery.addCharge(worn, CHARGE);
                }
            }
            level.playSound(
                    null, pos, ModSounds.ITEM_BATTERY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
