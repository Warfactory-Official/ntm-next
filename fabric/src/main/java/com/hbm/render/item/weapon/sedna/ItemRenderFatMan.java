// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.items.weapon.sedna.mags.IMagazine;
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

public class ItemRenderFatMan extends ItemRenderWeaponBase {
    private static final String BALEFIRE = "ammo_standard_nuke_balefire";
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.fatman_tex);

    public static void renderNuke(
            SubmitNodeCollector collector, PoseStack pose, int light, Object type) {
        if (type == XFactoryCatapult.nuke_balefire) {
            renderBalefire(collector, pose, light);
        } else {
            submitPart(
                    collector,
                    pose,
                    RenderTypes.entityCutout(ResourceManager.fatman_mininuke_tex),
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    light);
        }
    }

    public static void renderBalefire(SubmitNodeCollector collector, PoseStack pose, int light) {
        submitPart(
                collector,
                pose,
                RenderTypes.entityCutout(ResourceManager.fatman_balefire_tex),
                ResourceManager.fatman,
                Parts.MINI_NUKE,
                light);

        for (int k = 0; k < 3; k++) {
            submitPart(
                    collector,
                    pose,
                    WeaponRenderTypes.balefireGlint(ResourceManager.glint_bf_tex, k),
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    light,
                    WeaponRenderTypes.BALEFIRE_GLINT_TINT);
        }
    }

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
                -1.5F * offset,
                -1.25F * offset,
                0.5F * offset,
                -1F * offset,
                -1.25F * offset,
                0F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        boolean isLoaded =
                gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null)
                        > 0;

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lid = HbmAnimations.getRelevantTransformation("LID");
        double[] nuke = HbmAnimations.getRelevantTransformation("NUKE");
        double[] piston = HbmAnimations.getRelevantTransformation("PISTON");
        double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
        double[] gauge = HbmAnimations.getRelevantTransformation("GAUGE");

        pose.translate(0, 1, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, -1, 2);

        submitPart(collector, pose, body, ResourceManager.fatman, Parts.LAUNCHER, light);

        pose.pushPose();
        pose.translate(0, 0, handle[2]);
        submitPart(collector, pose, body, ResourceManager.fatman, Parts.PART_HANDLE, light);

        pose.translate(0.4375, -0.875, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) gauge[2]));
        pose.translate(-0.4375, 0.875, 0);
        submitPart(collector, pose, body, ResourceManager.fatman, Parts.PART_GAUGE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.25, 0.125, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) lid[2]));
        pose.translate(-0.25, -0.125, 0);
        submitPart(collector, pose, body, ResourceManager.fatman, Parts.PART_LID, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, piston[2]);
        if (!isLoaded && piston[2] == 0) pose.translate(0, 0, 3);
        submitPart(collector, pose, body, ResourceManager.fatman, Parts.PART_PISTON, light);
        pose.popPose();

        if (isLoaded || nuke[0] != 0 || nuke[1] != 0 || nuke[2] != 0) {
            pose.pushPose();
            pose.translate(nuke[0], nuke[1], nuke[2]);
            renderNuke(
                    collector,
                    pose,
                    light,
                    gun.getConfig(stack, 0)
                            .getReceivers(stack)[0]
                            .getMagazine(stack)
                            .getType(stack, null));
            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> magazine = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        boolean isLoaded = magazine.getAmount(stack, null) > 0;
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1.25F * offset, 0.5F * offset).scale(0.5F);
        body.part(m, ResourceManager.fatman, Parts.LAUNCHER, ResourceManager.fatman_tex);
        body.part(m, ResourceManager.fatman, Parts.PART_HANDLE, ResourceManager.fatman_tex);
        body.part(m, ResourceManager.fatman, Parts.PART_GAUGE, ResourceManager.fatman_tex);
        body.part(m, ResourceManager.fatman, Parts.PART_LID, ResourceManager.fatman_tex);
        body.part(
                isLoaded ? m : new Matrix4f(m).translate(0F, 0F, 3F),
                ResourceManager.fatman,
                Parts.PART_PISTON,
                ResourceManager.fatman_tex);
        if (!isLoaded) return;
        if (magazine.getType(stack, null) == XFactoryCatapult.nuke_balefire) {
            body.part(
                    m,
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    ResourceManager.fatman_balefire_tex);
            body.balefireGlint(m, ResourceManager.fatman, Parts.MINI_NUKE);
        } else {
            body.part(
                    m,
                    ResourceManager.fatman,
                    Parts.MINI_NUKE,
                    ResourceManager.fatman_mininuke_tex);
        }
    }

    private static final class Parts {
        static final int LAUNCHER = ResourceManager.fatman.partId("Launcher");
        static final int PART_HANDLE = ResourceManager.fatman.partId("Handle");
        static final int PART_GAUGE = ResourceManager.fatman.partId("Gauge");
        static final int PART_LID = ResourceManager.fatman.partId("Lid");
        static final int PART_PISTON = ResourceManager.fatman.partId("Piston");
        static final int MINI_NUKE = ResourceManager.fatman.partId("MiniNuke");
    }
}
