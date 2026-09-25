// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.items.armor.ModArmorItem.Suit;
import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class ArmorPAWeapons {

    private static final Map<Suit, IPAMelee> MELEE = new EnumMap<>(Suit.class);
    private static final Map<Suit, IPARanged> RANGED = new EnumMap<>(Suit.class);

    private ArmorPAWeapons() {}

    public static void registerMelee(Suit suit, IPAMelee melee) {
        MELEE.put(suit, melee);
    }

    public static void registerRanged(Suit suit, IPARanged ranged) {
        RANGED.put(suit, ranged);
    }

    public static @Nullable IPAMelee melee(Suit suit) {
        return MELEE.get(suit);
    }

    public static @Nullable IPARanged ranged(Suit suit) {
        return RANGED.get(suit);
    }
}
