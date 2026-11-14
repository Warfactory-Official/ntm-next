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

package com.hbm.handler.pollution;

import com.hbm.data.RadiationData;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.hazard.Exposure;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.RegistryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class PollutionEffects {

    private static final AttributeModifier SOOT_HEALTH =
            new AttributeModifier(
                    RegistryUtil.id("soot_anger_health"),
                    1D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier SOOT_DAMAGE =
            new AttributeModifier(
                    RegistryUtil.id("soot_anger_damage"),
                    1.5D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final int LEAD_TICKS = 100;

    private PollutionEffects() {}

    public static void strengthenSpawn(Mob mob) {
        if (!(mob instanceof Enemy)
                || mob instanceof GlyphidEntity
                || !RadiationData.ENABLE_POLLUTION.get()
                || !(mob.level() instanceof ServerLevel level)) return;
        var field = PollutionHandler.existing(level);
        if (field == null) return;
        int slot = field.slot(PollutionSavedData.region(mob.getBlockX(), mob.getBlockZ()));
        if (slot < 0
                || field.value(slot, PollutionType.SOOT.ordinal())
                        <= RadiationData.BUFF_MOB_THRESHOLD.get()) return;
        addOnce(mob.getAttribute(Attributes.MAX_HEALTH), SOOT_HEALTH);
        addOnce(mob.getAttribute(Attributes.ATTACK_DAMAGE), SOOT_DAMAGE);
        mob.heal(mob.getMaxHealth());
    }

    public static void onBlockBroken(Player player, BlockPos pos) {
        if (!RadiationData.ENABLE_LEAD_FROM_BLOCKS.get()
                || ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, Exposure.PARTICLE_FINE))
            return;
        float metal = PollutionHandler.read(player.level(), pos, PollutionType.HEAVYMETAL);
        if (metal < 5F) return;
        int amplifier = metal < 10F ? 0 : metal < 25F ? 1 : 2;
        player.addEffect(new MobEffectInstance(HbmPotion.lead(), LEAD_TICKS, amplifier));
    }

    private static void addOnce(@Nullable AttributeInstance attribute, AttributeModifier modifier) {
        if (attribute != null && !attribute.hasModifier(modifier.id()))
            attribute.addPermanentModifier(modifier);
    }
}
