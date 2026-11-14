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

import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.layout.UnsignedIntegerRepr;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class ChainInstance extends TransformedInstance {
    public static final int CHANNELS = 6;
    public static final int STEPS = 7;
    public static final int CODE_WORDS = (STEPS + 3) / 4;
    public static final long REBASE_MILLIS = 1L << 13;
    public static final InstanceType<ChainInstance> TYPE =
            SimpleInstanceType.builder(ChainInstance::new)
                    .layout(
                            LayoutBuilder.create()
                                    .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                    .vector("overlay", IntegerRepr.SHORT, 2)
                                    .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                                    .matrix("pose", FloatRepr.FLOAT, 4)
                                    .scalarArray("previous", FloatRepr.FLOAT, CHANNELS)
                                    .scalarArray("current", FloatRepr.FLOAT, CHANNELS)
                                    .vectorArray("steps", FloatRepr.FLOAT, 4, STEPS)
                                    .vector("codes", UnsignedIntegerRepr.UNSIGNED_INT, CODE_WORDS)
                                    .vector("clock", UnsignedIntegerRepr.UNSIGNED_INT, 3)
                                    .build())
                    .seed(
                            pointer -> {
                                MemoryUtil.memPutInt(pointer, 0xFFFFFFFF);
                                ExtraMemoryOps.put2x16(pointer + 4, OverlayTexture.NO_OVERLAY);
                                ExtraMemoryOps.putMatrix4f(pointer + 12, new Matrix4f());
                            })
                    .vertexShader(RegistryUtil.id("instance/chain.vert"))
                    .cullShader(RegistryUtil.id("instance/cull/chain.glsl"))
                    .timeAnimated()
                    .build();
    private static final int OFF_PREVIOUS = offset("previous");
    private static final int OFF_CURRENT = offset("current");
    private static final int OFF_STEPS = offset("steps");
    private static final int OFF_CODES = offset("codes");
    private static final int OFF_CLOCK = offset("clock");

    private boolean rampedSlide;

    public ChainInstance(InstanceType<? extends ChainInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public static void initTypes() {}

    private static int offset(String element) {
        Layout.Element found = TYPE.layout().asMap().get(element);
        return found.byteOffset();
    }

    public ChainInstance chain(Chain chain) {
        long ptr = slabPtr();
        for (int i = 0; i < STEPS; i++) {
            ExtraMemoryOps.putVector4f(
                    ptr + OFF_STEPS + i * 16L, chain.x[i], chain.y[i], chain.z[i], chain.scale[i]);
        }
        MemoryUtil.memPutInt(ptr + OFF_CODES, chain.codes[0]);
        MemoryUtil.memPutInt(ptr + OFF_CODES + 4, chain.codes[1]);
        MemoryUtil.memPutInt(ptr + OFF_CLOCK + 8, chain.modes);
        rampedSlide = chain.rampedSlide();
        return this;
    }

    public ChainInstance channels(float[] previous, float[] current) {
        long ptr = slabPtr();
        for (int i = 0; i < CHANNELS; i++) {
            MemoryUtil.memPutFloat(
                    ptr + OFF_PREVIOUS + i * 4L, i < previous.length ? previous[i] : 0F);
            MemoryUtil.memPutFloat(
                    ptr + OFF_CURRENT + i * 4L, i < current.length ? current[i] : 0F);
        }
        return this;
    }

    public static long clockBase(long now, int period) {
        long base = now - 1000L;
        return period == 0 ? base : base - Math.floorMod(base, period);
    }

    public ChainInstance clock(long base, int period) {
        if (period < 0 || period == 0 && rampedSlide) {
            throw new IllegalArgumentException("clock period " + period);
        }
        long ptr = slabPtr();
        MemoryUtil.memPutInt(ptr + OFF_CLOCK, (int) base);
        MemoryUtil.memPutInt(ptr + OFF_CLOCK + 4, period);
        return this;
    }

    public static final class Chain {
        private static final int FIXED = 0, ROTATE = 1, SLIDE = 2, MULTIPLY_DIVIDE = 3;
        private final float[] x = new float[STEPS], y = new float[STEPS], z = new float[STEPS];
        private final float[] scale = new float[STEPS];
        private final int[] codes = new int[CODE_WORDS];
        private int modes;
        private int count;
        private int slideChannels;
        private int rotationOnlyChannels;
        private float tx, ty, tz;

        public Chain translate(float dx, float dy, float dz) {
            tx += dx;
            ty += dy;
            tz += dz;
            return this;
        }

        public Chain rotateX(int channel, float scale) {
            return step(ROTATE, 0, channel, scale);
        }

        public Chain rotateY(int channel, float scale) {
            return step(ROTATE, 1, channel, scale);
        }

        public Chain rotateZ(int channel, float scale) {
            return step(ROTATE, 2, channel, scale);
        }

        public Chain slideX(int channel, float scale) {
            return step(SLIDE, 0, channel, scale);
        }

        public Chain slideY(int channel, float scale) {
            return step(SLIDE, 1, channel, scale);
        }

        public Chain slideZ(int channel, float scale) {
            return step(SLIDE, 2, channel, scale);
        }

        public Chain rotationMultiplyDivide(int channel, float multiplier, float divisor) {
            int mask = 1 << checked(channel);
            if ((slideChannels & mask) != 0)
                throw new IllegalArgumentException("slide channel " + channel);
            end();
            rotationOnlyChannels |= mask;
            tx = multiplier;
            ty = divisor;
            return step(MULTIPLY_DIVIDE, 0, channel, 0F);
        }

        public Chain clock(int channel) {
            modes |= 1 << checked(channel);
            return this;
        }

        public Chain clamp(int channel) {
            modes |= 1 << (checked(channel) + 16);
            return this;
        }

        public Chain sine(int channel) {
            modes |= 1 << (checked(channel) + 8);
            return this;
        }

        public Chain end() {
            if (tx != 0F || ty != 0F || tz != 0F) step(FIXED, 0, 0, 0F);
            return this;
        }

        public Vector4f sweep(Vector4fc sphere, float @Nullable [] low, float @Nullable [] high) {
            float cx = sphere.x(), cy = sphere.y(), cz = sphere.z(), radius = sphere.w();
            for (int k = count - 1; k >= 0; k--) {
                int code = codes[k >> 2] >>> ((k & 3) << 3) & 0xFF;
                int kind = code >>> 5 & 3;
                int axis = code >>> 3 & 3;
                int channel = code & 7;
                if (kind == MULTIPLY_DIVIDE) continue;
                if (kind == ROTATE) {
                    float dx = axis == 0 ? 0F : cx,
                            dy = axis == 1 ? 0F : cy,
                            dz = axis == 2 ? 0F : cz;
                    radius += (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (axis != 0) cx = 0F;
                    if (axis != 1) cy = 0F;
                    if (axis != 2) cz = 0F;
                } else if (kind == SLIDE) {
                    float mid, half;
                    if ((modes >>> (channel + 8) & 1) != 0) {
                        mid = 0F;
                        half = Math.abs(scale[k]);
                    } else {
                        if (low == null || high == null)
                            throw new IllegalStateException("slide channel range");
                        float from = low[channel], to = high[channel];
                        if ((modes >>> (channel + 16) & 1) != 0) {
                            from = Math.clamp(from, 0F, 1F);
                            to = Math.clamp(to, 0F, 1F);
                        }
                        mid = (from + to) * .5F * scale[k];
                        half = Math.abs((to - from) * scale[k]) * .5F;
                    }
                    if (axis == 0) cx += mid;
                    else if (axis == 1) cy += mid;
                    else cz += mid;
                    radius += half;
                }
                cx += x[k];
                cy += y[k];
                cz += z[k];
            }
            return new Vector4f(cx, cy, cz, radius);
        }

        public Vector4f sweep(Vector4fc sphere) {
            return sweep(sphere, null, null);
        }

        private boolean rampedSlide() {
            for (int k = 0; k < count; k++) {
                int code = codes[k >> 2] >>> ((k & 3) << 3) & 0xFF;
                int channel = code & 7;
                if ((code >>> 5 & 3) == SLIDE
                        && (modes >>> channel & 1) != 0
                        && (modes >>> (channel + 8) & 1) == 0) {
                    return true;
                }
            }
            return false;
        }

        private static int checked(int channel) {
            if (channel < 0 || channel >= CHANNELS)
                throw new IllegalArgumentException("channel " + channel);
            return channel;
        }

        private Chain step(int kind, int axis, int channel, float scale) {
            if (count == STEPS)
                throw new IllegalStateException("chain longer than " + STEPS + " steps");
            checked(channel);
            if (kind == SLIDE) {
                int mask = 1 << channel;
                if ((rotationOnlyChannels & mask) != 0)
                    throw new IllegalArgumentException("slide channel " + channel);
                slideChannels |= mask;
            }
            x[count] = tx;
            y[count] = ty;
            z[count] = tz;
            this.scale[count] = scale;
            int code = 0x80 | kind << 5 | axis << 3 | channel;
            codes[count >> 2] |= code << ((count & 3) << 3);
            count++;
            tx = ty = tz = 0F;
            return this;
        }
    }
}
