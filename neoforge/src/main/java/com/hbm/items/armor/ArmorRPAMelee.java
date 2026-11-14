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

import com.hbm.client.HeldItemAnimations;
import com.hbm.items.armor.ArmorPAMeleeBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;

public class ArmorRPAMelee extends ArmorPAMeleeBase {

    @Override
    protected int lightCooldown() {
        return 14;
    }

    @Override
    protected int heavyCooldown() {
        return 20;
    }

    @Override
    protected boolean isLightHit(int timer) {
        return timer == 3 || timer == 9;
    }

    @Override
    protected boolean isHeavyHit(int timer) {
        return timer == 8;
    }

    @Override
    protected boolean refiresOnHold() {
        return true;
    }

    @Override
    protected boolean gibsOnKill(boolean light, LivingEntity victim) {
        return victim.getRandom().nextInt(light ? 10 : 3) == 0;
    }

    @Override
    protected SoundEvent hitSound() {
        return ModSounds.GUN_SMACK.get();
    }

    @Override
    public BusAnimation animation(ItemStack stack, GunAnimation type) {
        if (type == GunAnimation.EQUIP)
            return new BusAnimation()
                    .addBus(
                            "EQUIP",
                            new AnimationSequence()
                                    .jump(-1, 0, 0)
                                    .to(0, 0, 0, 250, IType.SIN_DOWN));
        if (type == GunAnimation.CYCLE)
            return new BusAnimation()
                    .addBus(
                            "SWINGRIGHT",
                            new AnimationSequence()
                                    .to(1, 0, 0, 150, IType.SIN_DOWN)
                                    .to(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus(
                            "SWINGLEFT",
                            new AnimationSequence()
                                    .to(0, 0, 0, 300)
                                    .to(1, 0, 0, 150, IType.SIN_DOWN)
                                    .to(0, 0, 0, 250, IType.SIN_FULL));
        if (type == GunAnimation.ALT_CYCLE)
            return new BusAnimation()
                    .addBus(
                            "SLAPTURN",
                            new AnimationSequence()
                                    .to(1, 0, 0, 250, IType.LINEAR)
                                    .stay(150)
                                    .to(0, 0, 0, 350, IType.LINEAR))
                    .addBus(
                            "SLAP",
                            new AnimationSequence()
                                    .stay(250)
                                    .to(1, 0, 0, 150, IType.SIN_DOWN)
                                    .to(0, 0, 0, 350, IType.SIN_FULL));
        return null;
    }

    @Override
    public void drawArms(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        renderArms(
                pose,
                collector,
                light,
                Types.ARM,
                ResourceManager.armor_remnant,
                Parts.LEFT,
                Parts.RIGHT);
    }

    @Override
    protected void armPose(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            HFRWavefrontObject mesh,
            int leftPart,
            int rightPart) {
        double[] equip = HeldItemAnimations.sampleChannels("EQUIP");
        float swingRight = (float) HeldItemAnimations.getChannelValue("SWINGRIGHT", 0);
        float swingLeft = (float) HeldItemAnimations.getChannelValue("SWINGLEFT", 0);
        float slapTurn = (float) HeldItemAnimations.getChannelValue("SLAPTURN", 0);
        float slap = (float) HeldItemAnimations.getChannelValue("SLAP", 0);

        float forwardTilt = (float) (60D - 60D * equip[0]);
        float outward = 3F;
        float roll = 60F;

        pose.pushPose();
        pose.translate(
                -12F * swingLeft + 2F * slapTurn - 5F * slap,
                6F * slap,
                5F * swingLeft + 8F * slap);
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(forwardTilt - swingRight * 20F));
        pose.translate(outward, 0F, 0F);
        submitArm(
                pose,
                collector,
                light,
                type,
                mesh,
                leftPart,
                6F,
                60F * swingLeft + 45F * slap,
                roll + 15F * swingLeft + 45F * slapTurn);
        pose.popPose();

        pose.pushPose();
        pose.translate(
                12F * swingRight - 2F * slapTurn + 5F * slap,
                6F * slap,
                5F * swingRight + 8F * slap);
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(forwardTilt - swingLeft * 20F));
        pose.translate(-outward, 0F, 0F);
        submitArm(
                pose,
                collector,
                light,
                type,
                mesh,
                rightPart,
                -6F,
                -60F * swingRight - 45F * slap,
                -roll - 15F * swingRight - 45F * slapTurn);
        pose.popPose();
    }

    private static final class Parts {
        static final int LEFT = ResourceManager.armor_remnant.partId("LeftArm");
        static final int RIGHT = ResourceManager.armor_remnant.partId("RightArm");
    }

    private static final class Types {
        static final RenderType ARM = RenderTypes.entityCutout(ResourceManager.rpa_arm_tex);
    }
}
