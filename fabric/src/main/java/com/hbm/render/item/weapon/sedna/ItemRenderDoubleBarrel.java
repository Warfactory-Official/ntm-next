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

public class ItemRenderDoubleBarrel extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderDoubleBarrel(Identifier texture) {
        this.texture = texture;
        this.body = RenderTypes.entityCutout(texture);
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
                stack, pose, -1.25F * offset, -1F * offset, 2F * offset, 0, -2 / 8D, 1);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");
        double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] shells = HbmAnimations.getRelevantTransformation("SHELLS");
        double[] shellFlip = HbmAnimations.getRelevantTransformation("SHELL_FLIP");
        double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
        double[] buckle = HbmAnimations.getRelevantTransformation("BUCKLE");
        double[] no_ammo = HbmAnimations.getRelevantTransformation("NO_AMMO");

        pose.translate(recoil[0] * 3, recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 10)));

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 4);

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.YP.rotationDegrees((float) turn[1]));
        pose.translate(0, 0, 4);

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 4);

        submitPart(collector, pose, body, ResourceManager.double_barrel, Parts.STOCK, light);

        pose.pushPose();

        pose.translate(0, -0.4375, -0.875);
        pose.mulPose(Axis.XP.rotationDegrees((float) barrel[0]));
        pose.translate(0, 0.4375, 0.875);

        submitPart(collector, pose, body, ResourceManager.double_barrel, Parts.BARREL_SHORT, light);
        if (!isSawedOff(stack))
            submitPart(
                    collector, pose, body, ResourceManager.double_barrel, Parts.PART_BARREL, light);

        pose.pushPose();
        pose.translate(0.75, 0, -0.6875);
        pose.mulPose(Axis.YP.rotationDegrees((float) buckle[1]));
        pose.translate(-0.75, 0, 0.6875);
        submitPart(collector, pose, body, ResourceManager.double_barrel, Parts.PART_BUCKLE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.3125, 0.3125, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) lever[2]));
        pose.translate(0.3125, -0.3125, 0);
        submitPart(collector, pose, body, ResourceManager.double_barrel, Parts.PART_LEVER, light);
        pose.popPose();

        if (no_ammo[0] == 0) {
            pose.pushPose();
            pose.translate(shells[0], shells[1], shells[2]);
            pose.translate(0, 0, -1);
            pose.mulPose(Axis.XP.rotationDegrees((float) shellFlip[0]));
            pose.translate(0, 0, 1);
            submitPart(
                    collector, pose, body, ResourceManager.double_barrel, Parts.PART_SHELLS, light);
            pose.popPose();
        }

        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(2, 2, 2);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.25F * offset, -1F * offset, 2F * offset).scale(0.375F);
        body.part(m, ResourceManager.double_barrel, Parts.STOCK, texture);
        body.part(m, ResourceManager.double_barrel, Parts.BARREL_SHORT, texture);
        if (!isSawedOff(stack))
            body.part(m, ResourceManager.double_barrel, Parts.PART_BARREL, texture);
        body.part(m, ResourceManager.double_barrel, Parts.PART_BUCKLE, texture);
        body.part(m, ResourceManager.double_barrel, Parts.PART_LEVER, texture);
        body.part(m, ResourceManager.double_barrel, Parts.PART_SHELLS, texture);
    }

    public boolean isSawedOff(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get()
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
    }

    private static final class Parts {
        static final int STOCK = ResourceManager.double_barrel.partId("Stock");
        static final int BARREL_SHORT = ResourceManager.double_barrel.partId("BarrelShort");
        static final int PART_BARREL = ResourceManager.double_barrel.partId("Barrel");
        static final int PART_BUCKLE = ResourceManager.double_barrel.partId("Buckle");
        static final int PART_LEVER = ResourceManager.double_barrel.partId("Lever");
        static final int PART_SHELLS = ResourceManager.double_barrel.partId("Shells");
    }
}
