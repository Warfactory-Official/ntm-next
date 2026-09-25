// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RubberBoatVisual extends HbmDynamicEntityVisual<EntityBoatRubber> {
    private static final float QUARTER_TURN = (float) (Math.PI / 2D);
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance hull;
    private final TransformedInstance waterPatch;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f waterPose = new Matrix4f();
    private final Matrix4f waterInitialPose;
    private final Quaternionf bubbleRotation = new Quaternionf();
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private float lastX, lastY, lastZ, lastYaw, lastHurt, lastDamage, lastHurtDir, lastBubble;
    private int lastLight;
    private boolean lastUnderWater, written;

    public RubberBoatVisual(VisualizationContext ctx, EntityBoatRubber entity, float partialTick) {
        super(ctx, entity, partialTick);
        hull =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, Models.HULL)
                        .createInstance();
        ModelTree waterTree =
                ModelTrees.of(ModelLayers.BOAT_WATER_PATCH, Models.WATER_MATERIAL)
                        .childOrThrow("water_patch");
        waterPatch =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, waterTree.model(), 1)
                        .createInstance();
        waterInitialPose = partPose(waterTree.initialPose());
        nameTag = new NameTagComponent(ctx, entity);
        shadow = new ShadowComponent(ctx, entity).radius(0.5F);
        writeFrame(partialTick);
    }

    public static void initModels() {
        Models.init();
    }

    private static Matrix4f partPose(PartPose pose) {
        return new Matrix4f()
                .translate(pose.x() / 16F, pose.y() / 16F, pose.z() / 16F)
                .rotateX(pose.xRot())
                .rotateY(pose.yRot())
                .rotateZ(pose.zRot())
                .scale(pose.xScale(), pose.yScale(), pose.zScale());
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
        nameTag.beginFrame(ctx);
        shadow.beginFrame(ctx);
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = (180F - entity.getYRot(partialTick)) * Mth.DEG_TO_RAD;
        float hurt = Math.max(entity.getHurtTime() - partialTick, 0F);
        float damage = hurt > 0F ? Math.max(entity.getDamage() - partialTick, 0F) : 0F;
        float hurtDir = hurt > 0F ? entity.getHurtDir() : 0F;
        boolean underWater = entity.isUnderWater();
        float bubbleAngle = underWater ? 0F : entity.getBubbleAngle(partialTick);
        int light = computePackedLight(partialTick);
        if (written
                && lastX == visualPos.x
                && lastY == visualPos.y
                && lastZ == visualPos.z
                && lastYaw == yaw
                && lastHurt == hurt
                && lastDamage == damage
                && lastHurtDir == hurtDir
                && lastBubble == bubbleAngle
                && lastUnderWater == underWater
                && lastLight == light) return;
        pose.translation(visualPos.x, visualPos.y, visualPos.z);
        pose.translate(0F, 0.375F, 0F);
        pose.rotateY(yaw);

        if (hurt > 0F) {
            pose.rotateX(Mth.sin(hurt) * hurt * damage / 10F * hurtDir * Mth.DEG_TO_RAD);
        }

        if (!underWater) {
            if (!Mth.equal(bubbleAngle, 0F)) {
                bubbleRotation.setAngleAxis(bubbleAngle * Mth.DEG_TO_RAD, 1F, 0F, 1F);
                pose.rotate(bubbleRotation);
            }
        }

        pose.scale(-1F, -1F, 1F);
        pose.rotateY(QUARTER_TURN);

        hull.setTransform(pose).light(light);
        hull.setChanged();
        waterPose.set(pose).mul(waterInitialPose);
        waterPatch.setVisible(!underWater);
        waterPatch.setTransform(waterPose);
        waterPatch.setChanged();
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastYaw = yaw;
        lastHurt = hurt;
        lastDamage = damage;
        lastHurtDir = hurtDir;
        lastBubble = bubbleAngle;
        lastUnderWater = underWater;
        lastLight = light;
        written = true;
    }

    @Override
    protected void _delete() {
        hull.delete();
        waterPatch.delete();
        nameTag.delete();
        shadow.delete();
    }

    private static final class Models {
        private static final Model HULL =
                new SingleMeshModel(
                        hullMesh(),
                        SimpleMaterial.builderOf(Materials.CUTOUT)
                                .texture(ResourceManager.boat_rubber_tex)
                                .mipmap(false)
                                .cutout(CutoutShaders.ONE_TENTH)
                                .cardinalLightingMode(CardinalLightingMode.ENTITY)
                                .build());
        private static final Material WATER_MATERIAL =
                SimpleMaterial.builder()
                        .texture(ResourceManager.boat_rubber_tex)
                        .mipmap(false)
                        .writeMask(WriteMask.DEPTH)
                        .fog(FogShaders.NONE)
                        .useOverlay(false)
                        .useLight(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .ambientOcclusion(false)
                        .build();

        private Models() {}

        static void init() {}

        private static PackedQuadMesh hullMesh() {
            CuboidMesh mesh = new CuboidMesh(64F, 32F);
            Matrix4f body = new Matrix4f().rotateY(-QUARTER_TURN);
            mesh.box(
                    new Matrix4f(body).translate(0F, 4F / 16F, 0F).rotateX(QUARTER_TURN),
                    0F,
                    8F,
                    -12F,
                    -8F,
                    -3F,
                    24F,
                    16F,
                    4F,
                    0F);
            wall(mesh, body, -11F, 0F, -QUARTER_TURN);
            wall(mesh, body, 11F, 0F, QUARTER_TURN);
            wall(mesh, body, 0F, -9F, (float) Math.PI);
            wall(mesh, body, 0F, 9F, 0F);
            return mesh.mesh();
        }

        private static void wall(CuboidMesh mesh, Matrix4f body, float x, float z, float yaw) {
            mesh.box(
                    new Matrix4f(body).translate(x / 16F, 4F / 16F, z / 16F).rotateY(yaw),
                    0F,
                    0F,
                    -10F,
                    -7F,
                    -1F,
                    20F,
                    6F,
                    2F,
                    0F);
        }
    }
}
