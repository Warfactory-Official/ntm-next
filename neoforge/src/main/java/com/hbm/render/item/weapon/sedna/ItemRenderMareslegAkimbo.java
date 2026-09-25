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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMareslegAkimbo extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.maresleg_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
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

            pose.pushPose();

            int index = i == -1 ? 0 : 1;

            standardAimingTransform(
                    stack, pose, -1.5F * offset * i, -1F * offset, 2F * offset, 0, -3.875 / 8D, 1);

            double scale = 0.375D;
            pose.scale((float) scale, (float) scale, (float) scale);

            double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
            double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
            double[] lever = HbmAnimations.getRelevantTransformation("LEVER", index);
            double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
            double[] flip = HbmAnimations.getRelevantTransformation("FLIP", index);
            double[] lift = HbmAnimations.getRelevantTransformation("LIFT", index);
            double[] shell = HbmAnimations.getRelevantTransformation("SHELL", index);
            double[] flag = HbmAnimations.getRelevantTransformation("FLAG", index);

            pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
            pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 5)));
            pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));

            pose.translate(0, 0, -4);
            pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
            pose.translate(0, 0, 4);

            pose.translate(0, 0, -4);
            pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
            pose.translate(0, 0, 4);

            pose.translate(0, 0, -2);
            pose.mulPose(Axis.XN.rotationDegrees((float) flip[0]));
            pose.translate(0, 0, 2);

            pose.pushPose();
            pose.translate(0, 1, 3.75);
            pose.mulPose(Axis.ZN.rotationDegrees((float) turn[2]));
            pose.mulPose(Axis.XP.rotationDegrees((float) flip[0]));
            pose.mulPose(Axis.YP.rotationDegrees(90));
            renderSmokeNodes(collector, pose, gun.getConfig(stack, index).smokeNodes, 0.25D, light);
            pose.popPose();

            submitPart(collector, pose, body, ResourceManager.maresleg, Parts.GUN, light);

            pose.pushPose();
            pose.translate(0, 0.125, -2.875);
            pose.mulPose(Axis.XP.rotationDegrees((float) lever[0]));
            pose.translate(0, -0.125, 2.875);
            submitPart(collector, pose, body, ResourceManager.maresleg, Parts.PART_LEVER, light);
            pose.popPose();

            pose.pushPose();
            pose.translate(shell[0], shell[1] - 0.75, shell[2]);
            submitPart(collector, pose, body, ResourceManager.maresleg, Parts.PART_SHELL, light);
            pose.popPose();

            if (flag[0] != 0) {
                pose.pushPose();
                pose.translate(0, -0.5, 0);
                submitPart(
                        collector, pose, body, ResourceManager.maresleg, Parts.PART_SHELL, light);
                pose.popPose();
            }

            pose.pushPose();
            pose.translate(0, 1, 3.75);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
            renderMuzzleFlash(collector, pose, gun.lastShot[index], 75, 5);
            pose.popPose();

            pose.popPose();
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        for (int i = -1; i <= 1; i += 2) {
            Matrix4f m =
                    restSetup(0.875F, -1.5F * offset * i, -1F * offset, 2F * offset).scale(0.375F);
            body.part(m, ResourceManager.maresleg, Parts.GUN, ResourceManager.maresleg_tex);
            body.part(m, ResourceManager.maresleg, Parts.PART_LEVER, ResourceManager.maresleg_tex);
            body.part(
                    new Matrix4f(m).translate(0F, -0.75F, 0F),
                    ResourceManager.maresleg,
                    Parts.PART_SHELL,
                    ResourceManager.maresleg_tex);
        }
    }

    private static final class Parts {
        static final int GUN = ResourceManager.maresleg.partId("Gun");
        static final int PART_LEVER = ResourceManager.maresleg.partId("Lever");
        static final int PART_SHELL = ResourceManager.maresleg.partId("Shell");
    }
}
