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

import com.hbm.client.ClientPlayerAccess;
import com.hbm.client.HeldItemAnimations;
import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.ToolTypeProvider;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.AnimatedItem;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.render.anim.AnimationEnums;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BoltgunItem extends Item implements ToolTypeProvider, AnimatedItem {

    private static final float MELEE_DAMAGE = 10F;

    public BoltgunItem(Properties props) {
        super(props);
    }

    @Override
    public ToolType getToolType() {
        return ToolType.BOLT;
    }

    private static List<Item> boltTypes() {
        return List.of(
                ModItems.BOLT_SPIKE.get(),
                Autogen.require(MaterialShape.BOLT, Materials.STEEL),
                Autogen.require(MaterialShape.BOLT, Materials.TUNGSTEN),
                Autogen.require(MaterialShape.BOLT, Materials.DURA_STEEL));
    }

    public static boolean handleAttack(Player player, Entity entity) {
        if (!(player.getMainHandItem().getItem() instanceof BoltgunItem)) return false;
        Level level = player.level();
        if (!entity.isAlive()) return false;

        for (Item bolt : boltTypes()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot.isEmpty() || slot.getItem() != bolt) continue;

                if (level instanceof ServerLevel server) {
                    EntitySounds.play(
                            entity,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ModSounds.BOLTGUN.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);
                    player.getInventory().removeItem(i, 1);
                    entity.hurtServer(
                            server,
                            ModifiedDamageSource.bypassingCooldown(boltDamage(level, player)),
                            MELEE_DAMAGE);

                    burst(
                            level,
                            entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2F,
                            entity.getZ());
                    playSwing(player);
                }
                return true;
            }
        }
        return false;
    }

    private static DamageSource boltDamage(Level level, Player player) {
        Holder<DamageType> type =
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.BOLT);
        return new DamageSource(type, player);
    }

    private static void burst(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void playSwing(@Nullable Player player) {
        if (player instanceof ServerPlayer sp)
            AnimatedItem.cue(sp, AnimationEnums.ToolAnimation.SWING);
    }

    public static void clientHeldTick() {
        Player player = ClientPlayerAccess.player();
        if (player == null
                || !ClientPlayerAccess.firstPersonCamera()
                || !(player.getMainHandItem().getItem() instanceof BoltgunItem)) return;
        if (HeldItemAnimations.getChannelValue("RECOIL", 0) != 0)
            ((LivingEntity) player).swingState.currentSwing = null;
    }

    public static BusAnimation animation(AnimationEnums.ToolAnimation type) {
        if (type != AnimationEnums.ToolAnimation.SWING) return null;
        return new BusAnimation()
                .addBus("RECOIL", new AnimationSequence().to(1, 0, 1, 50).to(0, 0, 1, 100));
    }

    @Override
    public @Nullable BusAnimation animation(AnimationEnums.ToolAnimation type, ItemStack stack) {
        return animation(type);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ToolInteractions.useOn(ctx, getToolType())) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        if (!level.isClientSide()) {
            WorldSounds.play(
                    level, null, pos, ModSounds.BOLTGUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Direction side = ctx.getClickedFace();
            double off = 0.25D;
            Vec3 hit = ctx.getClickLocation();
            burst(
                    level,
                    hit.x + side.getStepX() * off,
                    hit.y + side.getStepY() * off,
                    hit.z + side.getStepZ() * off);
            playSwing(ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
