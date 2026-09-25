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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderEOTT extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.eott_tex);

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);
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
                    -1.0F * offset * i,
                    -1.25F * offset,
                    1.25F * offset,
                    0,
                    -5.25 / 8D,
                    0.125);

            double scale = 0.25D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
            double[] rise = HbmAnimations.getRelevantTransformation("RISE", index);
            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);
            double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
            double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
            double[] roll = HbmAnimations.getRelevantTransformation("ROLL", index);
            double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
            double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL", index);
            double[] sight = HbmAnimations.getRelevantTransformation("SIGHT", index);

            pose.translate(0, rise[1], 0);

            pose.translate(0, 1, -2.25);
            pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
            pose.translate(0, -1, 2.25);

            pose.translate(0, -1, -4);
            pose.mulPose(Axis.XP.rotationDegrees((float) recoil[0]));
            pose.translate(0, 1, 4);

            pose.translate(0, 1, 0);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (roll[2] * i)));
            pose.translate(0, -1, 0);

            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.GUN, light);

            pose.pushPose();
            pose.translate(0, 2.4375, -1.9375);
            pose.mulPose(Axis.XP.rotationDegrees((float) sight[0]));
            pose.translate(0, -2.4375, 1.9375);
            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_SIGHT, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(mag[0] * i, mag[1], mag[2]);

            pose.translate(0, 1, 0);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (magroll[2] * i)));
            pose.translate(0, -1, 0);

            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.MAGAZINE, light);
            pose.translate(bullet[0], bullet[1], bullet[2]);
            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_BULLET, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, slide[2]);
            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_SLIDE, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 1.25, -3.625);
            pose.mulPose(Axis.XP.rotationDegrees((float) (-45 + hammer[0])));
            pose.translate(0, -1.25, 3.625);
            submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_HAMMER, light);
            pose.popPose();

            double smokeScale = 0.5;

            pose.pushPose();
            pose.translate(0, 2, 4);
            pose.mulPose(Axis.XN.rotationDegrees((float) recoil[0]));
            pose.mulPose(Axis.ZN.rotationDegrees((float) (roll[2] * i)));
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
            renderSmokeNodes(collector, pose, gun.getConfig(stack, index).smokeNodes, 0.5D, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 2, 4);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
            pose.scale(0.75F, 0.75F, 0.75F);
            renderMuzzleFlash(collector, pose, gun.lastShot[index], 75, 7.5);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 2, -1.5);
            pose.scale(0.5F, 0.5F, 0.5F);
            ItemRenderAberrator.renderFireball(collector, pose, gun.lastShot[index]);
            pose.popPose();

            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        for (int i = -1; i <= 1; i += 2) {
            Matrix4f m =
                    restSetup(1F, -1.0F * offset * i, -1.25F * offset, 1.25F * offset).scale(0.25F);
            body.part(m, ResourceManager.aberrator, Parts.GUN, ResourceManager.eott_tex);
            body.part(m, ResourceManager.aberrator, Parts.PART_SIGHT, ResourceManager.eott_tex);
            body.part(m, ResourceManager.aberrator, Parts.MAGAZINE, ResourceManager.eott_tex);
            body.part(m, ResourceManager.aberrator, Parts.PART_BULLET, ResourceManager.eott_tex);
            body.part(m, ResourceManager.aberrator, Parts.PART_SLIDE, ResourceManager.eott_tex);
            body.part(
                    m.translate(0F, 1.25F, -3.625F)
                            .rotate(Axis.XP.rotationDegrees(-45F))
                            .translate(0F, -1.25F, 3.625F),
                    ResourceManager.aberrator,
                    Parts.PART_HAMMER,
                    ResourceManager.eott_tex);
        }
    }

    private static final class Parts {
        static final int GUN = ResourceManager.aberrator.partId("Gun");
        static final int PART_SIGHT = ResourceManager.aberrator.partId("Sight");
        static final int MAGAZINE = ResourceManager.aberrator.partId("Magazine");
        static final int PART_BULLET = ResourceManager.aberrator.partId("Bullet");
        static final int PART_SLIDE = ResourceManager.aberrator.partId("Slide");
        static final int PART_HAMMER = ResourceManager.aberrator.partId("Hammer");
    }
}
