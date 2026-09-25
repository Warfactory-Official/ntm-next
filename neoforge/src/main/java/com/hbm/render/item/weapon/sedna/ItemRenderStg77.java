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

public class ItemRenderStg77 extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.stg77_tex);

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -1F * offset, 2.5F * offset, 0, -5.75 / 8D, 2);
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0.66F;
    }

    @Override
    protected float getBaseFOV(ItemStack stack) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        return 70F - aimingProgress * 65;
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        if (ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
        double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
        double[] safety = HbmAnimations.getRelevantTransformation("SAFETY");

        double[] inspectGun = HbmAnimations.getRelevantTransformation("INSPECT_GUN");
        double[] inspectBarrel = HbmAnimations.getRelevantTransformation("INSPECT_BARREL");
        double[] inspectMove = HbmAnimations.getRelevantTransformation("INSPECT_MOVE");
        double[] inspectLever = HbmAnimations.getRelevantTransformation("INSPECT_LEVER");

        pose.translate(0, -1, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 4);

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 4);

        pose.translate(0, 0, recoil[2]);

        pose.pushPose();

        pose.mulPose(Axis.ZP.rotationDegrees((float) inspectGun[2]));
        pose.mulPose(Axis.XP.rotationDegrees((float) inspectGun[0]));

        HbmAnimations.applyRelevantTransformation(pose, "Gun");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.GUN, light);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Magazine");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.MAGAZINE, light);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) inspectLever[2]));
        HbmAnimations.applyRelevantTransformation(pose, "Lever");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.LEVER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, bolt[2]);
        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Breech");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.BREECH, light);
        pose.popPose();
        pose.translate(0.125, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) handle[2]));
        pose.translate(-0.125, 0, 0);
        HbmAnimations.applyRelevantTransformation(pose, "Handle");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.PART_HANDLE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(safety[0], 0, 0);
        HbmAnimations.applyRelevantTransformation(pose, "Safety");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.PART_SAFETY, light);
        pose.popPose();

        pose.popPose();

        pose.pushPose();
        pose.translate(inspectMove[0], inspectMove[1], inspectMove[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) inspectBarrel[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) inspectBarrel[2]));
        HbmAnimations.applyRelevantTransformation(pose, "Gun");
        HbmAnimations.applyRelevantTransformation(pose, "Barrel");
        submitPart(collector, pose, body, ResourceManager.stg77, Parts.BARREL, light);
        pose.popPose();

        double smokeScale = 0.75;

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 7.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(0.25F, 0.25F, 0.25F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-5 + gun.shotRand * 10)));
        renderGapFlash(collector, pose, gun.lastShot[0]);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1F * offset, 2.5F * offset).scale(0.5F);
        body.part(m, ResourceManager.stg77, Parts.GUN, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.MAGAZINE, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.LEVER, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.BREECH, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.PART_HANDLE, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.PART_SAFETY, ResourceManager.stg77_tex);
        body.part(m, ResourceManager.stg77, Parts.BARREL, ResourceManager.stg77_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.stg77.partId("Gun");
        static final int MAGAZINE = ResourceManager.stg77.partId("Magazine");
        static final int LEVER = ResourceManager.stg77.partId("Lever");
        static final int BREECH = ResourceManager.stg77.partId("Breech");
        static final int PART_HANDLE = ResourceManager.stg77.partId("Handle");
        static final int PART_SAFETY = ResourceManager.stg77.partId("Safety");
        static final int BARREL = ResourceManager.stg77.partId("Barrel");
    }
}
