// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.lib.Library;
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
            return ResourceKey.create(Registries.JUKEBOX_SONG, Library.id(path()));
        }
    }
}
