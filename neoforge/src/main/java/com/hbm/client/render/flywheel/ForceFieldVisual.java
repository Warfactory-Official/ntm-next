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

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.client.model.Meshes;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ForceFieldVisual
        extends AbstractDynamicExtentBlockEntityVisual<RepulsionFieldBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final float LINE_WIDTH = 1F;
    private static final HFRWavefrontObject BODY_SOURCE =
            Meshes.load(RegistryUtil.id("models/machines/radar_base.obj"));
    private static final Material TOP_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.forcefield_top_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart[] TOP_PARTS =
            MeshPart.objParts(ResourceManager.forcefield_top, TOP_MATERIAL);
    private static final Material LINE_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .shaders(StandardMaterialShaders.LINE)
                    .light(LightShaders.NONE)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Model LINE_MODEL = MeshPart.line(LINE_WIDTH, LINE_MATERIAL);

    private final AABB bodyBounds;
    private final AABB litBounds;
    private static final int SPIN_PERIOD = 3600;
    private static final ChainInstance.Chain TOP_CHAIN =
            new ChainInstance.Chain().clock(0).rotateY(0, -Mth.DEG_TO_RAD);
    private final ChainParts top;
    private final ArrayList<TransformedInstance> lines = new ArrayList<>();
    private final Matrix4f linePose = new Matrix4f();
    private final Vector3f from = new Vector3f();
    private final Vector3f to = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Matrix4f topBasePose = new Matrix4f();
    private final Matrix4f shellBasePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private float lastRadius = Float.NaN;
    private int lastColor = Integer.MIN_VALUE;
    private volatile int lineLight;
    private int lastLineLight;
    private boolean lastUp;
    private boolean initialized;
    private int usedLines;
    private float boundsRadius = Float.NaN;

    public ForceFieldVisual(
            VisualizationContext context,
            RepulsionFieldBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose = new Matrix4f().translate(.5F, 0F, .5F).rotateY((float) Math.PI);
        topBasePose.translation(.5F, 1F, .5F).rotateY((float) Math.PI);
        shellBasePose.translation(.5F, 0F, .5F).rotateY((float) Math.PI).translate(0F, .5F, 0F);
        bodyBounds = LightBounds.of(BODY_SOURCE, "Cube_Cube.001", bodyPose, pos);
        Model[] topModels = new Model[TOP_PARTS.length];
        ChainInstance.Chain[] topChains = new ChainInstance.Chain[TOP_PARTS.length];
        for (int i = 0; i < TOP_PARTS.length; i++) {
            topModels[i] = TOP_PARTS[i].model();
            topChains[i] = TOP_CHAIN;
        }
        top = new ChainParts(context, visualPos, topBasePose, topModels, topChains, 1);
        top.clock(ChainInstance.clockBase(GameTime.now(), SPIN_PERIOD), SPIN_PERIOD);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        top.includeLightBounds(lightBoundsAccumulator, pos);
        litBounds =
                new AABB(
                        lightBoundsAccumulator[0],
                        lightBoundsAccumulator[1],
                        lightBoundsAccumulator[2],
                        lightBoundsAccumulator[3],
                        lightBoundsAccumulator[4],
                        lightBoundsAccumulator[5]);
        beforeFrame();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static int segments(float radius) {
        return (int) (16F + radius * .125F);
    }

    @Override
    public void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean up =
                blockEntity.requestedOn
                        && blockEntity.shieldHealth > 0
                        && blockEntity.storedEnergy > 0
                        && blockEntity.recoveryTicks == 0;
        float radius = blockEntity.fieldRadius;
        int color = ARGB.opaque(blockEntity.fieldColor);
        int light = lineLight;
        boolean upChanged = !initialized || up != lastUp;
        boolean shellChanged =
                upChanged
                        || up
                                && (radius != lastRadius
                                        || color != lastColor
                                        || light != lastLineLight);
        if (!upChanged && !shellChanged) return;
        lastUp = up;
        lastRadius = radius;
        lastColor = color;
        lastLineLight = light;
        initialized = true;
        if (upChanged) {
            top.set(0, 0F, up ? .1F : 0F);
            top.write();
        }
        if (shellChanged) {
            hideLines();
            if (up) {
                int l = segments(radius);
                emitShell(l, l * 2, radius, color);
            }
        }
    }

    private void emitShell(int l, int s, float radius, int color) {
        float meridianRotation = 360F / s;
        float latitudeRotation = (float) Math.PI / l;
        float latitudeCos = Mth.cos(latitudeRotation), latitudeSin = Mth.sin(latitudeRotation);
        for (int k = 0; k < s; k++) {
            double yaw = Math.toRadians((k + 1) * (double) meridianRotation);
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            double y = radius, z = 0D;
            for (int i = 0; i < l; i++) {
                double previousY = y, previousZ = z;
                y = previousY * latitudeCos + previousZ * latitudeSin;
                z = previousZ * latitudeCos - previousY * latitudeSin;
                segment(previousZ * sin, previousY, previousZ * cos, z * sin, y, z * cos, color);
            }
        }
        float parallelRotation = (float) Math.PI * 2F / s;
        float parallelCos = Mth.cos(parallelRotation), parallelSin = Mth.sin(parallelRotation);
        double x = 0D, y = radius, z = 0D;
        for (int k = 0; k < l; k++) {
            double turnedX = x * latitudeCos + y * latitudeSin;
            y = y * latitudeCos - x * latitudeSin;
            x = turnedX;
            for (int i = 0; i < s; i++) {
                double previousX = x, previousZ = z;
                x = previousX * parallelCos + previousZ * parallelSin;
                z = previousZ * parallelCos - previousX * parallelSin;
                segment(previousX, y, previousZ, x, y, z, color);
            }
        }
    }

    private void segment(
            double x0, double y0, double z0, double x1, double y1, double z1, int color) {
        while (lines.size() <= usedLines)
            lines.add(
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LINE_MODEL)
                            .createInstance());
        Vector3f a = shellBasePose.transformPosition(from.set((float) x0, (float) y0, (float) z0));
        Vector3f b = shellBasePose.transformPosition(to.set((float) x1, (float) y1, (float) z1));
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        TransformedInstance line = lines.get(usedLines++);
        line.setVisible(length > 0F);
        if (length <= 0F) return;
        rotation.rotationTo(0F, 1F, 0F, dx / length, dy / length, dz / length);
        linePose.translation(
                        pos.getX() - renderOrigin().getX() + a.x,
                        pos.getY() - renderOrigin().getY() + a.y,
                        pos.getZ() - renderOrigin().getZ() + a.z)
                .rotate(rotation)
                .scale(length);
        line.setTransform(linePose).colorArgb(color).light(lastLineLight).setChanged();
    }

    private void hideLines() {
        for (int i = 0; i < usedLines; i++) lines.get(i).setVisible(false);
        usedLines = 0;
    }

    @Override
    public void beforeFrame() {
        if (blockEntity.fieldRadius != boundsRadius) {
            boundsRadius = blockEntity.fieldRadius;
            refreshVisibleBounds();
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return litBounds;
    }

    @Override
    protected AABB visibleBounds() {
        return litBounds.minmax(new AABB(pos).inflate(boundsRadius + 2D));
    }

    @Override
    public void updateLight(float partialTick) {
        lineLight = LightCoordsUtil.getLightCoords(level, pos);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        top.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        top.delete();
        for (var line : lines) line.delete();
    }
}
