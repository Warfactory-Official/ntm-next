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

package com.hbm.items;

import com.hbm.util.RegistryUtil;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;

public final class ModJukeboxSongs {

    private ModJukeboxSongs() {}

    public enum Song {
        LC("music.record_lambda_core", 104.542F),
        SS("music.record_sector_sweep", 166.504F),
        VC("music.record_vortal_combat", 194.717F),
        GLASS("music.transmission", 62.229F);

        public static final Song[] VALUES = values();

        public static final int COMPARATOR_OUTPUT = 15;

        private final String soundName;
        private final float lengthSeconds;

        Song(String soundName, float lengthSeconds) {
            this.soundName = soundName;
            this.lengthSeconds = lengthSeconds;
        }

        public String path() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String soundName() {
            return soundName;
        }

        public float lengthSeconds() {
            return lengthSeconds;
        }

        public String itemName() {
            return "record_" + path();
        }

        public ResourceKey<JukeboxSong> key() {
            return ResourceKey.create(Registries.JUKEBOX_SONG, RegistryUtil.id(path()));
        }
    }
}
