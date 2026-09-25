// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
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

public class ItemRenderDrill extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.drill_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -1.25F * offset,
                -1.75F * offset,
                1.75F * offset,
                -1F * offset,
                -1.75F * offset,
                1.25F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        double gauge = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] deploy = HbmAnimations.getRelevantTransformation("DEPLOY");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] spin = HbmAnimations.getRelevantTransformation("SPIN");

        pose.mulPose(Axis.YP.rotationDegrees((float) (15 * (1 - deploy[0] * 0.5))));
        pose.mulPose(Axis.XP.rotationDegrees((float) (-10 * (1 - deploy[0] * 0.5))));

        pose.translate(0, 2, -6);
        pose.mulPose(Axis.YP.rotationDegrees((float) (equip[0] * -45)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (equip[0] * -20)));
        pose.translate(0, -2, 6);

        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));

        pose.translate(0, 0, deploy[0]);

        submitPart(collector, pose, body, ResourceManager.drill, Parts.BASE, light);

        pose.pushPose();
        pose.translate(1, 2.0625, -1.75);
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-135 + gauge * 270)));
        pose.mulPose(Axis.XP.rotationDegrees(-45));
        pose.translate(-1, -2.0625, 1.75);
        submitPart(collector, pose, body, ResourceManager.drill, Parts.GAUGE, light);
        pose.popPose();

        double rot = spin[0];
        double rot2 = rot * 5;

        pose.pushPose();
        pose.translate(0, Math.sin(rot2 * Math.PI / 180) * 0.125 - 0.125, 0);
        submitPart(collector, pose, body, ResourceManager.drill, Parts.PISTON1, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 2D / 3D) * 0.125 - 0.125, 0);
        submitPart(collector, pose, body, ResourceManager.drill, Parts.PISTON2, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 4D / 3D) * 0.125 - 0.125, 0);
        submitPart(collector, pose, body, ResourceManager.drill, Parts.PISTON3, light);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.ZN.rotationDegrees((float) rot));
        submitPart(collector, pose, body, ResourceManager.drill, Parts.DRILL_BACK, light);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot));
        submitPart(collector, pose, body, ResourceManager.drill, Parts.DRILL_FRONT, light);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        double gauge = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.25F * offset, -1.75F * offset, 1.75F * offset)
                        .scale(0.375F)
                        .rotate(Axis.YP.rotationDegrees(15F))
                        .rotate(Axis.XP.rotationDegrees(-10F));
        body.part(m, ResourceManager.drill, Parts.BASE, ResourceManager.drill_tex);
        body.part(
                new Matrix4f(m)
                        .translate(1F, 2.0625F, -1.75F)
                        .rotate(Axis.XP.rotationDegrees(45))
                        .rotate(Axis.ZP.rotationDegrees((float) (-135 + gauge * 270)))
                        .rotate(Axis.XP.rotationDegrees(-45))
                        .translate(-1F, -2.0625F, 1.75F),
                ResourceManager.drill,
                Parts.GAUGE,
                ResourceManager.drill_tex);
        body.part(
                new Matrix4f(m).translate(0F, -0.125F, 0F),
                ResourceManager.drill,
                Parts.PISTON1,
                ResourceManager.drill_tex);
        body.part(
                new Matrix4f(m)
                        .translate(0F, (float) (Math.sin(Math.PI * 2D / 3D) * 0.125 - 0.125), 0F),
                ResourceManager.drill,
                Parts.PISTON2,
                ResourceManager.drill_tex);
        body.part(
                new Matrix4f(m)
                        .translate(0F, (float) (Math.sin(Math.PI * 4D / 3D) * 0.125 - 0.125), 0F),
                ResourceManager.drill,
                Parts.PISTON3,
                ResourceManager.drill_tex);
        body.part(m, ResourceManager.drill, Parts.DRILL_BACK, ResourceManager.drill_tex);
        body.part(m, ResourceManager.drill, Parts.DRILL_FRONT, ResourceManager.drill_tex);
    }

    private static final class Parts {
        static final int BASE = ResourceManager.drill.partId("Base");
        static final int GAUGE = ResourceManager.drill.partId("Gauge");
        static final int PISTON1 = ResourceManager.drill.partId("Piston1");
        static final int PISTON2 = ResourceManager.drill.partId("Piston2");
        static final int PISTON3 = ResourceManager.drill.partId("Piston3");
        static final int DRILL_BACK = ResourceManager.drill.partId("DrillBack");
        static final int DRILL_FRONT = ResourceManager.drill.partId("DrillFront");
    }
}
