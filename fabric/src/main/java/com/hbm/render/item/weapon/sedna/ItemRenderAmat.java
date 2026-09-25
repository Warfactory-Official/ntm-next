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

public class ItemRenderAmat extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;
    private final RenderType attachments = RenderTypes.entityCutout(ResourceManager.g3_attachments);

    public ItemRenderAmat(Identifier texture) {
        this.texture = texture;
        this.body = RenderTypes.entityCutout(texture);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        return fov * (1 - aimingProgress * (isScoped(stack) ? 0.8F : 0.33F));
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        standardAimingTransform(
                stack, pose, -1F * offset, -1F * offset, 3.25F * offset, 0, -4.875 / 8D, 1.875);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        boolean isScoped = isScoped(stack);

        if (isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1)
            return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        boolean deployed =
                HbmAnimations.getRelevantAnim(0) == null
                        || HbmAnimations.getRelevantAnim(0).animation.getBus("BIPOD") == null;
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] bipod = HbmAnimations.getRelevantTransformation("BIPOD");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
        double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] scopeThrow = HbmAnimations.getRelevantTransformation("SCOPE_THROW");
        double[] scopeSpin = HbmAnimations.getRelevantTransformation("SCOPE_SPIN");

        pose.translate(0, 0, recoil[2]);

        pose.translate(0, -3, -8);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 3, 8);

        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_GUN, light);

        if (isScoped(stack)) {
            pose.pushPose();
            pose.translate(scopeThrow[0], scopeThrow[1], scopeThrow[2]);
            pose.translate(0, 1.5, -4.5);
            pose.mulPose(Axis.XP.rotationDegrees((float) scopeSpin[0]));
            pose.translate(0, -1.5, 4.5);
            submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_SCOPE, light);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(0, 0.625, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) boltTurn[2]));
        pose.translate(0, -0.625, 0);
        pose.translate(0, 0, boltPull[2]);
        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_BOLT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_MAGAZINE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.3125, -0.625, -1);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (deployed ? 25 : bipod[1])));
        pose.translate(-0.3125, 0.625, 1);
        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_BIPOD_HINGE_LEFT, light);
        pose.translate(0.3125, -0.625, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) (deployed ? 80 : bipod[0])));
        pose.translate(-0.3125, 0.625, 1);
        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_BIPOD_LEFT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.3125, -0.625, -1);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (deployed ? -25 : -bipod[1])));
        pose.translate(0.3125, 0.625, 1);
        submitPart(
                collector, pose, body, ResourceManager.amat, Parts.AMAT_BIPOD_HINGE_RIGHT, light);
        pose.translate(-0.3125, -0.625, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) (deployed ? 80 : bipod[0])));
        pose.translate(0.3125, 0.625, 1);
        submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_BIPOD_RIGHT, light);
        pose.popPose();

        if (isSilenced(stack)) {
            pose.translate(0, 0.625, -4.3125);
            pose.scale(1.25F, 1.25F, 1.25F);
            submitPart(collector, pose, attachments, ResourceManager.g3, Parts.G3_SILENCER, light);
        } else {
            submitPart(collector, pose, body, ResourceManager.amat, Parts.AMAT_MUZZLE_BRAKE, light);

            double smokeScale = 0.5;

            pose.pushPose();
            pose.translate(0, 0.625, 12);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
            renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 1D, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0.5, 11);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.75F, 0.75F, 0.75F);
            renderGapFlash(collector, pose, gun.lastShot[0]);
            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1F * offset, -1F * offset, 3.25F * offset).scale(0.375F);
        body.part(m, ResourceManager.amat, Parts.AMAT_GUN, texture);
        if (isScoped(stack)) body.part(m, ResourceManager.amat, Parts.AMAT_SCOPE, texture);
        body.part(m, ResourceManager.amat, Parts.AMAT_BOLT, texture);
        body.part(m, ResourceManager.amat, Parts.AMAT_MAGAZINE, texture);

        Matrix4f left =
                new Matrix4f(m)
                        .translate(0.3125F, -0.625F, -1F)
                        .rotate(Axis.ZP.rotationDegrees(25F))
                        .translate(-0.3125F, 0.625F, 1F);
        body.part(left, ResourceManager.amat, Parts.AMAT_BIPOD_HINGE_LEFT, texture);
        left.translate(0.3125F, -0.625F, -1F)
                .rotate(Axis.XP.rotationDegrees(80F))
                .translate(-0.3125F, 0.625F, 1F);
        body.part(left, ResourceManager.amat, Parts.AMAT_BIPOD_LEFT, texture);

        Matrix4f right =
                new Matrix4f(m)
                        .translate(-0.3125F, -0.625F, -1F)
                        .rotate(Axis.ZP.rotationDegrees(-25F))
                        .translate(0.3125F, 0.625F, 1F);
        body.part(right, ResourceManager.amat, Parts.AMAT_BIPOD_HINGE_RIGHT, texture);
        right.translate(-0.3125F, -0.625F, -1F)
                .rotate(Axis.XP.rotationDegrees(80F))
                .translate(0.3125F, 0.625F, 1F);
        body.part(right, ResourceManager.amat, Parts.AMAT_BIPOD_RIGHT, texture);

        if (isSilenced(stack)) {
            body.part(
                    new Matrix4f(m).translate(0F, 0.625F, -4.3125F).scale(1.25F),
                    ResourceManager.g3,
                    Parts.G3_SILENCER,
                    ResourceManager.g3_attachments);
        } else {
            body.part(m, ResourceManager.amat, Parts.AMAT_MUZZLE_BRAKE, texture);
        }
    }

    public boolean isScoped(ItemStack stack) {
        return true;
    }

    public boolean isSilenced(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_AMAT_PENANCE.get()
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
    }

    private static final class Parts {
        static final int AMAT_GUN = ResourceManager.amat.partId("Gun");
        static final int AMAT_SCOPE = ResourceManager.amat.partId("Scope");
        static final int AMAT_BOLT = ResourceManager.amat.partId("Bolt");
        static final int AMAT_MAGAZINE = ResourceManager.amat.partId("Magazine");
        static final int AMAT_BIPOD_HINGE_LEFT = ResourceManager.amat.partId("BipodHingeLeft");
        static final int AMAT_BIPOD_LEFT = ResourceManager.amat.partId("BipodLeft");
        static final int AMAT_BIPOD_HINGE_RIGHT = ResourceManager.amat.partId("BipodHingeRight");
        static final int AMAT_BIPOD_RIGHT = ResourceManager.amat.partId("BipodRight");
        static final int G3_SILENCER = ResourceManager.g3.partId("Silencer");
        static final int AMAT_MUZZLE_BRAKE = ResourceManager.amat.partId("MuzzleBrake");
    }
}
