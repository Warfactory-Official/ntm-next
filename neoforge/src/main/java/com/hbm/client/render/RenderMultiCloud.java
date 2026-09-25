// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RenderMultiCloud<T extends Entity>
        extends EntityRenderer<T, RenderMultiCloud.State>
        implements ConcurrentRenderStateExtraction {

    private final Identifier[] stages;
    private final ToIntFunction<T> age;
    private final ToIntFunction<T> maxAge;

    public RenderMultiCloud(
            EntityRendererProvider.Context context,
            String sprite,
            ToIntFunction<T> age,
            ToIntFunction<T> maxAge) {
        super(context);
        this.stages = new Identifier[8];
        for (int i = 0; i < 8; i++)
            this.stages[i] = Library.id("textures/item/" + sprite + (i + 1) + ".png");
        this.age = age;
        this.maxAge = maxAge;
        this.shadowRadius = 0F;
    }

    private static int stage(State state) {
        int eighth = state.maxAge / 8;
        if (eighth <= 0) return 0;
        return Math.clamp(state.age / eighth, 0, 7);
    }

    private static void drawCloud(
            VertexConsumer buffer, PoseStack.Pose pose, State state, Vector3f right, Vector3f up) {
        Random shade = state.shade;
        Random spread = state.spread;
        shade.setSeed(state.hash);
        spread.setSeed(100);
        for (int i = 0; i < 5; i++) {
            float brightness = 1F - shade.nextInt(10) * 0.05F;
            float x = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float y = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float z = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float scale = (float) ((spread.nextDouble() * 0.5D + 0.25D) * 3.75D);

            float bias = scale * 0.25F;
            quad(
                    buffer,
                    pose,
                    x * 3.75F + up.x * bias,
                    y * 3.75F + up.y * bias,
                    z * 3.75F + up.z * bias,
                    scale * 0.5F,
                    right,
                    up,
                    ARGB.colorFromFloat(1F, brightness, brightness, brightness),
                    state.light);
        }
    }

    private static void quad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float half,
            Vector3f right,
            Vector3f up,
            int color,
            int light) {
        float rx = right.x * half, ry = right.y * half, rz = right.z * half;
        float ux = up.x * half, uy = up.y * half, uz = up.z * half;
        vertex(buffer, pose, x - rx - ux, y - ry - uy, z - rz - uz, 0F, 1F, color, light);
        vertex(buffer, pose, x + rx - ux, y + ry - uy, z + rz - uz, 1F, 1F, color, light);
        vertex(buffer, pose, x + rx + ux, y + ry + uy, z + rz + uz, 1F, 0F, color, light);
        vertex(buffer, pose, x - rx + ux, y - ry + uy, z - rz + uz, 0F, 0F, color, light);
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color,
            int light) {
        Vertices.emit(buffer, pose, x, y, z, color, u, v, light, 0F, 1F, 0F);
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = age.applyAsInt(entity);
        state.maxAge = maxAge.applyAsInt(entity);
        state.hash = entity.hashCode();
        state.light = state.lightCoords;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.maxAge <= 0) return;
        RenderType type = RenderTypes.entityCutout(stages[stage(state)]);
        Quaternionf rotation = camera.orientation;
        rotation.transform(1F, 0F, 0F, state.right);
        rotation.transform(0F, 1F, 0F, state.up);
        collector.submitCustomGeometry(pose, type, state);
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState
            implements SubmitNodeCollector.CustomGeometryRenderer {
        final Vector3f right = new Vector3f();
        final Vector3f up = new Vector3f();
        final Random shade = new Random();
        final Random spread = new Random();
        int age;
        int maxAge;
        int hash;
        int light;

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer buffer) {
            drawCloud(buffer, pose, this, right, up);
        }
    }
}
