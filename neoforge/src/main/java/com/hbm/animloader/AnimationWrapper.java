// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.animloader;

import org.jspecify.annotations.Nullable;

public class AnimationWrapper {

    public static final AnimationWrapper EMPTY =
            new AnimationWrapper(0L, Animation.EMPTY) {
                @Override
                public AnimationWrapper onEnd(EndResult result) {
                    return this;
                }
            };

    public Animation anim;

    public long startTime;
    public float speedScale = 1F;
    public boolean reverse;
    public EndResult endResult = EndResult.END;
    public int prevFrame;

    public AnimationWrapper(long startTime, Animation anim) {
        this.anim = anim;
        this.startTime = startTime;
    }

    public AnimationWrapper(long startTime, float scale, Animation anim) {
        this.anim = anim;
        this.speedScale = scale;
        this.startTime = startTime;
    }

    public AnimationWrapper onEnd(EndResult result) {
        this.endResult = result;
        return this;
    }

    public AnimationWrapper reverse() {
        reverse = !reverse;
        return this;
    }

    public AnimationWrapper cloneStats(AnimationWrapper other) {
        anim = other.anim;
        startTime = other.startTime;
        reverse = other.reverse;
        endResult = other.endResult;
        return this;
    }

    public AnimationWrapper cloneStatsWithoutTime(AnimationWrapper other) {
        anim = other.anim;
        reverse = other.reverse;
        endResult = other.endResult;
        return this;
    }

    public enum EndType {
        END,
        REPEAT,
        REPEAT_REVERSE,
        START_NEW,
        STAY
    }

    public record EndResult(EndType type, @Nullable AnimationWrapper next) {

        public static final EndResult END = new EndResult(EndType.END);
        public static final EndResult STAY = new EndResult(EndType.STAY);

        public EndResult(EndType type) {
            this(type, null);
        }
    }
}
