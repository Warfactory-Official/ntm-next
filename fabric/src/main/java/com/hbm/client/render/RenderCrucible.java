// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.data.MachineData;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityCrucible;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCrucible
        implements BlockEntityRenderer<BlockEntityCrucible, RenderCrucible.State>,
                ConcurrentRenderStateExtraction {

    private static final float MAX_AGE = 20F;

    private final RenderType meltType;
    private final RenderType streamType;

    public RenderCrucible() {

        this.meltType = FlatCutout.of(ResourceManager.foundry_lava_tex);
        this.streamType = FlatCutout.of(ResourceManager.foundry_stream_tex);
    }

    public static void emitStream(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int colorHex,
            Direction dir,
            float length,
            float age,
            float base,
            float offset) {
        Direction rot = dir.getClockWise();
        float lifeFrac = age / MAX_AGE;
        float width = 0.0625F + lifeFrac * 0.0625F;
        float girth = 0.125F * (1F - lifeFrac);

        int argb = FoundryFaces.brightenMolten(colorHex);

        float dirXG = dir.getStepX() * girth;
        float dirZG = dir.getStepZ() * girth;
        float rotXW = rot.getStepX() * width;
        float rotZW = rot.getStepZ() * width;

        float uMin = 0.5F - width;
        float uMax = 0.5F + width;
        float vMin = 0F;
        float vMax = length;

        float add = (GameTime.now() / 100 % 16) / 16F;

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax + add + girth,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax + add + girth,
                -rotXW,
                -length,
                -rotZW,
                uMin,
                vMin + add,
                rotXW,
                -length,
                rotZW,
                uMax,
                vMin + add);

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                uMax,
                vMax + add,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                uMin,
                vMax + add,
                dirXG - rotXW,
                -length,
                dirZG - rotZW,
                uMin,
                vMin + add,
                dirXG + rotXW,
                -length,
                dirZG + rotZW,
                uMax,
                vMin + add);

        float wMin = 0F;
        float wMax = girth;

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                rotXW,
                girth,
                rotZW,
                wMin,
                vMax + add + girth,
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                wMax,
                vMax + add,
                dirXG + rotXW,
                -length,
                dirZG + rotZW,
                wMax,
                vMin + add,
                rotXW,
                -length,
                rotZW,
                wMin,
                vMin + add);

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                -rotXW,
                girth,
                -rotZW,
                wMin,
                vMax + add + girth,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                wMax,
                vMax + add,
                dirXG - rotXW,
                -length,
                dirZG - rotZW,
                wMax,
                vMin + add,
                -rotXW,
                -length,
                -rotZW,
                wMin,
                vMin + add);

        float dirOX = dir.getStepX() * offset;
        float dirOZ = dir.getStepZ() * offset;

        vMax = offset;

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                rotXW,
                0,
                rotZW,
                uMax,
                vMax - add,
                -rotXW,
                0,
                -rotZW,
                uMin,
                vMax - add,
                -rotXW - dirOX,
                base,
                -rotZW - dirOZ,
                uMin,
                vMin - add,
                rotXW - dirOX,
                base,
                rotZW - dirOZ,
                uMax,
                vMin - add);

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax - add + 0.25F,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax - add + 0.25F,
                -rotXW - dirOX,
                base + girth,
                -rotZW - dirOZ,
                uMin,
                vMin - add + 0.25F,
                rotXW - dirOX,
                base + girth,
                rotZW - dirOZ,
                uMax,
                vMin - add + 0.25F);

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                rotXW,
                0,
                rotZW,
                wMax,
                vMax - add + 0.75F,
                rotXW,
                girth,
                rotZW,
                wMin,
                vMax - add + 0.75F,
                rotXW - dirOX,
                base + girth,
                rotZW - dirOZ,
                wMin,
                vMin - add + 0.75F,
                rotXW - dirOX,
                base,
                rotZW - dirOZ,
                wMax,
                vMin - add + 0.75F);

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                -rotXW,
                0,
                -rotZW,
                wMax,
                vMax - add + 0.75F,
                -rotXW,
                girth,
                -rotZW,
                wMin,
                vMax - add + 0.75F,
                -rotXW - dirOX,
                base + girth,
                -rotZW - dirOZ,
                wMin,
                vMin - add + 0.75F,
                -rotXW - dirOX,
                base,
                -rotZW - dirOZ,
                wMax,
                vMin - add + 0.75F);

        vMax = 0.125F;

        quad(
                pose,
                buf,
                argb,
                FoundryFaces.MOLTEN_LIGHT,
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                uMax,
                vMin + add + 0.75F,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                uMin,
                vMin + add + 0.75F,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax + add + 0.75F,
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax + add + 0.75F);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            int light,
            float x0,
            float y0,
            float z0,
            float u0,
            float v0,
            float x1,
            float y1,
            float z1,
            float u1,
            float v1,
            float x2,
            float y2,
            float z2,
            float u2,
            float v2,
            float x3,
            float y3,
            float z3,
            float u3,
            float v3) {
        vertex(pose, buf, argb, light, x0, y0, z0, u0, v0);
        vertex(pose, buf, argb, light, x1, y1, z1, u1, v1);
        vertex(pose, buf, argb, light, x2, y2, z2, u2, v2);
        vertex(pose, buf, argb, light, x3, y3, z3, u3, v3);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(buf, pose, x, y, z, argb, u, v, light, 0F, 1F, 0F);
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
    public void extractRenderState(
            BlockEntityCrucible be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        int totalCap =
                MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                        + MachineData.CRUCIBLE_WASTE_CAPACITY.get();
        int totalMass = 0;
        for (MaterialStack stack : be.recipeStack) totalMass += stack.amount;
        for (MaterialStack stack : be.wasteStack) totalMass += stack.amount;
        state.meltLevel = totalMass == 0 ? -1 : ((double) totalMass / (double) totalCap) * 0.875D;

        state.streams.clear();
        Direction facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.facing = facing;
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) {
            float age = (now - stream.birth()) + partialTicks;
            if (age >= MAX_AGE) continue;
            state.streams.add(
                    new StreamState(
                            stream.color(),
                            stream.waste() ? facing.getOpposite() : facing,
                            stream.len(),
                            age));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.meltLevel >= 0) {

            final double y = 0.5 + s.meltLevel;
            ps.pushPose();
            ps.translate(0.5, 0.0, 0.5);

            ps.mulPose(Axis.YP.rotationDegrees(Facing.yaw(s.facing, 90)));
            col.submitCustomGeometry(
                    ps,
                    meltType,
                    (pose, buf) ->
                            quad(
                                    pose,
                                    buf,
                                    -1,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    -1F,
                                    (float) y,
                                    -1F,
                                    0F,
                                    0F,
                                    -1F,
                                    (float) y,
                                    1F,
                                    0F,
                                    1F,
                                    1F,
                                    (float) y,
                                    1F,
                                    1F,
                                    1F,
                                    1F,
                                    (float) y,
                                    -1F,
                                    1F,
                                    0F));
            ps.popPose();
        }

        for (StreamState stream : s.streams) {
            ps.pushPose();

            ps.translate(
                    0.5 + stream.dir().getStepX() * 1.875,
                    0.0,
                    0.5 + stream.dir().getStepZ() * 1.875);
            col.submitCustomGeometry(
                    ps,
                    streamType,
                    (pose, buf) ->
                            emitStream(
                                    pose,
                                    buf,
                                    stream.color(),
                                    stream.dir(),
                                    stream.len(),
                                    stream.age(),
                                    0.625F,
                                    0.625F));
            ps.popPose();
        }
    }

    private record StreamState(int color, Direction dir, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public double meltLevel = -1;
    }
}
