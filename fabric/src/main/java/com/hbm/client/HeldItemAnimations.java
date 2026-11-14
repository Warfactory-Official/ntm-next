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

package com.hbm.client;

import com.hbm.render.anim.AnimationPlayback;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.util.GameTime;
import org.jspecify.annotations.Nullable;

public final class HeldItemAnimations {

    private static final int RAILS = 8;
    private static final AnimationPlayback[] GRID = new AnimationPlayback[9 * RAILS];

    private HeldItemAnimations() {}

    public static @Nullable AnimationPlayback get(int slot, int rail) {
        return GRID[slot * RAILS + rail];
    }

    public static void put(int slot, int rail, @Nullable AnimationPlayback playback) {
        GRID[slot * RAILS + rail] = playback;
    }

    public static @Nullable AnimationPlayback getActiveAnimation() {
        return getActiveAnimation(GameTime.now());
    }

    public static double getChannelValue(String bus, int channel) {
        long now = GameTime.now();
        var playback = getActiveAnimation(now);
        var motion = playback == null ? null : playback.clip().getBus(bus);
        return motion == null
                ? AnimationSequence.rest(channel)
                : motion.getValue(channel, playback.millis(now), playback.timeScale());
    }

    public static double[] sampleChannels(String bus) {
        long now = GameTime.now();
        var playback = getActiveAnimation(now);
        var motion = playback == null ? null : playback.clip().getBus(bus);
        var out = new double[AnimationSequence.SIZE];
        return motion == null
                ? AnimationSequence.identity(out)
                : motion.sample(playback.millis(now), playback.timeScale(), out);
    }

    public static void removeExpired() {
        long millis = GameTime.millis();
        for (int i = 0; i < GRID.length; i++) {
            var playback = GRID[i];
            if (playback == null || playback.hold()) continue;
            long elapsed = millis - playback.start();
            if (elapsed > playback.clip().getDuration() || elapsed < 0) GRID[i] = null;
        }
    }

    private static @Nullable AnimationPlayback getActiveAnimation(long now) {
        var player = ClientPlayerAccess.player();
        if (player == null) return null;
        var held = player.getMainHandItem();
        if (held.isEmpty()) return null;
        var playback = get(player.getInventory().getSelectedSlot(), 0);
        return playback != null && playback.item() == held.getItem() && playback.live(now)
                ? playback
                : null;
    }
}
