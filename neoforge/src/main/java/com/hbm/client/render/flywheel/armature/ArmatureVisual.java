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

package com.hbm.client.render.flywheel.armature;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import java.util.function.Consumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public final class ArmatureVisual {
    public static final int CLOSED = 0;
    public static final int OPEN = 1;
    public static final int OPENING = 2;
    public static final int SHUTTING = 3;

    private final ArmatureModel prepared;
    private final ArmatureType type;
    private final Instance[] instances;
    private final boolean[] hidden;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f prefix = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final float[] matrix = new float[16];
    private final int rootOffset;
    private final int clockOffset;
    private final int styleOffset;
    private final int sphereOffset;
    private InstancerProvider provider;
    private boolean initialized;
    private boolean visible = true;
    private int lower = -1;
    private int phase = -1;
    private int clockValue;
    private int anchor;
    private boolean fixed;
    private boolean orthonormalBase;
    private int light;
    private int overlay;
    private float sphereScale;
    private long windowUploads;
    private long instanceUploads;
    private long metadataWrites;

    public ArmatureVisual(InstancerProvider provider, ArmatureModel prepared) {
        this.provider = provider;
        this.prepared = prepared;
        type = prepared.animatedType();
        rootOffset = type.offset("rootPose");
        clockOffset = type.offset("clock");
        styleOffset = type.offset("style");
        sphereOffset = type.offset("sphere");
        instances = new Instance[prepared.getScene().getGeometryCount()];
        hidden = new boolean[instances.length];
        for (int ordinal = 0; ordinal < instances.length; ordinal++) {
            var binding = prepared.binding(ordinal);
            if (binding.animatedNode >= 0) {
                var instance = provider.instancer(type, binding.model, ordinal).createInstance();
                prepared.writePost(instance.handle().slabPtr(), binding);
                instances[ordinal] = instance;
            } else {
                instances[ordinal] =
                        provider.instancer(InstanceTypes.POSED, binding.model, ordinal)
                                .createInstance();
            }
        }
    }

    public void update(
            InstancerProvider provider,
            Matrix4fc basePose,
            long logicalMillis,
            long shaderGameMillis,
            long anchorMillis,
            int directedPhase,
            boolean fixedClock,
            int packedLight,
            int packedOverlay) {
        if (this.provider != provider) {
            this.provider = provider;
            reacquireInstances();
        }
        boolean baseChanged = !initialized || !base.equals(basePose, 0);
        if (baseChanged) {
            base.set(basePose);
            sphereScale = affineScaleBound(base);
            orthonormalBase = (base.properties() & Matrix4fc.PROPERTY_ORTHONORMAL) != 0;
        }
        int duration = prepared.getClip().getDurationMillis();
        long elapsed = Math.clamp(logicalMillis - anchorMillis, 0L, duration);
        int effectivePhase = directedPhase;
        if (elapsed == duration) {
            if (directedPhase == OPENING) {
                effectivePhase = OPEN;
            } else if (directedPhase == SHUTTING) {
                effectivePhase = CLOSED;
            }
        }
        int sampleMillis =
                switch (effectivePhase) {
                    case CLOSED -> 0;
                    case OPEN -> duration;
                    case OPENING -> (int) elapsed;
                    case SHUTTING -> duration - (int) elapsed;
                    default -> throw new IllegalArgumentException("Unknown armature phase");
                };
        float normalized = (float) sampleMillis / (float) duration;
        float frame = normalized * (prepared.getClip().getFrameCount() - 1);
        int selectedLower = (int) Math.floor(frame);
        boolean windowChanged = selectedLower != lower;
        boolean moving = effectivePhase == OPENING || effectivePhase == SHUTTING;
        boolean held = !moving;
        boolean previouslyHeld = phase == CLOSED || phase == OPEN;
        boolean representationChanged = !initialized || held != previouslyHeld;
        boolean endpointDataChanged = held && (representationChanged || effectivePhase != phase);
        boolean movingDataChanged = moving && (windowChanged || representationChanged);
        boolean nextFixed = moving && fixedClock;
        int nextClock =
                moving ? (int) (fixedClock ? logicalMillis : logicalMillis - shaderGameMillis) : 0;
        int nextAnchor = moving ? (int) anchorMillis : 0;
        boolean clockChanged =
                !initialized
                        || effectivePhase != phase
                        || nextClock != clockValue
                        || nextAnchor != anchor
                        || nextFixed != fixed;
        boolean lightChanged = !initialized || packedLight != light || packedOverlay != overlay;
        if (baseChanged || windowChanged || clockChanged || lightChanged || representationChanged) {
            for (int ordinal = 0; ordinal < instances.length; ordinal++) {
                var binding = prepared.binding(ordinal);
                var instance = instances[ordinal];
                boolean nodeHidden = prepared.hidden(binding, selectedLower);
                boolean visibilityChanged = !initialized || hidden[ordinal] != nodeHidden;
                hidden[ordinal] = nodeHidden;
                if (binding.animatedNode >= 0) {
                    long destination = instance.handle().slabPtr();
                    if (baseChanged || representationChanged) {
                        if (held) {
                            pose.set(base);
                        } else {
                            prefix.set(binding.before);
                            pose.set(base).mul(prefix);
                        }
                        writePose(destination);
                    }
                    if (baseChanged) {
                        MemoryUtil.memPutFloat(destination + sphereOffset, base.m30());
                        MemoryUtil.memPutFloat(destination + sphereOffset + 4, base.m31());
                        MemoryUtil.memPutFloat(destination + sphereOffset + 8, base.m32());
                        MemoryUtil.memPutFloat(
                                destination + sphereOffset + 12,
                                Math.nextUp(binding.radius * sphereScale));
                    }
                    if (held) {
                        if (endpointDataChanged) {
                            prepared.writeEndpoint(destination, effectivePhase == OPEN, binding);
                            windowUploads++;
                        }
                    } else {
                        if (representationChanged) {
                            prepared.writePost(destination, binding);
                        }
                        if (movingDataChanged) {
                            prepared.writeWindow(destination, selectedLower, binding);
                            windowUploads++;
                        }
                    }
                    if (clockChanged
                            || windowChanged
                            || lightChanged
                            || visibilityChanged
                            || baseChanged
                            || representationChanged
                            || endpointDataChanged) {
                        int flags =
                                effectivePhase
                                        | (nextFixed ? 4 : 0)
                                        | (nodeHidden ? 8 : 0)
                                        | (orthonormalBase ? 16 : 0);
                        MemoryUtil.memPutInt(destination + clockOffset, nextClock);
                        MemoryUtil.memPutInt(destination + clockOffset + 4, nextAnchor);
                        MemoryUtil.memPutInt(destination + clockOffset + 8, duration);
                        MemoryUtil.memPutInt(
                                destination + clockOffset + 12,
                                prepared.getClip().getFrameCount() - 1);
                        MemoryUtil.memPutInt(destination + styleOffset, packedOverlay);
                        MemoryUtil.memPutInt(destination + styleOffset + 4, packedLight);
                        MemoryUtil.memPutInt(destination + styleOffset + 8, flags);
                        MemoryUtil.memPutInt(destination + styleOffset + 12, selectedLower);
                        metadataWrites++;
                    }
                    instance.setChanged();
                    instanceUploads++;
                } else {
                    var staticInstance = (PosedInstance) instance;
                    if (baseChanged) {
                        prefix.set(binding.before);
                        pose.set(base).mul(prefix);
                        normal.set(pose).invert().transpose();
                        staticInstance.setTransform(pose, normal);
                    }
                    if (lightChanged) {
                        staticInstance.light(packedLight);
                        staticInstance.overlay(packedOverlay);
                    }
                    if (visibilityChanged) {
                        staticInstance.setVisible(visible && !nodeHidden);
                    }
                    if (baseChanged || lightChanged) {
                        staticInstance.setChanged();
                        instanceUploads++;
                    }
                }
            }
        }
        lower = selectedLower;
        phase = effectivePhase;
        clockValue = nextClock;
        anchor = nextAnchor;
        fixed = nextFixed;
        light = packedLight;
        overlay = packedOverlay;
        initialized = true;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            for (int ordinal = 0; ordinal < instances.length; ordinal++) {
                var binding = prepared.binding(ordinal);
                instances[ordinal].setVisible(
                        visible && (binding.animatedNode >= 0 || !hidden[ordinal]));
            }
        }
    }

    public void collectCrumbling(Consumer<Instance> consumer) {
        if (visible) {
            for (int ordinal = 0; ordinal < instances.length; ordinal++) {
                if (!hidden[ordinal]) {
                    consumer.accept(instances[ordinal]);
                }
            }
        }
    }

    public long getWindowUploadCount() {
        return windowUploads;
    }

    public long getInstanceUploadCount() {
        return instanceUploads;
    }

    public long getMetadataWriteCount() {
        return metadataWrites;
    }

    public void delete() {
        for (var instance : instances) {
            instance.delete();
        }
    }

    private void reacquireInstances() {
        for (int ordinal = 0; ordinal < instances.length; ordinal++) {
            var binding = prepared.binding(ordinal);
            if (binding.animatedNode >= 0) {
                provider.instancer(type, binding.model, ordinal)
                        .stealInstance((ArmatureInstance) instances[ordinal]);
            } else {
                provider.instancer(InstanceTypes.POSED, binding.model, ordinal)
                        .stealInstance((PosedInstance) instances[ordinal]);
            }
        }
    }

    private void writePose(long destination) {
        pose.get(matrix);
        for (int component = 0; component < 16; component++) {
            MemoryUtil.memPutFloat(destination + rootOffset + component * 4L, matrix[component]);
        }
    }

    private static float affineScaleBound(Matrix4fc pose) {
        float row0 = Math.abs(pose.m00()) + Math.abs(pose.m10()) + Math.abs(pose.m20());
        float row1 = Math.abs(pose.m01()) + Math.abs(pose.m11()) + Math.abs(pose.m21());
        float row2 = Math.abs(pose.m02()) + Math.abs(pose.m12()) + Math.abs(pose.m22());
        float column0 = Math.abs(pose.m00()) + Math.abs(pose.m01()) + Math.abs(pose.m02());
        float column1 = Math.abs(pose.m10()) + Math.abs(pose.m11()) + Math.abs(pose.m12());
        float column2 = Math.abs(pose.m20()) + Math.abs(pose.m21()) + Math.abs(pose.m22());
        float maximumRow = Math.max(row0, Math.max(row1, row2));
        float maximumColumn = Math.max(column0, Math.max(column1, column2));
        return Math.nextUp((float) Math.sqrt((double) maximumRow * maximumColumn));
    }
}
