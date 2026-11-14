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

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class SingularityItem extends CustomLoreItem implements DroppedItemUpdateHandler {

    private final BiFunction<Level, Float, Entity> factory;
    private final float size;

    public SingularityItem(
            BiFunction<Level, Float, Entity> factory, float size, Properties properties) {
        super(properties);
        this.factory = factory;
        this.size = size;
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_SINGULARITY.get()) {
            Entity well = factory.apply(entity.level(), size);
            well.setPos(entity.getX(), entity.getY(), entity.getZ());
            entity.level().addFreshEntity(well);
        }
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
