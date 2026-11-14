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

package com.hbm.render.item.weapon.sedna;

import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GunAnimationChannels {

    private final Object2IntOpenHashMap<String> slots = new Object2IntOpenHashMap<>();
    private final List<Channel> channels = new ArrayList<>();
    private final Set<String> carried = new HashSet<>();
    private boolean partsRequired;

    public GunAnimationChannels() {
        slots.defaultReturnValue(-1);
    }

    public Scalar x(String bus) {
        return add(new Scalar(bus, channels.size(), 0));
    }

    public Scalar y(String bus) {
        return add(new Scalar(bus, channels.size(), 1));
    }

    public Scalar z(String bus) {
        return add(new Scalar(bus, channels.size(), 2));
    }

    public Vec3 vec3(String bus) {
        return add(new Vec3(bus, channels.size()));
    }

    public Part part(String bus) {
        return add(new Part(bus, channels.size()));
    }

    public void carries(String... buses) {
        for (var bus : buses) {
            if (slots.containsKey(bus) || !carried.add(bus))
                throw new IllegalArgumentException("bus " + bus + " declared twice");
        }
    }

    public AnimationSequence[] bind(BusAnimation clip) {
        var bound = new AnimationSequence[channels.size()];
        clip.forEachBus(
                (bus, sequence) -> {
                    int slot = slots.getInt(bus);
                    if (slot >= 0) bound[slot] = sequence;
                    else if (!carried.contains(bus))
                        throw new IllegalArgumentException("clip bus " + bus + " is no channel");
                });
        return bound;
    }

    public void requireParts(String file, Map<String, BusAnimation> clips) {
        if (partsRequired) return;
        partsRequired = true;
        for (var clip : clips.entrySet()) {
            clip.getValue()
                    .forEachBus(
                            (bus, _) -> {
                                int slot = slots.getInt(bus);
                                if (slot < 0 || !(channels.get(slot) instanceof Part))
                                    throw new IllegalStateException(
                                            file
                                                    + " clip "
                                                    + clip.getKey()
                                                    + " part "
                                                    + bus
                                                    + " is no part channel");
                            });
        }
    }

    private <C extends Channel> C add(C channel) {
        if (carried.contains(channel.bus) || slots.putIfAbsent(channel.bus, channel.slot) != -1)
            throw new IllegalArgumentException("bus " + channel.bus + " declared twice");
        channels.add(channel);
        return channel;
    }

    public abstract static sealed class Channel permits Scalar, Vec3 {
        final String bus;
        final int slot;

        Channel(String bus, int slot) {
            this.bus = bus;
            this.slot = slot;
        }
    }

    public static final class Scalar extends Channel implements GunRenderDefinition.Drive {
        final int axis;

        Scalar(String bus, int slot, int axis) {
            super(bus, slot);
            this.axis = axis;
        }

        @Override
        public double at(GunRenderDefinition.Reading r) {
            return r.get(this);
        }
    }

    public static sealed class Vec3 extends Channel permits Part {
        Vec3(String bus, int slot) {
            super(bus, slot);
        }
    }

    public static final class Part extends Vec3 {
        Part(String bus, int slot) {
            super(bus, slot);
        }
    }
}
