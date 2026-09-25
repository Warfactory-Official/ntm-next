// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.items.weapon.ItemMissile.MissileFormFactor;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadLarge;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchPadLarge
        implements BlockEntityRenderer<BlockEntityLaunchPadLarge, RenderLaunchPadLarge.State>,
                ConcurrentRenderStateExtraction {

    private static final RenderType PLATFORM =
            RenderTypes.entityCutoutCull(ResourceManager.missile_erector_pad_tex);

    private static final double MISSILE_Y = 2D;

    private @Nullable Erectors erectors;

    private Erectors erectors() {
        Erectors resolved = erectors;
        if (resolved != null) return resolved;
        Erector[] rows = new Erector[MissileFormFactor.values().length];
        rows[MissileFormFactor.ABM.ordinal()] =
                row("ABM", 1.5D, 1.25D, ResourceManager.missile_erector_abm_tex);
        rows[MissileFormFactor.MICRO.ordinal()] =
                row("Micro", 1.5D, 1.25D, ResourceManager.missile_erector_micro_tex);
        rows[MissileFormFactor.V2.ordinal()] =
                row("V2", 1.75D, 1.25D, ResourceManager.missile_erector_v2_tex);
        rows[MissileFormFactor.STRONG.ordinal()] =
                row("Strong", 3D, 1.5D, ResourceManager.missile_erector_strong_tex);
        rows[MissileFormFactor.HUGE.ordinal()] =
                row("Huge", 3D, 1.5D, ResourceManager.missile_erector_huge_tex);
        rows[MissileFormFactor.ATLAS.ordinal()] =
                row("Atlas", 4D, 1.5D, ResourceManager.missile_erector_atlas_tex);

        rows[MissileFormFactor.OTHER.ordinal()] = rows[MissileFormFactor.ABM.ordinal()];

        resolved = new Erectors(rows, ResourceManager.missile_erector.partId("Pad"));
        erectors = resolved;
        return resolved;
    }

    private static Erector row(String prefix, double pivotZ, double pivotY, Identifier texture) {
        HFRWavefrontObject mesh = ResourceManager.missile_erector;
        return new Erector(
                mesh.partId(prefix + "_Pad"),
                mesh.partId(prefix + "_Erector"),
                mesh.partId(prefix + "_Pivot"),
                mesh.partId(prefix + "_Rope"),
                pivotZ,
                pivotY,
                RenderTypes.entityCutoutCull(texture));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityLaunchPadLarge pad) {
        BlockPos pos = pad.getBlockPos();
        return new AABB(
                pos.getX() - 19.4375,
                pos.getY() - 1.4375,
                pos.getZ() - 19.4375,
                pos.getX() + 20.4375,
                pos.getY() + 17.9375,
                pos.getZ() + 20.4375);
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchPadLarge pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(pad.getBlockState());
        Erectors resolved = erectors();
        Erector[] rows = resolved.rows();
        state.erector =
                pad.formFactor >= 0 && pad.formFactor < rows.length ? rows[pad.formFactor] : null;
        state.platformPart = resolved.platformPart();
        state.erected = pad.erected;
        state.readyToLoad = pad.readyToLoad;
        state.erectorAngle = Mth.lerp(partialTicks, pad.prevErector, pad.erector);
        state.lift = Mth.lerp(partialTicks, pad.prevLift, pad.lift);
        state.missile =
                pad.loadedMissile == null ? null : PadMissiles.table().get(pad.loadedMissile);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));

        draw(poseStack, collector, light, PLATFORM, state.platformPart);

        Erector erector = state.erector;
        if (erector != null) {
            poseStack.pushPose();
            draw(poseStack, collector, light, erector.skin(), erector.pad());
            if (state.missile != null && state.erected) {
                draw(poseStack, collector, light, erector.skin(), erector.rope());
            }
            poseStack.translate(0D, erector.pivotY(), -erector.pivotZ());
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.erectorAngle));
            poseStack.translate(0D, -erector.pivotY(), erector.pivotZ());
            draw(poseStack, collector, light, erector.skin(), erector.pivot());
            poseStack.translate(0D, state.lift, 0D);
            draw(poseStack, collector, light, erector.skin(), erector.arm());

            if (state.erected) {
                poseStack.popPose();
                poseStack.pushPose();
            }

            PadMissiles.Missile missile = state.missile;
            if (missile != null && (state.erected || state.readyToLoad)) {
                poseStack.translate(0D, MISSILE_Y, 0D);
                if (missile.scale() != 1F) {
                    poseStack.scale(missile.scale(), missile.scale(), missile.scale());
                }
                collector.submitCustomGeometry(
                        poseStack,
                        missile.type(),
                        (pose, buffer) -> missile.mesh().render(pose, buffer, light, -1));
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void draw(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            int part) {
        HFRWavefrontObject mesh = ResourceManager.missile_erector;
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, part));
    }

    private record Erectors(Erector[] rows, int platformPart) {}

    private record Erector(
            int pad, int arm, int pivot, int rope, double pivotZ, double pivotY, RenderType skin) {}

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        @Nullable Erector erector;
        int platformPart;
        boolean erected;
        boolean readyToLoad;
        float erectorAngle;
        float lift;
        PadMissiles.@Nullable Missile missile;
    }
}
