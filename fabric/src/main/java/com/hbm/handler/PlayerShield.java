// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.packet.toclient.PlayerPropsPayload;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerShield {

    private PlayerShield() {}

    public static void tick(ServerPlayer player) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        float maximum = props.getEffectiveMaxShield(player);
        if (props.shield < maximum && player.tickCount > props.lastDamage + 60) {
            int sinceDelay = player.tickCount - (props.lastDamage + 60);
            props.shield += Math.min(maximum - props.shield, 0.005F * sinceDelay);
        }
        if (props.shield > maximum) props.shield = maximum;
        Services.NETWORK.sendTo(new PlayerPropsPayload(props), player);
    }

    public static float absorb(Player player, float amount) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        if (props.shield > 0) {
            float reduction = Math.min(props.shield, amount);
            props.shield -= reduction;
            amount -= reduction;
        }
        props.lastDamage = player.tickCount;
        return amount;
    }
}
