// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.effect.EntitySpear;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SpearVisual extends HbmDynamicEntityVisual<EntitySpear> {
    private static final float LIFT = 15F;
    private static final float SCALE = 2F;
    private static final Models MODELS = buildModels();
    private static final float BODY_REACH = reach(MODELS.body());
    private static final float FLASH_REACH = reach(MODELS.flash());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance body;
    private final TransformedInstance ghost;
    private final TransformedInstance flash;
    private final NameTagComponent nameTag;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f flashPose = new Matrix4f();
    private float lastX = Float.NaN, lastY, lastZ;
    private boolean lastVisible;

    public SpearVisual(VisualizationContext context, EntitySpear entity, float partialTick) {
        super(context, entity, partialTick);
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODELS.body, 0)
                        .createInstance();
        ghost =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODELS.ghost, 1)
                        .createInstance();
        flash =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODELS.flash, 2)
                        .createInstance();
        nameTag =
                new NameTagComponent(context, entity)
                        .shouldShow(
                                () ->
                                        !isFirstPersonCameraEntity()
                                                && (entity.shouldShowName()
                                                        || entity.hasCustomName()
                                                                && entity
                                                                        == Minecraft.getInstance()
                                                                                .getEntityRenderDispatcher()
                                                                                .crosshairPickEntity));
        writeFrame(partialTick, entity.position());
    }

    public static void initModels() {}

    private static Models buildModels() {
        var mesh = ResourceManager.lance;
        var part = PackedQuadMesh.of(mesh, mesh.partId("Spear"));
        Material body =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(ResourceManager.lance_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .ambientOcclusion(false)
                        .backfaceCulling(true)
                        .useOverlay(false)
                        .build();

        Material ghost =
                SimpleMaterial.builder()
                        .texture(ResourceManager.white_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .useLight(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .ambientOcclusion(false)
                        .backfaceCulling(true)
                        .useOverlay(false)
                        .build();
        Material flash =
                SimpleMaterial.builder()
                        .texture(ResourceManager.white_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .fog(EffectVisuals.FADE)
                        .useLight(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .ambientOcclusion(false)
                        .backfaceCulling(true)
                        .useOverlay(false)
                        .build();
        return new Models(
                new SingleMeshModel(part, body),
                new SingleMeshModel(part, ghost),
                new SingleMeshModel(flashMesh(), flash),
                body,
                ghost,
                flash);
    }

    private static float reach(Model model) {
        var sphere = model.boundingSphere();
        return SCALE * (new Vector3f(sphere.x(), sphere.y(), sphere.z()).length() + sphere.w());
    }

    static Mesh flashMesh() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(64 * 3);
        Random random = new Random(432L);
        Matrix4f rotation = new Matrix4f();
        int tip = ARGB.color(255, 255, 153, 153);
        int edge = ARGB.color(0, 255, 153, 153);
        Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f();
        for (int i = 0; i < 64; i++) {
            rotation.rotateX((random.nextFloat() * 360F) * Mth.DEG_TO_RAD);
            rotation.rotateY((random.nextFloat() * 360F) * Mth.DEG_TO_RAD);
            rotation.rotateZ((random.nextFloat() * 360F) * Mth.DEG_TO_RAD);
            rotation.rotateX((random.nextFloat() * 360F) * Mth.DEG_TO_RAD);
            rotation.rotateY((random.nextFloat() * 360F) * Mth.DEG_TO_RAD);
            float length = (random.nextFloat() * 20F + 5F + 10F) * 25F;
            float radius = (random.nextFloat() * 2F + 1F + 2F) * 25F;
            rotation.transformPosition(-.866F * radius, length, -.5F * radius, a);
            rotation.transformPosition(.866F * radius, length, -.5F * radius, b);
            rotation.transformPosition(0, length, radius, c);
            triangle(mesh, tip, edge, a, b);
            triangle(mesh, tip, edge, b, c);
            triangle(mesh, tip, edge, c, a);
        }
        return mesh.build();
    }

    private static void triangle(
            PackedQuadMesh.Builder mesh, int tip, int edge, Vector3f a, Vector3f b) {
        mesh.vertex(0, 0, 0, tip);
        mesh.vertex(a.x, a.y, a.z, edge);
        mesh.vertex(b.x, b.y, b.z, edge);
        mesh.vertex(b.x, b.y, b.z, edge);
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        float reach = BODY_REACH;
        if (entity.ticksInGround > 0) {
            float intensity = (entity.ticksInGround + 1F) / 200F;
            reach = Math.max(reach, FLASH_REACH * .2F * intensity * intensity);
        }
        return sphereVisible(frustum, LIFT, reach);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick(), context.camera().position());
        nameTag.beginFrame(context);
    }

    private void writeFrame(float partialTick, Vec3 camera) {
        boolean visible = entity.shouldRender(camera.x, camera.y, camera.z);

        Vector3f position =
                entity.tickCount == 0
                        ? getVisualPosition(interpolatedPosition)
                        : getVisualPosition(partialTick, interpolatedPosition);
        pose.translation(position.x, position.y + LIFT, position.z)
                .rotateX((float) Math.PI)
                .scale(SCALE);
        if (visible != lastVisible
                || position.x != lastX
                || position.y != lastY
                || position.z != lastZ) {
            body.setVisible(visible);
            if (visible) {
                body.setTransform(pose)
                        .overlay(OverlayTexture.NO_OVERLAY)
                        .light(LightCoordsUtil.FULL_BRIGHT)
                        .colorArgb(-1)
                        .setChanged();
            }
            lastVisible = visible;
            lastX = position.x;
            lastY = position.y;
            lastZ = position.z;
        }

        boolean grounded = visible && entity.ticksInGround > 0;
        ghost.setVisible(grounded);
        flash.setVisible(grounded);
        if (grounded) {
            float ticks = entity.ticksInGround + partialTick;
            float intensity = ticks / 200F;
            float intensitySquared = intensity * intensity;
            ghost.setTransform(pose)
                    .overlay(OverlayTexture.NO_OVERLAY)
                    .light(LightCoordsUtil.FULL_BRIGHT)
                    .colorArgb(
                            ARGB.color(
                                    Math.round(Math.min(ticks / 100F, 1F) * 255F), 255, 255, 255))
                    .setChanged();
            flashPose.set(pose).scale(.2F * intensitySquared);
            flash.setTransform(flashPose)
                    .overlay(OverlayTexture.NO_OVERLAY)
                    .light(LightCoordsUtil.FULL_BRIGHT)
                    .colorArgb(
                            ARGB.color(
                                    Math.round(Math.min(intensitySquared * 2F, 1F) * 255F),
                                    255,
                                    255,
                                    255))
                    .setChanged();
        }
    }

    @Override
    protected void _delete() {
        body.delete();
        ghost.delete();
        flash.delete();
        nameTag.delete();
    }

    private record Models(
            Model body,
            Model ghost,
            Model flash,
            Material bodyMaterial,
            Material ghostMaterial,
            Material flashMaterial) {}
}
