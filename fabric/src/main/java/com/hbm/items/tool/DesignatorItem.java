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

import com.hbm.api.item.TargetDesignator;
import com.hbm.blocks.bomb.LaunchPadBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
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

public class DesignatorItem extends Item implements TargetDesignator {

    public DesignatorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof LaunchPadBlock)
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        context.getItemInHand()
                .set(
                        ModDataComponents.TARGET_DESIGNATOR.get(),
                        TargetDesignator.pack(pos.getX(), pos.getZ()));
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
            player.sendSystemMessage(Component.translatable("desc.item.designator.positionSet"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (isReady(stack)) {
            adder.accept(Component.translatable("item.hbm.designator.target_coord"));
            adder.accept(Component.literal("X: " + getTargetX(stack)));
            adder.accept(Component.literal("Z: " + getTargetZ(stack)));
        } else {
            adder.accept(Component.translatable("item.hbm.designator.choose_target"));
        }
    }
}
