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

public class ItemRenderFlaregun extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.flaregun_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.25F * offset, -1.5F * offset, 2F * offset, 0, -5.5 / 8D, 0.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.125D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
        double[] open = HbmAnimations.getRelevantTransformation("OPEN");
        double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
        double[] flip = HbmAnimations.getRelevantTransformation("FLIP");

        pose.translate(recoil[0], recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));
        pose.mulPose(Axis.XP.rotationDegrees((float) flip[0]));

        pose.translate(0, 0, -8);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 8);

        submitPart(collector, pose, body, ResourceManager.flaregun, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 1.8125, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) (hammer[0] - 15)));
        pose.translate(0, -1.8125, 4);
        submitPart(collector, pose, body, ResourceManager.flaregun, Parts.PART_HAMMER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 2.156, 1.78);
        pose.mulPose(Axis.XP.rotationDegrees((float) open[0]));
        pose.translate(0, -2.156, -1.78);
        submitPart(collector, pose, body, ResourceManager.flaregun, Parts.BARREL, light);
        pose.translate(shell[0], shell[1], shell[2]);
        submitPart(collector, pose, body, ResourceManager.flaregun, Parts.FLARE, light);
        pose.popPose();

        double smokeScale = 0.5;

        pose.pushPose();
        pose.translate(0, 4, 9);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 2.5D, light);
        pose.translate(0, 0, 0.1);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 2D, light);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.25F * offset, -1.5F * offset, 2F * offset).scale(0.125F);
        body.part(m, ResourceManager.flaregun, Parts.GUN, ResourceManager.flaregun_tex);
        body.part(
                new Matrix4f(m)
                        .translate(0F, 1.8125F, -4F)
                        .rotate(Axis.XP.rotationDegrees(-15F))
                        .translate(0F, -1.8125F, 4F),
                ResourceManager.flaregun,
                Parts.PART_HAMMER,
                ResourceManager.flaregun_tex);
        body.part(m, ResourceManager.flaregun, Parts.BARREL, ResourceManager.flaregun_tex);
        body.part(m, ResourceManager.flaregun, Parts.FLARE, ResourceManager.flaregun_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.flaregun.partId("Gun");
        static final int PART_HAMMER = ResourceManager.flaregun.partId("Hammer");
        static final int BARREL = ResourceManager.flaregun.partId("Barrel");
        static final int FLARE = ResourceManager.flaregun.partId("Flare");
    }
}
