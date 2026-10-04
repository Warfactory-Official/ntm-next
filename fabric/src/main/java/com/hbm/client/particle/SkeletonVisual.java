// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.particle.ParticleSkeleton;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class SkeletonVisual implements EffectVisual<ParticleSkeleton>, SimpleDynamicVisual {
    private static final Model[] CLEAN = models(ResourceManager.skeleton_tex);
    private static final Model[] GIB = models(ResourceManager.skeleton_blood_tex);
    private static final Model VILLAGER =
            model(ParticleSkeleton.PART_SKULL_VILLAGER, ResourceManager.skoilet_tex);
    private static final Model VILLAGER_GIB =
            model(ParticleSkeleton.PART_SKULL_VILLAGER, ResourceManager.skoilet_blood_tex);

    private final ParticleSkeleton particle;
    private final TransformedInstance instance;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public SkeletonVisual(
            VisualizationContext context, ParticleSkeleton particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        int part = particle.part();
        Model model =
                part == ParticleSkeleton.PART_SKULL_VILLAGER
                        ? particle.getGroup() == ParticleSkeleton.GIB_EXT_GROUP
                                ? VILLAGER_GIB
                                : VILLAGER
                        : particle.getGroup() == ParticleSkeleton.GIB_GROUP
                                ? GIB[part]
                                : CLEAN[part];
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, model)
                        .createInstance();
        writeFrame(partialTick);
    }

    private static Model[] models(Identifier texture) {
        Model[] out = new Model[3];
        for (int i = 0; i < out.length; i++) out[i] = model(i, texture);
        return out;
    }

    private static Model model(int part, Identifier texture) {
        return new SingleMeshModel(
                PackedQuadMesh.of(ResourceManager.skeleton_obj, ParticleSkeleton.partId(part)),
                SimpleMaterial.builder()
                        .texture(texture)
                        .mipmap(false)
                        .useOverlay(false)
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .cutout(new SimpleCutoutShader(Library.id("cutout/zero.glsl")))
                        .light(LightShaders.FLAT)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .backfaceCulling(true)
                        .build());
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        instance.setTransform(
                pose.translation(x, y, z)
                        .rotateY((float) Math.toRadians(particle.yaw(partialTick)))
                        .rotateX((float) Math.toRadians(particle.pitch(partialTick)))
                        .rotateY(-(float) Math.PI / 2F));
        instance.colorArgb(particle.argb(partialTick))
                .light(particle.light(partialTick))
                .setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
