// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.world.entity.player.Player;

public final class ExperienceUtil {

    private ExperienceUtil() {}

    public static int total(Player player) {
        int xp = 0;
        for (int level = 0; level < player.experienceLevel; level++) xp += capAt(level);
        return xp + (int) (capAt(player.experienceLevel) * player.experienceProgress);
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
