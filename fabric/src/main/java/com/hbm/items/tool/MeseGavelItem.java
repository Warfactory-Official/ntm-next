/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.WeaponAbility;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MeseGavelItem extends SwordAbilityItem {

    public MeseGavelItem(Properties properties) {
        super(
                properties,
                AvailableAbilities.builder()
                        .add(WeaponAbility.PHOSPHORUS_TIP, 0)
                        .add(WeaponAbility.RADIOACTIVE_BLADE, 2)
                        .add(WeaponAbility.STUNNING, 3)
                        .add(WeaponAbility.VAMPIRE, 4)
                        .add(WeaponAbility.DECAPITATOR, 0)
                        .build());
    }

    public static Item.Properties properties() {
        return swordProperties(
                        ToolTier.MESE_GAVEL, 250.0F, 1.5D, ToolAbilityItem.repairTag("plate_paa"))
                .stacksTo(1);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (victim.level() instanceof ServerLevel level) {
            EntitySounds.play(
                    victim,
                    victim.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.HOSTILE,
                    3.0F,
                    1.0F);
        }
        super.hurtEnemy(stack, victim, attacker);
    }
}
