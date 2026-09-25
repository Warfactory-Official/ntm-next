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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderHeavyRevolver extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;
    private final RenderType scope = RenderTypes.entityCutout(ResourceManager.lilmac_scope_tex);

    public ItemRenderHeavyRevolver(Identifier texture) {
        this.texture = texture;
        this.body = RenderTypes.entityCutout(texture);
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
        pose.translate(0, 0, 1);

        boolean isScoped = this.isScoped(stack);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -1.0F * offset,
                -0.75F * offset,
                offset,
                0,
                isScoped ? (-4.75 / 8D) : (-3.875 / 8D),
                isScoped ? -0.25 : 0);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        boolean isScoped = this.isScoped(stack);
        if (isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1)
            return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        double scale = 0.125D;
        pose.scale((float) scale, (float) scale, (float) scale);
        pose.mulPose(Axis.YP.rotationDegrees(90));

        double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
        double[] spin = HbmAnimations.getRelevantTransformation("SPIN");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
        double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
        double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
        double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
        double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");

        pose.mulPose(Axis.ZP.rotationDegrees((float) spin[0]));

        pose.translate(6, -3, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) equipSpin[0]));
        pose.translate(-6, 3, 0);

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
        if (HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
            submitPart(collector, pose, body, ResourceManager.lilmac, Parts.BULLETS, light);
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

        if (isScoped) {
            submitPart(collector, pose, scope, ResourceManager.lilmac, Parts.SCOPE, light);
        }

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
        body.part(m, ResourceManager.lilmac, Parts.GUN, texture);
        body.part(m, ResourceManager.lilmac, Parts.PIVOT, texture);
        body.part(m, ResourceManager.lilmac, Parts.CYLINDER, texture);
        body.part(m, ResourceManager.lilmac, Parts.BULLETS, texture);
        body.part(m, ResourceManager.lilmac, Parts.CASINGS, texture);
        body.part(
                new Matrix4f(m)
                        .translate(4F, 1.25F, 0F)
                        .rotate(Axis.ZP.rotationDegrees(-30F))
                        .translate(-4F, -1.25F, 0F),
                ResourceManager.lilmac,
                Parts.PART_HAMMER,
                texture);
        if (isScoped(stack))
            body.part(m, ResourceManager.lilmac, Parts.SCOPE, ResourceManager.lilmac_scope_tex);
    }

    public boolean isScoped(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_HEAVY_REVOLVER_LILMAC.get()
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.lilmac.partId("Gun");
        static final int PIVOT = ResourceManager.lilmac.partId("Pivot");
        static final int CYLINDER = ResourceManager.lilmac.partId("Cylinder");
        static final int BULLETS = ResourceManager.lilmac.partId("Bullets");
        static final int CASINGS = ResourceManager.lilmac.partId("Casings");
        static final int PART_HAMMER = ResourceManager.lilmac.partId("Hammer");
        static final int SCOPE = ResourceManager.lilmac.partId("Scope");
    }
}
