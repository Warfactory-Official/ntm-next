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

package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.items.weapon.sedna.factory.XFactoryPA;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.EntityTargeting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public abstract class ArmorPAMeleeBase implements PowerArmorMeleeWeapon {
    protected static final float LIGHT_DAMAGE = 15F;
    protected static final float HEAVY_DAMAGE = 35F;
    protected static final double REACH = 3.0D;
    protected static final double THRESHOLD = 0.5D;

    protected abstract boolean isLightHit(int timer);

    protected abstract boolean isHeavyHit(int timer);

    protected abstract SoundEvent hitSound();

    protected boolean refiresOnHold() {
        return false;
    }

    protected boolean gibsOnKill(boolean light, LivingEntity victim) {
        return true;
    }

    protected abstract int lightCooldown();

    protected abstract int heavyCooldown();

    @Override
    public void primary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, lightCooldown());
    }

    @Override
    public void secondary(ItemStack stack, LambdaContext ctx) {
        XFactoryPA.doSwing(stack, ctx, GunAnimation.ALT_CYCLE, heavyCooldown());
    }

    @Override
    public void cuePass(ItemStack stack, LambdaContext ctx) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide()) return;
        GunAnimation type = GunBaseNTItem.getLastAnim(stack, ctx.configIndex());
        int timer = GunBaseNTItem.getAnimTimer(stack, ctx.configIndex());

        if (refiresOnHold()
                && type == GunAnimation.CYCLE
                && timer == lightCooldown()
                && GunBaseNTItem.getPrimary(stack, 0)) {
            XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, lightCooldown());
        }

        boolean light = type == GunAnimation.CYCLE && isLightHit(timer);
        boolean heavy = type == GunAnimation.ALT_CYCLE && isHeavyHit(timer);
        Player player = ctx.getPlayer();
        if ((!light && !heavy) || player == null) return;

        HitResult mop = EntityTargeting.getMouseOver(player, REACH, THRESHOLD);
        if (mop == null) return;

        if (mop instanceof EntityHitResult entityHit) {
            hitEntity(player, entity, entityHit.getEntity(), light);
        }
        if (mop instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = entity.level().getBlockState(pos);
            Vec3 at = mop.getLocation();
            WorldSounds.play(
                    entity.level(),
                    null,
                    at.x,
                    at.y,
                    at.z,
                    state.getSoundType().getStepSound(),
                    SoundSource.PLAYERS,
                    2F,
                    0.9F + entity.getRandom().nextFloat() * 0.2F);
        }
    }

    private void hitEntity(Player player, LivingEntity source, Entity hit, boolean light) {
        float damage = light ? LIGHT_DAMAGE : HEAVY_DAMAGE;
        double knockback = light ? 0D : 1.5D;
        float dt = light ? 5F : 15F;
        float pierce = light ? 0.1F : 0.25F;

        if (hit instanceof LivingEntity living) {
            if (living.getMaxHealth() >= 100F) damage *= 2.5F;
            living.hurtServer(
                    (ServerLevel) living.level(),
                    ModifiedDamageSource.of(
                            player.damageSources().playerAttack(player),
                            true,
                            knockback,
                            dt,
                            pierce),
                    damage);
            if (gibsOnKill(light, living) && !living.isAlive()) ConfettiUtil.gib(living);
        } else if (hit.level() instanceof ServerLevel server) {
            hit.hurtServer(server, player.damageSources().playerAttack(player), damage);
        }

        EntitySounds.play(
                hit,
                hit.getX(),
                hit.getY(),
                hit.getZ(),
                hitSound(),
                SoundSource.PLAYERS,
                1F,
                0.9F + source.getRandom().nextFloat() * 0.2F);
    }

    protected void renderArms(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            HFRWavefrontObject mesh,
            int leftPart,
            int rightPart) {
        pose.pushPose();
        pose.translate(0F, -1.5F, 0.5F);
        float scale = 0.125F;
        pose.scale(scale, scale, scale);

        armPose(pose, collector, light, type, mesh, leftPart, rightPart);

        pose.popPose();
    }

    protected abstract void armPose(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            HFRWavefrontObject mesh,
            int leftPart,
            int rightPart);

    protected static void submitArm(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            HFRWavefrontObject mesh,
            int part,
            float pivotX,
            float rollZ,
            float rollY) {
        pose.translate(pivotX, 8F, 0F);
        pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(rollZ));
        pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(rollY));
        pose.translate(-pivotX, -8F, 0F);
        collector.submitCustomGeometry(
                pose, type, (p, buffer) -> mesh.renderPart(p, buffer, light, -1, part));
    }
}
