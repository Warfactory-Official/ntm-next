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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderShredder extends ItemRenderWeaponBase {
    protected static String label = "[> <]";
    protected final RenderType body;
    private final Identifier texture;

    public ItemRenderShredder(Identifier texture) {
        this.body = RenderTypes.entityCutout(texture);
        this.texture = texture;
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
                stack, pose, -1.5F * offset, -1.25F * offset, 1.5F * offset, 0, -6.25 / 8D, 0.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        Player player = Minecraft.getInstance().player;
        double scale = 0.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] mag = HbmAnimations.getRelevantTransformation("MAG");
        double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
        double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");

        pose.translate(0, -2, -6);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 2, 6);

        pose.translate(0, 0, -4);
        pose.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        pose.translate(0, 0, 4);

        pose.translate(0, 0, recoil[2]);

        boolean sexy = stack.getItem() == ModItems.GUN_AUTOSHOTGUN_SEXY.get();

        if (sexy
                || (ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F)) {
            pose.pushPose();
            Font font = Minecraft.getInstance().font;
            float f3 = 0.04F;
            pose.translate((font.width(label) / 2) * f3, 3.25F, -1.75F);
            pose.scale(f3, -f3, f3);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            float variance = 0.9F + player.getRandom().nextFloat() * 0.1F;
            int channel = (int) (variance * 255F);
            int color = 0xFF000000 | (sexy ? channel << 16 : channel << 8);
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

        submitPart(collector, pose, body, ResourceManager.shredder, Parts.GUN, light);

        pose.pushPose();
        pose.translate(mag[0], mag[1], mag[2]);
        pose.translate(0, -1, -0.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) speen[0]));
        pose.translate(0, 1, 0.5);
        submitPart(collector, pose, body, ResourceManager.shredder, Parts.MAGAZINE, light);
        pose.translate(0, -1, -0.5);
        pose.mulPose(Axis.ZP.rotationDegrees((float) cycle[2]));
        pose.translate(0, 1, 0.5);
        submitPart(collector, pose, body, ResourceManager.shredder, Parts.SHELLS, light);
        pose.popPose();

        double smokeScale = 0.75;

        pose.pushPose();
        pose.translate(0, 1, 7.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
        renderSmokeNodes(collector, pose, gun.getConfig(stack, 0).smokeNodes, 0.5D, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 1, 7.5);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 7.5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -1.5F * offset, -1.25F * offset, 1.5F * offset).scale(0.25F);
        body.part(m, ResourceManager.shredder, Parts.GUN, texture);
        body.part(m, ResourceManager.shredder, Parts.MAGAZINE, texture);
        body.part(m, ResourceManager.shredder, Parts.SHELLS, texture);
    }

    private static final class Parts {
        static final int GUN = ResourceManager.shredder.partId("Gun");
        static final int MAGAZINE = ResourceManager.shredder.partId("Magazine");
        static final int SHELLS = ResourceManager.shredder.partId("Shells");
    }
}
