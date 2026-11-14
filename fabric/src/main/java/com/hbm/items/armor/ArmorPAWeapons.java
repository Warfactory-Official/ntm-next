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

package com.hbm.items.armor;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.items.armor.ModArmorItem.Suit;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ArmorPAWeapons {

    private static final Map<Suit, PowerArmorMeleeWeapon> MELEE = new EnumMap<>(Suit.class);
    private static final Map<Suit, PowerArmorWeapon> RANGED = new EnumMap<>(Suit.class);

    private ArmorPAWeapons() {}

    public static void registerMelee(Suit suit, PowerArmorMeleeWeapon melee) {
        MELEE.put(suit, melee);
    }

    public static void registerRanged(Suit suit, PowerArmorWeapon ranged) {
        RANGED.put(suit, ranged);
    }

    public static @Nullable PowerArmorMeleeWeapon melee(@Nullable Player player) {
        return lent(player, MELEE);
    }

    public static @Nullable PowerArmorWeapon ranged(@Nullable Player player) {
        return lent(player, RANGED);
    }

    public static @Nullable PowerArmorMeleeWeapon localMelee() {
        return melee(ClientPlayerAccess.player());
    }

    private static <W> @Nullable W lent(@Nullable Player player, Map<Suit, W> weapons) {
        if (player == null
                || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem()
                        instanceof ModArmorItem plate)) return null;
        W weapon = weapons.get(plate.suit());
        return weapon != null && ArmorSuitEffects.hasFullSet(player, plate.suit(), false)
                ? weapon
                : null;
    }
}
