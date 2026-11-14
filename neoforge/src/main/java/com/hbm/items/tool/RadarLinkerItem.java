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

import com.hbm.api.block.RadarCommandHandler;
import com.hbm.blockentity.machine.RadarScreenBlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class RadarLinkerItem extends Item {

    public RadarLinkerItem(Properties props) {
        super(props);
    }

    public static @Nullable BlockPos getPosition(ItemStack stack) {
        Long packed = stack.get(ModDataComponents.RADAR_LINKER_TARGET.get());
        return packed == null ? null : BlockPos.of(packed);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof RadarCommandHandler) && !(be instanceof RadarScreenBlockEntity)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ctx.getItemInHand().set(ModDataComponents.RADAR_LINKER_TARGET.get(), target.asLong());
            Player player = ctx.getPlayer();
            if (player != null) {
                EntitySounds.play(
                        player,
                        player.blockPosition(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        BlockPos target = getPosition(stack);
        if (target == null) {
            adder.accept(
                    Component.translatable("desc.item.radarLinker.noPositionSet")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        adder.accept(Component.literal("X: " + target.getX()));
        adder.accept(Component.literal("Y: " + target.getY()));
        adder.accept(Component.literal("Z: " + target.getZ()));
    }
}
