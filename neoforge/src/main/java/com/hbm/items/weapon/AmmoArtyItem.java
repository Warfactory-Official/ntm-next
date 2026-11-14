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

package com.hbm.items.weapon;

import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.MistEntity;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.entity.projectile.ArtilleryShellEntity;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AmmoArtyItem extends Item {

    private static final Casing.Template SIXTEEN_INCH_CASE =
            Casing.template(CasingShape.STRAIGHT)
                    .scale(15F, 15F, 10F)
                    .lifetime(300)
                    .bounce(1F, 0.5F);

    public final ArtilleryShellType type;

    public AmmoArtyItem(Properties properties, ArtilleryShellType type) {
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
        String desc = "item.hbm.ammo_arty." + type.name().toLowerCase(Locale.ROOT) + ".desc.";
        switch (type) {
            case NORMAL -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case CLASSIC -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case EXPLOSIVE -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case PHOSPHORUS -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
                line(adder, desc, 3, ChatFormatting.BLUE);
            }
            case PHOSPHORUS_MULTI -> line(adder, desc, 0, ChatFormatting.RED);
            case MINI_NUKE -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case MINI_NUKE_MULTI -> line(adder, desc, 0, ChatFormatting.RED);
            case NUKE -> {
                line(adder, desc, 0, ChatFormatting.RED);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case CARGO -> {
                var cargo = stack.get(ModDataComponents.ARTY_CARGO.get());
                if (cargo != null) {
                    adder.accept(
                            cargo.create().getHoverName().copy().withStyle(ChatFormatting.YELLOW));
                } else {
                    line(adder, desc, 0, ChatFormatting.RED);
                }
            }
            default -> {}
        }
    }

    private static void line(
            Consumer<Component> adder, String desc, int index, ChatFormatting color) {
        adder.accept(Component.translatable(desc + index).withStyle(color));
    }

    public enum ArtilleryShellType {
        NORMAL("ammo_arty", Casing.INCH_16) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                ProjectileExplosion.explode(shell, mop, 10F, 3F, null);
                ExplosionCreator.composeEffectSmall(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        CLASSIC("ammo_arty_classic", Casing.INCH_16) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                ProjectileExplosion.explode(shell, mop, 15F, 5F, null);
                ExplosionCreator.composeEffectStandard(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        EXPLOSIVE("ammo_arty_he", Casing.INCH_16) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                ProjectileExplosion.explode(shell, mop, 15F, 3F, ProjectileExplosion.CRACKED_SLAG);
                ExplosionCreator.composeEffectStandard(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },

        MINI_NUKE("ammo_arty_mini_nuke", Casing.INCH_16_NUCLEAR) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                shell.discard();
                Vec3 impact = ProjectileExplosion.behindImpact(shell, mop);
                MiniNukeExplosion.MEDIUM.detonate(shell.level(), impact.x, impact.y, impact.z);
            }
        },

        NUKE("ammo_arty_nuke", Casing.INCH_16_NUCLEAR) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                Vec3 hit = mop.getLocation();
                shell.level()
                        .addFreshEntity(
                                NukeExplosionMK5Entity.statFac(
                                        shell.level(),
                                        ExplosionData.MISSILE_RADIUS.get(),
                                        hit.x,
                                        hit.y,
                                        hit.z));
                NukeTorexEntity.statFac(
                        shell.level(), hit.x, hit.y, hit.z, ExplosionData.MISSILE_RADIUS.get());
                shell.discard();
            }
        },

        PHOSPHORUS("ammo_arty_phosphorus", Casing.INCH_16_PHOSPHORUS) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {

                Level level = shell.level();
                Vec3 hit = mop.getLocation();

                WorldSounds.play(
                        level,
                        null,
                        shell.getX(),
                        shell.getY(),
                        shell.getZ(),
                        ModSounds.WEAPON_EXPLOSION_MEDIUM.get(),
                        SoundSource.BLOCKS,
                        20.0F,
                        0.9F + level.getRandom().nextFloat() * 0.2F);
                ProjectileExplosion.explode(shell, mop, 10F, 3F, null);
                BlastDebris.shrapnel(level, hit.x, hit.y, hit.z, 15);
                if (level instanceof ServerLevel server) {
                    ExplosionChaos.igniteAllBlocks(
                            server, (int) hit.x, (int) hit.y, (int) hit.z, 12);
                }

                int radius = 15;
                List<Entity> hits =
                        level.getEntities(
                                shell,
                                new AABB(
                                        shell.getX() - radius,
                                        shell.getY() - radius,
                                        shell.getZ() - radius,
                                        shell.getX() + radius,
                                        shell.getY() + radius,
                                        shell.getZ() + radius));
                for (Entity e : hits) {
                    e.igniteForSeconds(5F);
                    if (e instanceof LivingEntity living) {
                        living.addEffect(
                                new UncurableEffectInstance(
                                        HbmPotion.phosphorus(), 30 * 20, 0, true, true));
                    }
                }

                ExplosionCreator.composeEffectRBMKMush(level, hit.x, hit.y, hit.z, 10F);
            }
        },

        MINI_NUKE_MULTI("ammo_arty_mini_nuke_multi", Casing.INCH_16_NUCLEAR) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                MINI_NUKE.onImpact(shell, mop);
            }

            @Override
            public void onUpdate(ArtilleryShellEntity shell) {
                standardCluster(shell, MINI_NUKE, 5, 300, 5);
            }
        },
        PHOSPHORUS_MULTI("ammo_arty_phosphorus_multi", Casing.INCH_16_PHOSPHORUS) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                PHOSPHORUS.onImpact(shell, mop);
            }

            @Override
            public void onUpdate(ArtilleryShellEntity shell) {
                standardCluster(shell, PHOSPHORUS, 10, 300, 5);
            }
        },

        CARGO("ammo_arty_cargo", Casing.INCH_16) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                if (mop instanceof BlockHitResult block) {
                    shell.setPos(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z);
                    shell.getStuck(block.getBlockPos(), block.getDirection().ordinal());
                }
            }
        },

        CHLORINE("ammo_arty_chlorine", Casing.INCH_16) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                shell.discard();
                Vec3 impact = ProjectileExplosion.behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                MistEntity mist = new MistEntity(shell.level());
                mist.setType(NTMFluids.CHLORINE);
                mist.setPos(impact.x, impact.y - 3, impact.z);
                mist.setArea(15, 7.5F);
                shell.level().addFreshEntity(mist);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 5F);
            }
        },
        PHOSGENE("ammo_arty_phosgene", Casing.INCH_16_NUCLEAR) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                shell.discard();
                Vec3 impact = ProjectileExplosion.behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                gasCloud(shell, impact, NTMFluids.PHOSGENE, 3, 15D, 15, 10);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 10F);
                pollute(shell, mop, PollutionType.POISON, 15F);
            }
        },
        MUSTARD("ammo_arty_mustard_gas", Casing.INCH_16_NUCLEAR) {
            @Override
            public void onImpact(ArtilleryShellEntity shell, HitResult mop) {
                shell.discard();
                Vec3 impact = ProjectileExplosion.behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                gasCloud(shell, impact, NTMFluids.MUSTARDGAS, 5, 25D, 20, 10);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 15F);
                pollute(shell, mop, PollutionType.POISON, 30F);
            }
        };

        public static final ArtilleryShellType[] VALUES = values();

        public final String name;
        public final Casing casing;

        ArtilleryShellType(String name, int casingColor) {
            this.name = name;
            this.casing = SIXTEEN_INCH_CASE.copy().tints(casingColor).register(name);
        }

        public abstract void onImpact(ArtilleryShellEntity shell, HitResult mop);

        public void onUpdate(ArtilleryShellEntity shell) {}
    }

    private static void pollute(
            ArtilleryShellEntity shell, HitResult mop, PollutionType type, float amount) {
        PollutionHandler.add(shell.level(), BlockPos.containing(mop.getLocation()), type, amount);
    }

    private static void gasCloud(
            ArtilleryShellEntity shell,
            Vec3 impact,
            Fluid fluid,
            int count,
            double spread,
            float width,
            float height) {
        for (int i = 0; i < count; i++) {
            MistEntity mist = new MistEntity(shell.level());
            mist.setType(fluid);
            double x = impact.x;
            double z = impact.z;
            if (i > 0) {
                x += shell.getRandom().nextGaussian() * spread;
                z += shell.getRandom().nextGaussian() * spread;
            }
            mist.setPos(x, impact.y - 5, z);
            mist.setArea(width, height);
            shell.level().addFreshEntity(mist);
        }
    }

    public static void standardCluster(
            ArtilleryShellEntity shell,
            ArtilleryShellType clusterType,
            int amount,
            double splitHeight,
            double deviation) {

        if (!shell.getWhistle() || shell.getDeltaMovement().y > 0) return;
        if (shell.getTargetHeight() + splitHeight < shell.getY()) return;

        shell.discard();

        for (int i = 0; i < amount; i++) {
            ArtilleryShellEntity cluster =
                    new ArtilleryShellEntity(ModEntities.ARTILLERY_SHELL.get(), shell.level());
            cluster.setType(clusterType.ordinal());
            Vec3 motion = shell.getDeltaMovement();
            cluster.setDeltaMovement(
                    i == 0 ? motion.x : motion.x + shell.getRandom().nextGaussian() * deviation,
                    motion.y,
                    i == 0 ? motion.z : motion.z + shell.getRandom().nextGaussian() * deviation);
            cluster.snapTo(
                    shell.getX(), shell.getY(), shell.getZ(), shell.getYRot(), shell.getXRot());
            double[] target = shell.getTarget();
            cluster.setTarget(target[0], target[1], target[2]);
            cluster.setWhistle(shell.getWhistle() && !shell.didWhistle());
            shell.level().addFreshEntity(cluster);
        }
    }
}
