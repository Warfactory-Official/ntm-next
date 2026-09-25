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
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ItemRenderBolter extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.bolter_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -2F * offset, 2.5F * offset, 0, -10.5 / 8D, 1.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        pose.mulPose(Axis.YP.rotationDegrees(180));

        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[0] * 5)));
        pose.translate(0, 0, recoil[0]);

        double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
        pose.translate(0, tilt[0], 3);
        pose.mulPose(Axis.XP.rotationDegrees((float) (tilt[0] * 35)));
        pose.translate(0, 0, -3);

        submitPart(collector, pose, body, ResourceManager.bolter, Parts.BODY, light);

        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        pose.pushPose();
        pose.translate(0, 0, 5);
        pose.mulPose(Axis.XN.rotationDegrees((float) (mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1))));
        pose.translate(0, 0, -5);
        submitPart(collector, pose, body, ResourceManager.bolter, Parts.PART_MAG, light);
        if (mag[2] != 1)
            submitPart(collector, pose, body, ResourceManager.bolter, Parts.BULLET, light);
        pose.popPose();

        pose.pushPose();
        Font font = Minecraft.getInstance().font;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        String s =
                gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null)
                        + "";
        float f3 = 0.04F;
        pose.translate(0.025F - (font.width(s) / 2) * 0.04F, 2.11F, 2.91F);
        pose.scale(f3, -f3, f3);
        pose.mulPose(Axis.XP.rotationDegrees(45));
        collector.submitText(
                pose,
                0,
                0,
                Component.literal(s).getVisualOrderText(),
                false,
                Font.DisplayMode.NORMAL,
                FULL_BRIGHT,
                0xFFFF0000,
                0,
                0);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -2F * offset, 2.5F * offset)
                        .scale(0.5F)
                        .rotate(Axis.YP.rotationDegrees(180));
        body.part(m, ResourceManager.bolter, Parts.BODY, ResourceManager.bolter_tex);
        body.part(m, ResourceManager.bolter, Parts.PART_MAG, ResourceManager.bolter_tex);
        body.part(m, ResourceManager.bolter, Parts.BULLET, ResourceManager.bolter_tex);

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        String s =
                gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null)
                        + "";
        float f3 = 0.04F;
        body.text(
                m.translate(0.025F, 2.11F, 2.91F),
                new Vector3f(-0.04F, 0F, 0F),
                new Matrix4f().scale(f3, -f3, f3).rotate(Axis.XP.rotationDegrees(45)),
                s,
                0xFFFF0000);
    }

    private static final class Parts {
        static final int BODY = ResourceManager.bolter.partId("Body");
        static final int PART_MAG = ResourceManager.bolter.partId("Mag");
        static final int BULLET = ResourceManager.bolter.partId("Bullet");
    }
}
