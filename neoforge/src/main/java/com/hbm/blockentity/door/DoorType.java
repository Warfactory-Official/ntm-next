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

package com.hbm.blockentity.door;

import com.hbm.client.render.DoorRenderDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public final class DoorType {

    private final int ticks;
    private final float volume;
    private final int skins;
    private final boolean remote, larpExempt;
    private final DoorFootprint footprint;
    private final DoorRow[] rows;
    private final float rowsFrom, rowsTo;
    private final DoorCollisionShapes bounds;
    private final @Nullable DoorSoundTimeline cues;
    private final @Nullable DoorHinge hinge;
    private final Supplier<DoorSounds> soundSource;
    private final Function<DoorType, DoorRenderDefinition> lookSource;
    private @Nullable DoorSounds sounds;
    private volatile @Nullable DoorRenderDefinition look;

    private DoorType(Builder builder) {
        ticks = builder.ticks;
        volume = builder.volume;
        skins = builder.skins;
        remote = builder.remote;
        larpExempt = builder.larpExempt;
        footprint =
                new DoorFootprint(
                        builder.main, builder.extra.toArray(int[][]::new), builder.offset);
        rows = builder.rows.toArray(DoorRow[]::new);
        rowsFrom = builder.rowsFrom;
        rowsTo = Float.isNaN(builder.rowsTo) ? ticks : builder.rowsTo;
        bounds = builder.bounds;
        cues = builder.cues;
        hinge = builder.hinge;
        soundSource = builder.sounds;
        if (builder.look == null)
            throw new IllegalArgumentException("a door kind needs a presentation");
        lookSource = builder.look;
        if (ticks <= 0)
            throw new IllegalArgumentException("a door transition of " + ticks + " ticks");
        if (!Float.isFinite(volume)) throw new IllegalArgumentException("door volume " + volume);
        if (skins < 0) throw new IllegalArgumentException("door skin count " + skins);
        if (cues != null) cues.checkWithin(ticks);
    }

    public static Builder builder(int ticks) {
        return new Builder(ticks);
    }

    public int getOpeningTicks() {
        return ticks;
    }

    public float getVolume() {
        return volume;
    }

    public int getSkinCount() {
        return skins;
    }

    public boolean hasSkins() {
        return skins > 0;
    }

    public boolean isRemote() {
        return remote;
    }

    public boolean isLarpExempt() {
        return larpExempt;
    }

    public DoorFootprint getFootprint() {
        return footprint;
    }

    public DoorRow[] getRows() {
        return rows;
    }

    public float getRowFraction(int ticks) {
        return DoorEasing.window(ticks, rowsFrom, rowsTo);
    }

    public DoorCollisionShapes getCollisionShapes() {
        return bounds;
    }

    public @Nullable DoorSoundTimeline getSoundTimeline() {
        return cues;
    }

    public @Nullable DoorHinge getHinge() {
        return hinge;
    }

    public DoorSounds getSounds() {
        var resolved = sounds;
        if (resolved == null) sounds = resolved = soundSource.get();
        return resolved;
    }

    public DoorRenderDefinition getRenderDefinition() {
        var resolved = look;
        if (resolved != null) return resolved;
        synchronized (this) {
            resolved = look;
            if (resolved == null) look = resolved = lookSource.apply(this);
            return resolved;
        }
    }

    public static final class Builder {
        private final int ticks;
        private final List<int[]> extra = new ArrayList<>();
        private final List<DoorRow> rows = new ArrayList<>();
        private float volume = 1F;
        private int skins;
        private boolean remote, larpExempt;
        private int[] main = new int[6];
        private int offset;
        private float rowsFrom, rowsTo = Float.NaN;
        private DoorCollisionShapes bounds = DoorCollisionShapes.DEFAULT;
        private @Nullable DoorSoundTimeline cues;
        private @Nullable DoorHinge hinge;
        private Supplier<DoorSounds> sounds = () -> DoorSounds.NONE;
        private @Nullable Function<DoorType, DoorRenderDefinition> look;

        private Builder(int ticks) {
            this.ticks = ticks;
        }

        public Builder footprint(int up, int down, int north, int south, int west, int east) {
            main = new int[] {up, down, north, south, west, east};
            return this;
        }

        public Builder extra(int up, int down, int north, int south, int west, int east) {
            extra.add(new int[] {up, down, north, south, west, east});
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder row(int x, int y, int z, int length, int width, int axis) {
            rows.add(new DoorRow(x, y, z, length, width, axis));
            return this;
        }

        public Builder rowsOver(float from, float to) {
            rowsFrom = from;
            rowsTo = to;
            return this;
        }

        public Builder bounds(DoorCollisionShapes bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder sounds(Supplier<DoorSounds> sounds) {
            this.sounds = sounds;
            return this;
        }

        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder skins(int skins) {
            this.skins = skins;
            return this;
        }

        public Builder remote() {
            remote = true;
            return this;
        }

        public Builder larpExempt() {
            larpExempt = true;
            return this;
        }

        public Builder cues(DoorSoundTimeline cues) {
            this.cues = cues;
            return this;
        }

        public Builder hinge(DoorHinge hinge) {
            this.hinge = hinge;
            return this;
        }

        public Builder look(Function<DoorType, DoorRenderDefinition> look) {
            this.look = look;
            return this;
        }

        public DoorType build() {
            return new DoorType(this);
        }
    }
}
