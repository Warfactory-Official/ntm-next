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

public class ItemRenderTau extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.tau_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -1.75F * offset,
                -1.75F * offset,
                3.5F * offset,
                -1.75F * offset,
                -1.75F * offset,
                3.5F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

        pose.translate(0, -1, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 4);

        pose.translate(0, 0, recoil[2]);

        pose.translate(0, 0, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 5)));
        pose.translate(0, 0, 2);

        submitPart(collector, pose, body, ResourceManager.tau, Parts.BODY, light);

        pose.pushPose();
        pose.translate(0, -0.25, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rotate[2]));
        pose.translate(0, 0.25, 0);
        submitPart(collector, pose, body, ResourceManager.tau, Parts.ROTOR, light);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.75F * offset, -1.75F * offset, 3.5F * offset).scale(0.75F);
        body.part(m, ResourceManager.tau, Parts.BODY, ResourceManager.tau_tex);
        body.part(m, ResourceManager.tau, Parts.ROTOR, ResourceManager.tau_tex);
    }

    private static final class Parts {
        static final int BODY = ResourceManager.tau.partId("Body");
        static final int ROTOR = ResourceManager.tau.partId("Rotor");
    }
}
