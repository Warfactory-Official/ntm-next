// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.main.ResourceManager;
import com.hbm.particle.SpentCasing;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
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

public class ItemRenderCongoLake extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.congolake_tex);
    private final RenderType casings = RenderTypes.entityCutout(ResourceManager.casings_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -2F * offset, 1.25F * offset, 0, -10 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        HbmAnimations.applyRelevantTransformation(pose, "Gun");
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.GUN, light);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Pump");
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.PUMP, light);
        pose.popPose();

        pose.pushPose();
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        HbmAnimations.applyRelevantTransformation(pose, "Sight");
        pose.translate(0, 2.125, 3);
        pose.mulPose(Axis.XP.rotationDegrees(aimingProgress * -90));
        pose.translate(0, -2.125, -3);
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.SIGHT, light);
        pose.popPose();

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Loop");
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.LOOP, light);
        pose.popPose();

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "GuardOuter");
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.GUARD_OUTER, light);
        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "GuardInner");
        submitPart(collector, pose, body, ResourceManager.congolake, Parts.GUARD_INNER, light);
        pose.popPose();
        pose.popPose();

        pose.pushPose();
        IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (ItemGunBaseNT.getLastAnim(stack, 0) != GunAnimation.INSPECT
                || mag.getAmount(stack, Minecraft.getInstance().player.getInventory()) > 0) {

            HbmAnimations.applyRelevantTransformation(pose, "Shell");

            SpentCasing casing =
                    mag.getCasing(stack, Minecraft.getInstance().player.getInventory());
            int[] colors =
                    casing != null ? casing.getColors() : new int[] {SpentCasing.COLOR_CASE_40MM};

            int shellColor = 0xFF000000 | (colors[0] & 0xFFFFFF);
            submitPart(
                    collector,
                    pose,
                    casings,
                    ResourceManager.congolake,
                    Parts.SHELL,
                    light,
                    shellColor);

            int shellForeColor =
                    0xFF000000 | ((colors.length > 1 ? colors[1] : colors[0]) & 0xFFFFFF);
            submitPart(
                    collector,
                    pose,
                    casings,
                    ResourceManager.congolake,
                    Parts.SHELL_FORE,
                    light,
                    shellForeColor);
        }
        pose.popPose();

        double smokeScale = 0.25;

        pose.pushPose();
        pose.translate(0, 1.75, 4.25);
        double[] transform = HbmAnimations.getRelevantTransformation("Gun");
        pose.mulPose(Axis.ZN.rotationDegrees((float) transform[5]));
        pose.mulPose(Axis.YN.rotationDegrees((float) transform[4]));
        pose.mulPose(Axis.XN.rotationDegrees((float) transform[3]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 1D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.75, 4.25);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.5F, 0.5F, 0.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 150, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -2F * offset, 1.25F * offset).scale(0.5F);
        body.part(m, ResourceManager.congolake, Parts.GUN, ResourceManager.congolake_tex);
        body.part(m, ResourceManager.congolake, Parts.PUMP, ResourceManager.congolake_tex);
        body.part(m, ResourceManager.congolake, Parts.SIGHT, ResourceManager.congolake_tex);
        body.part(m, ResourceManager.congolake, Parts.LOOP, ResourceManager.congolake_tex);
        body.part(m, ResourceManager.congolake, Parts.GUARD_OUTER, ResourceManager.congolake_tex);
        body.part(m, ResourceManager.congolake, Parts.GUARD_INNER, ResourceManager.congolake_tex);

        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        if (ItemGunBaseNT.getLastAnim(stack, 0) == GunAnimation.INSPECT
                && mag.getAmount(stack, null) <= 0) return;
        SpentCasing casing = mag.getCasing(stack, null);
        int[] colors =
                casing != null ? casing.getColors() : new int[] {SpentCasing.COLOR_CASE_40MM};
        body.part(
                m,
                ResourceManager.congolake,
                Parts.SHELL,
                ResourceManager.casings_tex,
                0xFF000000 | (colors[0] & 0xFFFFFF),
                false);
        body.part(
                m,
                ResourceManager.congolake,
                Parts.SHELL_FORE,
                ResourceManager.casings_tex,
                0xFF000000 | ((colors.length > 1 ? colors[1] : colors[0]) & 0xFFFFFF),
                false);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.congolake.partId("Gun");
        static final int PUMP = ResourceManager.congolake.partId("Pump");
        static final int SIGHT = ResourceManager.congolake.partId("Sight");
        static final int LOOP = ResourceManager.congolake.partId("Loop");
        static final int GUARD_OUTER = ResourceManager.congolake.partId("GuardOuter");
        static final int GUARD_INNER = ResourceManager.congolake.partId("GuardInner");
        static final int SHELL = ResourceManager.congolake.partId("Shell");
        static final int SHELL_FORE = ResourceManager.congolake.partId("ShellFore");
    }
}
