// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
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

public class ItemRenderG3 extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;
    private final RenderType attachments = RenderTypes.entityCutout(ResourceManager.g3_attachments);

    public ItemRenderG3(Identifier texture) {
        this.texture = texture;
        this.body = RenderTypes.entityCutout(texture);
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

        boolean isScoped = this.isScoped(stack);
        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -1.25F * offset,
                -1F * offset,
                2.75F * offset,
                0,
                isScoped ? (-5.53125 / 8D) : (-3.5625 / 8D),
                isScoped ? 1.46875 : 1.75);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        boolean isScoped = this.isScoped(stack);

        if (isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1)
            return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        RenderType texture =
                XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_GREEN)
                        ? RenderTypes.entityCutout(ResourceManager.g3_polymer_green_tex)
                        : XWeaponModManager.hasUpgrade(
                                        stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)
                                ? RenderTypes.entityCutout(ResourceManager.g3_polymer_black_tex)
                                : body;
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
        double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
        double[] plug = HbmAnimations.getRelevantTransformation("PLUG");
        double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

        pose.translate(0, -2, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 6);

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 4);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, texture, ResourceManager.g3, Parts.RIFLE, light);
        if (hasStock(stack))
            submitPart(collector, pose, texture, ResourceManager.g3, Parts.STOCK, light);
        boolean silenced = hasSilencer(stack);
        if (!silenced)
            submitPart(collector, pose, texture, ResourceManager.g3, Parts.FLASH_HIDER, light);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.TRIGGER, light);

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        pose.translate(0, -1.75, -0.5);
        pose.mulPose(Axis.ZP.rotationDegrees((float) speen[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) speen[1]));
        pose.translate(0, 1.75, 0.5);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.MAGAZINE, light);
        if (bullet[0] == 0)
            submitPart(collector, pose, texture, ResourceManager.g3, Parts.PART_BULLET, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, bolt[2]);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.GUIDE_AND_BOLT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.625, plug[2]);
        pose.mulPose(Axis.ZP.rotationDegrees((float) handle[2]));
        pose.translate(0, -0.625, 0);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.PART_PLUG, light);

        pose.translate(0, 0.625, 5.25);
        pose.mulPose(Axis.ZP.rotationDegrees(22.5F));
        pose.mulPose(Axis.YP.rotationDegrees((float) handle[1]));
        pose.mulPose(Axis.ZN.rotationDegrees(22.5F));
        pose.translate(0, -0.625, -5.25);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.PART_HANDLE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, -0.875, -3.5);
        pose.mulPose(Axis.XN.rotationDegrees(30 * (1 - ItemGunBaseNT.getMode(stack, 0))));
        pose.translate(0, 0.875, 3.5);
        submitPart(collector, pose, texture, ResourceManager.g3, Parts.SELECTOR, light);
        pose.popPose();

        if (silenced || isScoped) {
            if (silenced)
                submitPart(collector, pose, attachments, ResourceManager.g3, Parts.SILENCER, light);
            if (isScoped)
                submitPart(collector, pose, attachments, ResourceManager.g3, Parts.SCOPE, light);
        }

        if (!silenced) {
            double smokeScale = 0.75;

            pose.pushPose();
            pose.translate(0, 0, 13);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
            renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, 12);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees((float) (-25 + gun.shotRand * 10)));
            pose.scale(0.75F, 0.75F, 0.75F);
            renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 10);
            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        Identifier tex =
                XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_GREEN)
                        ? ResourceManager.g3_polymer_green_tex
                        : XWeaponModManager.hasUpgrade(
                                        stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)
                                ? ResourceManager.g3_polymer_black_tex
                                : texture;
        boolean silenced = hasSilencer(stack);
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.25F * offset, -1F * offset, 2.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.g3, Parts.RIFLE, tex);
        if (hasStock(stack)) body.part(m, ResourceManager.g3, Parts.STOCK, tex);
        if (!silenced) body.part(m, ResourceManager.g3, Parts.FLASH_HIDER, tex);
        body.part(m, ResourceManager.g3, Parts.TRIGGER, tex);
        body.part(m, ResourceManager.g3, Parts.MAGAZINE, tex);
        body.part(m, ResourceManager.g3, Parts.PART_BULLET, tex);
        body.part(m, ResourceManager.g3, Parts.GUIDE_AND_BOLT, tex);
        body.part(m, ResourceManager.g3, Parts.PART_PLUG, tex);
        body.part(m, ResourceManager.g3, Parts.PART_HANDLE, tex);
        body.part(
                new Matrix4f(m)
                        .translate(0F, -0.875F, -3.5F)
                        .rotate(Axis.XN.rotationDegrees(30 * (1 - ItemGunBaseNT.getMode(stack, 0))))
                        .translate(0F, 0.875F, 3.5F),
                ResourceManager.g3,
                Parts.SELECTOR,
                tex);
        if (silenced)
            body.part(m, ResourceManager.g3, Parts.SILENCER, ResourceManager.g3_attachments);
        if (isScoped(stack))
            body.part(m, ResourceManager.g3, Parts.SCOPE, ResourceManager.g3_attachments);
    }

    public boolean hasStock(ItemStack stack) {
        return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK);
    }

    public boolean hasSilencer(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_G3_ZEBRA.get()
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
    }

    public boolean isScoped(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_G3_ZEBRA.get()
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
    }

    private static final class Parts {
        static final int RIFLE = ResourceManager.g3.partId("Rifle");
        static final int STOCK = ResourceManager.g3.partId("Stock");
        static final int FLASH_HIDER = ResourceManager.g3.partId("Flash_Hider");
        static final int TRIGGER = ResourceManager.g3.partId("Trigger");
        static final int MAGAZINE = ResourceManager.g3.partId("Magazine");
        static final int PART_BULLET = ResourceManager.g3.partId("Bullet");
        static final int GUIDE_AND_BOLT = ResourceManager.g3.partId("Guide_And_Bolt");
        static final int PART_PLUG = ResourceManager.g3.partId("Plug");
        static final int PART_HANDLE = ResourceManager.g3.partId("Handle");
        static final int SELECTOR = ResourceManager.g3.partId("Selector");
        static final int SILENCER = ResourceManager.g3.partId("Silencer");
        static final int SCOPE = ResourceManager.g3.partId("Scope");
    }
}
