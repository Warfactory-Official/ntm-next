// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BombletZetaVisual extends HbmDynamicEntityVisual<EntityBombletZeta> {
    private static final float SCALE = 0.5F;
    private static final Model MODEL = buildModel();
    private static final float REACH = reach(MODEL);
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance instance;
    private final NameTagComponent nameTag;
    private final Matrix4f pose = new Matrix4f();

    public BombletZetaVisual(
            VisualizationContext context, EntityBombletZeta entity, float partialTick) {
        super(context, entity, partialTick);
        instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        instance.overlay(OverlayTexture.NO_OVERLAY);
        nameTag = new NameTagComponent(context, entity);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Model buildModel() {
        HFRWavefrontObject mesh = ResourceManager.bomblet_theta;
        Material material =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .texture(ResourceManager.bomblet_zeta_tex)
                        .mipmap(false)
                        .backfaceCulling(true)
                        .useOverlay(false)
                        .cardinalLightingMode(CardinalLightingMode.ENTITY)
                        .ambientOcclusion(false)
                        .build();
        List<Model.ConfiguredMesh> parts = new ArrayList<>(mesh.groups.length);
        for (int i = 0; i < mesh.groups.length; i++) {
            parts.add(new Model.ConfiguredMesh(material, PackedQuadMesh.of(mesh, i)));
        }
        return new SimpleModel(parts);
    }

    private static float reach(Model model) {
        var sphere = model.boundingSphere();
        return SCALE * (Vector3f.length(sphere.x(), sphere.y(), sphere.z()) + sphere.w());
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, REACH + (float) entity.getDeltaMovement().length());
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPosition = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.renderPitchO, entity.renderPitch);
        pose.translation(visualPosition.x, visualPosition.y, visualPosition.z)
                .rotateY((yaw - 90F) * Mth.DEG_TO_RAD)
                .rotateZ(pitch * Mth.DEG_TO_RAD)
                .scale(SCALE);
        instance.setTransform(pose).light(computePackedLight(partialTick)).setChanged();
    }

    @Override
    protected void _delete() {
        instance.delete();
        nameTag.delete();
    }
}
