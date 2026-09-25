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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderHangman extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.hangman_tex);

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.5F * offset, -0.875F * offset, 1.75F * offset, 0, -1.5 / 8D, 1.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
        double[] turn = HbmAnimations.getRelevantTransformation("TURN");
        double[] smack = HbmAnimations.getRelevantTransformation("SMACK");
        double[] lid = HbmAnimations.getRelevantTransformation("LID");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");

        pose.translate(1.5F * offset, 0, -1);
        pose.mulPose(Axis.YP.rotationDegrees((float) turn[1]));
        pose.translate(-1.5F * offset, 0, 1);

        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(smack[0], smack[1], smack[2]);

        double scale = 0.125D;
        pose.scale((float) scale, (float) scale, (float) scale);

        pose.translate(0, -4, -10);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 4, 10);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.hangman, Parts.RIFLE, light);
        submitPart(collector, pose, body, ResourceManager.hangman, Parts.INTERNALS, light);

        pose.pushPose();
        pose.translate(-2.1875, -1.75, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) lid[2]));
        pose.translate(2.1875, 1.75, 0);
        submitPart(collector, pose, body, ResourceManager.hangman, Parts.PART_LID, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        submitPart(collector, pose, body, ResourceManager.hangman, Parts.MAGAZINE, light);
        if (bullets[0] == 0)
            submitPart(collector, pose, body, ResourceManager.hangman, Parts.PART_BULLETS, light);
        pose.popPose();

        double smokeScale = 1.5;

        pose.pushPose();
        pose.translate(0, 0, 29);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 29);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(2, 2, 2);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -0.875F * offset, 1.75F * offset).scale(0.125F);
        body.part(m, ResourceManager.hangman, Parts.RIFLE, ResourceManager.hangman_tex);
        body.part(m, ResourceManager.hangman, Parts.INTERNALS, ResourceManager.hangman_tex);
        body.part(m, ResourceManager.hangman, Parts.PART_LID, ResourceManager.hangman_tex);
        body.part(m, ResourceManager.hangman, Parts.MAGAZINE, ResourceManager.hangman_tex);
        body.part(m, ResourceManager.hangman, Parts.PART_BULLETS, ResourceManager.hangman_tex);
    }

    private static final class Parts {
        static final int RIFLE = ResourceManager.hangman.partId("Rifle");
        static final int INTERNALS = ResourceManager.hangman.partId("Internals");
        static final int PART_LID = ResourceManager.hangman.partId("Lid");
        static final int MAGAZINE = ResourceManager.hangman.partId("Magazine");
        static final int PART_BULLETS = ResourceManager.hangman.partId("Bullets");
    }
}
