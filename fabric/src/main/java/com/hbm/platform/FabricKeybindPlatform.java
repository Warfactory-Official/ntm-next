// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.client.KeyMapping;

public final class FabricKeybindPlatform {
    public static boolean sameChord(KeyMapping first, KeyMapping second) {
        return !first.isUnbound() && first.same(second);
    }

    public static boolean modifierMatches(KeyMapping mapping, int modifiers) {
        return true;
    }

    public static void inGame(KeyMapping mapping) {}
}
