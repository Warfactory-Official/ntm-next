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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderMissileLauncher extends ItemRenderWeaponBase {
    protected static String label = "AUTO";
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.missile_launcher_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
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
                stack,
                pose,
                -1.5F * offset,
                -1.25F * offset,
                0.5F * offset,
                -1F * offset,
                -1.25F * offset,
                0F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        Player player = Minecraft.getInstance().player;
        double scale = 0.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
        double[] open = HbmAnimations.getRelevantTransformation("OPEN");
        double[] missile = HbmAnimations.getRelevantTransformation("MISSILE");

        pose.translate(0, -2, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 2);

        submitPart(collector, pose, body, ResourceManager.missile_launcher, Parts.LAUNCHER, light);

        pose.pushPose();

        pose.translate(0, 0.25, 1.6875);
        pose.mulPose(Axis.XP.rotationDegrees((float) open[0]));
        pose.translate(0, -0.25, -1.6875);

        submitPart(collector, pose, body, ResourceManager.missile_launcher, Parts.FRONT, light);

        pose.pushPose();
        pose.translate(0, 0, barrel[2]);
        submitPart(
                collector, pose, body, ResourceManager.missile_launcher, Parts.PART_BARREL, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(missile[0], missile[1], missile[2]);
        submitPart(
                collector, pose, body, ResourceManager.missile_launcher, Parts.PART_MISSILE, light);
        pose.popPose();

        pose.popPose();

        if (ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F) {
            pose.pushPose();
            Font font = Minecraft.getInstance().font;
            float f3 = 0.04F;
            pose.translate(0.9375F, 2.25F, -0.5625F + (font.width(label) / 2) * f3);
            pose.scale(f3, -f3, f3);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            float variance = 0.7F + player.getRandom().nextFloat() * 0.3F;
            int color = 0xFF000000 | ((int) (variance * 255F) << 16);
            collector.submitText(
                    pose,
                    0,
                    0,
                    Component.literal(label).getVisualOrderText(),
                    false,
                    Font.DisplayMode.NORMAL,
                    FULL_BRIGHT,
                    color,
                    0,
                    0);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(0, 1, 6.75);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1.25F * offset, 0.5F * offset).scale(0.5F);
        body.part(
                m,
                ResourceManager.missile_launcher,
                Parts.LAUNCHER,
                ResourceManager.missile_launcher_tex);
        body.part(
                m,
                ResourceManager.missile_launcher,
                Parts.FRONT,
                ResourceManager.missile_launcher_tex);
        body.part(
                m,
                ResourceManager.missile_launcher,
                Parts.PART_BARREL,
                ResourceManager.missile_launcher_tex);
        body.part(
                m,
                ResourceManager.missile_launcher,
                Parts.PART_MISSILE,
                ResourceManager.missile_launcher_tex);
    }

    private static final class Parts {
        static final int LAUNCHER = ResourceManager.missile_launcher.partId("Launcher");
        static final int FRONT = ResourceManager.missile_launcher.partId("Front");
        static final int PART_BARREL = ResourceManager.missile_launcher.partId("Barrel");
        static final int PART_MISSILE = ResourceManager.missile_launcher.partId("Missile");
    }
}
