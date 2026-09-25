// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderQuadro extends ItemRenderWeaponBase {
    protected static String label = ">> <<";
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.quadro_tex);
    private final RenderType rockets = RenderTypes.entityCutout(ResourceManager.quadro_rocket_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -2.5F * offset,
                -3.5F * offset,
                2.5F * offset,
                -1.5F * offset,
                -3F * offset,
                2.5F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 1.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] reloadPush = HbmAnimations.getRelevantTransformation("RELOAD_PUSH");
        double[] reloadRotate = HbmAnimations.getRelevantTransformation("RELOAD_ROTATE");

        pose.translate(0, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 1);

        pose.translate(0, 0, recoil[2]);

        pose.translate(0, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) reloadRotate[2]));
        pose.translate(0, 1, 1);

        submitPart(collector, pose, body, ResourceManager.quadro, Parts.LAUNCHER, light);

        pose.pushPose();
        pose.translate(0, -1, 0);
        pose.translate(0, 3, 0);
        pose.mulPose(Axis.XP.rotationDegrees((float) (reloadPush[1] * 30)));
        pose.translate(0, -3, 0);
        pose.translate(0, 0, reloadPush[0] * 3);
        submitPart(collector, pose, rockets, ResourceManager.quadro, Parts.ROCKETS, light);
        pose.popPose();

        if (ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F) {
            pose.pushPose();
            Font font = Minecraft.getInstance().font;
            float f3 = 0.04F;
            pose.translate(-0.375F, 2.25F, 0.875F);
            pose.mulPose(Axis.YN.rotationDegrees((float) (180D + (GameTime.now() / 2) % 360D)));
            pose.translate(-(font.width(label) / 2) * f3, 0, 0);
            pose.scale(f3, -f3, f3);
            collector.submitText(
                    pose,
                    0,
                    0,
                    Component.literal(label).getVisualOrderText(),
                    false,
                    Font.DisplayMode.NORMAL,
                    FULL_BRIGHT,
                    0xFF00FFFF,
                    0,
                    0);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(-1, 0.75, 6.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 150, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -2.5F * offset, -3.5F * offset, 2.5F * offset).scale(1.75F);
        body.part(m, ResourceManager.quadro, Parts.LAUNCHER, ResourceManager.quadro_tex);
        body.part(
                new Matrix4f(m).translate(0F, -1F, 0F),
                ResourceManager.quadro,
                Parts.ROCKETS,
                ResourceManager.quadro_rocket_tex);
    }

    private static final class Parts {
        static final int LAUNCHER = ResourceManager.quadro.partId("Launcher");
        static final int ROCKETS = ResourceManager.quadro.partId("Rockets");
    }
}
