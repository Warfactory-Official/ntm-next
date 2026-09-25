// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
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

public class ItemRenderCoilgun extends ItemRenderWeaponBase {

    private final RenderType body = RenderTypes.entityCutout(ResourceManager.coilgun_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.25F * offset, -1.5F * offset, 2.5F * offset, 0, -7.5 / 8D, 1);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        pose.mulPose(Axis.YN.rotationDegrees(90));

        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        pose.translate(-1.5 - recoil[0] * 0.5, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (recoil[0] * 45)));
        pose.translate(1.5, 0, 0);

        double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
        pose.translate(-2.5, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (reload[0] * -45)));
        pose.translate(2.5, 0, 0);

        collector.submitCustomGeometry(
                pose, body, (p, buffer) -> ResourceManager.coilgun.render(p, buffer, light, -1));
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        body.model(
                restSetup(0.875F, -1.25F * offset, -1.5F * offset, 2.5F * offset)
                        .scale(0.75F)
                        .rotate(Axis.YN.rotationDegrees(90)),
                ResourceManager.coilgun,
                ResourceManager.coilgun_tex);
    }
}
