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

import com.hbm.animloader.ArmatureScene;
import com.hbm.animloader.SampledClip;
import com.hbm.animloader.TriangleSource;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import java.util.Arrays;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

public final class ArmatureModel {
    private static final int COMPONENTS_PER_NODE = 24;
    private static final float LINEAR_QUATERNION_THRESHOLD = 0.9999999f;

    private final ArmatureScene scene;
    private final SampledClip clip;
    private final float[] windows;
    private final GeometryBinding[] bindings;
    private final ArmatureType animatedType = new ArmatureType();
    private final int postOffset = animatedType.offset("postRows");
    private final int keysOffset = animatedType.offset("poseKeys");
    private final int curveOffset = animatedType.offset("orientationCurve");

    public ArmatureModel(ArmatureScene scene, SampledClip clip, Material material) {
        this(scene, clip, materials(scene.getGeometryCount(), material));
    }

    public ArmatureModel(ArmatureScene scene, SampledClip clip, Material[] materials) {
        if (materials.length != scene.getGeometryCount()) {
            throw new IllegalArgumentException("Armature material count");
        }
        this.scene = scene;
        this.clip = clip;
        windows = new float[clip.getFrameCount() * scene.getNodeCount() * COMPONENTS_PER_NODE];
        bindings = new GeometryBinding[materials.length];
        var animated = new boolean[scene.getNodeCount()];
        var prefixes = new float[scene.getNodeCount() * 16];
        var prefix = new Matrix4f();
        var local = new Matrix4f();
        for (int node = 0; node < scene.getNodeCount(); node++) {
            int parent = scene.getParent(node);
            animated[node] = scene.hasLocal(node) && scene.getTrack(node) >= 0;
            if (parent < 0 || animated[parent]) {
                prefix.identity();
            } else {
                prefix.set(prefixes, parent * 16);
            }
            if (!animated[node] && scene.hasLocal(node)) {
                scene.getLocalPose(node, local);
                prefix.mul(local);
            }
            prefix.get(prefixes, node * 16);
        }
        prepareWindows(animated);
        for (int ordinal = 0; ordinal < materials.length; ordinal++) {
            int node = scene.getGeometryNode(ordinal);
            int animatedNode = -1;
            for (int ancestor = node; ancestor >= 0; ancestor = scene.getParent(ancestor)) {
                if (animated[ancestor]) {
                    if (animatedNode >= 0) {
                        throw new IllegalArgumentException(
                                "Unsupported animated hierarchy in hbm:models/doors/seal.dae");
                    }
                    animatedNode = ancestor;
                }
            }
            var before = new float[16];
            var after = new float[16];
            if (animatedNode < 0) {
                System.arraycopy(prefixes, node * 16, before, 0, 16);
                new Matrix4f().get(after);
            } else {
                System.arraycopy(prefixes, animatedNode * 16, before, 0, 16);
                if (node == animatedNode) {
                    new Matrix4f().get(after);
                } else {
                    System.arraycopy(prefixes, node * 16, after, 0, 16);
                }
            }
            var mesh = new SourceMesh(scene.getGeometryAt(ordinal));
            var configured = new Model.ConfiguredMesh(materials[ordinal], mesh);
            var geometryModel = new SceneModel(List.of(configured), mesh.boundingSphere());
            bindings[ordinal] =
                    new GeometryBinding(
                            animatedNode,
                            scene.getTrack(node),
                            before,
                            after,
                            geometryModel,
                            scene.getNodeEnvelope(node));
            if (animatedNode >= 0) {
                prepareEndpoints(bindings[ordinal]);
            }
        }
    }

    public ArmatureScene getScene() {
        return scene;
    }

    public SampledClip getClip() {
        return clip;
    }

    ArmatureType animatedType() {
        return animatedType;
    }

    GeometryBinding binding(int ordinal) {
        return bindings[ordinal];
    }

