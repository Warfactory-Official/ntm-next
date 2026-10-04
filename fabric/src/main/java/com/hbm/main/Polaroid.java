// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.main;

import java.util.Random;
import net.minecraft.network.chat.Component;

public final class Polaroid {
    public static final int BALEFIRE_ID = 11;

    private static int polaroidID = 1;

    private Polaroid() {}

    public static void roll() {
        Random rand = new Random();
        int id = rand.nextInt(18) + 1;
        while (id == 4 || id == 9) id = rand.nextInt(18) + 1;
        polaroidID = id;
    }

    public static int id() {
        return polaroidID;
    }

    public static Component lore(String prefix) {
        return Component.translatableWithFallback(prefix + polaroidID, "");
    }

    public static boolean isBalefireDay() {
        return polaroidID == BALEFIRE_ID;
    }
}
