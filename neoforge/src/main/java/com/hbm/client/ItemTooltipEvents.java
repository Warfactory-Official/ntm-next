// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.gui.ScreenArmorTable;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.config.HudConfig;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.tileentity.bomb.CustomNukeEntries;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.TooltipStyle;
import com.hbm.wiaj.CanneryClient;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

public final class ItemTooltipEvents {

    private ItemTooltipEvents() {}

    public static void append(
            ItemStack stack, @Nullable Player player, TooltipFlag flag, List<Component> tooltip) {
        int from = tooltip.size();
        boolean shift = ModifierKeys.leftShiftHeld();
        DamageResistanceHandler.addInfo(stack, tooltip::add);
        ArmorRegistry.addProtectionTooltip(stack, shift, tooltip::add);

        double rad = ((int) (HazmatRegistry.getResistance(stack) * 1000)) / 1000D;
        if (rad > 0)
            tooltip.add(
                    Component.translatable("trait.radResistance", rad)
                            .withStyle(ChatFormatting.YELLOW));

        ArmorModHandler.addInstalledModTooltip(
                stack,
                tooltip,
                shift || Minecraft.getInstance().gui.screen() instanceof ScreenArmorTable);
        HazardSystem.addHazardInfo(stack, player, tooltip, flag);

        if (flag.isAdvanced() && HudConfig.tooltipOreDict) {
            List<String> tags =
                    stack.typeHolder()
                            .tags()
                            .map(tag -> tag.location().toString())
                            .sorted()
                            .toList();
            if (!tags.isEmpty()) {
                tooltip.add(
                        Component.translatable("desc.item.oreDict").withStyle(ChatFormatting.BLUE));
                for (String tag : tags)
                    tooltip.add(Component.literal(" -" + tag).withStyle(ChatFormatting.AQUA));
            }
        }

        CustomNukeEntries.Entry nuke =
                HudConfig.tooltipCustomNuke ? CustomNukeEntries.of(stack) : null;
        if (nuke != null) {
            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable(
                                    nuke.multiplier()
                                            ? "desc.item.customNuke.multiplier"
                                            : "desc.item.customNuke.adds",
                                    String.valueOf(nuke.value()),
                                    nuke.stage().name())
                            .withStyle(ChatFormatting.GOLD));
        }

        for (int i = from; i < tooltip.size(); i++) {
            tooltip.set(i, TooltipStyle.defaultColor(tooltip.get(i), ChatFormatting.GRAY));
        }
        QMAWClient.tooltip(stack, tooltip);
        CanneryClient.tooltip(stack, tooltip);
    }
}
