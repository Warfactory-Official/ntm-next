// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityChemical.ChemicalStyle;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ChemicalVisual extends HbmDynamicEntityVisual<EntityChemical> {
    private static final Models MODELS = buildModels();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance beam;
    private final UvTransformedInstance cloud;
    private final NameTagComponent nameTag;
    private final int gasMirrorU;
    private final int gasMirrorV;
    private final Matrix4f beamPose = new Matrix4f();
    private final Matrix4f cloudPose = new Matrix4f();

    public ChemicalVisual(VisualizationContext context, EntityChemical entity, float partialTick) {
        super(context, entity, partialTick);
        Random random = new Random(entity.getId());
        gasMirrorU = random.nextInt(2);
        gasMirrorV = random.nextInt(2);
        beam =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODELS.beam)
                        .createInstance();
        cloud =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, MODELS.cloud)
                        .createInstance();
        beam.overlay(OverlayTexture.NO_OVERLAY).colorArgb(-1).setVisible(false);
        cloud.overlay(OverlayTexture.NO_OVERLAY).setVisible(false);
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
    }

    public static void initModels() {}

    private static Models buildModels() {
        Material beam =
                SimpleMaterial.builder()
                        .texture(EffectVisuals.Shared.WHITE)
                        .mipmap(false)
                        .cutout(CutoutShaders.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .fog(EffectVisuals.FADE)
                        .light(LightShaders.FLAT)
                        .useOverlay(false)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .ambientOcclusion(false)
                        .backfaceCulling(false)
                        .build();
        Material cloud =
                SimpleMaterial.builder()
                        .texture(ResourceManager.particle_base_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .light(LightShaders.FLAT)
                        .useOverlay(false)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .ambientOcclusion(false)
                        .build();
        return new Models(
                new SingleMeshModel(beamMesh(), beam),
                new SingleMeshModel(cloudMesh(), cloud),
                beam,
                cloud);
    }

    static PackedQuadMesh beamMesh() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(4).normal(0, 0, 1);
        int root = ARGB.color(51, 255, 255, 255), tip = ARGB.color(0, 255, 255, 255);
        float s = .0625F;
        beamSide(mesh, root, tip, -s, -s, s, -s);
        beamSide(mesh, root, tip, -s, s, s, s);
        beamSide(mesh, root, tip, -s, -s, -s, s);
        beamSide(mesh, root, tip, s, -s, s, s);
        return mesh.build();
    }

    private static void beamSide(
            PackedQuadMesh.Builder mesh,
            int root,
            int tip,
            float x0,
            float z0,
            float x1,
            float z1) {
        mesh.vertex(x0, 0, z0, root);
        mesh.vertex(x1, 0, z1, root);
        mesh.vertex(x1, 1, z1, tip);
        mesh.vertex(x0, 1, z0, tip);
    }

    static PackedQuadMesh cloudMesh() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(1).normal(0, 1, 0);
        mesh.vertex(-1, -1, 0, 1, 1, -1);
        mesh.vertex(1, -1, 0, 0, 1, -1);
        mesh.vertex(1, 1, 0, 0, 0, -1);
        mesh.vertex(-1, 1, 0, 1, 0, -1);
        return mesh.build();
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, reach());
    }

    private float reach() {
        double motion = entity.getDeltaMovement().length();
        float age = entity.tickCount + 1F;
        float extent =
                switch (entity.getStyle()) {
                    case AMAT, LIGHTNING ->
                            (float) (motion * age * .75D) + .0625F * Mth.SQRT_OF_TWO;
                    case GAS -> (float) (age / (double) entity.getMaxAge() * 10D) * Mth.SQRT_OF_TWO;
                    case GASFLAME -> age / entity.getMaxAge() * 2F * Mth.SQRT_OF_TWO;
                    default -> 0F;
                };
        return extent + (float) motion;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick(), context);
    }

    private void writeFrame(float partialTick, Context context) {
        var camera = context.camera();
        Vec3 eye = camera.position();
        boolean visible = !isFirstPersonCameraEntity() && entity.shouldRender(eye.x, eye.y, eye.z);
        ChemicalStyle style = entity.getStyle();
        Vector3f position =
                entity.tickCount == 0
                        ? getVisualPosition(interpolatedPosition)
                        : getVisualPosition(partialTick, interpolatedPosition);
        boolean beamVisible =
                visible && (style == ChemicalStyle.AMAT || style == ChemicalStyle.LIGHTNING);
        boolean cloudVisible =
                visible && (style == ChemicalStyle.GAS || style == ChemicalStyle.GASFLAME);
        beam.setVisible(beamVisible);
        cloud.setVisible(cloudVisible);

        if (beamVisible) {
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            float length =
                    (float)
                            (entity.getDeltaMovement().length()
                                    * (entity.tickCount + partialTick)
                                    * .75D);
            beamPose.translation(position.x, position.y, position.z)
                    .rotateY(yaw * Mth.DEG_TO_RAD)
                    .rotateX((-pitch - 90F) * Mth.DEG_TO_RAD)
                    .scale(1F, length, 1F);
            beam.setTransform(beamPose).light(computePackedLight(partialTick)).setChanged();
        }
        if (cloudVisible) {
            float progress = (entity.tickCount + partialTick) / entity.getMaxAge();
            double gasProgress = (entity.tickCount + partialTick) / (double) entity.getMaxAge();
            float size = style == ChemicalStyle.GAS ? (float) (gasProgress * 10D) : progress * 2F;
            int rgb;
            int alpha;
            int mirrorU = 0, mirrorV = 0;
            if (style == ChemicalStyle.GAS) {
                var property = NTMFluidProperties.get(entity.getChemType());
                rgb = property == null ? 0xFFFFFF : property.color();
                alpha = Math.max((int) (127D * (1D - gasProgress)), 0);
                mirrorU = gasMirrorU;
                mirrorV = gasMirrorV;
            } else {
                rgb =
                        Color.HSBtoRGB(
                                        Math.max((60 - progress * 100) / 360F, 0),
                                        1 - progress * .25F,
                                        1 - progress * .5F)
                                & 0xFFFFFF;
                alpha = Math.max((int) (255F * (1F - progress)), 0);
            }
            cloudPose
                    .translation(position.x, position.y, position.z)
                    .rotate(camera.rotation())
                    .scale(size, size, 1F);
            cloud.setTransform(cloudPose)
                    .light(computePackedLight(partialTick))
                    .colorArgb(ARGB.color(alpha, rgb));
            cloud.uvRegion(mirrorU, mirrorV, 1 - 2 * mirrorU, 1 - 2 * mirrorV).setChanged();
        }
        nameTag.beginFrame(context);
    }

    @Override
    protected void _delete() {
        beam.delete();
        cloud.delete();
        nameTag.delete();
    }

    private record Models(Model beam, Model cloud, Material beamMaterial, Material cloudMaterial) {}
}
