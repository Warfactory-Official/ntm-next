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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderChemthrower extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.chemthrower_tex);

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack, pose, -2.5F * offset, -2.5F * offset, 2.5F * offset, 0, -4.375 / 8D, 1);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

        pose.translate(0, -2, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 4);

        pose.mulPose(Axis.YP.rotationDegrees(90));
        submitPart(collector, pose, body, ResourceManager.chemthrower, Parts.GUN, light);
        submitPart(collector, pose, body, ResourceManager.chemthrower, Parts.HOSE, light);
        submitPart(collector, pose, body, ResourceManager.chemthrower, Parts.NOZZLE, light);

        pose.translate(0, 0.875, 1.75);
        IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        double d =
                (double) mag.getAmount(stack, Minecraft.getInstance().player.getInventory())
                        / (double) mag.getCapacity(stack);
        pose.mulPose(Axis.XP.rotationDegrees((float) (135 - d * 270)));
        pose.translate(0, -0.875, -1.75);

        submitPart(collector, pose, body, ResourceManager.chemthrower, Parts.GAUGE, light);
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float offset = 0.8F;
        Matrix4f m =
                restSetup(0.875F, -2.5F * offset, -2.5F * offset, 2.5F * offset)
                        .scale(0.75F)
                        .rotate(Axis.YP.rotationDegrees(90));
        body.part(m, ResourceManager.chemthrower, Parts.GUN, ResourceManager.chemthrower_tex);
        body.part(m, ResourceManager.chemthrower, Parts.HOSE, ResourceManager.chemthrower_tex);
        body.part(m, ResourceManager.chemthrower, Parts.NOZZLE, ResourceManager.chemthrower_tex);

        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        double d = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);
        body.part(
                m.translate(0F, 0.875F, 1.75F)
                        .rotate(Axis.XP.rotationDegrees((float) (135 - d * 270)))
                        .translate(0F, -0.875F, -1.75F),
                ResourceManager.chemthrower,
                Parts.GAUGE,
                ResourceManager.chemthrower_tex);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.chemthrower.partId("Gun");
        static final int HOSE = ResourceManager.chemthrower.partId("Hose");
        static final int NOZZLE = ResourceManager.chemthrower.partId("Nozzle");
        static final int GAUGE = ResourceManager.chemthrower.partId("Gauge");
    }
}
