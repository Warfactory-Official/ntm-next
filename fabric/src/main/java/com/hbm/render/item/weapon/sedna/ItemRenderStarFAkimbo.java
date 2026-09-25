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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderStarFAkimbo extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.star_f_elite_tex);
    private final RenderType uziTex = RenderTypes.entityCutout(ResourceManager.uzi_tex);

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

            pose.pushPose();
            standardAimingTransform(
                    stack,
                    pose,
                    -2F * offset * i,
                    -1.75F * offset,
                    2.5F * offset,
                    0,
                    -7.625 / 8D,
                    1);

            double scale = 0.25D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
            double[] tilt = HbmAnimations.getRelevantTransformation("TILT", index);
            double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
            double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
            double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
            double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);

            pose.translate(0, -2, -8);
            pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
            pose.translate(0, 2, 8);

            pose.translate(0, 1, -3);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (turn[2] * i)));
            pose.mulPose(Axis.XP.rotationDegrees((float) tilt[0]));
            pose.translate(0, -1, 3);

            pose.translate(0, 0, recoil[2]);

            submitPart(collector, pose, body, ResourceManager.star_f, Parts.STAR_F_GUN, light);

            pose.pushPose();
            pose.translate(0, 1.75, -4.25);
            pose.mulPose(Axis.XP.rotationDegrees((float) (60 * (hammer[0] - 1))));
            pose.translate(0, -1.75, 4.25);
            submitPart(collector, pose, body, ResourceManager.star_f, Parts.STAR_F_HAMMER, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, slide[2] * 2.3125);
            submitPart(collector, pose, body, ResourceManager.star_f, Parts.STAR_F_SLIDE, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(mag[0], mag[1], mag[2]);
            submitPart(collector, pose, body, ResourceManager.star_f, Parts.STAR_F_MAG, light);
            pose.translate(bullet[0], bullet[1], bullet[2]);
            submitPart(collector, pose, body, ResourceManager.star_f, Parts.STAR_F_BULLET, light);
            pose.popPose();

            if (hasSilencer(stack, index)) {
                pose.pushPose();
                pose.translate(0, 2.375, -0.25);
                submitPart(collector, pose, uziTex, ResourceManager.uzi, Parts.UZI_SILENCER, light);
                pose.popPose();

            } else {
                double smokeScale = 0.5;

                pose.pushPose();
                pose.translate(0, 3, 6.125);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
                renderSmokeNodes(
                        collector, pose, gun.getConfig(stack, index).smokeNodes, 0.75D, light);
                pose.popPose();

                pose.pushPose();
                pose.translate(0, 3, 6.125);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
                renderMuzzleFlash(collector, pose, gun.lastShot[index], 75, 7.5);
                pose.popPose();
            }

            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        for (int i = -1; i <= 1; i += 2) {
            int index = i == -1 ? 0 : 1;
            Matrix4f m =
                    restSetup(0.875F, -2F * offset * i, -1.75F * offset, 2.5F * offset)
                            .scale(0.25F);
            body.part(
                    m, ResourceManager.star_f, Parts.STAR_F_GUN, ResourceManager.star_f_elite_tex);
            Matrix4f hammer =
                    new Matrix4f(m)
                            .translate(0F, 1.75F, -4.25F)
                            .rotate(Axis.XP.rotationDegrees(-60))
                            .translate(0F, -1.75F, 4.25F);
            body.part(
                    hammer,
                    ResourceManager.star_f,
                    Parts.STAR_F_HAMMER,
                    ResourceManager.star_f_elite_tex);
            body.part(
                    m,
                    ResourceManager.star_f,
                    Parts.STAR_F_SLIDE,
                    ResourceManager.star_f_elite_tex);
            body.part(
                    m, ResourceManager.star_f, Parts.STAR_F_MAG, ResourceManager.star_f_elite_tex);
            body.part(
                    m,
                    ResourceManager.star_f,
                    Parts.STAR_F_BULLET,
                    ResourceManager.star_f_elite_tex);
            if (hasSilencer(stack, index)) {
                body.part(
                        new Matrix4f(m).translate(0F, 2.375F, -0.25F),
                        ResourceManager.uzi,
                        Parts.UZI_SILENCER,
                        ResourceManager.uzi_tex);
            }
        }
    }

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
    }

    private static final class Parts {
        static final int STAR_F_GUN = ResourceManager.star_f.partId("Gun");
        static final int STAR_F_HAMMER = ResourceManager.star_f.partId("Hammer");
        static final int STAR_F_SLIDE = ResourceManager.star_f.partId("Slide");
        static final int STAR_F_MAG = ResourceManager.star_f.partId("Mag");
        static final int STAR_F_BULLET = ResourceManager.star_f.partId("Bullet");
        static final int UZI_SILENCER = ResourceManager.uzi.partId("Silencer");
    }
}
