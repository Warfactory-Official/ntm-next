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

public final class SampledClip {
    public static final int POSE_COMPONENTS = 10;

    private final int durationMillis;
    private final int frameCount;
    private final int trackCount;
    private final float[] poses;
    private final boolean[] hidden;

    public SampledClip(int durationMillis, int frameCount, LocalPose[][] tracks) {
        if (durationMillis <= 0 || frameCount <= 0) {
            throw new IllegalArgumentException("A clip requires positive duration and frame count");
        }
        this.durationMillis = durationMillis;
        this.frameCount = frameCount;
        trackCount = tracks.length;
        poses =
                new float
                        [Math.multiplyExact(
                                Math.multiplyExact(trackCount, frameCount), POSE_COMPONENTS)];
        hidden = new boolean[Math.multiplyExact(trackCount, frameCount)];
        for (int track = 0; track < trackCount; track++) {
            if (tracks[track].length != frameCount) {
                throw new IllegalArgumentException("Inconsistent clip frame count");
            }
            for (int frame = 0; frame < frameCount; frame++) {
                var pose = tracks[track][frame];
                int sample = track * frameCount + frame;
                int offset = sample * POSE_COMPONENTS;
                for (int component = 0; component < POSE_COMPONENTS; component++) {
                    poses[offset + component] = pose.getComponent(component);
                }
                hidden[sample] = pose.isHidden();
            }
        }
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public float getComponent(int track, int frame, int component) {
        return poses[(track * frameCount + frame) * POSE_COMPONENTS + component];
    }

    public boolean isHidden(int track, int frame) {
        return hidden[track * frameCount + frame];
    }

    public void sample(int track, int lower, int upper, float fraction, Matrix4f destination) {
        int first = (track * frameCount + lower) * POSE_COMPONENTS;
        int second = (track * frameCount + upper) * POSE_COMPONENTS;
        LocalPose.interpolate(poses, first, poses, second, fraction, destination);
    }

    public void selectFrames(
            long logicalMillis, long anchorMillis, boolean reverse, FrameWindow destination) {
        long elapsed = Math.clamp(logicalMillis - anchorMillis, 0, durationMillis);
        long directedMillis = reverse ? durationMillis - elapsed : elapsed;
        float normalized = (float) directedMillis / (float) durationMillis;
        float framePosition = normalized * (frameCount - 1);
        destination.lower = (int) Math.floor(framePosition);
        destination.upper = Math.min(destination.lower + 1, frameCount - 1);
        destination.fraction = framePosition - destination.lower;
    }

    public static final class FrameWindow {
        public int lower;
        public int upper;
        public float fraction;
    }
}