    void writePost(long destination, GeometryBinding binding) {
        long output = destination + postOffset;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 4; column++) {
                MemoryUtil.memPutFloat(
                        output + row * 16L + column * 4L, binding.after[column * 4 + row]);
            }
        }
    }

    boolean hidden(GeometryBinding binding, int lower) {
        return binding.visibilityTrack >= 0 && clip.isHidden(binding.visibilityTrack, lower);
    }

    void writeWindow(long destination, int lower, GeometryBinding binding) {
        int nodes = scene.getNodeCount();
        int sampled = binding.animatedNode;
        int start = lower * nodes * COMPONENTS_PER_NODE + sampled * 4;
        int t0 = start;
        int q0 = start + nodes * 4;
        int s0 = start + nodes * 8;
        int t1 = start + nodes * 12;
        int q1 = start + nodes * 16;
        int s1 = start + nodes * 20;
        long output = destination + keysOffset;
        putVector(output, windows[t0], windows[t0 + 1], windows[t0 + 2], windows[t1]);
        putVector(output + 16, windows[t1 + 1], windows[t1 + 2], windows[s0], windows[s0 + 1]);
        putVector(output + 32, windows[s0 + 2], windows[s1], windows[s1 + 1], windows[s1 + 2]);
        putVector(output + 48, windows[q0], windows[q0 + 1], windows[q0 + 2], windows[q0 + 3]);
        putVector(output + 64, windows[q1], windows[q1 + 1], windows[q1 + 2], windows[q1 + 3]);
        output = destination + curveOffset;
        putVector(output, windows[s0 + 3], windows[s1 + 3], windows[t1 + 3], 0);
    }

    void writeEndpoint(long destination, boolean open, GeometryBinding binding) {
        int endpoint = open ? 1 : 0;
        int poseStart = endpoint * 16;
        for (int component = 0; component < 16; component++) {
            MemoryUtil.memPutFloat(
                    destination + keysOffset + component * 4L,
                    binding.endpointData[poseStart + component]);
        }
        int normalStart = 32 + endpoint * 12;
        for (int component = 0; component < 12; component++) {
            MemoryUtil.memPutFloat(
                    destination + postOffset + component * 4L,
                    binding.endpointData[normalStart + component]);
        }
    }

    private void prepareEndpoints(GeometryBinding binding) {
        var before = new Matrix4f().set(binding.before);
        var after = new Matrix4f().set(binding.after);
        var localPose = new Matrix4f();
        var modelPose = new Matrix4f();
        var normal = new Matrix3f();
        for (int endpoint = 0; endpoint < 2; endpoint++) {
            int frame = endpoint == 0 ? 0 : clip.getFrameCount() - 1;
            int track = scene.getTrack(binding.animatedNode);
            clip.sample(track, frame, frame, 0, localPose);
            modelPose.set(before).mul(localPose).mul(after);
            modelPose.get(binding.endpointData, endpoint * 16);
            normal.set(modelPose).invert().transpose();
            int start = 32 + endpoint * 12;
            binding.endpointData[start] = normal.m00();
            binding.endpointData[start + 1] = normal.m01();
            binding.endpointData[start + 2] = normal.m02();
            binding.endpointData[start + 4] = normal.m10();
            binding.endpointData[start + 5] = normal.m11();
            binding.endpointData[start + 6] = normal.m12();
            binding.endpointData[start + 8] = normal.m20();
            binding.endpointData[start + 9] = normal.m21();
            binding.endpointData[start + 10] = normal.m22();
        }
    }

    private void prepareWindows(boolean[] animated) {
        int nodes = scene.getNodeCount();
        for (int lower = 0; lower < clip.getFrameCount(); lower++) {
            int upper = Math.min(lower + 1, clip.getFrameCount() - 1);
            int window = lower * nodes * COMPONENTS_PER_NODE;
            for (int node = 0; node < nodes; node++) {
                if (!animated[node]) {
                    continue;
                }
                int track = scene.getTrack(node);
                int t0 = window + node * 4;
                int q0 = t0 + nodes * 4;
                int s0 = q0 + nodes * 4;
                int t1 = s0 + nodes * 4;
                int q1 = t1 + nodes * 4;
                int s1 = q1 + nodes * 4;
                for (int component = 0; component < 3; component++) {
                    windows[t0 + component] = clip.getComponent(track, lower, component);
                    windows[t1 + component] = clip.getComponent(track, upper, component);
                    windows[s0 + component] = clip.getComponent(track, lower, component + 7);
                    windows[s1 + component] = clip.getComponent(track, upper, component + 7);
                }
                float dot = 0;
                for (int component = 0; component < 4; component++) {
                    float from = clip.getComponent(track, lower, component + 3);
                    float to = clip.getComponent(track, upper, component + 3);
                    windows[q0 + component] = from;
                    windows[q1 + component] = to;
                    dot += from * to;
                }
                if (dot < 0) {
                    dot = -dot;
                    for (int component = 0; component < 4; component++) {
                        windows[q1 + component] = -windows[q1 + component];
                    }
                }
                dot = Math.min(dot, 1);
                windows[s0 + 3] = dot;
                if (dot <= LINEAR_QUATERNION_THRESHOLD) {
                    float angle = (float) Math.acos(dot);
                    windows[s1 + 3] = angle;
                    windows[t1 + 3] = (float) (1 / Math.sin(angle));
                }
            }
        }
    }

    private static void putVector(long destination, float x, float y, float z, float w) {
        MemoryUtil.memPutFloat(destination, x);
        MemoryUtil.memPutFloat(destination + 4, y);
        MemoryUtil.memPutFloat(destination + 8, z);
        MemoryUtil.memPutFloat(destination + 12, w);
    }

    private static Material[] materials(int count, Material material) {
        var result = new Material[count];
        Arrays.fill(result, material);
        return result;
    }

    static final class GeometryBinding {
        final int animatedNode;
        final int visibilityTrack;
        final float[] before;
        final float[] after;
        final float[] endpointData;
        final Model model;
        final float radius;

        private GeometryBinding(
                int animatedNode,
                int visibilityTrack,
                float[] before,
                float[] after,
                Model model,
                float radius) {
            this.animatedNode = animatedNode;
            this.visibilityTrack = visibilityTrack;
            this.before = before;
            this.after = after;
            endpointData = animatedNode >= 0 ? new float[56] : null;
            this.model = model;
            this.radius = radius;
        }
    }

    private record SceneModel(List<Model.ConfiguredMesh> meshes, Vector4fc boundingSphere)
            implements Model {}

    private static final class SourceMesh implements Mesh {
        private final TriangleSource source;
        private final Vector4fc sphere;
        private final IndexSequence indices = this::writeIndices;

        private SourceMesh(TriangleSource source) {
            this.source = source;
            sphere = new Vector4f(0, 0, 0, source.getRadius());
        }

        @Override
        public int vertexCount() {
            return source.getCornerCount() / 3 * 4;
        }

        @Override
        public void write(MutableVertexList vertices) {
            int destination = 0;
            for (int triangle = 0; triangle < source.getCornerCount(); triangle += 3) {
                for (int corner = 0; corner < 4; corner++) {
                    int index = triangle + Math.min(corner, 2);
                    vertices.x(destination, source.getComponent(index, 0));
                    vertices.y(destination, source.getComponent(index, 1));
                    vertices.z(destination, source.getComponent(index, 2));
                    vertices.r(destination, 1);
                    vertices.g(destination, 1);
                    vertices.b(destination, 1);
                    vertices.a(destination, 1);
                    vertices.u(destination, source.getComponent(index, 3));
                    vertices.v(destination, source.getComponent(index, 4));
                    vertices.overlay(destination, 0);
                    vertices.light(destination, 0);
                    vertices.normalX(destination, source.getComponent(index, 5));
                    vertices.normalY(destination, source.getComponent(index, 6));
                    vertices.normalZ(destination, source.getComponent(index, 7));
                    destination++;
                }
            }
        }

        @Override
        public IndexSequence indexSequence() {
            return indices;
        }

        @Override
        public int indexCount() {
            return source.getCornerCount() / 3 * 6;
        }

        @Override
        public Vector4fc boundingSphere() {
            return sphere;
        }

        private void writeIndices(long address, int count) {
            for (int index = 0; index < count; index++) {
                int quad = index / 6 * 4;
                int within = index % 6;
                int corner =
                        switch (within) {
                            case 0, 5 -> 0;
                            case 1 -> 1;
                            case 2, 3 -> 2;
                            default -> 3;
                        };
                MemoryUtil.memPutInt(address + index * 4L, quad + corner);
            }
        }
    }
}
