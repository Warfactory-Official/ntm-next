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

import com.hbm.handler.ability.AvailableAbilities;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class SwordAbilityItem extends Item {

    private final AvailableAbilities abilities;

    public SwordAbilityItem(Properties properties, AvailableAbilities abilities) {
        super(properties);
        this.abilities = abilities;
    }

    public static Properties swordProperties(
            ToolTier tier,
            float attackDamage,
            double movement,
            @Nullable TagKey<Item> repairItems) {
        var blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        Properties properties = new Properties().stacksTo(1);
        if (tier.enchantmentValue() > 0) properties.enchantable(tier.enchantmentValue());
        if (tier.durability() > 0) properties.durability(tier.durability());
        if (repairItems != null) properties.repairable(repairItems);

        return properties
                .component(
                        DataComponents.TOOL,
                        new Tool(
                                List.of(
                                        Tool.Rule.minesAndDrops(
                                                HolderSet.direct(
                                                        Blocks.COBWEB.builtInRegistryHolder()),
                                                15.0F),
                                        Tool.Rule.overrideSpeed(
                                                blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES),
                                                Float.MAX_VALUE),
                                        Tool.Rule.overrideSpeed(
                                                blocks.getOrThrow(BlockTags.SWORD_EFFICIENT),
                                                1.5F)),
                                1.0F,
                                2,
                                false))
                .attributes(ToolAbilityItem.attributes(attackDamage, -2.4F, movement))
                .component(DataComponents.WEAPON, new Weapon(1));
    }

    public AvailableAbilities abilities() {
        return abilities;
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (!canOperate(stack)) return;

        var hits = abilities.hits();
        for (int i = 0, n = hits.size(); i < n; i++) {
            var hit = hits.get(i);
            hit.ability().hit(level, hit.maxLevel(), player, victim);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        abilities.appendTooltip(adder);
    }
}
