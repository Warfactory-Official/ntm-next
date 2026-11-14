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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.hbm.render.anim.BusAnimationKeyframe.EType;
import com.hbm.render.anim.BusAnimationKeyframe.HType;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class AnimationLoader {

    private static final String[] GROUPS = {"location", "rotation_euler", "scale"};
    private static final String[] AXES = {"x", "y", "z"};

    private AnimationLoader() {}

    public static Map<String, BusAnimation> load(Identifier id) {
        try (var reader =
                Minecraft.getInstance()
                        .getResourceManager()
                        .getResourceOrThrow(id)
                        .openAsReader()) {
            return read(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("animation clips " + id, e);
        }
    }

    public static Map<String, BusAnimation> read(Reader reader) {
        var file = JsonParser.parseReader(reader).getAsJsonObject();
        var pivots = member(file, "offset").getAsJsonObject();
        var clips = new HashMap<String, BusAnimation>();
        for (var clip : member(file, "anim").getAsJsonObject().entrySet()) {
            var animation = new BusAnimation();
            for (var part : clip.getValue().getAsJsonObject().entrySet()) {
                var motion = new AnimationSequence();
                var pivot = pivots.get(part.getKey());
                if (pivot != null) {
                    var xyz = pivot.getAsJsonArray();
                    motion.pivot(
                            xyz.get(0).getAsDouble(),
                            xyz.get(1).getAsDouble(),
                            xyz.get(2).getAsDouble());
                }
                var groups = part.getValue().getAsJsonObject();
                for (int group = 0; group < GROUPS.length; group++) {
                    var axes = groups.get(GROUPS[group]);
                    if (axes == null) continue;
                    for (int axis = 0; axis < AXES.length; axis++) {
                        var keys = axes.getAsJsonObject().get(AXES[axis]);
                        if (keys != null) keys(motion, group * 3 + axis, keys.getAsJsonArray());
                    }
                }
                animation.addBus(part.getKey(), motion);
            }
            clips.put(clip.getKey(), animation);
        }
        return Map.copyOf(clips);
    }

    private static void keys(AnimationSequence motion, int channel, JsonArray keys) {
        BusAnimationKeyframe previous = null;
        for (int i = 0; i < keys.size(); i++) {
            var fields = keys.get(i).getAsJsonArray();
            var type =
                    fields.size() > 2 ? IType.valueOf(fields.get(2).getAsString()) : IType.LINEAR;
            var easing =
                    fields.size() > 3 ? EType.valueOf(fields.get(3).getAsString()) : EType.AUTO;
            if (i + 1 < keys.size()) AnimationTrack.requireCurve(type, easing);
            var key =
                    new BusAnimationKeyframe(
                            fields.get(0).getAsDouble(), fields.get(1).getAsInt(), type, easing);
            int at = 4;
            if (previous != null && previous.interpolationType == IType.BEZIER) {
                key.leftX = fields.get(at++).getAsDouble();
                key.leftY = fields.get(at++).getAsDouble();
                key.leftType = HType.valueOf(fields.get(at++).getAsString());
            }
            if (type == IType.BEZIER) {
                key.rightX = fields.get(at++).getAsDouble();
                key.rightY = fields.get(at++).getAsDouble();
                key.rightType = HType.valueOf(fields.get(at++).getAsString());
            } else if (type == IType.ELASTIC) {
                key.amplitude = fields.get(at++).getAsDouble();
                key.period = fields.get(at).getAsDouble();
            } else if (type == IType.BACK) {
                key.back = fields.get(at).getAsDouble();
            }
            motion.key(channel, key);
            previous = key;
        }
    }

    private static JsonElement member(JsonObject object, String name) {
        var member = object.get(name);
        if (member == null) throw new JsonParseException("missing member " + name);
        return member;
    }
}
