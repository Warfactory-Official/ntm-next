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

package com.hbm.handler;

import com.hbm.extprop.PlayerState;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerPropsLayout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerShield {

    private PlayerShield() {}

    public static void tick(ServerPlayer player) {
        PlayerState props = PlayerState.getOrCreate(player);
        float maximum = props.shieldCapacity(player);
        if (props.shield < maximum && player.tickCount > props.lastDamage + 60) {
            int sinceDelay = player.tickCount - (props.lastDamage + 60);
            props.shield += Math.min(maximum - props.shield, 0.005F * sinceDelay);
        }
        if (props.shield > maximum) props.shield = maximum;
        PacketWire.sendTo(
                PlayerPropsLayout.encode(
                        props.shield,
                        props.maxShield,
                        props.reputation,
                        props.enableBackpack,
                        props.enableHUD,
                        props.enableMagnet),
                player);
    }

    public static float absorb(Player player, float amount) {
        PlayerState props = PlayerState.getOrCreate(player);
        if (props.shield > 0) {
            float reduction = Math.min(props.shield, amount);
            props.shield -= reduction;
            amount -= reduction;
        }
        props.lastDamage = player.tickCount;
        return amount;
    }
}
