// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public class RenderGlyphid<T extends EntityGlyphid> extends EntityRenderer<T, GlyphidRenderState>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType INFESTED_TYPE =
            WorldRenderPipeline.oneSidedTranslucent(ResourceManager.glyphid_infestation_tex, false);

    protected final int body;
    protected final int jawTop;
    protected final int jawLeft;
    protected final int jawRight;
    protected final int armorFront;
    protected final int armorLeft;
    protected final int armorRight;
    protected final int armLeftUpper;
    protected final int armLeftMid;
    protected final int armLeftLower;
    protected final int armLeftArmor;
    protected final int armRightUpper;
    protected final int armRightMid;
    protected final int armRightLower;
    protected final int armRightArmor;
    protected final int legLeftUpper;
    protected final int legLeftLower;
    protected final int legRightUpper;
    protected final int legRightLower;

    public RenderGlyphid(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;

        int[] parts =
                ResourceManager.glyphid.partIds(
                        "Body",
                        "JawTop",
                        "JawLeft",
                        "JawRight",
                        "ArmorFront",
                        "ArmorLeft",
                        "ArmorRight",
                        "ArmLeftUpper",
                        "ArmLeftMid",
                        "ArmLeftLower",
                        "ArmLeftArmor",
                        "ArmRightUpper",
                        "ArmRightMid",
                        "ArmRightLower",
                        "ArmRightArmor",
                        "LegLeftUpper",
                        "LegLeftLower",
                        "LegRightUpper",
                        "LegRightLower");

        body = parts[0];
        jawTop = parts[1];
        jawLeft = parts[2];
        jawRight = parts[3];
        armorFront = parts[4];
        armorLeft = parts[5];
        armorRight = parts[6];
        armLeftUpper = parts[7];
        armLeftMid = parts[8];
        armLeftLower = parts[9];
        armLeftArmor = parts[10];
        armRightUpper = parts[11];
        armRightMid = parts[12];
        armRightLower = parts[13];
        armRightArmor = parts[14];
        legLeftUpper = parts[15];
        legLeftLower = parts[16];
        legRightUpper = parts[17];
        legRightLower = parts[18];
    }

    @Override
    public GlyphidRenderState createRenderState() {
        return new GlyphidRenderState();
    }

    @Override
    public void extractRenderState(T entity, GlyphidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = entity.getSkin();
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.scale = entity.getGlyphidScale();
        state.armor = entity.armor();
        state.subtype = entity.subtype();
        state.walkCycle = entity.walkAnimation.position(partialTicks);
        state.swingProgress = entity.getAttackAnim(partialTicks);
    }

    @Override
    public void submit(
            GlyphidRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        RenderType type = WorldRenderPipeline.oneSidedCutout(state.texture);

        pose.pushPose();
        applyBaseTransform(pose, state);
        submitLimbs(state, pose, collector, light, type, -1);
        if (state.subtype == EntityGlyphid.TYPE_INFECTED) {
            submitLimbs(state, pose, collector, light, INFESTED_TYPE, -1);
        }
        pose.popPose();

        submitVanillaTail(state, pose, collector, camera);
    }

    protected final void submitVanillaTail(
            GlyphidRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
    }

    protected void applyBaseTransform(PoseStack pose, GlyphidRenderState state) {
        pose.mulPose(Axis.YP.rotationDegrees(-state.bodyYaw));
        float s = (float) state.scale;
        pose.scale(s, s, s);
    }

    protected final void submitLimbs(
            GlyphidRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            int color) {
        byte armor = state.armor;
        double walkCycle = state.walkCycle;

        double cy0 = Math.sin(walkCycle % (Math.PI * 2));
        double cy1 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.5);
        double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);

        double bite =
                Mth.clamp(Math.sin(state.swingProgress * Math.PI * 2 - Math.PI * 0.5), 0D, 1D) * 20;
        double headTilt = Math.sin(state.swingProgress * Math.PI) * 30;

        part(pose, collector, light, type, color, body);
        if ((armor & (1 << 0)) > 0) part(pose, collector, light, type, color, armorFront);
        if ((armor & (1 << 1)) > 0) part(pose, collector, light, type, color, armorLeft);
        if ((armor & (1 << 2)) > 0) part(pose, collector, light, type, color, armorRight);

        pose.pushPose();
        pose.translate(0.25F, 0.625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(10F));
        pose.mulPose(Axis.XP.rotationDegrees((float) (35 + cy1 * 20)));
        pose.translate(-0.25F, -0.625F, -0.0625F);
        part(pose, collector, light, type, color, armLeftUpper);
        pose.translate(0.25F, 0.625F, 0.4375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy1 * 20 + cy0 * 20)));
        pose.translate(-0.25F, -0.625F, -0.4375F);
        part(pose, collector, light, type, color, armLeftMid);
        pose.translate(0.25F, 0.625F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 - cy0 * 45)));
        pose.translate(-0.25F, -0.625F, -0.9375F);
        part(pose, collector, light, type, color, armLeftLower);
        if ((armor & (1 << 3)) > 0) part(pose, collector, light, type, color, armLeftArmor);
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.25F, 0.625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(-10F));
        pose.mulPose(Axis.XP.rotationDegrees((float) (35 + cy2 * 20)));
        pose.translate(0.25F, -0.625F, -0.0625F);
        part(pose, collector, light, type, color, armRightUpper);
        pose.translate(-0.25F, 0.625F, 0.4375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy2 * 20 + cy3 * 20)));
        pose.translate(0.25F, -0.625F, -0.4375F);
        part(pose, collector, light, type, color, armRightMid);
        pose.translate(-0.25F, 0.625F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 - cy3 * 45)));
        pose.translate(0.25F, -0.625F, -0.9375F);
        part(pose, collector, light, type, color, armRightLower);
        if ((armor & (1 << 4)) > 0) part(pose, collector, light, type, color, armRightArmor);
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.ZP.rotationDegrees((float) headTilt));
        pose.translate(0F, -0.5F, -0.25F);

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.XP.rotationDegrees((float) -bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, collector, light, type, color, jawTop);
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees((float) bite));
        pose.mulPose(Axis.XP.rotationDegrees((float) bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, collector, light, type, color, jawLeft);
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees((float) -bite));
        pose.mulPose(Axis.XP.rotationDegrees((float) bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, collector, light, type, color, jawRight);
        pose.popPose();
        pose.popPose();

        double steppy = 15;
        double bend = 60;

        for (int i = 0; i < 3; i++) {
            double c0 = cy0 * (i == 1 ? -1 : 1);
            double c1 = cy1 * (i == 1 ? -1 : 1);

            pose.pushPose();
            pose.translate(0F, 0.25F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 15 + c0 * 7.5)));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (steppy + c1 * steppy)));
            pose.translate(0F, -0.25F, 0F);
            part(pose, collector, light, type, color, legLeftUpper);
            pose.translate(0.5625F, 0.25F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (-bend - c1 * steppy)));
            pose.translate(-0.5625F, -0.25F, 0F);
            part(pose, collector, light, type, color, legLeftLower);
            pose.popPose();

            pose.pushPose();
            pose.translate(0F, 0.25F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 45 + c0 * 7.5)));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (-steppy + c1 * steppy)));
            pose.translate(0F, -0.25F, 0F);
            part(pose, collector, light, type, color, legRightUpper);
            pose.translate(-0.5625F, 0.25F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (bend - c1 * steppy)));
            pose.translate(0.5625F, -0.25F, 0F);
            part(pose, collector, light, type, color, legRightLower);
            pose.popPose();
        }
    }

    private void part(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            int color,
            int partId) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buf) -> ResourceManager.glyphid.renderPart(p, buf, light, color, partId));
    }
}
