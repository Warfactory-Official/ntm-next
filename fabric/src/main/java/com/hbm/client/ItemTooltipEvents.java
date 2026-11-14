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

package com.hbm.client;

import com.hbm.client.gui.ArmorTableScreen;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.config.HudConfig;
import com.hbm.explosion.assembly.ChargeIngredients;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.resistance.DamageResistanceHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.UpgradeTooltipMenu;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.TooltipStyle;
import com.hbm.wiaj.CanneryClient;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

public final class ItemTooltipEvents {

    private ItemTooltipEvents() {}

    public static void append(
            ItemStack stack, @Nullable Player player, TooltipFlag flag, List<Component> tooltip) {
        RadiationResistanceTooltip.append(stack, tooltip);
        int from = tooltip.size();
        if (stack.getItem() instanceof MachineUpgradeItem upgrade
                && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getMenu() instanceof UpgradeTooltipMenu menu) {
            menu.appendUpgradeTooltip(upgrade, tooltip);
        }
        boolean shift = ModifierKeys.detailsHeld();
        DamageResistanceHandler.appendTooltip(stack, tooltip::add);
        ArmorRegistry.addProtectionTooltip(stack, shift, tooltip::add);

        double rad = ((int) (HazmatRegistry.getResistance(stack) * 1000)) / 1000D;
        if (rad > 0)
            tooltip.add(
                    Component.translatable("trait.radResistance", rad)
                            .withStyle(ChatFormatting.YELLOW));

        ArmorModHandler.addInstalledModTooltip(
                stack,
                tooltip,
                shift || Minecraft.getInstance().gui.screen() instanceof ArmorTableScreen);
        HazardSystem.addHazardInfo(stack, player, tooltip, flag);

        ChargeIngredients.Contribution nuke =
                HudConfig.tooltipCustomNuke ? ChargeIngredients.lookup(stack) : null;
        if (nuke != null) {
            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable(
                                    (nuke.mode() == ChargeIngredients.Mode.MULTIPLY)
                                            ? "desc.item.customNuke.multiplier"
                                            : "desc.item.customNuke.adds",
                                    String.valueOf(nuke.value()),
                                    nuke.channel().displayLabel())
                            .withStyle(ChatFormatting.GOLD));
        }

        for (int i = from; i < tooltip.size(); i++) {
            tooltip.set(i, TooltipStyle.defaultColor(tooltip.get(i), ChatFormatting.GRAY));
        }
        QMAWClient.tooltip(stack, tooltip);
        CanneryClient.tooltip(stack, tooltip);
    }
}
