// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
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

public class ItemRenderM2 extends ItemRenderWeaponBase {

    private final RenderType body = RenderTypes.entityCutout(ResourceManager.m2_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -2.5F * offset, 1.75F * offset, 0, -12.5 / 8D, 1.75);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");

        pose.translate(0, 1, -2.25);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, -1, 2.25);

        pose.translate(0, 0, recoil[2]);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180));
        collector.submitCustomGeometry(
                pose, body, (p, buffer) -> ResourceManager.m2.render(p, buffer, light, -1));
        pose.popPose();

        double smokeScale = 0.5;

        pose.pushPose();
        pose.translate(0, 1.625, 5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.375D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.625, 5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.5F, 0.5F, 0.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        body.model(
                restSetup(0.875F, -1.5F * offset, -2.5F * offset, 1.75F * offset)
                        .scale(0.75F)
                        .rotate(Axis.YP.rotationDegrees(180)),
                ResourceManager.m2,
                ResourceManager.m2_tex);
    }
}
