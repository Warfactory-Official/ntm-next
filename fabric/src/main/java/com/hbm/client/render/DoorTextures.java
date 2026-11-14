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

import java.util.List;
import net.minecraft.resources.Identifier;

public sealed interface DoorTextures {

    static DoorTextures fixed(Identifier texture) {
        return new Fixed(texture);
    }

    static DoorTextures cycle(Identifier... skins) {
        return new Cycle(List.of(skins));
    }

    static DoorTextures perPart(List<Identifier> body, String part, List<Identifier> own) {
        return new PerPart(List.copyOf(body), part, List.copyOf(own));
    }

    Identifier texture(int skin, String part);

    Identifier skin(int skin);

    List<Integer> cycleLengths();

    record Fixed(Identifier texture) implements DoorTextures {
        @Override
        public Identifier texture(int skin, String part) {
            return texture;
        }

        @Override
        public Identifier skin(int skin) {
            throw new IllegalStateException(
                    "a door with one fixed texture " + texture + " has no skin list");
        }

        @Override
        public List<Integer> cycleLengths() {
            return List.of();
        }
    }

    record Cycle(List<Identifier> skins) implements DoorTextures {
        @Override
        public Identifier texture(int skin, String part) {
            return skins.get(skin);
        }

        @Override
        public Identifier skin(int skin) {
            return skins.get(skin);
        }

        @Override
        public List<Integer> cycleLengths() {
            return List.of(skins.size());
        }
    }

    record PerPart(List<Identifier> body, String part, List<Identifier> own)
            implements DoorTextures {
        @Override
        public Identifier texture(int skin, String part) {
            return (this.part.equals(part) ? own : body).get(skin);
        }

        @Override
        public Identifier skin(int skin) {
            return body.get(skin);
        }

        @Override
        public List<Integer> cycleLengths() {
            return List.of(body.size(), own.size());
        }
    }
}
