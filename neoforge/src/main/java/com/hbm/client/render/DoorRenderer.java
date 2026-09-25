// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import org.jspecify.annotations.Nullable;

public interface DoorRenderer {

    double[] IDENTITY = {0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2};

    static double[] getRelevantTransformation(
            String bus, HbmAnimations.@Nullable Animation anim, long nowMillis) {
        if (anim != null) {
            BusAnimationSequence seq = anim.animation.getBus(bus);
            if (seq != null) {
                double[] trans = seq.getTransformation((int) (nowMillis - anim.startMillis));
                if (trans != null) return trans;
            }
        }
        return IDENTITY;
    }

    static float rad(double degrees) {
        return (float) Math.toRadians(degrees);
    }

    void emit(DoorState state, DoorFrame frame);

    default boolean culls() {
        return true;
    }

    default boolean blends() {
        return false;
    }
}
