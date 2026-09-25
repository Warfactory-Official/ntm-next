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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderUzi extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.uzi_tex);
    private final RenderType bodySaturnite =
            RenderTypes.entityCutout(ResourceManager.uzi_saturnite_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.75F * offset, -1.5F * offset, 2.5F * offset, 0, -4.375 / 8D, 1);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        RenderType tex = isSaturnite(stack) ? bodySaturnite : body;
        double scale = 0.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] stockFront = HbmAnimations.getRelevantTransformation("STOCKFRONT");
        double[] stockBack = HbmAnimations.getRelevantTransformation("STOCKBACK");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
        double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
        double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
        double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");

        pose.translate(yeet[0], yeet[1], yeet[2]);
        pose.mulPose(Axis.ZP.rotationDegrees((float) speen[0]));

        pose.translate(0, -2, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 4);

        pose.translate(0, 0, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 6);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, tex, ResourceManager.uzi, Parts.GUN, light);

        boolean silenced = hasSilencer(stack, 0);
        if (silenced) submitPart(collector, pose, tex, ResourceManager.uzi, Parts.SILENCER, light);

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
            renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.75D, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0.75, 8.5);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
            renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        Identifier texture =
                isSaturnite(stack) ? ResourceManager.uzi_saturnite_tex : ResourceManager.uzi_tex;
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.75F * offset, -1.5F * offset, 2.5F * offset).scale(0.25F);
        body.part(m, ResourceManager.uzi, Parts.GUN, texture);
        if (hasSilencer(stack, 0)) body.part(m, ResourceManager.uzi, Parts.SILENCER, texture);
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

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
    }

    public boolean isSaturnite(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_UZI_SATURN);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.uzi.partId("Gun");
        static final int SILENCER = ResourceManager.uzi.partId("Silencer");
        static final int STOCK_FRONT = ResourceManager.uzi.partId("StockFront");
        static final int STOCK_BACK = ResourceManager.uzi.partId("StockBack");
        static final int PART_SLIDE = ResourceManager.uzi.partId("Slide");
        static final int MAGAZINE = ResourceManager.uzi.partId("Magazine");
        static final int PART_BULLET = ResourceManager.uzi.partId("Bullet");
    }
}
