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

package com.hbm.client.render;

import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.door.DoorType;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class DoorRenderDefinition {

    private static final String[] NONE = new String[0];

    private final DoorType kind;
    private final @Nullable DoorMotion motion;
    private final @Nullable DoorAnimations clips;
    private final @Nullable Supplier<HFRWavefrontObject> model;
    private final String[] staticParts, movingParts;
    private final int[] movingIds;
    private final DoorTextures skins;
    private final Identifier[] movingTextures;
    private final List<Identifier> sheets;
    private final int[] sheetBySkin;
    private final int yaw;
    private final Vector3fc offset;
    private final int hingeIndex;
    private volatile float @Nullable [] extent;

    private DoorRenderDefinition(Builder builder, DoorType kind) {
        this.kind = kind;
        motion = builder.motion;
        clips = builder.clips;
        model = builder.model;
        staticParts = builder.staticParts;
        movingParts = builder.movingParts;
        skins = builder.skins;
        yaw = builder.yaw;
        offset = builder.offset;
        if (yaw % 90 != 0)
            throw new IllegalArgumentException("static door yaw " + yaw + " is not a quarter turn");
        if (clips != null && motion == null)
            throw new IllegalArgumentException("door clips with no strategy");
        for (int length : skins.cycleLengths()) {
            if (length != kind.getSkinCount()) {
                throw new IllegalArgumentException(
                        "door skin list of " + length + " for " + kind.getSkinCount() + " skins");
            }
        }
        var hinge = kind.getHinge();
        hingeIndex = hinge == null ? -1 : List.of(movingParts).indexOf(hinge.part());
        if (hinge != null && (motion != null || hingeIndex < 0)) {
            throw new IllegalArgumentException(
                    "door hinge on '" + hinge.part() + "' needs a strategy-free moving part");
        }
        movingIds = resolve(builder.externalParts);
        int variants = Math.max(1, kind.getSkinCount());
        movingTextures = new Identifier[variants * movingParts.length];
        for (int skin = 0; skin < variants; skin++) {
            for (int i = 0; i < movingParts.length; i++) {
                movingTextures[skin * movingParts.length + i] = skins.texture(skin, movingParts[i]);
            }
        }
        sheetBySkin = new int[variants];
        var distinct = new ArrayList<Identifier>();
        if (staticParts.length > 0) {
            for (int skin = 0; skin < variants; skin++) {
                var texture = skins.texture(skin, staticParts[0]);
                if (!distinct.contains(texture)) distinct.add(texture);
                sheetBySkin[skin] = distinct.indexOf(texture);
            }
        }
        sheets = List.copyOf(distinct);
    }

    public static Builder builder() {
        return new Builder();
    }

    private int[] resolve(String[] external) {
        if (model == null) {
            if (staticParts.length > 0 || movingParts.length > 0) {
                throw new IllegalArgumentException("door parts with no model");
            }
            return new int[0];
        }
        var obj = model.get();
        int[] ids = obj.partIds(movingParts);
        var claimed = new boolean[obj.groups.length];
        int count = 0;
        for (var names : new String[][] {staticParts, movingParts, external}) {
            for (int id : obj.partIds(names)) {
                if (claimed[id])
                    throw new IllegalArgumentException(
                            obj.source + " group " + id + " claimed twice");
                claimed[id] = true;
                count++;
            }
        }
        if (count != obj.groups.length) {
            throw new IllegalArgumentException(
                    obj.source + " has groups no door part list claims: " + obj.getGroupNames());
        }
        return ids;
    }

    public DoorType getDoorType() {
        return kind;
    }

    public @Nullable DoorMotion getMotion() {
        return motion;
    }

    public @Nullable HFRWavefrontObject getModel() {
        return model == null ? null : model.get();
    }

    public String[] getStaticParts() {
        return staticParts;
    }

    public String[] getMovingParts() {
        return movingParts;
    }

    public int[] getMovingPartIds() {
        return movingIds;
    }

    public int getHingeIndex() {
        return hingeIndex;
    }

    public Identifier getTexture(int skin, String part) {
        return skins.texture(skin, part);
    }

    public Identifier getMovingTexture(int skin, int part) {
        return movingTextures[skin * movingParts.length + part];
    }

    public Identifier getSkin(int skin) {
        return skins.skin(skin);
    }

    public List<Identifier> getTextures() {
        return sheets;
    }

    public int getTextureIndex(int skin) {
        return sheetBySkin[skin];
    }

    public int getYaw() {
        return yaw;
    }

    public Vector3fc getOffset() {
        return offset;
    }

    public @Nullable BusAnimation getAnimation(byte state) {
        if (clips == null) return null;
        if (state == DoorBlockEntity.OPENING) return clips.clip(true);
        if (state == DoorBlockEntity.SHUTTING) return clips.clip(false);
        return null;
    }

    public @Nullable AnimationPlayback createPlayback(byte state, long start) {
        var clip = getAnimation(state);
        return clip == null ? null : new AnimationPlayback(null, start, clip);
    }

    public float[] getExtent() {
        var sampled = extent;
        if (sampled == null) extent = sampled = DoorState.extent(kind);
        return sampled;
    }

    public static final class Builder {
        private @Nullable DoorMotion motion;
        private @Nullable DoorAnimations clips;
        private @Nullable Supplier<HFRWavefrontObject> model;
        private String[] staticParts = NONE, movingParts = NONE, externalParts = NONE;
        private @Nullable DoorTextures skins;
        private int yaw;
        private Vector3fc offset = new Vector3f();

        private Builder() {}

        public Builder motion(DoorMotion motion) {
            this.motion = motion;
            return this;
        }

        public Builder clips(DoorAnimations clips) {
            this.clips = clips;
            return this;
        }

        public Builder model(Supplier<HFRWavefrontObject> model) {
            this.model = model;
            return this;
        }

        public Builder staticParts(String... parts) {
            staticParts = parts;
            return this;
        }

        public Builder movingParts(String... parts) {
            movingParts = parts;
            return this;
        }

        public Builder externalParts(String... parts) {
            externalParts = parts;
            return this;
        }

        public Builder skins(DoorTextures skins) {
            this.skins = skins;
            return this;
        }

        public Builder yaw(int yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder offset(float x, float y, float z) {
            offset = new Vector3f(x, y, z);
            return this;
        }

        public DoorRenderDefinition build(DoorType kind) {
            if (skins == null) throw new IllegalArgumentException("a door look needs its textures");
            return new DoorRenderDefinition(this, kind);
        }
    }
}
