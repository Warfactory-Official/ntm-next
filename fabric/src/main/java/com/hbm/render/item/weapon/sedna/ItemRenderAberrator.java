// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.render.FlatCutout;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.loader.UnitQuad;
import com.hbm.render.util.Vertices;
import com.hbm.util.GameTime;
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

public class ItemRenderAberrator extends ItemRenderWeaponBase {

    private static final Identifier HALO_TEXTURE =
            Identifier.withDefaultNamespace("textures/item/golden_sword.png");
    private static final RenderType HALO = FlatCutout.of(HALO_TEXTURE);
    public static final int FIREBALL_FLASH = 150;
    private static final UnitQuad PLUME =
            UnitQuad.of(new float[] {0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F}, 0F, 1F, 0F);
    private static final UnitQuad HALO_QUAD =
            UnitQuad.of(new float[] {1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F}, 0F, 1F, 0F);
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.aberrator_tex);

    public static void renderFireball(
            SubmitNodeCollector collector, PoseStack pose, long lastShot) {
        MuzzleFlash.FIREBALL.submit(collector, pose, lastShot, 0, 0, 0);
    }

    public static void fireballQuads(double fire, Matrix4f scratch, UnitQuad.Visitor visitor) {
        float height = (float) (5 * fire);
        float length = (float) (10 * fire);
        float offset = (float) (1 * fire);
        float lengthOffset = -1.125F;
        PLUME.visit(
                visitor,
                scratch,
                height,
                -offset,
                0,
                -height,
                -offset,
                0,
                -height,
                -offset + length,
                -lengthOffset,
                height,
                -offset + length,
                -lengthOffset);
        PLUME.visit(
                visitor,
                scratch,
                height,
                -offset,
                0,
                -height,
                -offset,
                0,
                -height,
                -offset + length,
                lengthOffset,
                height,
                -offset + length,
                lengthOffset);
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -1.0F * offset, -1.25F * offset, 1.25F * offset, 0, -5.25 / 8D, 0.125);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] rise = HbmAnimations.getRelevantTransformation("RISE");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
        double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
        double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
        double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL");
        double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");

        pose.translate(0, rise[1], 0);

        pose.translate(0, 1, -2.25);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, -1, 2.25);

        pose.translate(0, -1, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) recoil[0]));
        pose.translate(0, 1, 4);

        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) roll[2]));
        pose.translate(0, -1, 0);

        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.GUN, light);

        pose.pushPose();
        pose.translate(0, 2.4375, -1.9375);
        pose.mulPose(Axis.XP.rotationDegrees((float) sight[0]));
        pose.translate(0, -2.4375, 1.9375);
        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_SIGHT, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);

        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) magroll[2]));
        pose.translate(0, -1, 0);

        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.MAGAZINE, light);
        pose.translate(bullet[0], bullet[1], bullet[2]);
        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_BULLET, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, slide[2]);
        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_SLIDE, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1.25, -3.625);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-45 + hammer[0])));
        pose.translate(0, -1.25, 3.625);
        submitPart(collector, pose, body, ResourceManager.aberrator, Parts.PART_HAMMER, light);
        pose.popPose();

        double smokeScale = 0.5;

        pose.pushPose();
        pose.translate(0, 2, 4);
        pose.mulPose(Axis.XN.rotationDegrees((float) recoil[0]));
        pose.mulPose(Axis.ZN.rotationDegrees((float) roll[2]));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 2, 4);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 2, -1.5);
        pose.scale(0.5F, 0.5F, 0.5F);
        renderFireball(collector, pose, gun.lastShot[0]);
        pose.popPose();

        pose.translate(0, 2, 4.5);
        pose.mulPose(Axis.ZN.rotationDegrees((float) roll[2]));
        pose.mulPose(Axis.XN.rotationDegrees((float) recoil[0]));
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (GameTime.now() / 50D % 360D)));

        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        aimingProgress = Math.min(1F, aimingProgress * 2);

        int amount = 16;
        for (int i = 0; i < amount; i++) {
            pose.pushPose();
            pose.translate(0, -1.5 - aimingProgress, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90 * aimingProgress));
            pose.mulPose(Axis.ZP.rotationDegrees(-45));
            collector.submitCustomGeometry(
                    pose,
                    HALO,
                    (p, tess) -> {
                        Vertices.emit(tess, p, -0.5F, -0.5F, -0.5F, -1, 1, 1, light, 0F, 1F, 0F);
                        Vertices.emit(tess, p, 0.5F, -0.5F, -0.5F, -1, 0, 1, light, 0F, 1F, 0F);
                        Vertices.emit(tess, p, 0.5F, 0.5F, -0.5F, -1, 0, 0, light, 0F, 1F, 0F);
                        Vertices.emit(tess, p, -0.5F, 0.5F, -0.5F, -1, 1, 0, light, 0F, 1F, 0F);
                    });
            pose.popPose();
            pose.mulPose(Axis.ZP.rotationDegrees(360F / amount));
        }
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(1F, -1.0F * offset, -1.25F * offset, 1.25F * offset).scale(0.25F);
        body.part(m, ResourceManager.aberrator, Parts.GUN, ResourceManager.aberrator_tex);
        body.part(m, ResourceManager.aberrator, Parts.PART_SIGHT, ResourceManager.aberrator_tex);
        body.part(m, ResourceManager.aberrator, Parts.MAGAZINE, ResourceManager.aberrator_tex);
        body.part(m, ResourceManager.aberrator, Parts.PART_BULLET, ResourceManager.aberrator_tex);
        body.part(m, ResourceManager.aberrator, Parts.PART_SLIDE, ResourceManager.aberrator_tex);
        body.part(
                new Matrix4f(m)
                        .translate(0F, 1.25F, -3.625F)
                        .rotate(Axis.XP.rotationDegrees(-45F))
                        .translate(0F, -1.25F, 3.625F),
                ResourceManager.aberrator,
                Parts.PART_HAMMER,
                ResourceManager.aberrator_tex);
        body.spinZ(
                new Matrix4f(m).translate(0F, 2F, 4.5F),
                50D,
                turned -> {
                    Matrix4f corners = new Matrix4f();
                    UnitQuad.matrix(
                            -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F,
                            -0.5F, corners);
                    Matrix4f turn = new Matrix4f();
                    int amount = 16;
                    for (int i = 0; i < amount; i++) {
                        turned.quad(
                                new Matrix4f(turn)
                                        .translate(0F, -1.5F, 0F)
                                        .rotate(Axis.ZP.rotationDegrees(-45)),
                                corners,
                                HALO_QUAD,
                                HALO_TEXTURE);
                        turn.rotate(Axis.ZP.rotationDegrees(360F / amount));
                    }
                });
    }

    private static final class Parts {
        static final int GUN = ResourceManager.aberrator.partId("Gun");
        static final int PART_SIGHT = ResourceManager.aberrator.partId("Sight");
        static final int MAGAZINE = ResourceManager.aberrator.partId("Magazine");
        static final int PART_BULLET = ResourceManager.aberrator.partId("Bullet");
        static final int PART_SLIDE = ResourceManager.aberrator.partId("Slide");
        static final int PART_HAMMER = ResourceManager.aberrator.partId("Hammer");
    }
}
