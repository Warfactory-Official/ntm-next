// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderUziAkimbo extends ItemRenderWeaponBase {
    private static final String SATURNITE = "textures/models/weapons/uzi_saturnite.png";
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.uzi_tex);
    private final RenderType bodySaturnite =
            RenderTypes.entityCutout(ResourceManager.uzi_saturnite_tex);

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
            RenderType tex = isSaturnite(stack, index) ? bodySaturnite : body;

            pose.pushPose();
            standardAimingTransform(
                    stack,
                    pose,
                    -2.25F * offset * i,
                    -1.5F * offset,
                    2.5F * offset,
                    0,
                    -4.375 / 8D,
                    1);

            double scale = 0.25D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
            double[] stockFront = HbmAnimations.getRelevantTransformation("STOCKFRONT", index);
            double[] stockBack = HbmAnimations.getRelevantTransformation("STOCKBACK", index);
            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] lift = HbmAnimations.getRelevantTransformation("LIFT", index);
            double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
            double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
            double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);
            double[] yeet = HbmAnimations.getRelevantTransformation("YEET", index);
            double[] speen = HbmAnimations.getRelevantTransformation("SPEEN", index);

            pose.translate(yeet[0], yeet[1], yeet[2]);
            pose.mulPose((i == 1 ? Axis.ZP : Axis.ZN).rotationDegrees((float) speen[0]));

            pose.translate(0, -2, -4);
            pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
            pose.translate(0, 2, 4);

            pose.translate(0, 0, -6);
            pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
            pose.translate(0, 0, 6);

            pose.translate(0, 0, recoil[2]);

            submitPart(
                    collector,
                    pose,
                    tex,
                    ResourceManager.uzi,
                    index == 0 ? Parts.GUN_MIRROR : Parts.PART_GUN,
                    light);

            boolean silenced = hasSilencer(stack, index);
            if (silenced)
                submitPart(collector, pose, tex, ResourceManager.uzi, Parts.SILENCER, light);

            pose.pushPose();
            pose.translate(0, 0.3125D, -5.75);
            pose.mulPose(Axis.XP.rotationDegrees((float) (180 - stockFront[0])));
            pose.translate(0, -0.3125D, 5.75);
            submitPart(collector, pose, tex, ResourceManager.uzi, Parts.STOCK_FRONT, light);

            pose.translate(0, -0.3125D, -3);
            pose.mulPose(Axis.XP.rotationDegrees((float) (-200 - stockBack[0])));
            pose.translate(0, 0.3125D, 3);
            submitPart(collector, pose, tex, ResourceManager.uzi, Parts.STOCK_BACK, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, slide[2]);
            submitPart(collector, pose, tex, ResourceManager.uzi, Parts.PART_SLIDE, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(mag[0], mag[1], mag[2]);
            submitPart(collector, pose, tex, ResourceManager.uzi, Parts.MAGAZINE, light);
            if (bullet[0] == 1)
                submitPart(collector, pose, tex, ResourceManager.uzi, Parts.PART_BULLET, light);
            pose.popPose();

            if (!silenced) {
                double smokeScale = 0.5;

                pose.pushPose();
                pose.translate(0, 0.75, 8.5);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
                renderSmokeNodes(
                        collector, pose, gun.getConfig(stack, index).smokeNodes, 0.75D, light);
                pose.popPose();

                pose.pushPose();
                pose.translate(0, 0.75, 8.5);
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
            Identifier texture =
                    isSaturnite(stack, index)
                            ? ResourceManager.uzi_saturnite_tex
                            : ResourceManager.uzi_tex;
            Matrix4f m =
                    restSetup(0.875F, -2.25F * offset * i, -1.5F * offset, 2.5F * offset)
                            .scale(0.25F);
            body.part(
                    m,
                    ResourceManager.uzi,
                    index == 0 ? Parts.GUN_MIRROR : Parts.PART_GUN,
                    texture);
            if (hasSilencer(stack, index))
                body.part(m, ResourceManager.uzi, Parts.SILENCER, texture);
            Matrix4f stock =
                    new Matrix4f(m)
                            .translate(0F, 0.3125F, -5.75F)
                            .rotate(Axis.XP.rotationDegrees(180))
                            .translate(0F, -0.3125F, 5.75F);
            body.part(stock, ResourceManager.uzi, Parts.STOCK_FRONT, texture);
            stock.translate(0F, -0.3125F, -3F)
                    .rotate(Axis.XP.rotationDegrees(-200))
                    .translate(0F, 0.3125F, 3F);
            body.part(stock, ResourceManager.uzi, Parts.STOCK_BACK, texture);
            body.part(m, ResourceManager.uzi, Parts.PART_SLIDE, texture);
            body.part(m, ResourceManager.uzi, Parts.MAGAZINE, texture);
        }
    }

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
    }

    public boolean isSaturnite(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_UZI_SATURN);
    }

    private static final class Parts {
        static final int GUN_MIRROR = ResourceManager.uzi.partId("GunMirror");
        static final int PART_GUN = ResourceManager.uzi.partId("Gun");
        static final int SILENCER = ResourceManager.uzi.partId("Silencer");
        static final int STOCK_FRONT = ResourceManager.uzi.partId("StockFront");
        static final int STOCK_BACK = ResourceManager.uzi.partId("StockBack");
        static final int PART_SLIDE = ResourceManager.uzi.partId("Slide");
        static final int MAGAZINE = ResourceManager.uzi.partId("Magazine");
        static final int PART_BULLET = ResourceManager.uzi.partId("Bullet");
    }
}
