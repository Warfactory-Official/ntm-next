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

package com.hbm.animloader;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class ArmatureScene {
    private final int[] parents;
    private final int[] tracks;
    private final boolean[] hasLocal;
    private final float[] localMatrices;
    private final TriangleSource[] geometry;
    private final int[] geometryNodes;
    private final float[] envelopes;
    private final float radius;

    private ArmatureScene(List<Node> nodes, int[] parents, SampledClip clip) {
        this.parents = parents;
        int count = nodes.size();
        tracks = new int[count];
        hasLocal = new boolean[count];
        localMatrices = new float[count * 16];
        geometry = new TriangleSource[count];
        envelopes = new float[count];
        var geometryIndices = new ArrayList<Integer>();
        var translations = new double[count];
        var linearNorms = new double[count];
        var values = new float[16];
        for (int node = 0; node < count; node++) {
            var definition = nodes.get(node);
            tracks[node] = definition.track;
            if (definition.track < -1 || definition.track >= clip.getTrackCount()) {
                throw new IllegalArgumentException("Armature track outside clip");
            }
            hasLocal[node] = definition.local != null;
            geometry[node] = definition.geometry;
            if (definition.geometry != null) geometryIndices.add(node);
            if (definition.local == null) {
                values[0] = values[5] = values[10] = values[15] = 1;
                values[1] = values[2] = values[3] = values[4] = values[6] = values[7] = 0;
                values[8] = values[9] = values[11] = values[12] = values[13] = values[14] = 0;
            } else {
                definition.local.get(values);
            }
            System.arraycopy(values, 0, localMatrices, node * 16, 16);
            if (hasLocal[node] && tracks[node] >= 0) {
                int track = tracks[node];
                double translation = 0;
                double scale = 0;
                for (int frame = 0; frame < clip.getFrameCount(); frame++) {
                    double x = clip.getComponent(track, frame, 0);
                    double y = clip.getComponent(track, frame, 1);
                    double z = clip.getComponent(track, frame, 2);
                    translation = Math.max(translation, Math.sqrt(x * x + y * y + z * z));
                    scale =
                            Math.max(
                                    scale,
                                    Math.max(
                                            clip.getComponent(track, frame, 7),
                                            Math.max(
                                                    clip.getComponent(track, frame, 8),
                                                    clip.getComponent(track, frame, 9))));
                }
                translations[node] = translation;
                linearNorms[node] = scale;
            } else {
                translations[node] =
                        Math.sqrt(
                                (double) values[12] * values[12]
                                        + (double) values[13] * values[13]
                                        + (double) values[14] * values[14]);
                double columns = 0;
                double rows = 0;
                for (int axis = 0; axis < 3; axis++) {
                    columns =
                            Math.max(
                                    columns,
                                    Math.abs((double) values[axis * 4])
                                            + Math.abs((double) values[axis * 4 + 1])
                                            + Math.abs((double) values[axis * 4 + 2]));
                    rows =
                            Math.max(
                                    rows,
                                    Math.abs((double) values[axis])
                                            + Math.abs((double) values[axis + 4])
                                            + Math.abs((double) values[axis + 8]));
                }
                linearNorms[node] = Math.sqrt(columns * rows);
            }
        }
        geometryNodes = new int[geometryIndices.size()];
        float sceneRadius = 0;
        for (int ordinal = 0; ordinal < geometryNodes.length; ordinal++) {
            int node = geometryIndices.get(ordinal);
            geometryNodes[ordinal] = node;
            double envelope = geometry[node].getRadius();
            for (int ancestor = node; ancestor >= 0; ancestor = parents[ancestor]) {
                envelope = translations[ancestor] + linearNorms[ancestor] * envelope;
            }
            envelopes[node] = Math.nextUp((float) envelope);
            sceneRadius = Math.max(sceneRadius, envelopes[node]);
        }
        radius = sceneRadius;
    }

    public static ArmatureScene freeze(Node root, SampledClip clip) {
        var nodes = new ArrayList<Node>();
        var parentIndices = new ArrayList<Integer>();
        flatten(root, -1, nodes, parentIndices);
        var parents = new int[parentIndices.size()];
        for (int index = 0; index < parents.length; index++)
            parents[index] = parentIndices.get(index);
        return new ArmatureScene(nodes, parents, clip);
    }

    private static void flatten(Node node, int parent, List<Node> nodes, List<Integer> parents) {
        int index = nodes.size();
        nodes.add(node);
        parents.add(parent);
        for (var child : node.children) flatten(child, index, nodes, parents);
    }

    public int getNodeCount() {
        return parents.length;
    }

    public int getParent(int node) {
        return parents[node];
    }

    public int getTrack(int node) {
        return tracks[node];
    }

    public boolean hasLocal(int node) {
        return hasLocal[node];
    }

    public void getLocalPose(int node, Matrix4f destination) {
        int offset = node * 16;
        destination.set(
                localMatrices[offset],
                localMatrices[offset + 1],
                localMatrices[offset + 2],
                localMatrices[offset + 3],
                localMatrices[offset + 4],
                localMatrices[offset + 5],
                localMatrices[offset + 6],
                localMatrices[offset + 7],
                localMatrices[offset + 8],
                localMatrices[offset + 9],
                localMatrices[offset + 10],
                localMatrices[offset + 11],
                localMatrices[offset + 12],
                localMatrices[offset + 13],
                localMatrices[offset + 14],
                localMatrices[offset + 15]);
    }

    public float getLocalComponent(int node, int component) {
        return localMatrices[node * 16 + component];
    }

    public @Nullable TriangleSource getGeometry(int node) {
        return geometry[node];
    }

    public int getGeometryCount() {
        return geometryNodes.length;
    }

    public int getGeometryNode(int ordinal) {
        return geometryNodes[ordinal];
    }

    public TriangleSource getGeometryAt(int ordinal) {
        return geometry[geometryNodes[ordinal]];
    }

    public float getNodeEnvelope(int node) {
        return envelopes[node];
    }

    public float getRadius() {
        return radius;
    }

    public static final class Node {
        private final @Nullable Matrix4f local;
        private final int track;
        private final @Nullable TriangleSource geometry;
        private final List<Node> children;

        public Node(
                @Nullable Matrix4fc local,
                int track,
                @Nullable TriangleSource geometry,
                List<Node> children) {
            this.local = local == null ? null : new Matrix4f(local);
            if (local != null) {
                var values = new float[16];
                local.get(values);
                for (float value : values) {
                    if (!Float.isFinite(value))
                        throw new IllegalArgumentException("Nonfinite static local matrix");
                }
                if (values[3] != 0 || values[7] != 0 || values[11] != 0 || values[15] != 1) {
                    throw new IllegalArgumentException("A static local matrix must be affine");
                }
            }
            this.track = track;
            this.geometry = geometry;
            this.children = List.copyOf(children);
        }
    }
}
