// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.render.OtherHandCollector;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.config.GunVisualConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.platform.Services;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.UnitQuad;
import com.hbm.render.util.Vertices;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public abstract class ItemRenderWeaponBase {

    public static final int FULL_BRIGHT = LightCoordsUtil.FULL_BRIGHT;
    public static final int FLASH_LIGHT = LightCoordsUtil.pack(15, 0);
    public static final int GAP_FLASH = 75;
    private static final UnitQuad FLASH_A =
            UnitQuad.of(new float[] {1F, 1F, 0F, 1F, 0F, 0F, 1F, 0F}, 0F, 1F, 0F);
    private static final UnitQuad FLASH_B =
            UnitQuad.of(new float[] {0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F}, 0F, 1F, 0F);
    private static final HashMap<Item, ItemRenderWeaponBase> RENDERERS = new HashMap<>();
    public static float interp;

    public static HashMap<LivingEntity, Long> flashMap = new HashMap<>();

    public static void register(Supplier<? extends Item> item, ItemRenderWeaponBase renderer) {
        RENDERERS.put(item.get(), renderer);
    }

    public static void expireFlashes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            flashMap.clear();
            return;
        }
        if (level.getGameTime() % 30L != 0L) return;
        long now = GameTime.millis(level);
        flashMap.values().removeIf(stamp -> now - stamp >= 150L);
    }

    public static ItemRenderWeaponBase get(ItemStack stack) {
        return stack.isEmpty() ? null : RENDERERS.get(stack.getItem());
    }

    public static float adjustHudFov(float computed) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return computed;
        ItemStack stack = player.getMainHandItem();
        ItemRenderWeaponBase renderer = get(stack);
        if (renderer == null) return computed;
        float base =
                GunVisualConfig.modelFov
                        ? Minecraft.getInstance().options.fov().get().intValue()
                        : renderer.getBaseFOV(stack);
        return computed * (base / 70F);
    }

    public static float adjustViewFov(float computed) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.getCameraEntity() != player) return computed;
        ItemStack stack = player.getMainHandItem();
        ItemRenderWeaponBase renderer = get(stack);
        if (renderer == null) return computed;
        return renderer.getViewFOV(stack, computed);
    }

    public static void standardAimingTransform(
            ItemStack stack,
            PoseStack pose,
            double sX,
            double sY,
            double sZ,
            double aX,
            double aY,
            double aZ) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        double x = sX + (aX - sX) * aimingProgress;
        double y = sY + (aY - sY) * aimingProgress;
        double z = sZ + (aZ - sZ) * aimingProgress;
        pose.translate(x, y, z);
    }

    public static void submitPart(
            SubmitNodeCollector collector,
            PoseStack pose,
            RenderType type,
            HFRWavefrontObject model,
            int part,
            int light) {
        submitPart(collector, pose, type, model, part, light, -1);
    }

    public static void submitPart(
            SubmitNodeCollector collector,
            PoseStack pose,
            RenderType type,
            HFRWavefrontObject model,
            int part,
            int light,
            int color) {
        collector.submitCustomGeometry(
                pose, type, (p, buffer) -> model.renderPart(p, buffer, light, color, part));
    }

    public static void renderSmokeNodes(
            SubmitNodeCollector collector,
            PoseStack pose,
            List<SmokeNode> nodes,
            double scale,
            int light) {
        if (nodes.size() > 1) {
            SmokeNode[] snapshot = nodes.toArray(new SmokeNode[0]);
            int edge = ARGB.colorFromFloat(0F, 1F, 1F, 1F);

            collector.submitCustomGeometry(
                    pose,
                    WeaponRenderTypes.SMOKE,
                    (p, tess) -> {
                        for (int i = 0; i < snapshot.length - 1; i++) {
                            SmokeNode node = snapshot[i];
                            SmokeNode past = snapshot[i + 1];
                            int nodeColor = ARGB.colorFromFloat((float) node.alpha, 1F, 1F, 1F);
                            int pastColor = ARGB.colorFromFloat((float) past.alpha, 1F, 1F, 1F);

                            smokeVertex(
                                    tess, p, node.forward, node.lift, node.side, nodeColor, light);
                            smokeVertex(
                                    tess,
                                    p,
                                    node.forward,
                                    node.lift,
                                    node.side + node.width * scale,
                                    edge,
                                    light);
                            smokeVertex(
                                    tess,
                                    p,
                                    past.forward,
                                    past.lift,
                                    past.side + past.width * scale,
                                    edge,
                                    light);
                            smokeVertex(
                                    tess, p, past.forward, past.lift, past.side, pastColor, light);

                            smokeVertex(
                                    tess, p, node.forward, node.lift, node.side, nodeColor, light);
                            smokeVertex(
                                    tess,
                                    p,
                                    node.forward,
                                    node.lift,
                                    node.side - node.width * scale,
                                    edge,
                                    light);
                            smokeVertex(
                                    tess,
                                    p,
                                    past.forward,
                                    past.lift,
                                    past.side - past.width * scale,
                                    edge,
                                    light);
                            smokeVertex(
                                    tess, p, past.forward, past.lift, past.side, pastColor, light);
                        }
                    });
        }
    }

    private static void smokeVertex(
            VertexConsumer tess,
            PoseStack.Pose p,
            double x,
            double y,
            double z,
            int color,
            int light) {
        Vertices.emit(tess, p, (float) x, (float) y, (float) z, color, 0F, 0F, light, 0F, 1F, 0F);
    }

    public static void renderMuzzleFlash(
            SubmitNodeCollector collector, PoseStack pose, long lastShot) {
        renderMuzzleFlash(collector, pose, lastShot, 75, 15);
    }

    public static void renderMuzzleFlash(
            SubmitNodeCollector collector, PoseStack pose, long lastShot, int duration, double l) {
        MuzzleFlash.NORMAL.submit(collector, pose, lastShot, duration, l, 0);
    }

    public static void renderGapFlash(
            SubmitNodeCollector collector, PoseStack pose, long lastShot) {
        MuzzleFlash.GAP.submit(collector, pose, lastShot, 0, 0, 0);
    }

    public static void renderLaserFlash(
            SubmitNodeCollector collector,
            PoseStack pose,
            long lastShot,
            int flash,
            double scale,
            int color) {
        MuzzleFlash.LASER.submit(collector, pose, lastShot, flash, scale, color);
    }

    public static double flashFire(long lastShot, int duration) {
        long elapsed = GameTime.now() - lastShot;
        return elapsed < duration ? elapsed / (double) duration : Double.NaN;
    }

    public static void muzzleFlashQuads(
            double fire, double l, Matrix4f scratch, UnitQuad.Visitor visitor) {
        float width = (float) (6 * fire);
        float length = (float) (l * fire);
        float inset = 2;
        FLASH_A.visit(
                visitor,
                scratch,
                0,
                -width,
                -inset,
                0,
                width,
                -inset,
                0.1F,
                width,
                length - inset,
                0.1F,
                -width,
                length - inset);
        FLASH_B.visit(
                visitor,
                scratch,
                0,
                width,
                inset,
                0,
                -width,
                inset,
                0.1F,
                -width,
                -length + inset,
                0.1F,
                width,
                -length + inset);
        FLASH_B.visit(
                visitor,
                scratch,
                0,
                -inset,
                width,
                0,
                -inset,
                -width,
                0.1F,
                length - inset,
                -width,
                0.1F,
                length - inset,
                width);
        FLASH_A.visit(
                visitor,
                scratch,
                0,
                inset,
                -width,
                0,
                inset,
                width,
                0.1F,
                -length + inset,
                width,
                0.1F,
                -length + inset,
                -width);
    }

    public static void gapFlashQuads(double fire, Matrix4f scratch, UnitQuad.Visitor visitor) {
        float height = (float) (4 * fire);
        float length = (float) (15 * fire);
        float lift = (float) (3 * fire);
        float offset = (float) (1 * fire);
        float lengthOffset = 0.125F;
        FLASH_A.visit(
                visitor,
                scratch,
                0,
                -height,
                -offset,
                0,
                height,
                -offset,
                0,
                height + lift,
                length - offset,
                0,
                -height + lift,
                length - offset);
        FLASH_B.visit(
                visitor,
                scratch,
                0,
                height,
                offset,
                0,
                -height,
                offset,
                0,
                -height + lift,
                -length + offset,
                0,
                height + lift,
                -length + offset);
        FLASH_A.visit(
                visitor,
                scratch,
                0,
                -height,
                -offset,
                0,
                height,
                -offset,
                lengthOffset,
                height,
                length - offset,
                lengthOffset,
                -height,
                length - offset);
        FLASH_B.visit(
                visitor,
                scratch,
                0,
                height,
                offset,
                0,
                -height,
                offset,
                lengthOffset,
                -height,
                -length + offset,
                lengthOffset,
                height,
                -length + offset);
    }

    public static void laserFlashQuads(
            double fire, double scale, Matrix4f scratch, UnitQuad.Visitor visitor) {
        float size = (float) (4 * fire * scale);
        FLASH_A.visit(
                visitor, scratch, 0, -size, -size, 0, size, -size, 0, size, size, 0, -size, size);
    }

    public void submitFirstPerson(
            ItemStack stack,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            boolean otherArm) {
        Minecraft minecraft = Minecraft.getInstance();
        interp =
                minecraft
                        .gameRenderer
                        .mainCamera()
                        .getCameraEntityPartialTicks(minecraft.getDeltaTracker());
        pose.pushPose();
        pose.setIdentity();

        pose.mulPose(RenderSystem.getModelViewMatrixCopy().invert());
        if (otherArm) pose.scale(-1F, 1F, 1F);
        setupTransformsAndSubmit(
                stack,
                minecraft.player,
                pose,
                otherArm ? new OtherHandCollector(collector) : collector,
                light);
        pose.popPose();
    }

    public void submitRest(
            ItemStack stack,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            boolean otherArm) {
        pose.pushPose();
        if (otherArm) pose.scale(-1F, 1F, 1F);
        pose.translate(-0.56F, 0.52F, 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        submitRestBody(
                stack, pose, otherArm ? new OtherHandCollector(collector) : collector, light);
        pose.popPose();
    }

    public static Matrix4f restFrame(Matrix4f frame, boolean otherArm) {
        if (otherArm) frame.scale(-1F, 1F, 1F);
        return frame.translate(-0.56F, 0.52F, 0.72F).rotate(Axis.YP.rotationDegrees(180));
    }

    public void submitRestBody(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        restBody(stack, new NativeRestBody(pose, collector, light));
    }

    public abstract void restBody(ItemStack stack, RestBody body);

    protected static Matrix4f restSetup(float anchor, double sX, double sY, double sZ) {
        return new Matrix4f()
                .translate(0F, 0F, anchor)
                .translate((float) sX, (float) sY, (float) sZ);
    }

    public boolean isAkimbo(LivingEntity entity) {
        return false;
    }

    protected float getBaseFOV(ItemStack stack) {
        return 70F;
    }

    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        return fov * (1 - aimingProgress * aimZoom(stack));
    }

    protected float aimZoom(ItemStack stack) {
        return 0.33F;
    }

    protected float getSwayMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F;
    }

    protected float getSwayPeriod(ItemStack stack) {
        return 0.75F;
    }

    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? aimedTurnMagnitude() : idleTurnMagnitude();
    }

    protected float aimedTurnMagnitude() {
        return 2.5F;
    }

    protected float idleTurnMagnitude() {
        return -0.25F;
    }

    protected void setupTransformsAndSubmit(
            ItemStack stack,
            LocalPlayer player,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light) {

        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

        float armPitch = Mth.lerp(interp, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(interp, player.yBobO, player.yBob);
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        (player.getViewXRot(interp) - armPitch) * 0.1F * turnMagnitude));
        pose.mulPose(
                Axis.YP.rotationDegrees(
                        (player.getViewYRot(interp) - armYaw) * 0.1F * turnMagnitude));

        pose.mulPose(Axis.YP.rotationDegrees(180));

        float distanceInterp = player.avatarState().getBackwardsInterpolatedWalkDistance(interp);
        float camYaw = player.avatarState().getInterpolatedBob(interp);
        pose.translate(
                Mth.sin(distanceInterp * (float) Math.PI * swayPeriod)
                        * camYaw
                        * 0.5F
                        * swayMagnitude,
                -Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod) * camYaw)
                        * swayMagnitude,
                0.0F);
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 3.0F));
        pose.mulPose(
                Axis.XP.rotationDegrees(
                        Math.abs(
                                        Mth.cos(
                                                        distanceInterp
                                                                        * (float) Math.PI
                                                                        * swayPeriod
                                                                - 0.2F)
                                                * camYaw)
                                * 5.0F));

        setupFirstPerson(stack, pose);
        renderFirstPerson(stack, pose, collector, light);
    }

    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);

        if (Minecraft.getInstance().player.isShiftKeyDown()) {
            pose.translate(0, -3.875 / 8D, 0);
        } else {
            float offset = 0.8F;
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(offset, -0.75F * offset, -0.5F * offset);
            pose.mulPose(Axis.YP.rotationDegrees(180));
        }
    }

    public abstract void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light);
}
