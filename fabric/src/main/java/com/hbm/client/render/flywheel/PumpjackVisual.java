/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.machine.oil.PumpjackBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class PumpjackVisual extends AbstractBoundedBlockEntityVisual<PumpjackBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final int ROTOR = ResourceManager.pumpjack.partId("Rotor");
    private static final int HEAD = ResourceManager.pumpjack.partId("Head");
    private static final int CARRIAGE = ResourceManager.pumpjack.partId("Carriage");
    private static final int LINKAGE_QUADS = 14;
    private static final int LINKAGE_TRIANGLES = LINKAGE_QUADS * 2;
    private static final float[] TRIANGLE = {
        0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F,
        1F, 0F, 0F, 1F, 0F, 0F, 0F, 1F,
        0F, 1F, 0F, 0F, 1F, 0F, 0F, 1F,
        0F, 1F, 0F, 0F, 1F, 0F, 0F, 1F
    };
    private static final Material BODY_MATERIAL = MeshPart.litCutout(ResourceManager.pumpjack_tex);
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    ResourceManager.pumpjack.groups[ROTOR],
                    ResourceManager.pumpjack.smoothing(),
                    BODY_MATERIAL);
    private static final MeshPart HEAD_PART =
            MeshPart.obj(
                    ResourceManager.pumpjack.groups[HEAD],
                    ResourceManager.pumpjack.smoothing(),
                    BODY_MATERIAL);
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.pumpjack.groups[CARRIAGE],
                    ResourceManager.pumpjack.smoothing(),
                    BODY_MATERIAL);
    private static final Material LINKAGE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final PackedQuadMesh LINKAGE_MESH =
            PackedQuadMesh.of(TRIANGLE, new int[] {-1, -1, -1, -1}, new int[4]);
    private static final Model LINKAGE_MODEL = new SingleMeshModel(LINKAGE_MESH, LINKAGE_MATERIAL);
    private final TransformedInstance rotor;
    private final TransformedInstance head;
    private final TransformedInstance carriage;
    private final AffineUvTransformedInstance[] linkage =
            new AffineUvTransformedInstance[LINKAGE_TRIANGLES];
    private final boolean[] linkageHidden = new boolean[LINKAGE_TRIANGLES];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f rotorPose = new Matrix4f();
    private final Matrix4f headPose = new Matrix4f();
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f linkagePose = new Matrix4f();
    private final Matrix4f linkageWorld = new Matrix4f();
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f();
    private @Nullable AABB lastLightBounds;
    private final AABB rawBodyBounds;
    private float lastRotation = Float.NaN;
    private final double[] lightBounds = new double[6];

    public PumpjackVisual(
            VisualizationContext context, PumpjackBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(LightBounds.of(ResourceManager.pumpjack, "Base", basePose, pos));
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR_PART.model())
                        .createInstance();
        head =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, HEAD_PART.model())
                        .createInstance();
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        for (int i = 0; i < linkage.length; i++)
            linkage[i] =
                    instancerProvider()
                            .instancer(AffineUvTransformedInstance.TYPE, LINKAGE_MODEL)
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevAngle, blockEntity.angle);
        if (rotation == lastRotation) return;
        rotorPose
                .set(basePose)
                .translate(0F, 1.5F, -5.5F)
                .rotateX((rotation - 90F) * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 5.5F);
        headPose.set(basePose)
                .translate(0F, 3.5F, -3.5F)
                .rotateX(
                        ((float) Math.toDegrees(Math.sin(Math.toRadians(rotation))) * .25F)
                                * Mth.DEG_TO_RAD)
                .translate(0F, -3.5F, 3.5F);
        carriagePose.set(basePose).translate(0F, (float) -Math.sin(Math.toRadians(rotation)), 0F);
        writeBody(rotor, rotorPose);
        writeBody(head, headPose);
        writeBody(carriage, carriagePose);
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        LightBounds.includeLightBounds(lightBounds, ROTOR_PART.model(), rotorPose, pos);
        LightBounds.includeLightBounds(lightBounds, HEAD_PART.model(), headPose, pos);
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);

        linkagePose.set(basePose);
        writeLinkage(linkagePose, rotation);
        lastRotation = rotation;
    }

    private void writeBody(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    private void writeLinkage(Matrix4f pose, float rotation) {
        float backAngle = -(float) Math.sin(Math.toRadians(rotation)) * .25F;
        double backY = -2D * Mth.sin(backAngle), backZ = -2D * Mth.cos(backAngle);
        float rotorAngle = -(float) Math.toRadians(rotation - 90F);
        double rotorY = .5D * Mth.cos(rotorAngle), rotorZ = -.5D * Mth.sin(rotorAngle);
        int gray = ARGB.colorFromFloat(1F, .5F, .5F, .5F);
        int index = 0;
        for (int side = -1; side <= 1; side += 2) {
            float xi = .53125F * side;
            double y1 = 1.5F + rotorY, z1 = -5.5F + rotorZ;
            double y2 = 3.5F + backY, z2 = -3.5F + backZ;
            index =
                    quad(
                            index,
                            pose,
                            gray,
                            xi,
                            y1,
                            z1 - .0625F,
                            xi,
                            y1,
                            z1 + .0625F,
                            xi,
                            y2,
                            z2 + .0625F,
                            xi,
                            y2,
                            z2 - .0625F);
        }

        int dark = ARGB.colorFromFloat(1F, .2F, .2F, .2F);
        float pd = .03125F, width = .25F;
        float height = -(float) Math.sin(Math.toRadians(rotation));
        float cutlet = 360F / 32F;
        for (int side = -1; side <= 1; side += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * .25D);
            float frontCos = Mth.cos(pRot), frontSin = Mth.sin(pRot);
            double frontY = frontSin, frontZ = frontCos;
            float dist = .03125F;
            double radiusY = (2.5F + dist) * (double) frontSin;
            double radiusZ = (2.5F + dist) * (double) frontCos;
            float startAngle = -(float) Math.toRadians(cutlet * -3F);
            float startCos = Mth.cos(startAngle), startSin = Mth.sin(startAngle);
            double turnedY = radiusY * startCos + radiusZ * startSin;
            radiusZ = radiusZ * startCos - radiusY * startSin;
            radiusY = turnedY;
            float stepAngle = -(float) Math.toRadians(cutlet);
            float stepCos = Mth.cos(stepAngle), stepSin = Mth.sin(stepAngle);
            for (int j = 0; j < 4; j++) {
                double sumY1 = frontY + radiusY;
                double sumZ1 = radiusY < 0F ? 3.5F + dist * .5F : frontZ + radiusZ;
                turnedY = radiusY * stepCos + radiusZ * stepSin;
                radiusZ = radiusZ * stepCos - radiusY * stepSin;
                radiusY = turnedY;
                double sumY2 = frontY + radiusY;
                double sumZ2 = radiusY < 0F ? 3.5F + dist * .5F : frontZ + radiusZ;
                float xL = (width - pd) * side, xR = (width + pd) * side;
                index =
                        quad(
                                index,
                                pose,
                                dark,
                                xL,
                                3.5F + sumY1,
                                -3.5F + sumZ1,
                                xR,
                                3.5F + sumY1,
                                -3.5F + sumZ1,
                                xR,
                                3.5F + sumY2,
                                -3.5F + sumZ2,
                                xL,
                                3.5F + sumY2,
                                -3.5F + sumZ2);
            }
            double sumY = frontY + radiusY;
            double sumZ = radiusY < 0F ? 3.5F + dist * .5F : frontZ + radiusZ;
            float xR = (width + pd) * side, xL = (width - pd) * side;
            index =
                    quad(
                            index,
                            pose,
                            dark,
                            xR,
                            3.5F + sumY,
                            -3.5F + sumZ,
                            xL,
                            3.5F + sumY,
                            -3.5F + sumZ,
                            xL,
                            2F + height,
                            0F,
                            xR,
                            2F + height,
                            0F);
        }
        float p = .03125F;
        quad(
                index,
                pose,
                dark,
                p,
                height + 1.5F,
                p,
                -p,
                height + 1.5F,
                -p,
                -p,
                .75F,
                -p,
                p,
                .75F,
                p);
        index += 2;
        quad(
                index,
                pose,
                dark,
                -p,
                height + 1.5F,
                p,
                p,
                height + 1.5F,
                -p,
                p,
                .75F,
                -p,
                -p,
                .75F,
                p);
    }

    private int quad(
            int index,
            Matrix4f pose,
            int color,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            double cx,
            double cy,
            double cz,
            double dx,
            double dy,
            double dz) {
        writeTriangle(index++, pose, color, ax, ay, az, bx, by, bz, cx, cy, cz);
        writeTriangle(index++, pose, color, ax, ay, az, cx, cy, cz, dx, dy, dz);
        return index;
    }

    private void writeTriangle(
            int index,
            Matrix4f pose,
            int color,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        p0.set((float) x0, (float) y0, (float) z0);
        p1.set((float) x1, (float) y1, (float) z1);
        p2.set((float) x2, (float) y2, (float) z2);
        float ax = p1.x - p0.x, ay = p1.y - p0.y, az = p1.z - p0.z;
        float bx = p2.x - p0.x, by = p2.y - p0.y, bz = p2.z - p0.z;
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float magnitude = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (magnitude == 0F) {
            if (!linkageHidden[index]) linkage[index].setVisible(false);
            linkageHidden[index] = true;
            return;
        }
        local.identity()
                .m00(ax)
                .m01(ay)
                .m02(az)
                .m10(bx)
                .m11(by)
                .m12(bz)
                .m20(nx / magnitude)
                .m21(ny / magnitude)
                .m22(nz / magnitude)
                .m30(p0.x)
                .m31(p0.y)
                .m32(p0.z);
        linkageWorld.set(pose).mul(local);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(linkageWorld);
        if (linkageHidden[index]) linkage[index].setVisible(true);
        linkageHidden[index] = false;
        linkage[index].setTransform(instancePose).colorArgb(color).light(0);
        linkage[index].uv(1F, 0F, 0F, 1F, 0F, 0F).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                        pos.getX() - 8,
                        pos.getY(),
                        pos.getZ() - 8,
                        pos.getX() + 9,
                        pos.getY() + 7,
                        pos.getZ() + 9));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotor);
        consumer.accept(head);
        consumer.accept(carriage);
        for (AffineUvTransformedInstance instance : linkage) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        head.delete();
        carriage.delete();
        for (int i = 0; i < linkage.length; i++) {
            linkage[i].delete();
        }
    }
}
