/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.items.special.CustomLoreItem;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MemespoonItem extends CustomLoreItem {
    private static final float CRIT_FALL = 2F, BLAST_FALL = 20F;
    private static final float CRIT_DAMAGE = 50F;
    private static final float BLAST = 15F;
    private static final int PIERCE = 25;

    public MemespoonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (attacker.fallDistance < CRIT_FALL) return;

        EntitySounds.play(
                victim,
                victim.blockPosition(),
                ModSounds.BANG.get(),
                SoundSource.PLAYERS,
                3.0F,
                0.75F);
        victim.hurtServer(level, level.damageSources().playerAttack(player), CRIT_DAMAGE);

        if (attacker.fallDistance < BLAST_FALL || player.hasInfiniteMaterials()) return;
        ComposableExplosion blast =
                new ComposableExplosion(
                        level,
                        victim.getX(),
                        victim.getY() + victim.getBbHeight() / 2D,
                        victim.getZ(),
                        BLAST,
                        attacker);
        blast.entityProcessor(new WeaponExplosionEntityProcessor(1, 150).piercing(PIERCE, 0.5F));
        ExplosionCreator.composeEffectSmall(
                level, victim.getX(), victim.getY() + victim.getBbHeight() / 2D, victim.getZ());
        blast.explode();
    }
}
