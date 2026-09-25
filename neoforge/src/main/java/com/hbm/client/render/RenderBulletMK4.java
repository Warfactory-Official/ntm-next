// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

public class RenderBulletMK4 extends EntityRenderer<EntityBulletBaseMK4, RenderBulletMK4.State>
        implements ConcurrentRenderStateExtraction {

    private static final Map<BulletConfig, Tracer> RENDERERS = new HashMap<>();

    public RenderBulletMK4(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void setRenderer(BulletConfig config, Tracer tracer) {
        RENDERERS.put(config, tracer);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(EntityBulletBaseMK4 bullet) {
        return !bullet.ignoreFrustum;
    }

    @Override
    protected AABB getBoundingBoxForCulling(EntityBulletBaseMK4 bullet) {
        var mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;

        double trail = 2D * Math.max(bullet.prevVelocity, bullet.velocity);
        double dx = bullet.getX() - camera.pos.x;
        double dy = bullet.getY() - camera.pos.y;
        double dz = bullet.getZ() - camera.pos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) + trail;
        double focal =
                Math.min(
                                Math.abs(camera.projectionMatrix.m00()) * mc.getWindow().getWidth(),
                                Math.abs(camera.projectionMatrix.m11())
                                        * mc.getWindow().getHeight())
                        * 0.5D;
        assert focal > 0D;
        double pixelRadius =
                Math.sqrt(2D)
                        * (TracerRibbon.MIN_WIDTH_PIXELS * 0.5D
                                + TracerRibbon.FILTER_PADDING_PIXELS);
        return bullet.getBoundingBox().inflate(trail + distance * pixelRadius / focal);
    }

    @Override
    public void extractRenderState(EntityBulletBaseMK4 bullet, State state, float partialTicks) {
        super.extractRenderState(bullet, state, partialTicks);
        if (bullet.config == null) bullet.config = bullet.getBulletConfig();
        state.config = bullet.config;
        state.length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * partialTicks;
        state.yaw = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        state.pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());
        state.light =
                Minecraft.getInstance()
                        .getEntityRenderDispatcher()
                        .getPackedLightCoords(bullet, partialTicks);
        state.age = bullet.tickCount + partialTicks;

        Tracer tracer = RENDERERS.get(state.config);
        if (tracer != null) tracer.extract(bullet, state, partialTicks);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.config == null) return;

        state.cameraOrientation = camera.orientation;
        pose.pushPose();

        if (state.config.renderRotations) {
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180));
        }

        Tracer tracer = RENDERERS.get(state.config);
        if (tracer != null) {
            tracer.draw(state, pose, collector);
        }

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public interface Tracer {
        void draw(State state, PoseStack pose, SubmitNodeCollector collector);

        default void extract(EntityBulletBaseMK4 bullet, State state, float partialTicks) {}
    }

    public static class State extends EntityRenderState {
        public BulletConfig config;
        public double length;
        public float yaw;
        public float pitch;
        public int light;
        public float age;
        public Quaternionf cameraOrientation;
        public boolean wire;
        public double wireX;
        public double wireY;
        public double wireZ;
        public int[] wireLight;
    }
}
