// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
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

public class ItemRenderNI4NI extends ItemRenderWeaponBase {

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.0F * offset, -1F * offset, offset, 0, -5 / 8D, 0.125);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int[] colors = ItemGunNI4NI.getColors(stack);
        int dark = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[0];
        int lightColor = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[1];
        int grip = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[2];
        RenderType body =
                RenderTypes.entityCutout(
                        colors == null
                                ? ResourceManager.n_i_4_n_i_tex
                                : ResourceManager.n_i_4_n_i_greyscale_tex);

        double scale = 0.3125D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] drum = HbmAnimations.getRelevantTransformation("DRUM");

        pose.translate(0, 0, -2.25);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, 2.25);

        pose.translate(0, -1, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) recoil[0]));
        pose.translate(0, 1, 6);

        pose.pushPose();

        submitPart(collector, pose, body, ResourceManager.n_i_4_n_i, Parts.FRAME_DARK, light, dark);
        submitPart(collector, pose, body, ResourceManager.n_i_4_n_i, Parts.GRIP, light, grip);
        submitPart(
                collector,
                pose,
                body,
                ResourceManager.n_i_4_n_i,
                Parts.FRAME_LIGHT,
                light,
                lightColor);

        pose.pushPose();
        pose.translate(0, 1.1875D, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) drum[2]));
        pose.translate(0, -1.1875D, 0);
        submitPart(
                collector,
                pose,
                body,
                ResourceManager.n_i_4_n_i,
                Parts.CYLINDER,
                light,
                lightColor);
        submitPart(
                collector,
                pose,
                body,
                ResourceManager.n_i_4_n_i,
                Parts.CYLINDER_HIGHLIGHTS,
                FULL_BRIGHT);
        pose.popPose();

        submitPart(collector, pose, body, ResourceManager.n_i_4_n_i, Parts.BARREL, FULL_BRIGHT);

        int coinCount = ItemGunNI4NI.getCoinCount(stack);
        final PoseStack coinPose = pose;
        collector.submitCustomGeometry(
                coinPose,
                WeaponRenderTypes.UNTEXTURED,
                (p, buf) -> {
                    if (coinCount > 3)
                        ResourceManager.n_i_4_n_i.renderPart(
                                p,
                                buf,
                                FULL_BRIGHT,
                                0xFF00FF00 | (coinCount > 7 ? 0xFF0000 : 0),
                                Parts.COIN1);
                    if (coinCount > 2)
                        ResourceManager.n_i_4_n_i.renderPart(
                                p,
                                buf,
                                FULL_BRIGHT,
                                0xFF00FF00 | (coinCount > 6 ? 0xFF0000 : 0),
                                Parts.COIN2);
                    if (coinCount > 1)
                        ResourceManager.n_i_4_n_i.renderPart(
                                p,
                                buf,
                                FULL_BRIGHT,
                                0xFF00FF00 | (coinCount > 5 ? 0xFF0000 : 0),
                                Parts.COIN3);
                    if (coinCount > 0)
                        ResourceManager.n_i_4_n_i.renderPart(
                                p,
                                buf,
                                FULL_BRIGHT,
                                0xFF00FF00 | (coinCount > 4 ? 0xFF0000 : 0),
                                Parts.COIN4);
                });

        pose.pushPose();
        pose.translate(0, 0.75, 4);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.125F, 0.125F, 0.125F);
        renderLaserFlash(collector, pose, gun.lastShot[0], 75, 7.5, 0xFFFFFF);
        pose.popPose();

        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        int[] colors = ItemGunNI4NI.getColors(stack);
        int dark = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[0];
        int lightColor = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[1];
        int grip = colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[2];
        Identifier texture =
                colors == null
                        ? ResourceManager.n_i_4_n_i_tex
                        : ResourceManager.n_i_4_n_i_greyscale_tex;
        float offset = 0.8F;
        Matrix4f m = restSetup(1F, -1.0F * offset, -1F * offset, offset).scale(0.3125F);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.FRAME_DARK, texture, dark, false);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.GRIP, texture, grip, false);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.FRAME_LIGHT, texture, lightColor, false);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.CYLINDER, texture, lightColor, false);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.CYLINDER_HIGHLIGHTS, texture, -1, true);
        body.part(m, ResourceManager.n_i_4_n_i, Parts.BARREL, texture, -1, true);
        int coinCount = ItemGunNI4NI.getCoinCount(stack);
        int[] coins = {Parts.COIN1, Parts.COIN2, Parts.COIN3, Parts.COIN4};
        for (int c = 0; c < coins.length; c++) {
            if (coinCount <= 3 - c) continue;
            body.part(
                    m,
                    ResourceManager.n_i_4_n_i,
                    coins[c],
                    RenderTextures.WHITE,
                    0xFF00FF00 | (coinCount > 7 - c ? 0xFF0000 : 0),
                    true);
        }
    }

    private static final class Parts {
        static final int FRAME_DARK = ResourceManager.n_i_4_n_i.partId("FrameDark");
        static final int GRIP = ResourceManager.n_i_4_n_i.partId("Grip");
        static final int FRAME_LIGHT = ResourceManager.n_i_4_n_i.partId("FrameLight");
        static final int CYLINDER = ResourceManager.n_i_4_n_i.partId("Cylinder");
        static final int CYLINDER_HIGHLIGHTS =
                ResourceManager.n_i_4_n_i.partId("CylinderHighlights");
        static final int BARREL = ResourceManager.n_i_4_n_i.partId("Barrel");
        static final int COIN1 = ResourceManager.n_i_4_n_i.partId("Coin1");
        static final int COIN2 = ResourceManager.n_i_4_n_i.partId("Coin2");
        static final int COIN3 = ResourceManager.n_i_4_n_i.partId("Coin3");
        static final int COIN4 = ResourceManager.n_i_4_n_i.partId("Coin4");
    }
}
