// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.BobMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMK108 extends ItemRenderWeaponBase {
    private static final double[] ANGLES_LOADED = {0, 0, -5, 0, -5, 60, 45, -10, 0};
    private static final double[] ANGLES_UNLOADED = {0, -30, -60, -45, -45, 0, 0, 0, 0};
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.mk108_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        standardAimingTransform(
                stack, pose, -1F * offset, -1.5F * offset, 2.5F * offset, -0.75F, -0.75F, 1.5F);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        boolean doesYeet =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("GRENH1") != null;
        boolean doesCycle =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("CYCLE") != null;
        boolean reloading =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("BELT") != null;
        boolean useShellCount =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("SHELLS") != null;
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
        double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] lid = HbmAnimations.getRelevantTransformation("LID");
        double[] belt = HbmAnimations.getRelevantTransformation("BELT");
        double[] drum = HbmAnimations.getRelevantTransformation("DRUM");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] shellCount = HbmAnimations.getRelevantTransformation("SHELLS");

        if (doesYeet) {
            double[][] horizontal =
                    new double[][] {
                        HbmAnimations.getRelevantTransformation("GRENH1"),
                        HbmAnimations.getRelevantTransformation("GRENH2"),
                        HbmAnimations.getRelevantTransformation("GRENH3"),
                    };
            double[][] vertical =
                    new double[][] {
                        HbmAnimations.getRelevantTransformation("GRENV1"),
                        HbmAnimations.getRelevantTransformation("GRENV2"),
                        HbmAnimations.getRelevantTransformation("GRENV3"),
                    };
            double[][] spin =
                    new double[][] {
                        HbmAnimations.getRelevantTransformation("GRENS1"),
                        HbmAnimations.getRelevantTransformation("GRENS2"),
                        HbmAnimations.getRelevantTransformation("GRENS3"),
                    };

            for (int i = 0; i < 3; i++) {
                if (horizontal[i][0] <= -4) continue;
                pose.pushPose();
                pose.translate(horizontal[i][0], vertical[i][1], 0);
                pose.translate(0, 0, -2.3125);
                pose.mulPose(Axis.XN.rotationDegrees(90));
                pose.mulPose(Axis.YN.rotationDegrees((float) spin[i][0]));
                pose.translate(0, 0, 2.3125);
                submitPart(collector, pose, body, ResourceManager.mk108, Parts.GRENADE, light);
                pose.popPose();
            }
        }

        pose.translate(0, -1, -8);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 8);

        pose.translate(0, 1, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, -1, 4);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.mk108, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 0, barrel[2] * 2);
        submitPart(collector, pose, body, ResourceManager.mk108, Parts.PART_BARREL, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.6875, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) lid[0]));
        pose.translate(0, -0.6875, 1);
        submitPart(collector, pose, body, ResourceManager.mk108, Parts.PART_LID, light);
        pose.popPose();

        pose.pushPose();

        pose.translate(drum[0], drum[1], drum[2]);
        submitPart(collector, pose, body, ResourceManager.mk108, Parts.PART_DRUM, light);

        double reloadProgress = !reloading ? 1D : belt[0];
        double cycleProgress = !doesCycle ? 1 : cycle[0];
        double[][] links = beltLinks(reloadProgress, cycleProgress);

        int shellAmount =
                useShellCount
                        ? (int) shellCount[0]
                        : gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null);

        for (int i = 0; i < links.length; i++) {
            renderShell(
                    collector,
                    pose,
                    light,
                    links[i][0],
                    links[i][1],
                    links[i][2],
                    loaded(i, shellAmount));
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8.125);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 50, 5);
        pose.popPose();
    }

    private void renderShell(
            SubmitNodeCollector collector,
            PoseStack pose,
            int light,
            double x,
            double y,
            double rot,
            boolean shell) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot));
        submitPart(collector, pose, body, ResourceManager.mk108, Parts.PART_BELT, light);
        if (shell) submitPart(collector, pose, body, ResourceManager.mk108, Parts.GRENADE, light);
        pose.popPose();
    }

    private static double[][] beltLinks(double reloadProgress, double cycleProgress) {
        double p = 0.0625D;
        double x = p * 22;
        double y = p * -46;
        double angle = 0;
        double vecX = 0, vecY = 0.53125;

        double[][] shells = new double[ANGLES_LOADED.length][3];
        for (int i = 0; i < ANGLES_LOADED.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = BobMathUtil.interp(ANGLES_UNLOADED[i], ANGLES_LOADED[i], reloadProgress);
            angle += delta;

            double rad = Math.toRadians(-delta);
            double nvx = vecX * Math.cos(rad) + vecY * Math.sin(rad);
            double nvy = vecY * Math.cos(rad) - vecX * Math.sin(rad);
            vecX = nvx;
            vecY = nvy;
            x += vecX;
            y += vecY;
        }

        double[][] links = new double[shells.length - 1][];
        for (int i = 0; i < links.length; i++) {
            double[] prevShell = shells[i];
            double[] nextShell = shells[i + 1];
            links[i] =
                    new double[] {
                        BobMathUtil.interp(prevShell[0], nextShell[0], cycleProgress),
                        BobMathUtil.interp(prevShell[1], nextShell[1], cycleProgress),
                        BobMathUtil.interp(prevShell[2], nextShell[2], cycleProgress)
                    };
        }
        return links;
    }

    private static boolean loaded(int link, int shellAmount) {
        return ANGLES_LOADED.length - link < shellAmount + 2;
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int shellAmount =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getAmount(stack, null);
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1F * offset, -1.5F * offset, 2.5F * offset).scale(0.375F);
        body.part(m, ResourceManager.mk108, Parts.GUN, ResourceManager.mk108_tex);
        body.part(m, ResourceManager.mk108, Parts.PART_BARREL, ResourceManager.mk108_tex);
        body.part(m, ResourceManager.mk108, Parts.PART_LID, ResourceManager.mk108_tex);
        body.part(m, ResourceManager.mk108, Parts.PART_DRUM, ResourceManager.mk108_tex);
        double[][] links = beltLinks(1D, 1D);
        for (int i = 0; i < links.length; i++) {
            Matrix4f link =
                    new Matrix4f(m)
                            .translate((float) links[i][0], (float) links[i][1], 0F)
                            .rotate(Axis.ZP.rotationDegrees((float) links[i][2]));
            body.part(link, ResourceManager.mk108, Parts.PART_BELT, ResourceManager.mk108_tex);
            if (loaded(i, shellAmount)) {
                body.part(link, ResourceManager.mk108, Parts.GRENADE, ResourceManager.mk108_tex);
            }
        }
    }

    private static final class Parts {
        static final int GRENADE = ResourceManager.mk108.partId("Grenade");
        static final int GUN = ResourceManager.mk108.partId("Gun");
        static final int PART_BARREL = ResourceManager.mk108.partId("Barrel");
        static final int PART_LID = ResourceManager.mk108.partId("Lid");
        static final int PART_DRUM = ResourceManager.mk108.partId("Drum");
        static final int PART_BELT = ResourceManager.mk108.partId("Belt");
    }
}
