// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMAS36 extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.mas36_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        return fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        if (isScoped(stack)) {
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.25F * offset,
                    1.75F * offset,
                    -0.2,
                    -5.875 / 8D,
                    1.125);
        } else {
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.25F * offset,
                    1.75F * offset,
                    0,
                    -4.6825 / 8D,
                    0.75);
        }
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        boolean isScoped = isScoped(stack);
        if (isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1)
            return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
        double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
        double[] showClip = HbmAnimations.getRelevantTransformation("SHOW_CLIP");
        double[] clip = HbmAnimations.getRelevantTransformation("CLIP");
        double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");
        double[] stab = HbmAnimations.getRelevantTransformation("STAB");

        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 3, 3);

        pose.translate(stab[0], stab[1], stab[2]);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.mas36, Parts.GUN, light);
        if (hasBayonet(stack))
            submitPart(collector, pose, body, ResourceManager.mas36, Parts.BAYONET, light);

        pose.pushPose();
        pose.translate(0, 0.3125, -2.125);
        pose.mulPose(Axis.XP.rotationDegrees((float) stock[0]));
        pose.translate(0, -0.3125, 2.125);
        submitPart(collector, pose, body, ResourceManager.mas36, Parts.PART_STOCK, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.0625 * 18.5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) boltTurn[2]));
        pose.translate(0, 0.0625 * -18.5, 0);
        pose.translate(0, 0, boltPull[2]);
        submitPart(collector, pose, body, ResourceManager.mas36, Parts.BOLT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(bullet[0], bullet[1], bullet[2]);
        submitPart(collector, pose, body, ResourceManager.mas36, Parts.PART_BULLET, light);
        pose.popPose();

        if (isScoped) submitPart(collector, pose, body, ResourceManager.mas36, Parts.SCOPE, light);

        if (showClip[0] != 0) {
            pose.pushPose();
            pose.translate(clip[0], clip[1], clip[2]);
            submitPart(collector, pose, body, ResourceManager.mas36, Parts.PART_CLIP, light);
            pose.popPose();
            pose.pushPose();
            pose.translate(bullets[0], bullets[1], bullets[2]);
            if (bullets[0] == 0) {

                collector.submitCustomGeometry(
                        pose,
                        body,
                        (pp, buffer) ->
                                ResourceManager.mas36.renderPartClipped(
                                        pp,
                                        buffer,
                                        light,
                                        -1,
                                        Parts.PART_BULLETS,
                                        0F,
                                        1F,
                                        0F,
                                        (float) (0.5 - bullets[1])));
            } else {
                submitPart(collector, pose, body, ResourceManager.mas36, Parts.PART_BULLETS, light);
            }
            pose.popPose();
        }

        double smokeScale = 0.25;

        pose.pushPose();
        pose.translate(0, 1.125, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 1D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.5F, 0.5F, 0.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -1.25F * offset, 1.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.mas36, Parts.GUN, ResourceManager.mas36_tex);
        if (hasBayonet(stack))
            body.part(m, ResourceManager.mas36, Parts.BAYONET, ResourceManager.mas36_tex);
        body.part(m, ResourceManager.mas36, Parts.PART_STOCK, ResourceManager.mas36_tex);
        body.part(m, ResourceManager.mas36, Parts.BOLT, ResourceManager.mas36_tex);
        body.part(m, ResourceManager.mas36, Parts.PART_BULLET, ResourceManager.mas36_tex);
        if (isScoped(stack))
            body.part(m, ResourceManager.mas36, Parts.SCOPE, ResourceManager.mas36_tex);
    }

    public boolean isScoped(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }

    public boolean hasBayonet(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_MAS_BAYONET);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.mas36.partId("Gun");
        static final int BAYONET = ResourceManager.mas36.partId("Bayonet");
        static final int PART_STOCK = ResourceManager.mas36.partId("Stock");
        static final int BOLT = ResourceManager.mas36.partId("Bolt");
        static final int PART_BULLET = ResourceManager.mas36.partId("Bullet");
        static final int SCOPE = ResourceManager.mas36.partId("Scope");
        static final int PART_CLIP = ResourceManager.mas36.partId("Clip");
        static final int PART_BULLETS = ResourceManager.mas36.partId("Bullets");
    }
}
