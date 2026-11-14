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

package com.hbm.items.machine.upgrade;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class MachineUpgradeItem extends Item {

    public final UpgradeType type;
    public final int tier;
    private final Component[] description;

    public MachineUpgradeItem(Properties props, UpgradeType type, int tier) {
        super(props);
        this.type = type;
        this.tier = tier;
        this.description = null;
    }

    public MachineUpgradeItem(Properties props, Component... description) {
        super(props);
        this.type = UpgradeType.SPECIAL;
        this.tier = 0;
        this.description = description;
    }

    public static boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof MachineUpgradeItem;
    }

    public static int getLevel(ItemStack stack, UpgradeType type) {
        return stack.getItem() instanceof MachineUpgradeItem upgrade && upgrade.type == type
                ? upgrade.tier
                : 0;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (description != null) for (Component line : description) tooltip.accept(line);
    }
}
