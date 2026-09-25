// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
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

public class ItemRenderPanzerschreck extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.panzerschreck_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -2.75F * offset,
                -2F * offset,
                2.5F * offset,
                -0.9375,
                -9.25 / 8D,
                0.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 1.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
        double[] rocket = HbmAnimations.getRelevantTransformation("ROCKET");

        pose.translate(0, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 1);

        pose.translate(0, -4, -3);
        pose.mulPose(Axis.XP.rotationDegrees((float) reload[0]));
        pose.translate(0, 4, 3);

        submitPart(collector, pose, body, ResourceManager.panzerschreck, Parts.TUBE, light);
        if (hasShield(stack))
            submitPart(collector, pose, body, ResourceManager.panzerschreck, Parts.SHIELD, light);

        pose.pushPose();
        pose.translate(rocket[0], rocket[1], rocket[2]);
        submitPart(collector, pose, body, ResourceManager.panzerschreck, Parts.PART_ROCKET, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 6.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 150, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -2.75F * offset, -2F * offset, 2.5F * offset).scale(1.25F);
        body.part(m, ResourceManager.panzerschreck, Parts.TUBE, ResourceManager.panzerschreck_tex);
        if (hasShield(stack)) {
            body.part(
                    m,
                    ResourceManager.panzerschreck,
                    Parts.SHIELD,
                    ResourceManager.panzerschreck_tex);
        }
        body.part(
                m,
                ResourceManager.panzerschreck,
                Parts.PART_ROCKET,
                ResourceManager.panzerschreck_tex);
    }

    public boolean hasShield(ItemStack stack) {
        return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_SHIELD);
    }

    private static final class Parts {
        static final int TUBE = ResourceManager.panzerschreck.partId("Tube");
        static final int SHIELD = ResourceManager.panzerschreck.partId("Shield");
        static final int PART_ROCKET = ResourceManager.panzerschreck.partId("Rocket");
    }
}
