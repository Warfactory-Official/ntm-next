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

import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.ItemBoxMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.util.RegistryUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LeadBoxItem extends Item {
    public static final ItemBoxMenu.Layout LAYOUT =
            new ItemBoxMenu.Layout(
                    4,
                    5,
                    43,
                    18,
                    8,
                    104,
                    162,
                    176,
                    186,
                    6,
                    0x404040,
                    92,
                    RegistryUtil.id("textures/gui/gui_containment.png"));

    public LeadBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new ItemBoxMenu(
                                            ModMenus.CONTAINMENT_BOX.get(),
                                            id,
                                            inv,
                                            LAYOUT,
                                            new ItemStackContainer(opener, stack, LAYOUT.slots())),
                            stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
