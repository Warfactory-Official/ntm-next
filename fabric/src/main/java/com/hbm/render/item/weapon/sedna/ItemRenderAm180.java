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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderAm180 extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.am180_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1F * offset, -1F * offset, offset, 0, -4.1875 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.1875D;
        pose.scale((float) scale, (float) scale, (float) scale);

        boolean silenced = hasSilencer(stack);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] magazine = HbmAnimations.getRelevantTransformation("MAG");
        double[] magTurn = HbmAnimations.getRelevantTransformation("MAGTURN");
        double[] magSpin = HbmAnimations.getRelevantTransformation("MAGSPIN");
        double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");

        pose.translate(0, -2, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 6);

        pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));

        pose.translate(0, 0, recoil[2]);

        HbmAnimations.applyRelevantTransformation(pose, "Gun");
        submitPart(collector, pose, body, ResourceManager.am180, Parts.GUN, light);
        if (silenced)
            submitPart(collector, pose, body, ResourceManager.am180, Parts.SILENCER, light);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Trigger");
        submitPart(collector, pose, body, ResourceManager.am180, Parts.TRIGGER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, bolt[2]);
        HbmAnimations.applyRelevantTransformation(pose, "Bolt");
        submitPart(collector, pose, body, ResourceManager.am180, Parts.PART_BOLT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(magazine[0], magazine[1], magazine[2]);

        pose.translate(0, 2.0625, 3.75);
        pose.mulPose(Axis.XP.rotationDegrees((float) magTurn[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) magTurn[2]));
        pose.translate(0, -2.0625, -3.75);

        pose.translate(0, 2.3125, 1.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) magSpin[0]));
        pose.translate(0, -2.3125, -1.5);

        HbmAnimations.applyRelevantTransformation(pose, "Mag");

        pose.pushPose();
        int mag =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getAmount(stack, Minecraft.getInstance().player.getInventory());
        pose.translate(0, 0, 1.5);
        pose.mulPose(Axis.YN.rotationDegrees((float) (mag / 59D * 360D)));
        pose.translate(0, 0, -1.5);
        submitPart(collector, pose, body, ResourceManager.am180, Parts.PART_MAG, light);
        pose.popPose();

        submitPart(collector, pose, body, ResourceManager.am180, Parts.MAG_PLATE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.875, silenced ? 17 : 13);
        pose.mulPose(Axis.ZN.rotationDegrees((float) turn[2]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.25D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.875, silenced ? 16.75 : 12);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        float flashScale = silenced ? 0.5F : 0.75F;
        pose.scale(flashScale, flashScale, flashScale);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], silenced ? 75 : 50, silenced ? 5 : 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1F * offset, -1F * offset, offset).scale(0.1875F);
        body.part(m, ResourceManager.am180, Parts.GUN, ResourceManager.am180_tex);
        if (hasSilencer(stack))
            body.part(m, ResourceManager.am180, Parts.SILENCER, ResourceManager.am180_tex);
        body.part(m, ResourceManager.am180, Parts.TRIGGER, ResourceManager.am180_tex);
        body.part(m, ResourceManager.am180, Parts.PART_BOLT, ResourceManager.am180_tex);
        int mag =
                gun.getConfig(stack, 0)
                        .getReceivers(stack)[0]
                        .getMagazine(stack)
                        .getAmount(stack, null);
        body.part(
                new Matrix4f(m)
                        .translate(0F, 0F, 1.5F)
                        .rotate(Axis.YN.rotationDegrees((float) (mag / 59D * 360D)))
                        .translate(0F, 0F, -1.5F),
                ResourceManager.am180,
                Parts.PART_MAG,
                ResourceManager.am180_tex);
        body.part(m, ResourceManager.am180, Parts.MAG_PLATE, ResourceManager.am180_tex);
    }

    public boolean hasSilencer(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.am180.partId("Gun");
        static final int SILENCER = ResourceManager.am180.partId("Silencer");
        static final int TRIGGER = ResourceManager.am180.partId("Trigger");
        static final int PART_BOLT = ResourceManager.am180.partId("Bolt");
        static final int PART_MAG = ResourceManager.am180.partId("Mag");
        static final int MAG_PLATE = ResourceManager.am180.partId("MagPlate");
    }
}
