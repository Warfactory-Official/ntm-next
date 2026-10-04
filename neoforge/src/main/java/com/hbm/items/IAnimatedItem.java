// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.HbmAnimationPayload;
import com.hbm.render.anim.AnimationEnums.ToolAnimation;
import com.hbm.render.anim.BusAnimation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface IAnimatedItem extends IEquipReceiver {

    @Nullable BusAnimation getAnimation(ToolAnimation type, ItemStack stack);

    default void playAnimation(ServerPlayer player, ToolAnimation type) {
        PacketWire.sendTo(
                new HbmAnimationPayload(player, player.getMainHandItem(), type.ordinal(), 0, 0),
                player);
    }

    @Override
    default void onEquip(Player player, ItemStack stack) {}
}
