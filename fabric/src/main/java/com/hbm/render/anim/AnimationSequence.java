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

package com.hbm.render.anim;

import com.hbm.render.anim.BusAnimationKeyframe.IType;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class AnimationSequence {

    public static final int SIZE = 15;
    private static final int CHANNELS = 9;

    @SuppressWarnings("unchecked")
    private final ArrayList<BusAnimationKeyframe>[] keys = new ArrayList[CHANNELS];

    private final int[] authored = new int[CHANNELS];
    private final int[] played = new int[CHANNELS];
    private double pivotX, pivotY, pivotZ;
    private volatile AnimationTrack @Nullable [] unscaled;
    private volatile @Nullable Scaled scaled;

    public static double rest(int channel) {
        return channel < 6 ? 0 : 1;
    }

    public static double[] identity(double[] out) {
        for (int channel = 0; channel < CHANNELS; channel++) out[channel] = rest(channel);
        out[9] = out[10] = out[11] = 0;
        out[12] = 0;
        out[13] = 1;
        out[14] = 2;
        return out;
    }

    public AnimationSequence key(int channel, BusAnimationKeyframe key) {
        var list = keys[channel];
        if (list == null) keys[channel] = list = new ArrayList<>();
        list.add(key);
        authored[channel] += key.authoredDuration;
        played[channel] += key.duration;
        return this;
    }

    public AnimationSequence jump(double x, double y, double z) {
        return to(x, y, z, 0);
    }

    public AnimationSequence to(double x, double y, double z, int millis) {
        return to(x, y, z, millis, IType.LINEAR);
    }

    public AnimationSequence to(double x, double y, double z, int millis, IType type) {
        return key(0, new BusAnimationKeyframe(x, millis, type))
                .key(1, new BusAnimationKeyframe(y, millis, type))
                .key(2, new BusAnimationKeyframe(z, millis, type));
    }

    public AnimationSequence stay(int millis) {
        for (int channel = 0; channel < 3; channel++) {
            var list = keys[channel];
            key(channel, new BusAnimationKeyframe(list == null ? 0 : list.getLast().value, millis));
        }
        return this;
    }

    public AnimationSequence stayUntil(int millis) {
        int end = Integer.MIN_VALUE;
        for (int sum : authored) end = Math.max(end, sum);
        return stay(millis - end);
    }

    public AnimationSequence pivot(double x, double y, double z) {
        pivotX = x;
        pivotY = y;
        pivotZ = z;
        return this;
    }

    public int getDuration() {
        int duration = Integer.MIN_VALUE;
        for (int sum : played) duration = Math.max(duration, sum);
        return duration;
    }

    public double[] sample(int millis, double timeScale, double[] out) {
        var tracks = tracks(timeScale);
        for (int channel = 0; channel < CHANNELS; channel++)
            out[channel] = tracks[channel].getValue(millis);
        out[9] = pivotX;
        out[10] = pivotY;
        out[11] = pivotZ;
        out[12] = 0;
        out[13] = 1;
        out[14] = 2;
        return out;
    }

    public double getValue(int channel, int millis, double timeScale) {
        return tracks(timeScale)[channel].getValue(millis);
    }

    public double getValue(int channel, int millis) {
        return getValue(channel, millis, 1);
    }

    public GpuAnimationTrack compileGpuTrack(int channel) {
        return tracks(1)[channel].getGpuTrack();
    }

    private AnimationTrack[] tracks(double timeScale) {
        if (timeScale == 1) {
            var tracks = unscaled;
            return tracks != null ? tracks : (unscaled = compile(1));
        }
        var last = scaled;
        if (last != null && last.timeScale == timeScale) return last.tracks;
        var tracks = compile(timeScale);
        scaled = new Scaled(timeScale, tracks);
        return tracks;
    }

    private AnimationTrack[] compile(double timeScale) {
        var tracks = new AnimationTrack[CHANNELS];
        for (int channel = 0; channel < CHANNELS; channel++) {
            var list = keys[channel];
            tracks[channel] =
                    AnimationTrack.compile(
                            list == null ? List.of() : list, timeScale, rest(channel));
        }
        return tracks;
    }

    private record Scaled(double timeScale, AnimationTrack[] tracks) {}
}
