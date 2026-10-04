// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.handler.radiation.RadResistanceRule;
import com.hbm.handler.radiation.RadiationSystemNT;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class RadiationResistanceTooltip {

    private static volatile ReferenceOpenHashSet<Block> blocks = new ReferenceOpenHashSet<>(0);

    private RadiationResistanceTooltip() {}

    public static void joined(RegistryAccess registries) {
        ReferenceOpenHashSet<Block> next = new ReferenceOpenHashSet<>(64);
        RadiationSystemNT.forEachResistantState(state -> next.add(state.getBlock()));
        for (RadResistanceRule rule : registries.lookupOrThrow(RadResistanceRule.REGISTRY)) {
            for (Holder<Block> target : rule.targets()) {
                Block block = target.value();
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (!rule.matches(state)) continue;
                    next.add(block);
                    break;
                }
            }
        }
        blocks = next;
    }

    public static void disconnected() {
        blocks = new ReferenceOpenHashSet<>(0);
    }

    public static void append(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof BlockItem item) || !blocks.contains(item.getBlock()))
            return;
        Component line =
                Component.literal("[")
                        .append(Component.translatable("trait.radshield"))
                        .append("]")
                        .withStyle(ChatFormatting.DARK_GREEN);
        int at = tooltip.size();
        for (int i = 1; i < tooltip.size(); i++) {
            if (tooltip.get(i).getContents() instanceof TranslatableContents translated
                    && (translated.getKey().equals("desc.block.blastInfo.blastResistance")
                            || translated.getKey().equals("hbm.desc.nospawn"))) {
                at = i;
                break;
            }
        }
        tooltip.add(at, line);
    }
}
