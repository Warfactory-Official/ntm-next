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

import com.hbm.entity.projectile.RubbleEntity;
import com.hbm.items.special.CustomLoreItem;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class WeaponSpecialItem extends CustomLoreItem {
    private static final float CABER_BLAST = 7.5F;
    private static final int CABER_COST = 505;
    private static final float LOUD = 3.0F;

    private final Effect effect;

    public WeaponSpecialItem(Properties properties, Effect effect) {
        super(properties);
        this.effect = effect;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level)) return;

        switch (effect) {
            case INSTANT_KILL ->
                    victim.hurtServer(
                            level,
                            level.damageSources().source(ModDamageTypes.HEALTH_DRAIN, attacker),
                            Float.MAX_VALUE);
            case HALVE ->
                    victim.hurtServer(
                            level,
                            level.damageSources().source(ModDamageTypes.HEALTH_DRAIN, attacker),
                            victim.getHealth() / 2.0F);
            case LAUNCH ->
                    victim.setDeltaMovement(
                            victim.getDeltaMovement().add(attacker.getLookAngle().scale(5)));
            case CABER -> {
                level.explode(
                        null,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        CABER_BLAST,
                        Level.ExplosionInteraction.BLOCK);
                stack.hurtAndBreak(CABER_COST, attacker, EquipmentSlot.MAINHAND);
            }
            case SOUND_ONLY, NONE -> {}
        }
        SoundEvent sound = effect.sound();
        if (sound != null) {
            EntitySounds.play(
                    victim,
                    victim.blockPosition(),
                    sound,
                    SoundSource.PLAYERS,
                    effect.volume,
                    1.0F);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (effect != Effect.LAUNCH) return super.useOn(context);

        Player player = context.getPlayer();
        if (player != null && context.getLevel() instanceof ServerLevel level) {
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000F) {
                RubbleEntity rubble =
                        new RubbleEntity(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
                rubble.setBlockState(state);
                rubble.setDeltaMovement(player.getLookAngle().scale(5));
                WorldSounds.play(
                        level,
                        null,
                        rubble.getX(),
                        rubble.getY(),
                        rubble.getZ(),
                        ModSounds.BANG.get(),
                        SoundSource.PLAYERS,
                        3.0F,
                        1.0F);
                level.addFreshEntity(rubble);
                level.destroyBlock(pos, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public enum Effect {
        INSTANT_KILL(() -> ModSounds.BONK.get(), LOUD),
        HALVE(() -> ModSounds.SLICE.get(), LOUD),
        LAUNCH(() -> ModSounds.BANG.get(), LOUD),
        CABER(null, 0F),
        SOUND_ONLY(() -> ModSounds.STOP.get(), 1.0F),
        NONE(null, 0F);

        private final @Nullable Supplier<SoundEvent> sound;
        private final float volume;

        Effect(@Nullable Supplier<SoundEvent> sound, float volume) {
            this.sound = sound;
            this.volume = volume;
        }

        @Nullable SoundEvent sound() {
            return sound == null ? null : sound.get();
        }
    }
}
