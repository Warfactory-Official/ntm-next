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

package com.hbm.items.tool;

import com.hbm.blockentity.turret.TurretBaseArtilleryBlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DesignatorArtyRangeItem extends Item {

    private static final double REACH = 500D;

    public DesignatorArtyRangeItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> out,
            TooltipFlag flag) {
        Long packed = stack.get(ModDataComponents.ARTY_DESIGNATOR_TARGET.get());
        if (packed == null) {
            out.accept(
                    Component.translatable("desc.item.designatorArtyRange.noTurretLinked")
                            .withStyle(ChatFormatting.RED));
        } else {
            BlockPos pos = BlockPos.of(packed);
            out.accept(
                    Component.translatable(
                                    "desc.item.designatorArtyRange.linkedTo",
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos core = MultiblockSurface.coreOfAny(level, context.getClickedPos());
        if (core == null) core = context.getClickedPos();

        if (!(level.getBlockEntity(core) instanceof TurretBaseArtilleryBlockEntity))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        context.getItemInHand().set(ModDataComponents.ARTY_DESIGNATOR_TARGET.get(), core.asLong());
        Player player = context.getPlayer();
        if (player != null) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Long packed = stack.get(ModDataComponents.ARTY_DESIGNATOR_TARGET.get());
        if (packed == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        HitResult hit = player.pick(REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos aim = ((BlockHitResult) hit).getBlockPos();

        BlockEntity be = level.getBlockEntity(BlockPos.of(packed));
        if (be instanceof TurretBaseArtilleryBlockEntity arty) {
            arty.enqueueTarget(aim.getX() + 0.5D, aim.getY() + 0.5D, aim.getZ() + 0.5D);
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }

        return InteractionResult.CONSUME;
    }
}
