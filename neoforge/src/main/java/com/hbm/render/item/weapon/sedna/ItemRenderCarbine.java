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

public class ItemRenderCarbine extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.carbine_tex);
    private final RenderType scope = RenderTypes.entityCutout(ResourceManager.carbine_scope_tex);
    private final RenderType bayonet =
            RenderTypes.entityCutout(ResourceManager.carbine_bayonet_tex);

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
        return fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;

        if (this.isScoped(stack)) {
            standardAimingTransform(
                    stack, pose, -1.5F * offset, -1.5F * offset, 0.875F * offset, 0, -8 / 8D, 0.25);
        } else {
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.5F * offset,
                    0.875F * offset,
                    0,
                    -6.25 / 8D,
                    0.25);
        }
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        boolean isScoped = isScoped(stack);
        if (isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1)
            return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
        double[] rel = HbmAnimations.getRelevantTransformation("REL");
        double[] stab = HbmAnimations.getRelevantTransformation("STAB");

        pose.translate(0, -1, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 2);

        pose.translate(0, 0, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 2);

        pose.translate(stab[0], stab[1], stab[2]);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.carbine, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 0, slide[2]);
        submitPart(collector, pose, body, ResourceManager.carbine, Parts.PART_SLIDE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body, ResourceManager.carbine, Parts.MAGAZINE, light);
        pose.translate(rel[0], rel[1], rel[2]);
        if (bullet[0] != 1)
            submitPart(collector, pose, body, ResourceManager.carbine, Parts.PART_BULLET, light);
        pose.popPose();

        if (!isScoped(stack)) {
            submitPart(collector, pose, body, ResourceManager.carbine, Parts.IRON_SIGHT, light);
        } else {
            submitPart(collector, pose, scope, ResourceManager.carbine, Parts.SCOPE, light);
        }

        if (hasBayonet(stack)) {
            submitPart(collector, pose, bayonet, ResourceManager.carbine, Parts.BAYONET, light);
        }

        pose.pushPose();
        pose.translate(0, 1, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.25D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.5F, 0.5F, 0.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1.5F * offset, 0.875F * offset).scale(0.5F);
        body.part(m, ResourceManager.carbine, Parts.GUN, ResourceManager.carbine_tex);
        body.part(m, ResourceManager.carbine, Parts.PART_SLIDE, ResourceManager.carbine_tex);
        body.part(m, ResourceManager.carbine, Parts.MAGAZINE, ResourceManager.carbine_tex);
        body.part(m, ResourceManager.carbine, Parts.PART_BULLET, ResourceManager.carbine_tex);
        if (!isScoped(stack)) {
            body.part(m, ResourceManager.carbine, Parts.IRON_SIGHT, ResourceManager.carbine_tex);
        } else {
            body.part(m, ResourceManager.carbine, Parts.SCOPE, ResourceManager.carbine_scope_tex);
        }
        if (hasBayonet(stack)) {
            body.part(
                    m, ResourceManager.carbine, Parts.BAYONET, ResourceManager.carbine_bayonet_tex);
        }
    }

    public boolean isScoped(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }

    public boolean hasBayonet(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_CARBINE_BAYONET);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.carbine.partId("Gun");
        static final int PART_SLIDE = ResourceManager.carbine.partId("Slide");
        static final int MAGAZINE = ResourceManager.carbine.partId("Magazine");
        static final int PART_BULLET = ResourceManager.carbine.partId("Bullet");
        static final int IRON_SIGHT = ResourceManager.carbine.partId("IronSight");
        static final int SCOPE = ResourceManager.carbine.partId("Scope");
        static final int BAYONET = ResourceManager.carbine.partId("Bayonet");
    }
}
