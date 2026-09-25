// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class MobObjVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Spec<T> spec;
    private final Model[] models;
    private final List<TransformedInstance> instances = new ArrayList<>();
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private final Matrix4f[] lastPoses;
    private final int[] lastLights, lastOverlays;
    private final boolean[] visible;

    public MobObjVisual(
            VisualizationContext ctx, T entity, float partialTick, Prepared<T> prepared) {
        super(ctx, entity, partialTick);
        this.spec = prepared.spec();
        this.models = prepared.models();

        for (Model model : models) {
            TransformedInstance instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, model)
                            .createInstance();
            instance.setVisible(false);
            instances.add(instance);
        }
        lastPoses = new Matrix4f[instances.size()];
        lastLights = new int[instances.size()];
        lastOverlays = new int[instances.size()];
        visible = new boolean[instances.size()];
        for (int i = 0; i < lastPoses.length; i++) lastPoses[i] = new Matrix4f();

        this.nameTag =
                new NameTagComponent(ctx, entity)
                        .shouldShow(
                                () ->
                                        spec.instanced().test(entity)
                                                && (entity.shouldShowName()
                                                        || entity.hasCustomName()
                                                                && entity
                                                                        == Minecraft.getInstance()
                                                                                .getEntityRenderDispatcher()
                                                                                .crosshairPickEntity));
        this.shadow =
                new ShadowComponent(ctx, entity)
                        .radius(spec.shadowRadius())
                        .strength(spec.shadowStrength());

        writeFrame(partialTick);
    }

    public static <T extends Entity> Prepared<T> prepare(Spec<T> spec) {
        var source = spec.mesh().get();
        var material = spec.material().get();
        var models = new Model[spec.parts().size()];
        for (int i = 0; i < models.length; i++) {
            var part = spec.parts().get(i);
            var sheet = part.material() == null ? material : part.material().get();
            models[i] =
                    new SimpleModel(
                            List.of(
                                    new Model.ConfiguredMesh(
                                            sheet,
                                            PackedQuadMesh.of(
                                                    source, source.partId(part.name())))));
        }
        return new Prepared<>(spec, models);
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
        boolean drawn = spec.instanced().test(entity);
        shadow.radius(drawn ? spec.shadowRadius() : 0F);

        int light = computePackedLight(partialTick);
        int overlay = ObjEntityVisual.damageOverlay(entity);

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        base.translation(visualPos.x, visualPos.y, visualPos.z);
        spec.pose().apply(base, entity, partialTick);

        resetDrawn();
        for (int i = 0; i < instances.size(); i++) {
            Part<T> part = spec.parts().get(i);
            TransformedInstance instance = instances.get(i);
            scratch.set(base);
            part.pose().apply(scratch, entity, partialTick);
            boolean drawPart = drawn && part.visible().test(entity);
            if (!drawPart) {
                if (visible[i]) instance.setVisible(false);
                visible[i] = false;
                continue;
            }
            includeDrawn(scratch, models[i]);
            if (visible[i]
                    && lastPoses[i].equals(scratch)
                    && lastLights[i] == light
                    && lastOverlays[i] == overlay) continue;
            if (!visible[i]) instance.setVisible(true);
            instance.setTransform(scratch).overlay(overlay).light(light).setChanged();
            lastPoses[i].set(scratch);
            lastLights[i] = light;
            lastOverlays[i] = overlay;
            visible[i] = true;
        }
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        nameTag.delete();
        shadow.delete();
    }

    public record Part<T extends Entity>(
            String name,
            Predicate<T> visible,
            ObjEntityVisual.Pose<T> pose,
            @Nullable Supplier<Material> material) {
        public static <T extends Entity> Part<T> of(String name) {
            return new Part<>(name, entity -> true, (pose, entity, partialTick) -> {}, null);
        }

        public static <T extends Entity> Part<T> when(String name, Predicate<T> visible) {
            return Part.<T>of(name).visible(visible);
        }

        public static <T extends Entity> Part<T> at(String name, ObjEntityVisual.Pose<T> pose) {
            return Part.<T>of(name).pose(pose);
        }

        public Part<T> visible(Predicate<T> visible) {
            return new Part<>(name, visible, pose, material);
        }

        public Part<T> pose(ObjEntityVisual.Pose<T> pose) {
            return new Part<>(name, visible, pose, material);
        }

        public Part<T> material(Supplier<Material> material) {
            return new Part<>(name, visible, pose, material);
        }
    }

    public record Spec<T extends Entity>(
            Supplier<HFRWavefrontObject> mesh,
            Supplier<Material> material,
            List<Part<T>> parts,
            ObjEntityVisual.Pose<T> pose,
            float shadowRadius,
            float shadowStrength,
            Predicate<T> instanced) {
        @SafeVarargs
        public static <T extends Entity> Spec<T> of(
                Supplier<HFRWavefrontObject> mesh, Supplier<Material> material, Part<T>... parts) {
            return new Spec<>(
                    mesh,
                    material,
                    List.of(parts),
                    (pose, entity, partialTick) -> {},
                    0F,
                    1F,
                    entity -> true);
        }

        public Spec<T> pose(ObjEntityVisual.Pose<T> pose) {
            return new Spec<>(mesh, material, parts, pose, shadowRadius, shadowStrength, instanced);
        }

        public Spec<T> shadow(float radius, float strength) {
            return new Spec<>(mesh, material, parts, pose, radius, strength, instanced);
        }

        public Spec<T> instanced(Predicate<T> instanced) {
            return new Spec<>(mesh, material, parts, pose, shadowRadius, shadowStrength, instanced);
        }
    }

    public record Prepared<T extends Entity>(Spec<T> spec, Model[] models) {}
}
