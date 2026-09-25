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
import org.joml.Matrix4f;

public class ItemRenderLag extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.mike_hawk_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -1F * offset, 1.5F * offset, 0, -3.375 / 8D, 0.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.25D;
        pose.scale((float) scale, (float) scale, (float) scale);
        pose.mulPose(Axis.YP.rotationDegrees(90));

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] addTrans = HbmAnimations.getRelevantTransformation("ADD_TRANS");
        double[] addRot = HbmAnimations.getRelevantTransformation("ADD_ROT");

        pose.translate(4, -4, 0);
        pose.mulPose(Axis.ZN.rotationDegrees((float) equip[0]));
        pose.translate(-4, 4, 0);

        pose.translate(addTrans[0], addTrans[1], addTrans[2]);
        pose.mulPose(Axis.ZP.rotationDegrees((float) addRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) addRot[1]));

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Grip");
        submitPart(collector, pose, body, ResourceManager.mike_hawk, Parts.GRIP, light);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Slide");
        submitPart(collector, pose, body, ResourceManager.mike_hawk, Parts.SLIDE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(3.125, 0.125, 0);
        pose.mulPose(Axis.ZN.rotationDegrees(25));
        pose.translate(-3.125, -0.125, 0);
        HbmAnimations.applyRelevantTransformation(pose, "Hammer");
        submitPart(collector, pose, body, ResourceManager.mike_hawk, Parts.HAMMER, light);
        pose.popPose();

        if (gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null)
                > 0) {
            pose.pushPose();
            HbmAnimations.applyRelevantTransformation(pose, "Bullet");
            submitPart(collector, pose, body, ResourceManager.mike_hawk, Parts.BULLET, light);
            pose.popPose();
        }

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Magazine");
        submitPart(collector, pose, body, ResourceManager.mike_hawk, Parts.MAGAZINE, light);
        pose.popPose();

        double smokeScale = 0.5;

        pose.pushPose();
        pose.translate(-10.25, 1, 0);
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(-10.25, 1, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();

        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -1F * offset, 1.5F * offset)
                        .scale(0.25F)
                        .rotate(Axis.YP.rotationDegrees(90));
        body.part(m, ResourceManager.mike_hawk, Parts.GRIP, ResourceManager.mike_hawk_tex);
        body.part(m, ResourceManager.mike_hawk, Parts.SLIDE, ResourceManager.mike_hawk_tex);
        body.part(
                new Matrix4f(m)
                        .translate(3.125F, 0.125F, 0F)
                        .rotate(Axis.ZN.rotationDegrees(25))
                        .translate(-3.125F, -0.125F, 0F),
                ResourceManager.mike_hawk,
                Parts.HAMMER,
                ResourceManager.mike_hawk_tex);
        if (gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null)
                > 0) {
            body.part(m, ResourceManager.mike_hawk, Parts.BULLET, ResourceManager.mike_hawk_tex);
        }
        body.part(m, ResourceManager.mike_hawk, Parts.MAGAZINE, ResourceManager.mike_hawk_tex);
    }

    private static final class Parts {
        static final int GRIP = ResourceManager.mike_hawk.partId("Grip");
        static final int SLIDE = ResourceManager.mike_hawk.partId("Slide");
        static final int HAMMER = ResourceManager.mike_hawk.partId("Hammer");
        static final int BULLET = ResourceManager.mike_hawk.partId("Bullet");
        static final int MAGAZINE = ResourceManager.mike_hawk.partId("Magazine");
    }
}
