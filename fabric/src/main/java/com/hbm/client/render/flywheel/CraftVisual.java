// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CraftVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Vector3f scratch3 = new Vector3f();

    private final Function<T, Rig<T>> rigs;
    private final Matrix4f root = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private @Nullable Rig<T> rig;
    private TransformedInstance[] instances = new TransformedInstance[0];
    private Matrix4f[] written = new Matrix4f[0];
    private int[] writtenLight = new int[0];
    private float reach;

    public CraftVisual(
            VisualizationContext ctx, T entity, float partialTick, Function<T, Rig<T>> rigs) {
        super(ctx, entity, partialTick);
        this.rigs = rigs;
        writeFrame(partialTick);
    }

    private static <T extends Entity> Model[] buildModels(List<Part<T>> parts, boolean cull) {
        var models = new Model[parts.size()];
        for (int i = 0; i < models.length; i++) {
            var part = parts.get(i);
            Material material =
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(part.texture())
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .backfaceCulling(cull)
                            .build();
            models[i] =
                    new SimpleModel(
                            List.of(
                                    new Model.ConfiguredMesh(
                                            material,
                                            PackedQuadMesh.of(
                                                    part.mesh(),
                                                    part.mesh().partId(part.part())))));
        }
        return models;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        double dx = entity.getX() - entity.xOld,
                dy = entity.getY() - entity.yOld,
                dz = entity.getZ() - entity.zOld;
        return sphereVisible(frustum, 0F, reach + (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    @Override
    protected void frame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Rig<T> current = rigs.apply(entity);
        if (current != rig) sync(current);

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        int light = computePackedLight(partialTick);

        root.translation(visualPos.x, visualPos.y, visualPos.z);
        ObjEntityVisual.Pose<T> rootPose = current.root();
        if (rootPose != null) rootPose.apply(root, entity, partialTick);

        List<Part<T>> parts = current.parts();
        for (int i = 0; i < parts.size(); i++) {
            Part<T> part = parts.get(i);
            Matrix4f pose = root;
            if (part.pose() != null) {
                pose = scratch.set(root);
                part.pose().apply(pose, entity, partialTick);
            }
            int partLight = part.fullbright() ? LightCoordsUtil.FULL_BRIGHT : light;
            if (written[i].equals(pose) && writtenLight[i] == partLight) continue;
            written[i].set(pose);
            writtenLight[i] = partLight;
            instances[i].setTransform(pose).light(partLight);
            instances[i].setChanged();
            var sphere = current.models()[i].boundingSphere();
            pose.getScale(scratch3);
            float scale = Math.max(scratch3.x, Math.max(scratch3.y, scratch3.z));
            pose.transformPosition(sphere.x(), sphere.y(), sphere.z(), scratch3).sub(visualPos);
            reach = Math.max(reach, scratch3.length() + sphere.w() * scale);
        }
    }

    private void sync(Rig<T> next) {
        for (TransformedInstance instance : instances) instance.delete();
        rig = next;
        reach = 0F;

        int count = next.parts().size();
        instances = new TransformedInstance[count];
        written = new Matrix4f[count];
        writtenLight = new int[count];
        for (int i = 0; i < count; i++) {
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, next.models()[i])
                            .createInstance();
            written[i] = new Matrix4f().zero();
            writtenLight[i] = -1;
        }
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }

    public record Part<T extends Entity>(
            HFRWavefrontObject mesh,
            String part,
            Identifier texture,
            boolean fullbright,
            ObjEntityVisual.@Nullable Pose<T> pose) {
        public static <T extends Entity> Part<T> of(
                HFRWavefrontObject mesh, String part, Identifier texture) {
            return new Part<>(mesh, part, texture, false, null);
        }

        public Part<T> at(ObjEntityVisual.Pose<T> pose) {
            return new Part<>(mesh, part, texture, fullbright, pose);
        }

        public Part<T> asFullbright() {
            return new Part<>(mesh, part, texture, true, pose);
        }
    }

    public record Rig<T extends Entity>(
            ObjEntityVisual.@Nullable Pose<T> root, List<Part<T>> parts, Model[] models) {
        public Rig(ObjEntityVisual.@Nullable Pose<T> root, List<Part<T>> parts) {
            this(root, parts, buildModels(parts, true));
        }

        public static <T extends Entity> Rig<T> unculled(
                ObjEntityVisual.@Nullable Pose<T> root, List<Part<T>> parts) {
            return new Rig<>(root, parts, buildModels(parts, false));
        }
    }
}
