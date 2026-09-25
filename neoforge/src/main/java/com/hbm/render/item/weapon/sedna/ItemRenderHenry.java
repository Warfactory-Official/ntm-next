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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderHenry extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderHenry(Identifier texture) {
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
                stack, pose, -1.25F * offset, -1F * offset, 1.75F * offset, 0, -5 / 8D, 1);

        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        pose.mulPose(Axis.XP.rotationDegrees(-2.5F * aimingProgress));
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
        double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
        double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
        double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

        pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) (recoil[2] * 5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) turn[2]));

        pose.translate(yeet[0], yeet[1], yeet[2]);

        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(0, -1, 0);

        pose.translate(0, -4, 4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 4, -4);

        pose.translate(0, 2, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, -2, 4);

        pose.pushPose();
        pose.translate(0, 1, 8);
        pose.mulPose(Axis.ZN.rotationDegrees((float) turn[2]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.25D, light);
        pose.popPose();

        submitPart(collector, pose, body, ResourceManager.henry, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 1.25, -0.1875);
        pose.mulPose(Axis.XP.rotationDegrees((float) sight[0]));
        pose.translate(0, -1.25, 0.1875);
        submitPart(collector, pose, body, ResourceManager.henry, Parts.PART_SIGHT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.625, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-30 + hammer[0])));
        pose.translate(0, -0.625, 3);
        submitPart(collector, pose, body, ResourceManager.henry, Parts.PART_HAMMER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0.25, -2.3125);
        pose.mulPose(Axis.XP.rotationDegrees((float) lever[0]));
        pose.translate(0, -0.25, 2.3125);
        submitPart(collector, pose, body, ResourceManager.henry, Parts.PART_LEVER, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) twist[2]));
        pose.translate(0, -1, 0);
        submitPart(collector, pose, body, ResourceManager.henry, Parts.FRONT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(bullet[0], bullet[1], bullet[2] - 1);
        submitPart(collector, pose, body, ResourceManager.henry, Parts.PART_BULLET, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.25F * offset, -1F * offset, 1.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.henry, Parts.GUN, texture);
        body.part(m, ResourceManager.henry, Parts.PART_SIGHT, texture);
        body.part(
                new Matrix4f(m)
                        .translate(0F, 0.625F, -3F)
                        .rotate(Axis.XP.rotationDegrees(-30F))
                        .translate(0F, -0.625F, 3F),
                ResourceManager.henry,
                Parts.PART_HAMMER,
                texture);
        body.part(m, ResourceManager.henry, Parts.PART_LEVER, texture);
        body.part(m, ResourceManager.henry, Parts.FRONT, texture);
        body.part(
                new Matrix4f(m).translate(0F, 0F, -1F),
                ResourceManager.henry,
                Parts.PART_BULLET,
                texture);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.henry.partId("Gun");
        static final int PART_SIGHT = ResourceManager.henry.partId("Sight");
        static final int PART_HAMMER = ResourceManager.henry.partId("Hammer");
        static final int PART_LEVER = ResourceManager.henry.partId("Lever");
        static final int FRONT = ResourceManager.henry.partId("Front");
        static final int PART_BULLET = ResourceManager.henry.partId("Bullet");
    }
}
