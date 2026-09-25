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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ItemRenderSexy extends ItemRenderWeaponBase {
    protected final RenderType body;
    private final Identifier texture;
    private final RenderType whiskey = RenderTypes.entityCutout(ResourceManager.whiskey_tex);

    public ItemRenderSexy(Identifier texture) {
        this.body = RenderTypes.entityCutout(texture);
        this.texture = texture;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        standardAimingTransform(
                stack, pose, -1F * offset, -0.75F * offset, 3F * offset, -0.5F, -0.5F, 2F);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        boolean doesCycle =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("CYCLE") != null;
        boolean reloading =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("BELT") != null;
        boolean useShellCount =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("SHELLS") != null;
        boolean girldinner =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("BOTTLE") != null;
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lower = HbmAnimations.getRelevantTransformation("LOWER");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
        double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
        double[] hood = HbmAnimations.getRelevantTransformation("HOOD");
        double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
        double[] belt = HbmAnimations.getRelevantTransformation("BELT");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] magRot = HbmAnimations.getRelevantTransformation("MAGROT");
        double[] shellCount = HbmAnimations.getRelevantTransformation("SHELLS");
        double[] bottle = HbmAnimations.getRelevantTransformation("BOTTLE");
        double[] sippy = HbmAnimations.getRelevantTransformation("SIP");

        if (girldinner) {
            pose.pushPose();
            pose.translate(bottle[0], bottle[1], bottle[2]);
            pose.translate(0, 2, 0);
            pose.mulPose(Axis.XP.rotationDegrees((float) sippy[0]));
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(-15));
            pose.translate(0, -2, 0);
            pose.scale(1.5F, 1.5F, 1.5F);
            collector.submitCustomGeometry(
                    pose,
                    whiskey,
                    (p, buffer) -> ResourceManager.whiskey.render(p, buffer, light, -1));
            pose.popPose();
        }

        pose.translate(0, -1, -8);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 8);

        pose.translate(0, 0, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) lower[0]));
        pose.translate(0, 0, 6);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.sexy, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 0, barrel[2]);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.PART_BARREL, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, -0.375);
        pose.scale(1, 1, (float) (1 + 0.457247371D * barrel[2]));
        pose.translate(0, 0, 0.375);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.RECOIL_SPRING, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.4375, -2.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) hood[0]));
        pose.translate(0, -0.4375, 2.875);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.PART_HOOD, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.46875, -6.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) (lever[2] * 60)));
        pose.translate(0, -0.46875, 6.875);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.PART_LEVER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, -6.75);
        pose.scale(1, 1, (float) (1 - lever[2] * 0.25));
        pose.translate(0, 0, 6.75);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.LOCK_SPRING, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        pose.translate(0, -1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) magRot[2]));
        pose.translate(0, 1, 0);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.MAGAZINE, light);

        double reloadProgress = !reloading ? 1D : belt[0];
        double cycleProgress = !doesCycle ? 1 : cycle[0];

        double[][] shells = beltShells(reloadProgress);

        int shellAmount =
                useShellCount
                        ? (int) shellCount[0]
                        : gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null);

        for (int i = 0; i < shells.length - 1; i++) {
            double[] prevShell = shells[i];
            double[] nextShell = shells[i + 1];
            renderShell(
                    collector,
                    pose,
                    light,
                    prevShell[0],
                    nextShell[0],
                    prevShell[1],
                    nextShell[1],
                    prevShell[2],
                    nextShell[2],
                    shells.length - i < shellAmount + 2,
                    cycleProgress);
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 150, 7.5);
        pose.popPose();
    }

    private static double[][] beltShells(double reloadProgress) {
        double p = 0.0625D;
        double x = p * 17;
        double y = p * -26;
        double angle = 0;
        Vec3 vec = new Vec3(0, 0.4375, 0);
        double[] anglesLoaded = new double[] {0, 0, 20, 20, 50, 60, 70};
        double[] anglesUnloaded = new double[] {0, -10, -50, -60, -60, 0, 0};
        double[][] shells = new double[anglesLoaded.length][3];
        for (int i = 0; i < anglesLoaded.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = BobMathUtil.interp(anglesUnloaded[i], anglesLoaded[i], reloadProgress);
            angle += delta;
            vec = vec.zRot((float) Math.toRadians(-delta));
            x += vec.x;
            y += vec.y;
        }
        return shells;
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1F * offset, -0.75F * offset, 3F * offset).scale(0.375F);
        body.part(m, ResourceManager.sexy, Parts.GUN, texture);
        body.part(m, ResourceManager.sexy, Parts.PART_BARREL, texture);
        body.part(m, ResourceManager.sexy, Parts.RECOIL_SPRING, texture);
        body.part(m, ResourceManager.sexy, Parts.PART_HOOD, texture);
        body.part(m, ResourceManager.sexy, Parts.PART_LEVER, texture);
        body.part(m, ResourceManager.sexy, Parts.LOCK_SPRING, texture);
        body.part(m, ResourceManager.sexy, Parts.MAGAZINE, texture);
        double[][] shells = beltShells(1D);
        int shellAmount =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getAmount(stack, null);
        for (int i = 0; i < shells.length - 1; i++) {
            double[] shell = shells[i + 1];
            Matrix4f segment =
                    new Matrix4f(m)
                            .translate((float) shell[0], (float) (0.375 + shell[1]), 0F)
                            .rotate(Axis.ZP.rotationDegrees((float) shell[2]))
                            .translate(0F, -0.375F, 0F);
            body.part(segment, ResourceManager.sexy, Parts.PART_BELT, texture);
            if (shells.length - i < shellAmount + 2)
                body.part(segment, ResourceManager.sexy, Parts.SHELL, texture);
        }
    }

    public void renderShell(
            SubmitNodeCollector collector,
            PoseStack pose,
            int light,
            double x0,
            double x1,
            double y0,
            double y1,
            double rot0,
            double rot1,
            boolean shell,
            double interp) {
        renderShell(
                collector,
                pose,
                light,
                BobMathUtil.interp(x0, x1, interp),
                BobMathUtil.interp(y0, y1, interp),
                BobMathUtil.interp(rot0, rot1, interp),
                shell);
    }

    public void renderShell(
            SubmitNodeCollector collector,
            PoseStack pose,
            int light,
            double x,
            double y,
            double rot,
            boolean shell) {
        pose.pushPose();
        pose.translate(x, 0.375 + y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot));
        pose.translate(0, -0.375, 0);
        submitPart(collector, pose, body, ResourceManager.sexy, Parts.PART_BELT, light);
        if (shell) submitPart(collector, pose, body, ResourceManager.sexy, Parts.SHELL, light);
        pose.popPose();
    }

    private static final class Parts {
        static final int GUN = ResourceManager.sexy.partId("Gun");
        static final int PART_BARREL = ResourceManager.sexy.partId("Barrel");
        static final int RECOIL_SPRING = ResourceManager.sexy.partId("RecoilSpring");
        static final int PART_HOOD = ResourceManager.sexy.partId("Hood");
        static final int PART_LEVER = ResourceManager.sexy.partId("Lever");
        static final int LOCK_SPRING = ResourceManager.sexy.partId("LockSpring");
        static final int MAGAZINE = ResourceManager.sexy.partId("Magazine");
        static final int PART_BELT = ResourceManager.sexy.partId("Belt");
        static final int SHELL = ResourceManager.sexy.partId("Shell");
    }
}
