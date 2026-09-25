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

public class ItemRenderLaserPistol extends ItemRenderWeaponBase {
    private final Identifier texture;
    private final RenderType body;

    public ItemRenderLaserPistol(Identifier texture) {
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
                stack, pose, -1.75F * offset, -2F * offset, 2.75F * offset, 0, -10 / 8D, 1.25);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] jolt = HbmAnimations.getRelevantTransformation("JOLT");
        double[] battery = HbmAnimations.getRelevantTransformation("BATTERY");
        double[] swirl = HbmAnimations.getRelevantTransformation("SWIRL");

        pose.translate(0, -1, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 1, 6);

        pose.translate(0, 2, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, -2, 2);

        pose.translate(0, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) swirl[0]));
        pose.translate(0, 1, 1);

        pose.translate(0, 0, recoil[2]);
        pose.translate(jolt[0], jolt[1], jolt[2]);

        submitPart(collector, pose, body, ResourceManager.laser_pistol, Parts.GUN, light);
        if (hasCapacitors(stack))
            submitPart(
                    collector, pose, body, ResourceManager.laser_pistol, Parts.CAPACITORS, light);
        if (hasTape(stack))
            submitPart(collector, pose, body, ResourceManager.laser_pistol, Parts.TAPE, light);

        pose.pushPose();
        pose.translate(1.125, 0, -1.9125);
        pose.mulPose(Axis.YP.rotationDegrees((float) latch[1]));
        pose.translate(-1.125, 0, 1.9125);
        submitPart(collector, pose, body, ResourceManager.laser_pistol, Parts.PART_LATCH, light);
        pose.translate(battery[0], battery[1], battery[2]);
        submitPart(collector, pose, body, ResourceManager.laser_pistol, Parts.PART_BATTERY, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 2, 4.75);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        renderLaserFlash(
                collector,
                pose,
                gun.lastShot[0],
                150,
                1.5D,
                hasEmerald(stack) ? 0x008000 : 0xff0000);
        pose.translate(0, 0, -0.25);
        renderLaserFlash(
                collector,
                pose,
                gun.lastShot[0],
                150,
                0.75D,
                hasEmerald(stack) ? 0x80ff00 : 0xff8000);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.75F * offset, -2F * offset, 2.75F * offset).scale(0.375F);
        body.part(m, ResourceManager.laser_pistol, Parts.GUN, texture);
        if (hasCapacitors(stack))
            body.part(m, ResourceManager.laser_pistol, Parts.CAPACITORS, texture);
        if (hasTape(stack)) body.part(m, ResourceManager.laser_pistol, Parts.TAPE, texture);
        body.part(m, ResourceManager.laser_pistol, Parts.PART_LATCH, texture);
        body.part(m, ResourceManager.laser_pistol, Parts.PART_BATTERY, texture);
    }

    public boolean hasCapacitors(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_LASER_PISTOL_PEW_PEW.get();
    }

    public boolean hasTape(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_LASER_PISTOL_PEW_PEW.get();
    }

    public boolean hasEmerald(ItemStack stack) {
        return stack.getItem() == ModItems.GUN_LASER_PISTOL_MORNING_GLORY.get();
    }

    private static final class Parts {
        static final int GUN = ResourceManager.laser_pistol.partId("Gun");
        static final int CAPACITORS = ResourceManager.laser_pistol.partId("Capacitors");
        static final int TAPE = ResourceManager.laser_pistol.partId("Tape");
        static final int PART_LATCH = ResourceManager.laser_pistol.partId("Latch");
        static final int PART_BATTERY = ResourceManager.laser_pistol.partId("Battery");
    }
}
