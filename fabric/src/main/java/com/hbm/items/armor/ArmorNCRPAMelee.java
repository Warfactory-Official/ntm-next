// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.client.model.Meshes;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public class ArmorNCRPAMelee extends ArmorPAMeleeBase {

    private static HFRWavefrontObject flatSource;
    private static HFRWavefrontObject flatArms;

    @Override
    protected int lightCooldown() {
        return 25;
    }

    @Override
    protected int heavyCooldown() {
        return 30;
    }

    @Override
    protected boolean isLightHit(int timer) {
        return timer == 5 || timer == 15;
    }

    @Override
    protected boolean isHeavyHit(int timer) {
        return timer == 5;
    }

    @Override
    protected SoundEvent hitSound() {
        return ModSounds.GUN_STAB.get();
    }

    @Override
    public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
        if (type == GunAnimation.EQUIP)
            return new BusAnimation()
                    .addBus(
                            "EQUIP",
                            new BusAnimationSequence()
                                    .setPos(-1, 0, 0)
                                    .addPos(0, 0, 0, 750, IType.SIN_DOWN));
        if (type == GunAnimation.CYCLE)
            return new BusAnimation()
                    .addBus(
                            "SWINGRIGHT",
                            new BusAnimationSequence()
                                    .addPos(1, 0, 0, 250, IType.SIN_DOWN)
                                    .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus(
                            "SWINGLEFT",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 500)
                                    .addPos(1, 0, 0, 250, IType.SIN_DOWN)
                                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
        if (type == GunAnimation.ALT_CYCLE)
            return new BusAnimation()
                    .addBus(
                            "SWEEPTURN",
                            new BusAnimationSequence()
                                    .addPos(1, 0, 0, 100, IType.LINEAR)
                                    .hold(350)
                                    .addPos(0, 0, 0, 500, IType.LINEAR))
                    .addBus(
                            "SWEEPCUT",
                            new BusAnimationSequence()
                                    .hold(100)
                                    .addPos(1, 0, 0, 250, IType.SIN_DOWN)
                                    .hold(100)
                                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
        return null;
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        renderArms(pose, collector, light, Types.ARM, flatArms(), Parts.LEFT, Parts.RIGHT);
    }

    private static HFRWavefrontObject flatArms() {
        HFRWavefrontObject mesh = ResourceManager.armor_ncr;
        if (mesh != flatSource) {
            flatArms = Meshes.flatShaded(mesh);
            flatSource = mesh;
        }
        return flatArms;
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
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        float swingRight = (float) HbmAnimations.getRelevantTransformation("SWINGRIGHT")[0];
        float swingLeft = (float) HbmAnimations.getRelevantTransformation("SWINGLEFT")[0];
        float sweepTurn = (float) HbmAnimations.getRelevantTransformation("SWEEPTURN")[0];
        float sweepCut = (float) HbmAnimations.getRelevantTransformation("SWEEPCUT")[0];

        float forwardTilt = (float) (60D - 60D * equip[0]);
        float outward = 3F;
        float roll = 60F;

        pose.pushPose();
        pose.translate(
                -14F * swingLeft - 4F * sweepTurn, 6F * sweepCut, 2F * swingLeft + 8F * sweepCut);
        pose.mulPose(Axis.XP.rotationDegrees(forwardTilt + swingRight * 40F - 60F * sweepCut));
        pose.translate(outward, 0F, 0F);
        submitArm(
                pose,
                collector,
                light,
                type,
                mesh,
                leftPart,
                6F,
                90F * swingLeft,
                roll + 30F * swingLeft - 90F * sweepTurn);
        pose.popPose();

        pose.pushPose();
        pose.translate(
                14F * swingRight + 4F * sweepTurn, 6F * sweepCut, 2F * swingRight + 8F * sweepCut);
        pose.mulPose(Axis.XP.rotationDegrees(forwardTilt + swingLeft * 40F - 60F * sweepCut));
        pose.translate(-outward, 0F, 0F);
        submitArm(
                pose,
                collector,
                light,
                type,
                mesh,
                rightPart,
                -6F,
                -90F * swingRight,
                -roll - 30F * swingRight + 90F * sweepTurn);
        pose.popPose();
    }

    private static final class Parts {
        static final int LEFT = ResourceManager.armor_ncr.partId("LeftArm");
        static final int RIGHT = ResourceManager.armor_ncr.partId("RightArm");
    }

    private static final class Types {
        static final RenderType ARM = RenderTypes.entityCutout(ResourceManager.ncrpa_arm_tex);
    }
}
