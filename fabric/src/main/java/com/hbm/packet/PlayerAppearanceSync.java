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

package com.hbm.packet;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.items.armor.ArmorFlight;
import com.hbm.packet.layout.PlayerAppearanceLayout;
import com.hbm.potion.HbmPotion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerAppearanceSync {

    private PlayerAppearanceSync() {}

    public static byte fromEffects(Player player) {
        byte flags = player.hasEffect(HbmPotion.death()) ? PlayerAppearance.MANLY : 0;
        MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
        if (invisibility != null && invisibility.getAmplifier() > 0)
            flags |= PlayerAppearance.STEALTH;
        return flags;
    }

    public static byte fromPlayer(Player player) {
        byte flags = fromEffects(player);
        if (ArmorFlight.prone(player)) flags |= PlayerAppearance.ARMOR_FLIGHT;
        return flags;
    }

    public static void update(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        byte flags = fromPlayer(player);
        if (((com.hbm.interfaces.injected.PlayerAppearance) (player)).hbm$appearance() == flags)
            return;
        ((com.hbm.interfaces.injected.PlayerAppearance) (player)).hbm$setAppearance(flags);
        PacketWire.sendToAllTracking(PlayerAppearanceLayout.encode(player.getId(), flags), player);
    }

    public static void sendInitial(Entity tracked, ServerPlayer observer) {
        if (tracked instanceof Player player) {
            byte flags = fromPlayer(player);
            if (flags != 0)
                PacketWire.sendTo(PlayerAppearanceLayout.encode(player.getId(), flags), observer);
        }
    }
}
