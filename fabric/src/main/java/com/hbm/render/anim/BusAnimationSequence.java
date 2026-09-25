// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.anim;

import com.hbm.render.anim.BusAnimationKeyframe.IType;
import java.util.ArrayList;
import java.util.List;

public class BusAnimationSequence {
    public double[] offset = new double[3];
    public double[] rotMode = new double[] {0, 1, 2};

    private final List<List<BusAnimationKeyframe>> transformKeyframes =
            new ArrayList<List<BusAnimationKeyframe>>(9);

    public BusAnimationSequence() {
        for (int i = 0; i < 9; i++) {
            transformKeyframes.add(new ArrayList<BusAnimationKeyframe>());
        }
    }

    public BusAnimationSequence addKeyframe(Dimension dimension, BusAnimationKeyframe keyframe) {
        transformKeyframes.get(dimension.ordinal()).add(keyframe);
        return this;
    }

    public BusAnimationSequence addKeyframe(Dimension dimension, double value, int duration) {
        return addKeyframe(dimension, new BusAnimationKeyframe(value, duration));
    }

    public BusAnimationSequence setPos(double x, double y, double z) {
        return addPos(x, y, z, 0, IType.LINEAR);
    }

    public BusAnimationSequence addPos(double x, double y, double z, int duration) {
        return addPos(x, y, z, duration, IType.LINEAR);
    }

    public BusAnimationSequence addPos(double x, double y, double z, int duration, IType type) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(x, duration, type));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(y, duration, type));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(z, duration, type));
        return this;
    }

    public BusAnimationSequence addRot(double x, double y, double z, int duration) {
        addKeyframe(Dimension.RX, new BusAnimationKeyframe(x, duration));
        addKeyframe(Dimension.RY, new BusAnimationKeyframe(y, duration));
        addKeyframe(Dimension.RZ, new BusAnimationKeyframe(z, duration));
        return this;
    }

    public BusAnimationSequence hold(int duration) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(getLast(Dimension.TX), duration));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(getLast(Dimension.TY), duration));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(getLast(Dimension.TZ), duration));
        return this;
    }

    public BusAnimationSequence holdUntil(int end) {
        int duration = end - getTotalTime();

        return hold(duration);
    }

    public BusAnimationSequence multiplyTime(double mult) {
        for (Dimension dim : Dimension.values()) {
            List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dim.ordinal());
            for (BusAnimationKeyframe keyframe : keyframes) {
                keyframe.duration = (int) (keyframe.originalDuration * mult);
            }
        }
        return this;
    }

    private double getLast(Dimension dim) {
        BusAnimationKeyframe frame = getLastFrame(dim);
        return frame != null ? frame.value : 0D;
    }

    private BusAnimationKeyframe getLastFrame(Dimension dim) {
        List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dim.ordinal());
        if (keyframes.isEmpty()) return null;
        return keyframes.get(keyframes.size() - 1);
    }

    public double[] getTransformation(int millis) {
        double[] transform = new double[15];

        for (int i = 0; i < 9; i++) transform[i] = value(i, millis);

        transform[9] = offset[0];
        transform[10] = offset[1];
        transform[11] = offset[2];

        transform[12] = rotMode[0];
        transform[13] = rotMode[1];
        transform[14] = rotMode[2];

        return transform;
    }

    public double value(int dimension, int millis) {
        List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dimension);
        BusAnimationKeyframe current = null, previous = null;
        int start = 0, end = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            start = end;
            var keyframe = keyframes.get(i);
            end += keyframe.duration;
            previous = current;
            current = keyframe;
            if (millis < end) break;
        }
        if (current == null) return dimension >= 6 ? 1 : 0;
        if (millis >= end || current.duration == 0) return current.value;
        if (previous != null && previous.interpolationType == IType.CONSTANT) return previous.value;
        return current.interpolate(start, millis, previous);
    }

    public int getTotalTime() {
        int highestTime = 0;

        for (List<BusAnimationKeyframe> keyframes : transformKeyframes) {
            int time = 0;
            for (BusAnimationKeyframe frame : keyframes) {
                time += frame.duration;
            }

            highestTime = Math.max(time, highestTime);
        }

        return highestTime;
    }

    public enum Dimension {
        TX,
        TY,
        TZ,
        RX,
        RY,
        RZ,
        SX,
        SY,
        SZ
    }
}
