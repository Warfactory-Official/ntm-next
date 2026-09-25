// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ObjEntityVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Spec<T> spec;
    private final Model[] models;
    private final List<TransformedInstance> instances = new ArrayList<>();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f lastPose = new Matrix4f();

    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private int lastLight, lastOverlay;
    private boolean written;

    public ObjEntityVisual(
            VisualizationContext ctx, T entity, float partialTick, Prepared<T> prepared) {
        super(ctx, entity, partialTick);
        this.spec = prepared.spec();
        this.models = prepared.models();

        for (Model model : models) {
            instances.add(
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, model)
                            .createInstance());
        }

        this.nameTag = new NameTagComponent(ctx, entity);
        this.shadow =
                new ShadowComponent(ctx, entity)
                        .radius(spec.shadowRadius())
                        .strength(spec.shadowStrength());

        writeFrame(partialTick);
    }

    public static <T extends Entity> Pose<T> defaultPose() {
        return (pose, entity, partialTick) -> {
            pose.rotateX((float) Math.PI);
            pose.rotateY(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) * Mth.DEG_TO_RAD);
        };
    }

    public static int damageOverlay(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return OverlayTexture.NO_OVERLAY;
        return OverlayTexture.pack(
                OverlayTexture.u(0F),
                OverlayTexture.v(living.hurtTime > 0 || living.deathTime > 0));
    }

    public static <T extends Entity> Prepared<T> prepare(Spec<T> spec) {
        Material material =
                SimpleMaterial.builder()
                        .texture(spec.texture())
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .backfaceCulling(spec.cull())
                        .fog(spec.fog() ? FogShaders.LINEAR : FogShaders.NONE)
                        .build();
        var models = new Model[spec.parts().size()];
        for (int i = 0; i < models.length; i++)
            models[i] =
                    new SimpleModel(
                            List.of(
                                    new Model.ConfiguredMesh(
                                            material,
                                            PackedQuadMesh.of(
                                                    spec.mesh(),
                                                    spec.mesh().partId(spec.parts().get(i))))));
        return new Prepared<>(spec, models);
    }

    public static <T extends Entity> void register(EntityType<T> type, Supplier<Spec<T>> source) {
        var binding = new Binding<T>();
        FlywheelResources.onInitialize(() -> binding.prepared = prepare(source.get()));
        SimpleEntityVisualizer.builder(type)
                .factory(
                        (ctx, entity, partialTick) ->
                                new ObjEntityVisual<>(ctx, entity, partialTick, binding.prepared))
                .apply();
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return super.isVisible(frustum) || drawnVisible(frustum);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
        shadow.beginFrame(context);
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        int light = computePackedLight(partialTick);
        int overlay = damageOverlay(entity);

        pose.translation(visualPos.x, visualPos.y, visualPos.z);
        spec.pose().apply(pose, entity, partialTick);
        if (spec.scale() != 1F) pose.scale(spec.scale());

        if (written && lastPose.equals(pose) && lastLight == light && lastOverlay == overlay)
            return;
        resetDrawn();
        for (Model model : models) includeDrawn(pose, model);

        for (TransformedInstance instance : instances) {
            instance.setTransform(pose).overlay(overlay).light(light);
            instance.setChanged();
        }
        lastPose.set(pose);
        lastLight = light;
        lastOverlay = overlay;
        written = true;
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        nameTag.delete();
        shadow.delete();
    }

    @FunctionalInterface
    public interface Pose<T extends Entity> {
        void apply(Matrix4f pose, T entity, float partialTick);
    }

    public record Spec<T extends Entity>(
            HFRWavefrontObject mesh,
            Identifier texture,
            List<String> parts,
            float scale,
            boolean cull,
            boolean fog,
            Pose<T> pose,
            float shadowRadius,
            float shadowStrength) {

        public static <T extends Entity> Spec<T> of(
                HFRWavefrontObject mesh, Identifier texture, String... parts) {
            return new Spec<>(mesh, texture, List.of(parts), 1F, true, true, defaultPose(), 0F, 1F);
        }

        public Spec<T> scale(float scale) {
            return new Spec<>(
                    mesh, texture, parts, scale, cull, fog, pose, shadowRadius, shadowStrength);
        }

        public Spec<T> pose(Pose<T> pose) {
            return new Spec<>(
                    mesh, texture, parts, scale, cull, fog, pose, shadowRadius, shadowStrength);
        }

        public Spec<T> unculled() {
            return new Spec<>(
                    mesh, texture, parts, scale, false, fog, pose, shadowRadius, shadowStrength);
        }

        public Spec<T> unfogged() {
            return new Spec<>(
                    mesh, texture, parts, scale, cull, false, pose, shadowRadius, shadowStrength);
        }

        public Spec<T> shadow(float radius, float strength) {
            return new Spec<>(mesh, texture, parts, scale, cull, fog, pose, radius, strength);
        }
    }

    public record Prepared<T extends Entity>(Spec<T> spec, Model[] models) {}

    private static final class Binding<T extends Entity> {
        Prepared<T> prepared;
    }
}
