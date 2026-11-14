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

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public record ActionUpdateActor(int id, String key, Tag value) implements SceneAction {
    public ActionUpdateActor(int id, String key, Object value) {
        this(
                id,
                key,
                switch (value) {
                    case String v -> StringTag.valueOf(v);
                    case Integer v -> IntTag.valueOf(v);
                    case Float v -> FloatTag.valueOf(v);
                    case Double v -> DoubleTag.valueOf(v);
                    case Boolean v -> ByteTag.valueOf(v);
                    case Byte v -> ByteTag.valueOf(v);
                    case Short v -> ShortTag.valueOf(v);
                    default ->
                            throw new IllegalArgumentException(
                                    "Unsupported actor data point " + key + ": " + value);
                });
    }

    @Override
    public int duration() {
        return 0;
    }

    @Override
    public void perform(WorldInAJar world, JarScene scene) {
        scene.script.actors.get(id).putData(key, value);
    }
}
