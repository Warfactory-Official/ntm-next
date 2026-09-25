// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderFlamer extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderFlamer(Identifier texture) {
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
                stack, pose, -1.5F * offset, -1.5F * offset, 2.75F * offset, 0, -4.625 / 8D, 0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

        pose.translate(0, 2, -6);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, -2, 6);

        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rotate[2]));
        pose.translate(0, -1, 0);

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Gun");
        submitPart(collector, pose, body, ResourceManager.flamethrower, Parts.GUN, light);
        if (hasShield(stack))
            submitPart(
                    collector, pose, body, ResourceManager.flamethrower, Parts.HEAT_SHIELD, light);
        pose.popPose();

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Tank");
        submitPart(collector, pose, body, ResourceManager.flamethrower, Parts.TANK, light);
        pose.popPose();

        pose.pushPose();
        HbmAnimations.applyRelevantTransformation(pose, "Gauge");
        pose.translate(1.25, 1.25, 0);
        IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        (float)
                                (-135
                                        + (mag.getAmount(
                                                        stack,
                                                        Minecraft.getInstance()
                                                                .player
                                                                .getInventory())
                                                * 270D
                                                / mag.getCapacity(stack)))));
        pose.translate(-1.25, -1.25, 0);
        submitPart(collector, pose, body, ResourceManager.flamethrower, Parts.GAUGE, light);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -1.5F * offset, -1.5F * offset, 2.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.flamethrower, Parts.GUN, texture);
        if (hasShield(stack))
            body.part(m, ResourceManager.flamethrower, Parts.HEAT_SHIELD, texture);
        body.part(m, ResourceManager.flamethrower, Parts.TANK, texture);

        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        body.part(
                m.translate(1.25F, 1.25F, 0F)
                        .rotate(
                                Axis.ZP.rotationDegrees(
                                        (float)
                                                (-135
                                                        + (mag.getAmount(stack, null)
                                                                * 270D
                                                                / mag.getCapacity(stack)))))
                        .translate(-1.25F, -1.25F, 0F),
                ResourceManager.flamethrower,
                Parts.GAUGE,
                texture);
    }

    public boolean hasShield(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_FLAMER_DAYBREAKER.get();
    }

    private static final class Parts {
        static final int GUN = ResourceManager.flamethrower.partId("Gun");
        static final int HEAT_SHIELD = ResourceManager.flamethrower.partId("HeatShield");
        static final int TANK = ResourceManager.flamethrower.partId("Tank");
        static final int GAUGE = ResourceManager.flamethrower.partId("Gauge");
    }
}
