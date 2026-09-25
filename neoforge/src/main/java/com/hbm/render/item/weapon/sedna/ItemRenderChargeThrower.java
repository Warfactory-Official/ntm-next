// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
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

public class ItemRenderChargeThrower extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.charge_thrower_tex);
    private final RenderType hook =
            RenderTypes.entityCutout(ResourceManager.charge_thrower_hook_tex);
    private final RenderType mortar =
            RenderTypes.entityCutout(ResourceManager.charge_thrower_mortar_tex);

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F;
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
        float zoom = 0.5F;

        if (isScoped(stack))
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.25F * offset,
                    3.5F * offset,
                    -0.15625,
                    -6.5 / 8D,
                    1.6875);
        else
            standardAimingTransform(
                    stack,
                    pose,
                    -1.5F * offset,
                    -1.25F * offset,
                    3.5F * offset,
                    -1.5F * zoom,
                    -1.25F * zoom,
                    3.5F * zoom);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        boolean usingScope =
                this.isScoped(stack)
                        && ItemGunBaseNT.aimingProgress == 1
                        && ItemGunBaseNT.prevAimingProgress == 1;
        MagazineFullReload mag =
                (MagazineFullReload)
                        gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (usingScope) {
            double scale = 3.5D;
            pose.scale((float) scale, (float) scale, (float) scale);
            pose.translate(-0.5, -1.5, -4);
        } else {
            double scale = 0.5D;
            pose.scale((float) scale, (float) scale, (float) scale);
        }

        boolean reloading =
                HbmAnimations.getRelevantAnim(0) != null
                        && HbmAnimations.getRelevantAnim(0).animation.getBus("AMMO") != null;
        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] raise = HbmAnimations.getRelevantTransformation("RAISE");
        double[] ammo = HbmAnimations.getRelevantTransformation("AMMO");
        double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");
        double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

        pose.translate(0, 0, -7);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 7);

        pose.translate(0, -7, 4);
        pose.mulPose(Axis.XP.rotationDegrees((float) raise[0]));
        pose.translate(0, 7, -4);

        pose.translate(recoil[0], recoil[1], recoil[2]);

        pose.translate(0, 0, -2);
        pose.mulPose(Axis.YP.rotationDegrees((float) turn[1]));
        pose.translate(0, 0, 2);
        pose.translate(0, -1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(0, 1, 0);

        submitPart(collector, pose, body, ResourceManager.charge_thrower, Parts.GUN, light);
        if (isScoped(stack) && !usingScope)
            submitPart(collector, pose, body, ResourceManager.charge_thrower, Parts.SCOPE, light);

        if (mag.getAmount(stack, null) > 0 || reloading) {

            pose.translate(ammo[0], ammo[1], ammo[2]);
            pose.mulPose(Axis.ZP.rotationDegrees((float) twist[2]));

            Object type = mag.getType(stack, null);
            if (type == XFactoryTool.ct_hook) {
                submitPart(
                        collector, pose, hook, ResourceManager.charge_thrower, Parts.HOOK, light);
            }
            if (type == XFactoryTool.ct_mortar) {
                submitPart(
                        collector,
                        pose,
                        mortar,
                        ResourceManager.charge_thrower,
                        Parts.MORTAR,
                        light);
            }
            if (type == XFactoryTool.ct_mortar_charge) {
                submitPart(
                        collector,
                        pose,
                        mortar,
                        ResourceManager.charge_thrower,
                        Parts.MORTAR,
                        light);
                submitPart(
                        collector,
                        pose,
                        mortar,
                        ResourceManager.charge_thrower,
                        Parts.OOMPH,
                        light);
            }
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1.25F * offset, 3.5F * offset).scale(0.5F);
        body.part(m, ResourceManager.charge_thrower, Parts.GUN, ResourceManager.charge_thrower_tex);
        if (isScoped(stack)) {
            body.part(
                    m,
                    ResourceManager.charge_thrower,
                    Parts.SCOPE,
                    ResourceManager.charge_thrower_tex);
        }
        if (mag.getAmount(stack, null) <= 0) return;

        Object type = mag.getType(stack, null);
        if (type == XFactoryTool.ct_hook) {
            body.part(
                    m,
                    ResourceManager.charge_thrower,
                    Parts.HOOK,
                    ResourceManager.charge_thrower_hook_tex);
        }
        if (type == XFactoryTool.ct_mortar) {
            body.part(
                    m,
                    ResourceManager.charge_thrower,
                    Parts.MORTAR,
                    ResourceManager.charge_thrower_mortar_tex);
        }
        if (type == XFactoryTool.ct_mortar_charge) {
            body.part(
                    m,
                    ResourceManager.charge_thrower,
                    Parts.MORTAR,
                    ResourceManager.charge_thrower_mortar_tex);
            body.part(
                    m,
                    ResourceManager.charge_thrower,
                    Parts.OOMPH,
                    ResourceManager.charge_thrower_mortar_tex);
        }
    }

    public boolean isScoped(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.charge_thrower.partId("Gun");
        static final int SCOPE = ResourceManager.charge_thrower.partId("Scope");
        static final int HOOK = ResourceManager.charge_thrower.partId("Hook");
        static final int MORTAR = ResourceManager.charge_thrower.partId("Mortar");
        static final int OOMPH = ResourceManager.charge_thrower.partId("Oomph");
    }
}
