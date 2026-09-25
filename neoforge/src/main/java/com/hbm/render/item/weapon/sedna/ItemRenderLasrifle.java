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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderLasrifle extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.lasrifle_tex);
    private final RenderType mods = RenderTypes.entityCutout(ResourceManager.lasrifle_mods_tex);

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        return fov * (1 - aimingProgress * (hasScope(stack) ? 0.75F : 0.66F));
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        if (hasScope(stack)) {
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.5F * offset,
                    2.5F * offset,
                    0,
                    -7.375 / 8D,
                    0.75);
        } else {
            standardAimingTransform(
                    stack, pose, -1.5F * offset, -1.5F * offset, 2.5F * offset, 0, -5.25 / 8D, 1);
        }
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        if (hasScope(stack)
                && ItemGunBaseNT.prevAimingProgress == 1
                && ItemGunBaseNT.aimingProgress == 1) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.3125D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");

        pose.translate(0, -1, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 6);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_GUN, light);
        submitPart(collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_STOCK, light);
        if (hasScope(stack))
            submitPart(
                    collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_SCOPE, light);

        pose.pushPose();
        pose.translate(0, -0.375, 2.375);
        pose.mulPose(Axis.XP.rotationDegrees((float) lever[0]));
        pose.translate(0, 0.375, -2.375);
        submitPart(collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_LEVER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_BATTERY, light);
        pose.popPose();

        if (!hasShotgun(stack))
            submitPart(
                    collector, pose, body, ResourceManager.lasrifle, Parts.LASRIFLE_BARREL, light);
        if (hasShotgun(stack))
            submitPart(
                    collector,
                    pose,
                    mods,
                    ResourceManager.lasrifle_mods,
                    Parts.LASRIFLE_MODS_BARREL_SHOTGUN,
                    light);
        if (hasCapacitor(stack))
            submitPart(
                    collector,
                    pose,
                    mods,
                    ResourceManager.lasrifle_mods,
                    Parts.LASRIFLE_MODS_UNDER_BARREL,
                    light);

        pose.pushPose();
        pose.translate(0, 1.5, 12);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderLaserFlash(collector, pose, gun.lastShot[0], 150, 1.5D, 0xff0000);
        pose.translate(0, 0, -0.25);
        renderLaserFlash(collector, pose, gun.lastShot[0], 150, 0.75D, 0xff8000);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        boolean shotgun = hasShotgun(stack);
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -1.5F * offset, 2.5F * offset).scale(0.3125F);
        body.part(m, ResourceManager.lasrifle, Parts.LASRIFLE_GUN, ResourceManager.lasrifle_tex);
        body.part(m, ResourceManager.lasrifle, Parts.LASRIFLE_STOCK, ResourceManager.lasrifle_tex);
        if (hasScope(stack))
            body.part(
                    m,
                    ResourceManager.lasrifle,
                    Parts.LASRIFLE_SCOPE,
                    ResourceManager.lasrifle_tex);
        body.part(m, ResourceManager.lasrifle, Parts.LASRIFLE_LEVER, ResourceManager.lasrifle_tex);
        body.part(
                m, ResourceManager.lasrifle, Parts.LASRIFLE_BATTERY, ResourceManager.lasrifle_tex);
        if (!shotgun)
            body.part(
                    m,
                    ResourceManager.lasrifle,
                    Parts.LASRIFLE_BARREL,
                    ResourceManager.lasrifle_tex);
        if (shotgun) {
            body.part(
                    m,
                    ResourceManager.lasrifle_mods,
                    Parts.LASRIFLE_MODS_BARREL_SHOTGUN,
                    ResourceManager.lasrifle_mods_tex);
        }
        if (hasCapacitor(stack)) {
            body.part(
                    m,
                    ResourceManager.lasrifle_mods,
                    Parts.LASRIFLE_MODS_UNDER_BARREL,
                    ResourceManager.lasrifle_mods_tex);
        }
    }

    public boolean hasScope(ItemStack stack) {
        return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_AUTO);
    }

    public boolean hasShotgun(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_SHOTGUN);
    }

    public boolean hasCapacitor(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_CAPACITOR);
    }

    private static final class Parts {
        static final int LASRIFLE_GUN = ResourceManager.lasrifle.partId("Gun");
        static final int LASRIFLE_STOCK = ResourceManager.lasrifle.partId("Stock");
        static final int LASRIFLE_SCOPE = ResourceManager.lasrifle.partId("Scope");
        static final int LASRIFLE_LEVER = ResourceManager.lasrifle.partId("Lever");
        static final int LASRIFLE_BATTERY = ResourceManager.lasrifle.partId("Battery");
        static final int LASRIFLE_BARREL = ResourceManager.lasrifle.partId("Barrel");
        static final int LASRIFLE_MODS_BARREL_SHOTGUN =
                ResourceManager.lasrifle_mods.partId("BarrelShotgun");
        static final int LASRIFLE_MODS_UNDER_BARREL =
                ResourceManager.lasrifle_mods.partId("UnderBarrel");
    }
}
