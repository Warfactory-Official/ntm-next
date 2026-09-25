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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderTeslaCannon extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.tesla_cannon_tex);
    private final RenderType yomi = RenderTypes.entityCutout(ResourceManager.yomi_tex);

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
                -0.5F * offset,
                1.75F * offset,
                -1.3125F * offset,
                0F * offset,
                -0.5F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
        double[] count = HbmAnimations.getRelevantTransformation("COUNT");
        double[] yomiT = HbmAnimations.getRelevantTransformation("YOMI");
        double[] squeeze = HbmAnimations.getRelevantTransformation("SQUEEZE");

        pose.translate(0, -2, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 2);

        pose.translate(0, 0, recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 2)));

        int amount =
                Math.max(
                        (int) count[0],
                        gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, Minecraft.getInstance().player.getInventory()));

        submitPart(collector, pose, body, ResourceManager.tesla_cannon, Parts.GUN, light);
        submitPart(collector, pose, body, ResourceManager.tesla_cannon, Parts.EXTENSION, light);

        double cogAngle = cycle[2];

        pose.pushPose();
        pose.translate(0, -1.625, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) cogAngle));
        pose.translate(0, 1.625, 0);
        submitPart(collector, pose, body, ResourceManager.tesla_cannon, Parts.COG, light);
        pose.popPose();

        pose.pushPose();

        pose.translate(0, -1.625, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) cogAngle));
        pose.translate(0, 1.625, 0);

        for (int i = 0; i < Math.min(amount, 8); i++) {
            submitPart(collector, pose, body, ResourceManager.tesla_cannon, Parts.CAPACITOR, light);

            if (i < 4) {
                pose.translate(0, -1.625, 0);
                pose.mulPose(Axis.ZN.rotationDegrees(22.5F));
                pose.translate(0, 1.625, 0);
            } else {
                if (i == 4) {
                    pose.translate(0, -1.625, 0);
                    pose.mulPose(Axis.ZN.rotationDegrees((float) cogAngle));
                    pose.translate(0, 1.625, 0);
                    pose.translate(-cogAngle * 0.5 / 22.5, 0, 0);
                }
                pose.translate(0.5, 0, 0);
            }
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(yomiT[0], yomiT[1], yomiT[2]);
        pose.mulPose(Axis.YP.rotationDegrees(135));
        pose.scale((float) squeeze[0], (float) squeeze[1], (float) squeeze[2]);
        collector.submitCustomGeometry(
                pose, yomi, (p, buf) -> ResourceManager.yomi.render(p, buf, light, -1));
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.75F * offset, -0.5F * offset, 1.75F * offset).scale(0.75F);
        body.part(m, ResourceManager.tesla_cannon, Parts.GUN, ResourceManager.tesla_cannon_tex);
        body.part(
                m, ResourceManager.tesla_cannon, Parts.EXTENSION, ResourceManager.tesla_cannon_tex);
        body.part(m, ResourceManager.tesla_cannon, Parts.COG, ResourceManager.tesla_cannon_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.tesla_cannon.partId("Gun");
        static final int EXTENSION = ResourceManager.tesla_cannon.partId("Extension");
        static final int COG = ResourceManager.tesla_cannon.partId("Cog");
        static final int CAPACITOR = ResourceManager.tesla_cannon.partId("Capacitor");
    }
}
