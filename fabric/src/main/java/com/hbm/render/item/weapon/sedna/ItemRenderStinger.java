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

public class ItemRenderStinger extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.stinger_tex);
    private final RenderType rocket = RenderTypes.entityCutout(ResourceManager.panzerschreck_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -3.75F * offset,
                -9F * offset,
                -3.5F * offset,
                -2.625F * offset,
                -6.5,
                -8.5F);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        if (ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 1.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
        double[] rocketAnim = HbmAnimations.getRelevantTransformation("ROCKET");

        pose.translate(0, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 1);

        pose.translate(0, -4, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) reload[0]));
        pose.translate(0, 4, 3);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180));
        collector.submitCustomGeometry(
                pose, body, (p, buffer) -> ResourceManager.stinger.render(p, buffer, light, -1));
        pose.popPose();

        pose.pushPose();
        pose.translate(rocketAnim[0], rocketAnim[1] + 3.5, rocketAnim[2] - 3);
        submitPart(
                collector, pose, rocket, ResourceManager.panzerschreck, Parts.PART_ROCKET, light);

        pose.pushPose();
        String label = "Not accurate";
        Font font = Minecraft.getInstance().font;
        float f3 = 0.04F;
        pose.translate(0.025F, -0.5F, (font.width(label) / 2) * f3 - 3);
        pose.scale(f3, -f3, f3);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XN.rotationDegrees(45));
        collector.submitText(
                pose,
                0,
                0,
                Component.literal(label).getVisualOrderText(),
                false,
                Font.DisplayMode.NORMAL,
                FULL_BRIGHT,
                0xFFFF0000,
                0,
                0);
        pose.popPose();

        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 6.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 150, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -3.75F * offset, -9F * offset, -3.5F * offset).scale(1.5F);
        body.model(
                new Matrix4f(m).rotate(Axis.YP.rotationDegrees(180)),
                ResourceManager.stinger,
                ResourceManager.stinger_tex);
        Matrix4f rocket = new Matrix4f(m).translate(0F, 3.5F, -3F);
        body.part(
                rocket,
                ResourceManager.panzerschreck,
                Parts.PART_ROCKET,
                ResourceManager.panzerschreck_tex);
        float f3 = 0.04F;
        Matrix4f label =
                new Matrix4f()
                        .scale(f3, -f3, f3)
                        .rotate(Axis.YP.rotationDegrees(90))
                        .rotate(Axis.XN.rotationDegrees(45));
        body.text(
                new Matrix4f(rocket).translate(0.025F, -0.5F, -3F),
                new Vector3f(0F, 0F, f3),
                label,
                "Not accurate",
                0xFFFF0000);
    }

    private static final class Parts {
        static final int PART_ROCKET = ResourceManager.panzerschreck.partId("Rocket");
    }
}
