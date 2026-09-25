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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderDANI extends ItemRenderWeaponBase {
    private final RenderType celestial =
            RenderTypes.entityCutout(ResourceManager.dani_celestial_tex);
    private final RenderType lunar = RenderTypes.entityCutout(ResourceManager.dani_lunar_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;

        for (int i = -1; i <= 1; i += 2) {

            int index = i == -1 ? 0 : 1;
            RenderType body = index == 0 ? celestial : lunar;

            pose.pushPose();

            standardAimingTransform(
                    stack, pose, -1.5F * offset * i, -0.75F * offset, offset, 0, -3.125 / 8D, 0.25);

            double scale = 0.125D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE", index);
            double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT", index);
            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);

            pose.translate(recoil[0], recoil[1], recoil[2]);
            pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));

            pose.translate(0, -2, -2);
            pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
            pose.translate(0, 2, 2);

            pose.pushPose();
            pose.translate(0, 1.5, 9.25);
            pose.mulPose(Axis.XP.rotationDegrees((float) (-recoil[2] * 10)));
            pose.mulPose(Axis.YP.rotationDegrees(90));
            renderSmokeNodes(collector, pose, gun.getConfig(stack, index).smokeNodes, 0.5D, light);
            pose.popPose();

            pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

            pose.mulPose(Axis.XP.rotationDegrees((float) reloadRot[0]));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (reloadRot[2] * i)));
            pose.mulPose(Axis.YP.rotationDegrees((float) (reloadRot[1] * i)));
            submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.GRIP, light);

            pose.pushPose();
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            (float) HbmAnimations.getRelevantTransformation("FRONT", index)[2]));
            submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.BARREL, light);
            pose.pushPose();
            pose.translate(0, 2.3125, -0.875);
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            (float) HbmAnimations.getRelevantTransformation("LATCH", index)[2]));
            pose.translate(0, -2.3125, 0.875);
            submitPart(
                    collector, pose, body, ResourceManager.bio_revolver, Parts.PART_LATCH, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 1, 0);
            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            (float)
                                    (HbmAnimations.getRelevantTransformation("DRUM", index)[2]
                                            * 60)));
            pose.translate(0, -1, 0);
            pose.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH", index)[2]);
            submitPart(collector, pose, body, ResourceManager.bio_revolver, Parts.PART_DRUM, light);
            pose.popPose();

            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, -4.5);
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            (float)
                                    (-45
                                            + 45
                                                    * HbmAnimations.getRelevantTransformation(
                                                            "HAMMER", index)[2])));
            pose.translate(0, 0, 4.5);
            submitPart(
                    collector, pose, body, ResourceManager.bio_revolver, Parts.PART_HAMMER, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 1.5, 9.25);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            renderMuzzleFlash(collector, pose, gun.lastShot[index], 75, 7.5);
            pose.popPose();

            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        for (int i = -1; i <= 1; i += 2) {
            Identifier texture =
                    i == -1 ? ResourceManager.dani_celestial_tex : ResourceManager.dani_lunar_tex;
            Matrix4f m =
                    restSetup(0.875F, -1.5F * offset * i, -0.75F * offset, offset).scale(0.125F);
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
    }

    private static final class Parts {
        static final int GRIP = ResourceManager.bio_revolver.partId("Grip");
        static final int BARREL = ResourceManager.bio_revolver.partId("Barrel");
        static final int PART_LATCH = ResourceManager.bio_revolver.partId("Latch");
        static final int PART_DRUM = ResourceManager.bio_revolver.partId("Drum");
        static final int PART_HAMMER = ResourceManager.bio_revolver.partId("Hammer");
    }
}
