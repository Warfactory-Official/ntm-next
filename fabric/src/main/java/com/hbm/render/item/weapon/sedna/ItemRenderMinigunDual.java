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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMinigunDual extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.minigun_dual_tex);

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        float offset = 0.8F;

        for (int i = -1; i <= 1; i += 2) {
            int index = i == -1 ? 0 : 1;

            pose.pushPose();
            standardAimingTransform(
                    stack, pose, -2.75F * offset * i, -1.75F * offset, 2.5F * offset, 0, 0, 0);

            double scale = 0.375D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE", index);

            pose.translate(0, 3, -6);
            pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
            pose.translate(0, -3, 6);

            pose.translate(0, 0, recoil[2]);

            submitPart(
                    collector,
                    pose,
                    body,
                    ResourceManager.minigun,
                    index == 0 ? Parts.GUN_DUAL : Parts.GUN,
                    light);

            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees((float) (rotate[2] * i)));
            submitPart(collector, pose, body, ResourceManager.minigun, Parts.BARRELS, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, 0, 12);
            pose.mulPose(Axis.YP.rotationDegrees(90));

            pose.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
            pose.scale(1.5F, 1.5F, 1.5F);
            renderMuzzleFlash(collector, pose, gun.lastShot[index], 50, 7.5);
            pose.popPose();

            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        for (int i = -1; i <= 1; i += 2) {
            Matrix4f m =
                    restSetup(0.875F, -2.75F * offset * i, -1.75F * offset, 2.5F * offset)
                            .scale(0.375F);
            body.part(
                    m,
                    ResourceManager.minigun,
                    i == -1 ? Parts.GUN_DUAL : Parts.GUN,
                    ResourceManager.minigun_dual_tex);
            body.part(m, ResourceManager.minigun, Parts.BARRELS, ResourceManager.minigun_dual_tex);
        }
    }

    private static final class Parts {
        static final int GUN_DUAL = ResourceManager.minigun.partId("GunDual");
        static final int GUN = ResourceManager.minigun.partId("Gun");
        static final int BARRELS = ResourceManager.minigun.partId("Barrels");
    }
}
