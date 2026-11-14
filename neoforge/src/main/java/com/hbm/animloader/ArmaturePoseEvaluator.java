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

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ArmaturePoseEvaluator {
    private Matrix4f[] poses = new Matrix4f[0];
    private final Matrix4f local = new Matrix4f();
    private final SampledClip.FrameWindow window = new SampledClip.FrameWindow();

    public void visit(
            ArmatureScene scene,
            SampledClip clip,
            long logicalMillis,
            long anchorMillis,
            boolean reverse,
            Matrix4fc root,
            GeometryVisitor visitor) {
        if (poses.length < scene.getNodeCount()) {
            var expanded = new Matrix4f[scene.getNodeCount()];
            System.arraycopy(poses, 0, expanded, 0, poses.length);
            for (int index = poses.length; index < expanded.length; index++)
                expanded[index] = new Matrix4f();
            poses = expanded;
        }
        clip.selectFrames(logicalMillis, anchorMillis, reverse, window);
        for (int node = 0; node < scene.getNodeCount(); node++) {
            int track = scene.getTrack(node);
            boolean animated = scene.hasLocal(node) && track >= 0;
            if (animated) clip.sample(track, window.lower, window.upper, window.fraction, local);
            else scene.getLocalPose(node, local);
            int parent = scene.getParent(node);
            Matrix4fc parentPose = parent < 0 ? root : poses[parent];
            parentPose.mul(local, poses[node]);
            var geometry = scene.getGeometry(node);
            if (geometry != null) {
                boolean hidden = track >= 0 && clip.isHidden(track, window.lower);
                visitor.geometry(geometry, poses[node], hidden);
            }
        }
    }

    @FunctionalInterface
    public interface GeometryVisitor {
        void geometry(TriangleSource geometry, Matrix4f borrowedCompletePose, boolean hidden);
    }
}
