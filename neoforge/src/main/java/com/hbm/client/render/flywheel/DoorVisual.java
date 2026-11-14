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

import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.door.DoorType;
import com.hbm.blockentity.door.DoorTypes;
import com.hbm.client.render.DoorRenderDefinition;
import com.hbm.client.render.flywheel.armature.ArmatureModel;
import com.hbm.client.render.flywheel.armature.ArmatureVisual;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.GpuAnimationTrack;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public final class DoorVisual extends AbstractDynamicExtentBlockEntityVisual<DoorBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final int NONE = 0, HALFSPACE = 1, SLAB = 2;
    private static final int SWEEP = 64;
    private static final float SWEEP_MARGIN = .125F;
    private static final GpuAnimationTrack CLOSED =
            new GpuAnimationTrack(new int[0], new float[0], new int[0], 0F);
    private static final GpuAnimationTrack OPEN =
            new GpuAnimationTrack(new int[0], new float[0], new int[0], 1F);
    private static final DoorAssets SILO_HATCH = new DoorAssets(DoorTypes.SILO_HATCH);
    private static final DoorAssets SILO_HATCH_LARGE = new DoorAssets(DoorTypes.SILO_HATCH_LARGE);
    private static final DoorAssets FIRE_DOOR = new DoorAssets(DoorTypes.FIRE_DOOR);
    private static final DoorAssets SECURE_ACCESS_DOOR =
            new DoorAssets(DoorTypes.SECURE_ACCESS_DOOR);
    private static final DoorAssets QE_SLIDING = new DoorAssets(DoorTypes.QE_SLIDING_DOOR);
    private static final DoorAssets CARGO_DOOR = new DoorAssets(DoorTypes.CARGO_DOOR);
    private static final DoorAssets WATER_DOOR = new DoorAssets(DoorTypes.WATER_DOOR);
    private static final DoorAssets VAULT_DOOR = new DoorAssets(DoorTypes.VAULT_DOOR);
    private static final DoorAssets SLIDING_SEAL_DOOR = new DoorAssets(DoorTypes.SLIDING_SEAL_DOOR);
    private static final DoorAssets QE_CONTAINMENT = new DoorAssets(DoorTypes.QE_CONTAINMENT);
    private static final DoorAssets ROUND_AIRLOCK_DOOR =
            new DoorAssets(DoorTypes.ROUND_AIRLOCK_DOOR);
    private static final DoorAssets SLIDE_DOOR = new DoorAssets(DoorTypes.SLIDING_BLAST_DOOR);
    private static final DoorAssets LARGE_VEHICLE_DOOR =
            new DoorAssets(DoorTypes.LARGE_VEHICLE_DOOR);
    private static final DoorAssets TRANSITION_SEAL = new DoorAssets(DoorTypes.TRANSITION_SEAL);
    private static final DoorAssets[] ASSETS = {
        SILO_HATCH,
        SILO_HATCH_LARGE,
        FIRE_DOOR,
        SECURE_ACCESS_DOOR,
        QE_SLIDING,
        CARGO_DOOR,
        WATER_DOOR,
        VAULT_DOOR,
        SLIDING_SEAL_DOOR,
        QE_CONTAINMENT,
        ROUND_AIRLOCK_DOOR,
        SLIDE_DOOR,
        LARGE_VEHICLE_DOOR,
        TRANSITION_SEAL
    };
    private final DoorType declaration;
    private final DoorAssets assets;
    private final String[] partNames;
    private final int kind;
    private final Instance[] instances;
    private final Model[] models;
    private final boolean[] blended;
    private final float[] spheres;
    private final Matrix4f base = new Matrix4f(),
            bodyPose = new Matrix4f(),
            worldBase = new Matrix4f();
    private final Matrix4f partPose = new Matrix4f();
    private final Matrix4f boundsPose = new Matrix4f();
    private final @Nullable ArmatureVisual armature;
    private final float[] clip = new float[DoorAnimationData.CLIP], origin = new float[3];
    private final Vector3f center = new Vector3f();
    private final double[] lightBounds = new double[6];
    private final AABB bodyBounds;
    private @Nullable AnimationPlayback animation;
    private GpuAnimationTrack firstTrack = CLOSED, secondTrack = CLOSED;
    private @Nullable AnimationSequence firstBus, secondBus;
    private int firstDimension, secondDimension, duration;
    private int skin = Integer.MIN_VALUE;
    private byte lastState = -1;
    private long lastStart;
    private float lastOpen = Float.NaN, lastDirection;
    private boolean initialized;
    private AABB renderBounds;
    private @Nullable AABB lastLightBounds;

    public DoorVisual(
            VisualizationContext context, DoorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        declaration = blockEntity.getDoorType();
        assets = assets(declaration);
        DoorRenderDefinition look = declaration.getRenderDefinition();
        partNames = look.getMovingParts();
        base.translation(.5F, 0, .5F)
                .rotateY(Facing.yaw(blockEntity.getFacing(), 90) * Mth.DEG_TO_RAD);
        worldBase.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(base);
        var offset = look.getOffset();
        bodyPose.set(base)
                .rotateY(look.getYaw() * Mth.DEG_TO_RAD)
                .translate(offset.x(), offset.y(), offset.z());
        var source = look.getModel();
        AABB extent = new AABB(pos).inflate(1);
        for (String name : look.getStaticParts())
            extent = extent.minmax(LightBounds.of(source, name, bodyPose, pos));
        bodyBounds = extent;
        renderBounds = bodyBounds;
        if (assets.armature != null) {
            worldBase.translate(0F, 0F, .5F);
            kind = -1;
            armature = new ArmatureVisual(instancerProvider(), assets.armature);
            float[] motionExtent = look.getExtent();
            double radius = motionExtent[2];
            var motionBounds =
                    new AABB(
                            pos.getX() + .5 - radius,
                            pos.getY() + motionExtent[0],
                            pos.getZ() + .5 - radius,
                            pos.getX() + .5 + radius,
                            pos.getY() + motionExtent[1],
                            pos.getZ() + .5 + radius);
            LightBounds.resetBounds(lightBounds, bodyBounds.minmax(motionBounds));
            publishBounds();
        } else {
            kind = DoorAnimationData.kind(declaration);
            armature = null;
        }
        int count = armature == null ? partNames.length : 0;
        instances = new Instance[count];
        models = new Model[count];
        blended = new boolean[count];
        spheres = new float[count * 4];
        installSkins(blockEntity.getSkinIndex());
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    public static void reloadTextures() {
        for (DoorAssets assets : ASSETS) assets.bindTextures();
    }

    private static DoorAssets assets(DoorType declaration) {
        if (declaration == DoorTypes.SILO_HATCH) return SILO_HATCH;
        if (declaration == DoorTypes.SILO_HATCH_LARGE) return SILO_HATCH_LARGE;
        if (declaration == DoorTypes.FIRE_DOOR) return FIRE_DOOR;
        if (declaration == DoorTypes.SECURE_ACCESS_DOOR) return SECURE_ACCESS_DOOR;
        if (declaration == DoorTypes.QE_SLIDING_DOOR) return QE_SLIDING;
        if (declaration == DoorTypes.CARGO_DOOR) return CARGO_DOOR;
        if (declaration == DoorTypes.WATER_DOOR) return WATER_DOOR;
        if (declaration == DoorTypes.VAULT_DOOR) return VAULT_DOOR;
        if (declaration == DoorTypes.SLIDING_SEAL_DOOR) return SLIDING_SEAL_DOOR;
        if (declaration == DoorTypes.QE_CONTAINMENT) return QE_CONTAINMENT;
        if (declaration == DoorTypes.ROUND_AIRLOCK_DOOR) return ROUND_AIRLOCK_DOOR;
        if (declaration == DoorTypes.SLIDING_BLAST_DOOR) return SLIDE_DOOR;
        if (declaration == DoorTypes.LARGE_VEHICLE_DOOR) return LARGE_VEHICLE_DOOR;
        if (declaration == DoorTypes.TRANSITION_SEAL) return TRANSITION_SEAL;
        throw new IllegalArgumentException();
    }

    private static int clip(DoorType declaration, String[] names, int index) {
        if (declaration == DoorTypes.ROUND_AIRLOCK_DOOR
                || declaration == DoorTypes.LARGE_VEHICLE_DOOR) return SLAB;
        if (declaration == DoorTypes.QE_CONTAINMENT || declaration == DoorTypes.SLIDING_SEAL_DOOR)
            return HALFSPACE;
        if (declaration == DoorTypes.SLIDING_BLAST_DOOR)
            return names[index].endsWith("Lock") ? HALFSPACE : SLAB;
        return NONE;
    }

    private static Model bind(
            Mesh mesh, Identifier texture, int clip, boolean cull, boolean blended) {
        var built = doorMaterial(texture, clip, cull, blended);
        return blended ? new SingleMeshModel(mesh, built) : MeshPart.create(mesh, built).model();
    }

    private static Material doorMaterial(
            Identifier texture, int clip, boolean cull, boolean blended) {
        var material =
                SimpleMaterial.builderOf(
                                switch (clip) {
                                    case HALFSPACE -> Materials.CUTOUT_CLIP_HALFSPACE;
                                    case SLAB -> Materials.CUTOUT_CLIP_SLAB;
                                    default -> Materials.CUTOUT;
                                })
                        .texture(texture)
                        .mipmap(false)
                        .backfaceCulling(cull)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .transparency(
                                blended ? Transparency.ORDER_INDEPENDENT : Transparency.OPAQUE)
                        .writeMask(blended ? WriteMask.COLOR : WriteMask.COLOR_DEPTH);
        if (clip == NONE) material.cutout(CutoutShaders.ONE_TENTH);
        return material.build();
    }

    private void installSkins(int skin) {
        this.skin = skin;
        for (int i = 0; i < instances.length; i++) {
            if (instances[i] != null) instances[i].delete();
            var model = assets.moving[Math.floorMod(skin, assets.skinCount)][i];
            models[i] = model;
            blended[i] = model.meshes().getFirst().material().transparency() != Transparency.OPAQUE;
            instances[i] = instancerProvider().instancer(DoorInstance.TYPE, model).createInstance();
        }
        initialized = false;
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        if (skin != blockEntity.getSkinIndex()) installSkins(blockEntity.getSkinIndex());
        if (armature != null) updateArmature();
        else if (kind == DoorAnimationData.SILO) updateSilo(partialTick);
        else updateTransition();
    }

    private void updateArmature() {
        int phase =
                switch (blockEntity.phase) {
                    case DoorBlockEntity.CLOSED -> ArmatureVisual.CLOSED;
                    case DoorBlockEntity.OPEN -> ArmatureVisual.OPEN;
                    case DoorBlockEntity.OPENING -> ArmatureVisual.OPENING;
                    case DoorBlockEntity.SHUTTING -> ArmatureVisual.SHUTTING;
                    default ->
                            throw new IllegalStateException(
                                    "Unknown door phase " + blockEntity.phase);
                };
        armature.update(
                instancerProvider(),
                worldBase,
                blockEntity.getAnimationTimeMillis(),
                GameTime.now(),
                blockEntity.transitionStart,
                phase,
                blockEntity.isAnimationDriven(),
                0,
                OverlayTexture.NO_OVERLAY);
    }

    private void updateSilo(float partialTick) {
        byte state = blockEntity.phase;
        float open, direction;
        if (blockEntity.isAnimationDriven()) {
            open = blockEntity.getOpenTicks(partialTick);
            direction = 0;
        } else {
            open = blockEntity.motionTicks;
            direction =
                    state == DoorBlockEntity.OPENING
                            ? 1
                            : state == DoorBlockEntity.SHUTTING ? -1 : 0;
        }
        if (initialized && open == lastOpen && direction == lastDirection) return;
        lastOpen = open;
        lastDirection = direction;
        if (!initialized) {
            int ticks = declaration.getOpeningTicks();
            sweep(step -> (double) ticks * step / SWEEP, _ -> 0D, SWEEP);
        }
        initialized = true;
        var hinge = declaration.getHinge();
        int hinged = declaration.getRenderDefinition().getHingeIndex();
        for (int i = 0; i < instances.length; i++) {
            boolean moves = i == hinged;
            origin[0] = moves ? hinge.originX() : 0F;
            origin[1] = moves ? hinge.originY() : 0F;
            origin[2] = moves ? hinge.originZ() : 0F;
            DoorInstance instance = write(i);
            instance.silo(open, direction, declaration.getOpeningTicks(), origin);
            instance.setChanged();
        }
    }

    private void updateTransition() {
        byte state = blockEntity.phase;
        var current = blockEntity.playback;
        boolean retracked =
                !initialized || current != animation || current == null && state != lastState;
        if (current != animation) {
            animation = current;
            if (current != null) {
                Channels channels = channels(declaration);
                firstBus = current.clip().getBus(channels.first());
                secondBus = current.clip().getBus(channels.second());
                firstDimension = channels.firstDimension();
                secondDimension = channels.secondDimension();
                duration = current.clip().getDuration();
            }
        }
        long start =
                current == null
                        ? 0L
                        : blockEntity.isAnimationDriven()
                                ? GameTime.now()
                                        - (blockEntity.getAnimationTimeMillis() - current.start())
                                : current.start();
        if (!retracked && start == lastStart) return;
        lastStart = start;
        lastState = state;
        if (retracked) {
            if (current == null) {
                firstTrack = secondTrack = state == DoorBlockEntity.OPEN ? OPEN : CLOSED;
                duration = 0;
                double rest = firstTrack.rest();
                sweep(_ -> rest, _ -> rest, 0);
            } else {
                firstTrack = firstBus == null ? CLOSED : firstBus.compileGpuTrack(firstDimension);
                secondTrack =
                        secondBus == null ? CLOSED : secondBus.compileGpuTrack(secondDimension);
                sweep(
                        step -> value(firstBus, firstDimension, step),
                        step -> value(secondBus, secondDimension, step),
                        SWEEP);
            }
        }
        initialized = true;
        for (int i = 0; i < instances.length; i++) {
            DoorInstance instance = write(i);
            instance.clips(firstTrack, secondTrack, start, duration);
            instance.setChanged();
        }
    }

    public static Channels channels(DoorType declaration) {
        if (declaration == DoorTypes.WATER_DOOR) return new Channels("DOOR", 1, "BOLT", 2);
        if (declaration == DoorTypes.VAULT_DOOR) return new Channels("PULL", 2, "SLIDE", 0);
        if (declaration == DoorTypes.CARGO_DOOR) return new Channels("TOP", 1, "BOT", 1);
        return new Channels("DOOR", 1, "LOCK", 0);
    }

    public record Channels(String first, int firstDimension, String second, int secondDimension) {}

    private double value(@Nullable AnimationSequence bus, int dimension, int step) {
        return bus == null ? 0 : bus.getValue(dimension, (int) ((long) duration * step / SWEEP));
    }

    private DoorInstance write(int index) {
        DoorInstance instance = (DoorInstance) instances[index];
        int at = index * 4;
        instance.setVisible(true);
        instance.setTransform(worldBase);
        instance.light(0);
        instance.part(kind, index)
                .bounds(spheres[at], spheres[at + 1], spheres[at + 2], spheres[at + 3]);
        return instance;
    }

    @FunctionalInterface
    private interface Channel {
        double at(int step);
    }

    private void sweep(Channel first, Channel second, int steps) {
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < instances.length; i++) {
            Vector4fc sphere = models[i].boundingSphere();
            float minX = Float.POSITIVE_INFINITY,
                    minY = Float.POSITIVE_INFINITY,
                    minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY,
                    maxY = Float.NEGATIVE_INFINITY,
                    maxZ = Float.NEGATIVE_INFINITY;
            for (int step = 0; step <= steps; step++) {
                DoorAnimationData.pose(
                        declaration, kind, i, first.at(step), second.at(step), partPose, clip);
                partPose.translate(clip[0], clip[1], clip[2]);
                partPose.transformPosition(sphere.x(), sphere.y(), sphere.z(), center);
                minX = Math.min(minX, center.x);
                minY = Math.min(minY, center.y);
                minZ = Math.min(minZ, center.z);
                maxX = Math.max(maxX, center.x);
                maxY = Math.max(maxY, center.y);
                maxZ = Math.max(maxZ, center.z);
                boundsPose.set(base).mul(partPose);
                LightBounds.includeLightBounds(lightBounds, models[i], boundsPose, pos);
            }
            int at = i * 4;
            spheres[at] = (minX + maxX) * .5F;
            spheres[at + 1] = (minY + maxY) * .5F;
            spheres[at + 2] = (minZ + maxZ) * .5F;
            spheres[at + 3] =
                    .5F
                                    * (float)
                                            Math.sqrt(
                                                    (maxX - minX) * (maxX - minX)
                                                            + (maxY - minY) * (maxY - minY)
                                                            + (maxZ - minZ) * (maxZ - minZ))
                            + sphere.w()
                            + SWEEP_MARGIN;
        }
        publishBounds();
    }

    private void publishBounds() {
        AABB next = LightBounds.sectionBounds(lightBounds, renderBounds);
        if (next != renderBounds) {
            renderBounds = next;
            refreshVisibleBounds();
        }
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (armature != null) armature.collectCrumbling(consumer);
        for (int i = 0; i < instances.length; i++) if (!blended[i]) consumer.accept(instances[i]);
    }

    @Override
    protected void _delete() {
        if (armature != null) armature.delete();
        for (var instance : instances) instance.delete();
    }

    private static final class DoorAssets {
        final DoorType declaration;
        final int skinCount;
        final Mesh[] movingMeshes;
        final @Nullable ArmatureModel armature;
        final Model[][] moving;
        final Model[][][] movingVariants;

        DoorAssets(DoorType declaration) {
            this.declaration = declaration;
            skinCount = Math.max(1, declaration.getSkinCount());
            var source = declaration.getRenderDefinition().getModel();
            if (declaration == DoorTypes.TRANSITION_SEAL) {
                armature =
                        new ArmatureModel(
                                ResourceManager.transition_seal(),
                                ResourceManager.transition_seal_anim(),
                                doorMaterial(
                                        ResourceManager.transition_seal_tex, NONE, true, false));
                movingMeshes = new Mesh[0];
            } else {
                armature = null;
                int[] ids = declaration.getRenderDefinition().getMovingPartIds();
                movingMeshes = new Mesh[ids.length];
                for (int i = 0; i < movingMeshes.length; i++)
                    movingMeshes[i] = PackedQuadMesh.of(source.groups[ids[i]], source.smoothing());
            }
            moving = new Model[skinCount][movingMeshes.length];
            movingVariants = new Model[skinCount][movingMeshes.length][2];
            buildBindings();
            bindTextures();
        }

        void buildBindings() {
            boolean cull =
                    declaration != DoorTypes.ROUND_AIRLOCK_DOOR
                            && declaration != DoorTypes.LARGE_VEHICLE_DOOR
                            && declaration != DoorTypes.QE_SLIDING_DOOR
                            && declaration != DoorTypes.SLIDING_BLAST_DOOR;
            String[] movingNames = declaration.getRenderDefinition().getMovingParts();
            for (int skin = 0; skin < skinCount; skin++) {
                for (int i = 0; i < movingMeshes.length; i++) {
                    Identifier texture =
                            declaration.getRenderDefinition().getMovingTexture(skin, i);
                    int clip = clip(declaration, movingNames, i);
                    movingVariants[skin][i][0] = bind(movingMeshes[i], texture, clip, cull, false);
                    movingVariants[skin][i][1] = bind(movingMeshes[i], texture, clip, cull, true);
                }
            }
        }

        void bindTextures() {
            boolean blend =
                    declaration == DoorTypes.SILO_HATCH
                            || declaration == DoorTypes.SILO_HATCH_LARGE;
            for (int skin = 0; skin < skinCount; skin++) {
                for (int i = 0; i < movingMeshes.length; i++) {
                    Identifier texture =
                            declaration.getRenderDefinition().getMovingTexture(skin, i);
                    moving[skin][i] =
                            movingVariants[skin][i][
                                    blend && VisualTextures.translucentTexture(texture) ? 1 : 0];
                }
            }
        }
    }
}
