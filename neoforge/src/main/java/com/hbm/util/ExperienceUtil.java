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

package com.hbm.util;

import net.minecraft.world.entity.player.Player;

public final class ExperienceUtil {
    private static final int SATURATING_LEVEL = 21_900;

    private ExperienceUtil() {}

    public static int total(Player player) {
        int level = player.experienceLevel;
        if (level >= SATURATING_LEVEL) return Integer.MAX_VALUE;
        long points = completedLevels(level) + (int) (capAt(level) * player.experienceProgress);
        return (int) Math.min(points, Integer.MAX_VALUE);
    }

    private static long completedLevels(int level) {
        if (level <= 0) return 0;
        if (level <= 15) return (long) level * level + 6L * level;
        if (level <= 30) {
            long past = level - 15;
            return 315 + 37 * past + 5 * past * (past - 1) / 2;
        }
        long past = level - 30;
        return 1395 + 112 * past + 9 * past * (past - 1) / 2;
    }

    public static void add(Player player, int xp) {
        int score = player.getScore();
        player.giveExperiencePoints(xp);
        player.setScore(score);
    }

    public static void set(Player player, int xp) {
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        add(player, xp);
    }

    private static int capAt(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }
}
