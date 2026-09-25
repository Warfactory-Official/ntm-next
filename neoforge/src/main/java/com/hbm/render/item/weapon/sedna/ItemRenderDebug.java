// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
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

public class ItemRenderDebug extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.debug_gun_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.0F * offset, -0.75F * offset, offset, 0, -3.875 / 8D, 0);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        pose.scale(0.125F, 0.125F, 0.125F);
        pose.mulPose(Axis.YP.rotationDegrees(90));

        double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
        double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
        double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
        double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
        double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");

        pose.mulPose(Axis.ZP.rotationDegrees((float) equipSpin[0]));
        standardAimingTransform(stack, pose, 0, 0, recoil[2], -recoil[2], 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (recoil[2] * 10)));

        pose.pushPose();
        pose.translate(-9, 2.5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (recoil[2] * -10)));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.mulPose(Axis.ZP.rotationDegrees((float) reloadLift[0]));
        pose.translate(reloadJolt[0], 0, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) reloadTilt[0]));

        submitPart(collector, pose, body, ResourceManager.lilmac, Parts.GUN, light);

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((float) cylinderFlip[0]));
        submitPart(collector, pose, body, ResourceManager.lilmac, Parts.PIVOT, light);
        pose.translate(0, 1.75, 0);
        pose.mulPose(
                Axis.XN.rotationDegrees(
                        (float) (HbmAnimations.getRelevantTransformation("DRUM")[2] * 60)));
        pose.translate(0, -1.75, 0);
        submitPart(collector, pose, body, ResourceManager.lilmac, Parts.CYLINDER, light);
        pose.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
        if (HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1) {
            submitPart(collector, pose, body, ResourceManager.lilmac, Parts.BULLETS, light);
        }
        submitPart(collector, pose, body, ResourceManager.lilmac, Parts.CASINGS, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(4, 1.25, 0);
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        (float) (-30 + 30 * HbmAnimations.getRelevantTransformation("HAMMER")[2])));
        pose.translate(-4, -1.25, 0);
        submitPart(collector, pose, body, ResourceManager.lilmac, Parts.PART_HAMMER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.125, 2.5, 0);
        renderGapFlash(collector, pose, gun.lastShot[0]);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(1F, -1.0F * offset, -0.75F * offset, offset)
                        .scale(0.125F)
                        .rotate(Axis.YP.rotationDegrees(90));
        body.part(m, ResourceManager.lilmac, Parts.GUN, ResourceManager.debug_gun_tex);
        body.part(m, ResourceManager.lilmac, Parts.PIVOT, ResourceManager.debug_gun_tex);
        body.part(m, ResourceManager.lilmac, Parts.CYLINDER, ResourceManager.debug_gun_tex);
        body.part(m, ResourceManager.lilmac, Parts.BULLETS, ResourceManager.debug_gun_tex);
        body.part(m, ResourceManager.lilmac, Parts.CASINGS, ResourceManager.debug_gun_tex);
        body.part(
                m.translate(4F, 1.25F, 0F)
                        .rotate(Axis.ZP.rotationDegrees(-30F))
                        .translate(-4F, -1.25F, 0F),
                ResourceManager.lilmac,
                Parts.PART_HAMMER,
                ResourceManager.debug_gun_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.lilmac.partId("Gun");
        static final int PIVOT = ResourceManager.lilmac.partId("Pivot");
        static final int CYLINDER = ResourceManager.lilmac.partId("Cylinder");
        static final int BULLETS = ResourceManager.lilmac.partId("Bullets");
        static final int CASINGS = ResourceManager.lilmac.partId("Casings");
        static final int PART_HAMMER = ResourceManager.lilmac.partId("Hammer");
    }
}
