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

public class ItemRenderPepperbox extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.pepperbox_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1.5);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.25F * offset, -0.75F * offset, offset, 0, -2.5 / 8D, 0.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        double scale = 0.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] cylinder = HbmAnimations.getRelevantTransformation("ROTATE");
        double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
        double[] trigger = HbmAnimations.getRelevantTransformation("TRIGGER");
        double[] translate = HbmAnimations.getRelevantTransformation("TRANSLATE");
        double[] loader = HbmAnimations.getRelevantTransformation("LOADER");
        double[] shot = HbmAnimations.getRelevantTransformation("SHOT");

        pose.translate(translate[0], translate[1], translate[2]);

        pose.translate(0, 0, -5);
        pose.mulPose(Axis.XN.rotationDegrees((float) recoil[0]));
        pose.translate(0, 0, 5);

        pose.pushPose();
        pose.translate(0, 0.5, 7);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        if (loader[0] != 0 || loader[1] != 0 || loader[2] != 0) {
            pose.pushPose();
            pose.translate(loader[0], loader[1], loader[2]);
            submitPart(collector, pose, body, ResourceManager.pepperbox, Parts.SPEEDLOADER, light);
            if (shot[0] != 0)
                submitPart(
                        collector, pose, body, ResourceManager.pepperbox, Parts.PART_SHOT, light);
            pose.popPose();
        }

        submitPart(collector, pose, body, ResourceManager.pepperbox, Parts.GRIP, light);

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) cylinder[0]));
        submitPart(collector, pose, body, ResourceManager.pepperbox, Parts.CYLINDER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.375, -1.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) hammer[0]));
        pose.translate(0, -0.375, 1.875);
        submitPart(collector, pose, body, ResourceManager.pepperbox, Parts.PART_HAMMER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, -trigger[0] * 0.5);
        submitPart(collector, pose, body, ResourceManager.pepperbox, Parts.PART_TRIGGER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.5, 7);
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0]);
        pose.mulPose(Axis.XP.rotationDegrees(45));
        renderMuzzleFlash(collector, pose, gun.lastShot[0]);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(1.5F, -1.25F * offset, -0.75F * offset, offset).scale(0.25F);
        body.part(m, ResourceManager.pepperbox, Parts.GRIP, ResourceManager.pepperbox_tex);
        body.part(m, ResourceManager.pepperbox, Parts.CYLINDER, ResourceManager.pepperbox_tex);
        body.part(m, ResourceManager.pepperbox, Parts.PART_HAMMER, ResourceManager.pepperbox_tex);
        body.part(m, ResourceManager.pepperbox, Parts.PART_TRIGGER, ResourceManager.pepperbox_tex);
    }

    private static final class Parts {
        static final int SPEEDLOADER = ResourceManager.pepperbox.partId("Speedloader");
        static final int PART_SHOT = ResourceManager.pepperbox.partId("Shot");
        static final int GRIP = ResourceManager.pepperbox.partId("Grip");
        static final int CYLINDER = ResourceManager.pepperbox.partId("Cylinder");
        static final int PART_HAMMER = ResourceManager.pepperbox.partId("Hammer");
        static final int PART_TRIGGER = ResourceManager.pepperbox.partId("Trigger");
    }
}
