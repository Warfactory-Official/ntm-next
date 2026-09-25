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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderAtlas extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderAtlas(Identifier texture) {
        this.texture = texture;
        this.body = RenderTypes.entityCutout(texture);
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.0F * offset, -0.75F * offset, offset, 0, -3.125 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.125D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE");
        double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT");
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

        pose.translate(recoil[0], recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));

        pose.translate(0, 0, -7);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 7);

        pose.pushPose();
        pose.translate(0, 1.5, 9.25);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-recoil[2] * 10)));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

        pose.mulPose(Axis.XP.rotationDegrees((float) reloadRot[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) reloadRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) reloadRot[1]));
        submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.GRIP, light);

        pose.pushPose();
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        (float) HbmAnimations.getRelevantTransformation("FRONT")[2]));
        submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.BARREL, light);
        pose.pushPose();
        pose.translate(0, 2.3125, -0.875);
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        (float) HbmAnimations.getRelevantTransformation("LATCH")[2]));
        pose.translate(0, -2.3125, 0.875);
        submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.PART_LATCH, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        (float) (HbmAnimations.getRelevantTransformation("DRUM")[2] * 60)));
        pose.translate(0, -1, 0);
        pose.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH")[2]);
        submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.PART_DRUM, light);
        pose.popPose();

        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, -4.5);
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        (float) (-45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER")[2])));
        pose.translate(0, 0, 4.5);
        submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.PART_HAMMER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.5, 9.25);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.0F * offset, -0.75F * offset, offset).scale(0.125F);
        body.part(m, ResourceManager.bio_revolver, Parts.GRIP, texture);
        body.part(m, ResourceManager.bio_revolver, Parts.BARREL, texture);
        body.part(m, ResourceManager.bio_revolver, Parts.PART_LATCH, texture);
        body.part(m, ResourceManager.bio_revolver, Parts.PART_DRUM, texture);
        body.part(
                m.translate(0F, 0F, -4.5F)
                        .rotate(Axis.XP.rotationDegrees(-45F))
                        .translate(0F, 0F, 4.5F),
                ResourceManager.bio_revolver,
                Parts.PART_HAMMER,
                texture);
    }

    private static final class Parts {
        static final int GRIP = ResourceManager.bio_revolver.partId("Grip");
        static final int BARREL = ResourceManager.bio_revolver.partId("Barrel");
        static final int PART_LATCH = ResourceManager.bio_revolver.partId("Latch");
        static final int PART_DRUM = ResourceManager.bio_revolver.partId("Drum");
        static final int PART_HAMMER = ResourceManager.bio_revolver.partId("Hammer");
    }
}
