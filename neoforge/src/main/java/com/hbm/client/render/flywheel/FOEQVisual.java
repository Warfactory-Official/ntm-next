// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderTextures;
import com.hbm.entity.projectile.EntityBurningFOEQ;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
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
import java.util.Random;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FOEQVisual extends HbmDynamicEntityVisual<EntityBurningFOEQ> {
    private static final int STACKS = 10;
    private static final float DROP = 75F;
    private static final int[] FLAME = {
        ARGB.colorFromFloat(1F, 1F, 0.75F, 0.25F), ARGB.colorFromFloat(1F, 1F, 0.5F, 0F),
        ARGB.colorFromFloat(1F, 1F, 0.25F, 0F), ARGB.colorFromFloat(1F, 1F, 0.15F, 0F)
    };
    private static final Model BODY_MODEL =
            model(
                    SimpleMaterial.builder()
                            .texture(ResourceManager.sat_foeq_burning_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .backfaceCulling(true)
                            .build(),
                    ResourceManager.sat_foeq_burning,
                    "Cylinder");
    private static final Model FLAME_MODEL =
            model(
                    SimpleMaterial.builderOf(Materials.ADDITIVE)
                            .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                            .fog(EffectVisuals.FADE)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .texture(RenderTextures.WHITE)
                            .mipmap(false)
                            .backfaceCulling(false)
                            .build(),
                    ResourceManager.sat_foeq_fire,
                    "Circle");
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Vector3f pivot = new Vector3f();
    private final Vector3f scratch = new Vector3f();
    private final TransformedInstance body;
    private final TransformedInstance[] flames = new TransformedInstance[STACKS * FLAME.length];
    private final Matrix4f pose = new Matrix4f();
    private final Random rand = new Random();
    private float lastX = Float.NaN, lastY, lastZ, lastYaw, lastPitch;
    private long lastSeed;
    private float reach;

    public FOEQVisual(VisualizationContext ctx, EntityBurningFOEQ entity, float partialTick) {
        super(ctx, entity, partialTick);
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BODY_MODEL)
                        .createInstance();
        body.light(LightCoordsUtil.FULL_BRIGHT);
        for (int i = 0; i < flames.length; i++) {
            flames[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, FLAME_MODEL)
                            .createInstance();
            flames[i].light(LightCoordsUtil.FULL_BRIGHT).colorArgb(FLAME[i % FLAME.length]);
        }
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Model model(Material material, HFRWavefrontObject mesh, String part) {
        return new SimpleModel(
                List.of(
                        new Model.ConfiguredMesh(
                                material, PackedQuadMesh.of(mesh, mesh.partId(part)))));
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        double dx = entity.getX() - entity.xOld,
                dy = entity.getY() - entity.yOld,
                dz = entity.getZ() - entity.zOld;
        return sphereVisible(
                frustum, -DROP, reach + (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    @Override
    protected void frame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        long seed = GameTime.millis(entity.level()) / 50L;
        if (visualPos.x == lastX
                && visualPos.y == lastY
                && visualPos.z == lastZ
                && yaw == lastYaw
                && pitch == lastPitch
                && seed == lastSeed) return;
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastYaw = yaw;
        lastPitch = pitch;
        lastSeed = seed;
        pivot.set(visualPos.x, visualPos.y - DROP, visualPos.z);

        pose.translation(visualPos.x, visualPos.y, visualPos.z)
                .translate(0F, -DROP, 0F)
                .rotateY((yaw - 90F) * Mth.DEG_TO_RAD)
                .rotateZ(180F * Mth.DEG_TO_RAD)
                .rotateZ(pitch * Mth.DEG_TO_RAD);
        write(body, BODY_MODEL);

        rand.setSeed(seed);
        pose.scale(1.15F, 0.75F, 1.15F).translate(0F, -0.5F, 0.3F);

        int index = 0;
        for (int stack = 0; stack < STACKS; stack++) {
            for (int shell = 0; shell < FLAME.length; shell++) {
                pose.rotateY(rand.nextInt(360) * Mth.DEG_TO_RAD);
                write(flames[index++], FLAME_MODEL);
                if (shell < FLAME.length - 1) pose.translate(0F, 2F, 0F);
            }
            pose.translate(0F, -3.8F, 0F).scale(0.95F, 1.2F, 0.95F);
        }
    }

    private void write(TransformedInstance instance, Model model) {
        instance.setTransform(pose);
        instance.setChanged();
        var sphere = model.boundingSphere();
        pose.getScale(scratch);
        float scale = Math.max(scratch.x, Math.max(scratch.y, scratch.z));
        pose.transformPosition(sphere.x(), sphere.y(), sphere.z(), scratch).sub(pivot);
        reach = Math.max(reach, scratch.length() + sphere.w() * scale);
    }

    @Override
    protected void _delete() {
        body.delete();
        for (TransformedInstance flame : flames) flame.delete();
    }
}
