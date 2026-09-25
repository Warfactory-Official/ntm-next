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

public class ItemRenderGreasegun extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.greasegun_tex);
    private final RenderType bodyClean =
            RenderTypes.entityCutout(ResourceManager.greasegun_clean_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -1F * offset, 1.75F * offset, 0, -2.625 / 8D, 1.125);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        RenderType tex = isRefurbished(stack) ? bodyClean : body;
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] flap = HbmAnimations.getRelevantTransformation("FLAP");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 3, 3);

        pose.translate(0, -3, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 3, 3);

        if (ItemGunBaseNT.aimingProgress < 1F)
            pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 0, -4 - stock[2]);
        submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.PART_STOCK, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.MAGAZINE, light);
        if (bullet[0] != 1)
            submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.PART_BULLET, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, -1.4375, -0.125);
        pose.mulPose(Axis.XP.rotationDegrees((float) handle[0]));
        pose.translate(0, 1.4375, 0.125);
        submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.PART_HANDLE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.53125, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) flap[2]));
        pose.translate(0, -0.5125, 0);
        submitPart(collector, pose, tex, ResourceManager.greasegun, Parts.PART_FLAP, light);
        pose.popPose();

        double smokeScale = 0.25;

        pose.pushPose();
        pose.translate(-0.25, 0, 1.5);
        pose.mulPose(Axis.ZN.rotationDegrees((float) turn[2]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 1D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.ZN.rotationDegrees((float) turn[2]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 1D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.5F, 0.5F, 0.5F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        Identifier tex =
                isRefurbished(stack)
                        ? ResourceManager.greasegun_clean_tex
                        : ResourceManager.greasegun_tex;
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1F * offset, 1.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.greasegun, Parts.GUN, tex);
        body.part(
                new Matrix4f(m).translate(0F, 0F, -4F),
                ResourceManager.greasegun,
                Parts.PART_STOCK,
                tex);
        body.part(m, ResourceManager.greasegun, Parts.MAGAZINE, tex);
        body.part(m, ResourceManager.greasegun, Parts.PART_BULLET, tex);
        body.part(m, ResourceManager.greasegun, Parts.PART_HANDLE, tex);
        body.part(
                new Matrix4f(m).translate(0F, 0.53125F, 0F).translate(0F, -0.5125F, 0F),
                ResourceManager.greasegun,
                Parts.PART_FLAP,
                tex);
    }

    public boolean isRefurbished(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_GREASEGUN_CLEAN);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.greasegun.partId("Gun");
        static final int PART_STOCK = ResourceManager.greasegun.partId("Stock");
        static final int MAGAZINE = ResourceManager.greasegun.partId("Magazine");
        static final int PART_BULLET = ResourceManager.greasegun.partId("Bullet");
        static final int PART_HANDLE = ResourceManager.greasegun.partId("Handle");
        static final int PART_FLAP = ResourceManager.greasegun.partId("Flap");
    }
}
