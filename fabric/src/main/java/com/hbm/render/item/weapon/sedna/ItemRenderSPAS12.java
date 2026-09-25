// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.particle.SpentCasing;
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

public class ItemRenderSPAS12 extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.spas_12_tex);
    private final RenderType casings = RenderTypes.entityCutout(ResourceManager.casings_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.25F * offset, -1.75F * offset, -0.5F * offset, 0, 0, 0);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);
        pose.mulPose(Axis.YP.rotationDegrees(180));

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));

        HbmAnimations.applyRelevantTransformation(pose, "MainBody");
        submitPart(collector, pose, body, ResourceManager.spas_12, Parts.MAIN_BODY, light);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "PumpGrip");
        submitPart(collector, pose, body, ResourceManager.spas_12, Parts.PUMP_GRIP, light);
        pose.popPose();

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Shell");
        SpentCasing casing =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getCasing(stack, Minecraft.getInstance().player.getInventory());
        int color0 = SpentCasing.COLOR_CASE_BRASS;
        int color1 = SpentCasing.COLOR_CASE_BRASS;

        if (casing != null) {
            int[] colors = casing.getColors();
            color0 = colors[0];
            color1 = colors[colors.length > 1 ? 1 : 0];
        }

        int shellColor = 0xFF000000 | color1;
        int shellForeColor = 0xFF000000 | color0;
        collector.submitCustomGeometry(
                pose,
                casings,
                (p, buffer) -> {
                    ResourceManager.spas_12.renderPart(p, buffer, light, shellColor, Parts.SHELL);
                    ResourceManager.spas_12.renderPart(
                            p, buffer, light, shellForeColor, Parts.SHELL_FORE);
                });

        double smokeScale = 0.25;

        pose.pushPose();
        pose.translate(0, 1.5, -11);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.75D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.5, -11);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();

        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.25F * offset, -1.75F * offset, -0.5F * offset)
                        .scale(0.5F)
                        .rotate(Axis.YP.rotationDegrees(180));
        body.part(m, ResourceManager.spas_12, Parts.MAIN_BODY, ResourceManager.spas_12_tex);
        body.part(m, ResourceManager.spas_12, Parts.PUMP_GRIP, ResourceManager.spas_12_tex);
        SpentCasing casing =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getCasing(stack, null);
        int color0 = SpentCasing.COLOR_CASE_BRASS;
        int color1 = SpentCasing.COLOR_CASE_BRASS;
        if (casing != null) {
            int[] colors = casing.getColors();
            color0 = colors[0];
            color1 = colors[colors.length > 1 ? 1 : 0];
        }
        body.part(
                m,
                ResourceManager.spas_12,
                Parts.SHELL,
                ResourceManager.casings_tex,
                0xFF000000 | color1,
                false);
        body.part(
                m,
                ResourceManager.spas_12,
                Parts.SHELL_FORE,
                ResourceManager.casings_tex,
                0xFF000000 | color0,
                false);
    }

    private static final class Parts {
        static final int MAIN_BODY = ResourceManager.spas_12.partId("MainBody");
        static final int PUMP_GRIP = ResourceManager.spas_12.partId("PumpGrip");
        static final int SHELL = ResourceManager.spas_12.partId("Shell");
        static final int SHELL_FORE = ResourceManager.spas_12.partId("ShellFore");
    }
}
