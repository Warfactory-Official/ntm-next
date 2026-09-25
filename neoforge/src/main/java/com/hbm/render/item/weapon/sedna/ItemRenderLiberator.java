// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunConfig;
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

public class ItemRenderLiberator extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.liberator_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -1.25F * offset, 1.25F * offset, 0, -4.625 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
        double[] brk = HbmAnimations.getRelevantTransformation("BREAK");
        double[] shell1 = HbmAnimations.getRelevantTransformation("SHELL1");
        double[] shell2 = HbmAnimations.getRelevantTransformation("SHELL2");
        double[] shell3 = HbmAnimations.getRelevantTransformation("SHELL3");
        double[] shell4 = HbmAnimations.getRelevantTransformation("SHELL4");

        pose.translate(0, -1, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 3);

        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 3, 3);

        pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));

        submitPart(collector, pose, body, ResourceManager.liberator, Parts.GUN, light);

        pose.pushPose();

        pose.translate(0, -0.5, 0.75);
        pose.mulPose(Axis.XP.rotationDegrees((float) brk[0]));
        pose.translate(0, 0.5, -0.75);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.BARREL, light);

        pose.pushPose();
        pose.translate(shell1[0], shell1[1], shell1[2]);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.PART_SHELL1, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(shell2[0], shell2[1], shell2[2]);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.PART_SHELL2, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(shell3[0], shell3[1], shell3[2]);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.PART_SHELL3, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(shell4[0], shell4[1], shell4[2]);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.PART_SHELL4, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.15625, 0.75);
        pose.mulPose(Axis.XP.rotationDegrees((float) latch[0]));
        pose.translate(0, -1.15625, -0.75);
        submitPart(collector, pose, body, ResourceManager.liberator, Parts.PART_LATCH, light);
        pose.popPose();
        pose.popPose();

        double smokeScale = 0.375;

        GunConfig cfg = gun.getConfig(stack, 0);

        pose.pushPose();
        pose.translate(0, 0.25, 7.25);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        pose.translate(0, 0, 0.25 / smokeScale);
        renderSmokeNodes(collector, pose, cfg.smokeNodes, 1D, light);
        pose.translate(0, 0, -0.5 / smokeScale);
        renderSmokeNodes(collector, pose, cfg.smokeNodes, 1D, light);
        pose.translate(0, 0.5 / smokeScale, 0);
        renderSmokeNodes(collector, pose, cfg.smokeNodes, 1D, light);
        pose.translate(0, 0, 0.5 / smokeScale);
        renderSmokeNodes(collector, pose, cfg.smokeNodes, 1D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.5, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(1.5F, 1.5F, 1.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -1.25F * offset, 1.25F * offset).scale(0.375F);
        body.part(m, ResourceManager.liberator, Parts.GUN, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.BARREL, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.PART_SHELL1, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.PART_SHELL2, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.PART_SHELL3, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.PART_SHELL4, ResourceManager.liberator_tex);
        body.part(m, ResourceManager.liberator, Parts.PART_LATCH, ResourceManager.liberator_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.liberator.partId("Gun");
        static final int BARREL = ResourceManager.liberator.partId("Barrel");
        static final int PART_SHELL1 = ResourceManager.liberator.partId("Shell1");
        static final int PART_SHELL2 = ResourceManager.liberator.partId("Shell2");
        static final int PART_SHELL3 = ResourceManager.liberator.partId("Shell3");
        static final int PART_SHELL4 = ResourceManager.liberator.partId("Shell4");
        static final int PART_LATCH = ResourceManager.liberator.partId("Latch");
    }
}
