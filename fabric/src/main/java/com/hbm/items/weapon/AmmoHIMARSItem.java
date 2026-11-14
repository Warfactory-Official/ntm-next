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

package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AmmoHIMARSItem extends Item {

    public final HIMARSRocketType type;

    public AmmoHIMARSItem(Properties properties, HIMARSRocketType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String desc = "item.hbm.ammo_himars." + type.name().toLowerCase(Locale.ROOT) + ".desc.";
        switch (type) {
            case SMALL -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case SMALL_WP -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
                line(adder, desc, 3, ChatFormatting.BLUE);
            }
            case SMALL_HE, SMALL_TB, LARGE, LARGE_TB -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case SMALL_MINI_NUKE, SMALL_LAVA -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
        }
    }

    private static void line(
            Consumer<Component> adder, String desc, int index, ChatFormatting color) {
        adder.accept(Component.translatable(desc + index).withStyle(color));
    }

    public enum HIMARSRocketType {
        SMALL(
                "standard",
                "himars_standard",
                0,
                (rocket, hit) -> {
                    ProjectileExplosion.explode(rocket, hit, 20F, 3F, null);
                    ExplosionCreator.composeEffectStandard(
                            rocket.level(),
                            hit.getLocation().x,
                            hit.getLocation().y,
                            hit.getLocation().z);
                }),
        LARGE(
                "single",
                "himars_single",
                1,
                (rocket, hit) -> {
                    ProjectileExplosion.explode(
                            rocket, hit, 50F, 5F, ProjectileExplosion.CRACKED_SLAG);
                    ExplosionCreator.composeEffectLarge(
                            rocket.level(),
                            hit.getLocation().x,
                            hit.getLocation().y,
                            hit.getLocation().z);
                }),
        SMALL_HE(
                "standard_he",
                "himars_standard_he",
                0,
                (rocket, hit) -> {
                    ProjectileExplosion.explode(
                            rocket, hit, 20F, 3F, ProjectileExplosion.CRACKED_SLAG);
                    ExplosionCreator.composeEffectStandard(
                            rocket.level(),
                            hit.getLocation().x,
                            hit.getLocation().y,
                            hit.getLocation().z);
                }),
        SMALL_WP("standard_wp", "himars_standard_wp", 0, HIMARSRocketType::phosphorus),
        SMALL_TB(
                "standard_tb",
                "himars_standard_tb",
                0,
                (rocket, hit) -> thermobaric(rocket, hit, 20F, 10F, 20F)),
        LARGE_TB(
                "single_tb",
                "himars_single_tb",
                1,
                (rocket, hit) -> thermobaric(rocket, hit, 50F, 12F, 35F)),
        SMALL_MINI_NUKE(
                "standard_mini_nuke",
                "himars_standard_mini_nuke",
                0,
                (rocket, hit) -> {
                    rocket.discard();
                    Vec3 at = ProjectileExplosion.behindImpact(rocket, hit);
                    MiniNukeExplosion.MEDIUM.detonate(rocket.level(), at.x, at.y, at.z);
                }),
        SMALL_LAVA(
                "standard_lava",
                "himars_standard_lava",
                0,
                (rocket, hit) ->
                        ProjectileExplosion.explode(
                                rocket,
                                hit,
                                20F,
                                3F,
                                () -> ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState()));

        public static final HIMARSRocketType[] VALUES = values();

        public final String name;
        public final Identifier texture;
        public final int amount;
        public final int modelType;
        private final BiConsumer<ArtilleryRocketEntity, HitResult> impact;

        HIMARSRocketType(
                String name,
                String texture,
                int type,
                BiConsumer<ArtilleryRocketEntity, HitResult> impact) {
            this.name = name;
            this.texture =
                    Identifier.fromNamespaceAndPath(
                            "hbm", "textures/models/projectiles/" + texture + ".png");
            this.amount = type == 0 ? 6 : 1;
            this.modelType = type;
            this.impact = impact;
        }

        public void onImpact(ArtilleryRocketEntity rocket, HitResult mop) {
            impact.accept(rocket, mop);
        }

        public void onUpdate(ArtilleryRocketEntity rocket) {}

        private static void phosphorus(ArtilleryRocketEntity rocket, HitResult hit) {
            Level level = rocket.level();
            Vec3 at = hit.getLocation();
            playBoom(rocket);
            ProjectileExplosion.explode(rocket, hit, 20F, 3F, null);
            BlastDebris.shrapnel(level, (int) at.x, (int) at.y, (int) at.z, 30);
            if (level instanceof ServerLevel server) {
                ExplosionChaos.igniteAllBlocks(server, (int) at.x, (int) at.y, (int) at.z, 20);
            }

            for (Entity burnt :
                    level.getEntities(rocket, AABB.ofSize(rocket.position(), 60, 60, 60))) {
                burnt.igniteForSeconds(5F);
                if (burnt instanceof LivingEntity living) {
                    living.addEffect(
                            new UncurableEffectInstance(
                                    HbmPotion.phosphorus(), 30 * 20, 0, true, true));
                }
            }

            ExplosionCreator.composeEffectRBMKMush(level, at.x, at.y, at.z, 15F);
        }

        private static void thermobaric(
                ArtilleryRocketEntity rocket,
                HitResult hit,
                float size,
                float rangeMod,
                float mush) {
            Vec3 at = hit.getLocation();
            playBoom(rocket);
            ProjectileExplosion.explode(
                    rocket, hit, size, rangeMod, ProjectileExplosion.CRACKED_SLAG);
            BlastDebris.shrapnel(rocket.level(), (int) at.x, (int) at.y, (int) at.z, 30);
            ExplosionCreator.composeEffectRBMKMush(rocket.level(), at.x, at.y, at.z, mush);
        }
    }

    private static void playBoom(ArtilleryRocketEntity rocket) {
        WorldSounds.play(
                rocket.level(),
                null,
                rocket.getX(),
                rocket.getY(),
                rocket.getZ(),
                ModSounds.WEAPON_EXPLOSION_MEDIUM.get(),
                SoundSource.BLOCKS,
                20.0F,
                0.9F + rocket.level().getRandom().nextFloat() * 0.2F);
    }
}
