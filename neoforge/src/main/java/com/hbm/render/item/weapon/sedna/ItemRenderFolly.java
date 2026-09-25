// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

public class ItemRenderFolly extends ItemRenderWeaponBase {
    public static long timeAiming;
    public static boolean jingle = false;
    public static boolean wasAiming = false;
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.folly_tex);

    public static Component getBootSplash() {
        long now = GameTime.now();
        if (timeAiming + 5000 < now) return null;
        if (timeAiming + 3000 > now) return null;
        int splashIndex = (int) ((now - timeAiming - 3000) * 35 / 2000) - 10;
        char[] letters = "VStarOS".toCharArray();
        MutableComponent splash = Component.empty();
        for (int i = 0; i < letters.length; i++) {
            ChatFormatting color;
            if (i < splashIndex - 1) color = ChatFormatting.LIGHT_PURPLE;
            else if (i == splashIndex - 1) color = ChatFormatting.AQUA;
            else if (i == splashIndex) color = ChatFormatting.WHITE;
            else if (i == splashIndex + 1) color = ChatFormatting.AQUA;
            else if (i == splashIndex + 2) color = ChatFormatting.LIGHT_PURPLE;
            else color = ChatFormatting.BLACK;
            splash.append(Component.literal(String.valueOf(letters[i])).withStyle(color));
        }
        return splash;
    }

    public static List<Component> getTTY() {
        List<Component> tty = new ArrayList<>();
        long now = GameTime.now();
        int time = (int) (now - timeAiming);
        if (time < 3000) {
            if (time > 250)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.postSuccessfulCode0")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.8388608Bytes")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.5187427Bytes")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 750)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.reticulatingSplines")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 1500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.noKeyboardFound")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 2000)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.bootingFromDevSda1")
                                .withStyle(ChatFormatting.GREEN));
        }
        if (time > 5000) {
            LocalPlayer player = Minecraft.getInstance().player;
            HitResult mop = EntityDamageUtil.getMouseOver(player, 250);
            String target = "Target: ";
            if (mop == null || mop.getType() == HitResult.Type.MISS) target += "N/A";
            if (mop instanceof BlockHitResult blockHit && mop.getType() == HitResult.Type.BLOCK)
                target +=
                        blockHit.getBlockPos().getX()
                                + "/"
                                + blockHit.getBlockPos().getY()
                                + "/"
                                + blockHit.getBlockPos().getZ();
            if (mop instanceof EntityHitResult entityHit)
                target += entityHit.getEntity().getName().getString();
            tty.add(Component.literal(target).withStyle(ChatFormatting.GREEN));
            tty.add(
                    Component.translatable(
                                    "desc.gui.renderFolly.angle",
                                    ((int) (-player.getXRot() * 100) / 100D))
                            .withStyle(ChatFormatting.GREEN));
        }
        return tty;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        float aim = 0.75F;
        standardAimingTransform(
                stack,
                pose,
                -2.5F * offset,
                -1.5F * offset,
                2.75F * offset,
                -2 * aim,
                -1 * aim,
                2.25F * offset);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        LocalPlayer player = Minecraft.getInstance().player;
        double scale = 0.75D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] load = HbmAnimations.getRelevantTransformation("LOAD");
        double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
        double[] screw = HbmAnimations.getRelevantTransformation("SCREW");
        double[] breech = HbmAnimations.getRelevantTransformation("BREECH");

        pose.translate(0, 1, -4);
        pose.mulPose(Axis.XN.rotationDegrees((float) equip[0]));
        pose.translate(0, -1, 4);

        pose.translate(0, -2, -2);
        pose.mulPose(Axis.XP.rotationDegrees((float) load[0]));
        pose.translate(0, 2, 2);

        submitPart(collector, pose, body, ResourceManager.folly, Parts.CANNON, light);

        pose.pushPose();
        pose.translate(recoil[0], recoil[1], recoil[2]);
        submitPart(collector, pose, body, ResourceManager.folly, Parts.BARREL, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(shell[0], shell[1], shell[2]);
        submitPart(collector, pose, body, ResourceManager.folly, Parts.PART_SHELL, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(breech[0], breech[1], breech[2]);
        submitPart(collector, pose, body, ResourceManager.folly, Parts.PART_BREECH, light);
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) screw[2]));
        pose.translate(0, -1, 0);
        submitPart(collector, pose, body, ResourceManager.folly, Parts.COG, light);
        pose.popPose();

        boolean isAiming =
                ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F;
        if (isAiming & !wasAiming) timeAiming = GameTime.millis();

        if (isAiming) {

            Component splash = getBootSplash();

            if (!jingle && splash != null) {
                player.level()
                        .playLocalSound(
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                ModSounds
                                        .GUN_VYLET_PONY_CUTIEMARKS_AND_THE_THINGS_THAT_BIND_US_INTRO_JINGLE
                                        .get(),
                                SoundSource.PLAYERS,
                                0.5F,
                                1F,
                                false);
                jingle = true;
            }

            Font font = Minecraft.getInstance().font;
            float variance = 0.85F + player.getRandom().nextFloat() * 0.15F;
            int color = ARGB.colorFromFloat(1F, variance, variance * 0.5F, 0F);

            if (GameTime.now() - timeAiming > 5000 && load[0] == 0) {
                IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
                String msg = mag.getAmount(stack, player.getInventory()) > 0 ? "+" : "No ammo";
                pose.pushPose();
                float crosshairSize = 0.01F;
                pose.translate(
                        (font.width(msg) / 2) * crosshairSize + 2,
                        1F + font.lineHeight * crosshairSize / 2F,
                        -2.75F);
                pose.scale(crosshairSize, -crosshairSize, crosshairSize);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                collector.submitText(
                        pose,
                        0,
                        0,
                        Component.literal(msg).getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        FULL_BRIGHT,
                        color,
                        0,
                        0);
                pose.popPose();
            }

            if (splash != null) {
                pose.pushPose();
                float splashSize = 0.02F;
                pose.translate(
                        (font.width(splash) / 2) * splashSize + 2,
                        1F + font.lineHeight * splashSize / 2F,
                        -2.75F);
                pose.scale(splashSize, -splashSize, splashSize);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                collector.submitText(
                        pose,
                        0,
                        0,
                        splash.getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        FULL_BRIGHT,
                        color,
                        0,
                        0);
                pose.popPose();
            }

            List<Component> tty = getTTY();
            if (!tty.isEmpty()) {
                pose.pushPose();
                float fontSize = 0.005F;
                pose.translate(2.5F, 1.375F, -2.75F);
                pose.scale(fontSize, -fontSize, fontSize);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                for (Component line : tty) {
                    collector.submitText(
                            pose,
                            0,
                            0,
                            line.getVisualOrderText(),
                            false,
                            Font.DisplayMode.NORMAL,
                            FULL_BRIGHT,
                            color,
                            0,
                            0);
                    pose.translate(0, font.lineHeight + 2, 0);
                }
                pose.popPose();
            }
        } else {
            jingle = false;
        }

        wasAiming = isAiming;
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -2.5F * offset, -1.5F * offset, 2.75F * offset).scale(0.75F);
        body.part(m, ResourceManager.folly, Parts.CANNON, ResourceManager.folly_tex);
        body.part(m, ResourceManager.folly, Parts.BARREL, ResourceManager.folly_tex);
        body.part(m, ResourceManager.folly, Parts.PART_SHELL, ResourceManager.folly_tex);
        body.part(m, ResourceManager.folly, Parts.PART_BREECH, ResourceManager.folly_tex);
        body.part(m, ResourceManager.folly, Parts.COG, ResourceManager.folly_tex);
    }

    private static final class Parts {
        static final int CANNON = ResourceManager.folly.partId("Cannon");
        static final int BARREL = ResourceManager.folly.partId("Barrel");
        static final int PART_SHELL = ResourceManager.folly.partId("Shell");
        static final int PART_BREECH = ResourceManager.folly.partId("Breech");
        static final int COG = ResourceManager.folly.partId("Cog");
    }
}
