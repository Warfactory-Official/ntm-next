// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMinigun extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderMinigun(Identifier texture) {
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
                stack, pose, -1.75F * offset, -1.75F * offset, 3.5F * offset, 0, -6.25 / 8D, 1);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

        pose.translate(0, 3, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, -3, 6);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.minigun, Parts.GUN, light);
        submitPart(collector, pose, body, ResourceManager.minigun, Parts.GRIP, light);

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) rotate[2]));
        submitPart(collector, pose, body, ResourceManager.minigun, Parts.BARRELS, light);
        pose.popPose();

        double smokeScale = 0.5;

        pose.pushPose();
        pose.translate(-2, 1.25, -3.5);
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 12);
        pose.mulPose(Axis.YP.rotationDegrees(90));

        if (stack.getItem() == ModItems.GUN_MINIGUN_LACUNAE.get()) {
            renderLaserFlash(collector, pose, gun.lastShot[0], 50, 1D, 0xff00ff);
            pose.translate(0, 0, -0.25);
            renderLaserFlash(collector, pose, gun.lastShot[0], 50, 0.5D, 0xff0080);
        } else {
            pose.translate(0, 0.5, 0);
            pose.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
            pose.scale(1.5F, 1.5F, 1.5F);
            renderMuzzleFlash(collector, pose, gun.lastShot[0], 50, 7.5);
        }
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.75F * offset, -1.75F * offset, 3.5F * offset).scale(0.375F);
        body.part(m, ResourceManager.minigun, Parts.GUN, texture);
        body.part(m, ResourceManager.minigun, Parts.GRIP, texture);
        body.part(m, ResourceManager.minigun, Parts.BARRELS, texture);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.minigun.partId("Gun");
        static final int GRIP = ResourceManager.minigun.partId("Grip");
        static final int BARRELS = ResourceManager.minigun.partId("Barrels");
    }
}
