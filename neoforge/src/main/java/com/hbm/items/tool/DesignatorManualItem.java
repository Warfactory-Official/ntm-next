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
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class DesignatorManualItem extends Item implements TargetDesignator {

    public static final int ADD = 0, SUBTRACT = 1, SET_HERE = 2;
    public static final int REFERENCE_X = 0, REFERENCE_Z = 1;

    public DesignatorManualItem(Properties props) {
        super(props);
    }

    public static long apply(
            long target, int operator, int value, int reference, double posX, double posZ) {
        int x = TargetDesignator.unpackX(target);
        int z = TargetDesignator.unpackZ(target);
        if (operator == SET_HERE) {
            if (reference == REFERENCE_X) x = (int) Math.round(posX);
            else z = (int) Math.round(posZ);
        } else {
            int delta = operator == ADD ? value : operator == SUBTRACT ? -value : 0;
            if (reference == REFERENCE_X) x += delta;
            else z += delta;
        }
        return TargetDesignator.pack(x, z);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HeldItemMenu.open(player, hand, ModMenus.DESIGNATOR_MANUAL, Component.empty());
        return InteractionResult.SUCCESS;
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
