// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemPollutionDetector extends Item {

    private static final int SOOT_ID = 100, POISON_ID = 101, METAL_ID = 102;
    private static final int LIFETIME = 4000;
    private static final int CADENCE = 10;

    public ItemPollutionDetector(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player) || level.getGameTime() % CADENCE != 0) return;

        PollutionData data = PollutionHandler.getPollutionData(level, player.blockPosition());
        if (data == null) data = new PollutionData();

        line(player, SOOT_ID, PollutionType.SOOT, data.pollution[PollutionType.SOOT.ordinal()]);
        line(
                player,
                POISON_ID,
                PollutionType.POISON,
                data.pollution[PollutionType.POISON.ordinal()]);
        line(
                player,
                METAL_ID,
                PollutionType.HEAVYMETAL,
                data.pollution[PollutionType.HEAVYMETAL.ordinal()]);
    }

    private static void line(ServerPlayer player, int id, PollutionType type, float value) {
        Services.NETWORK.sendTo(
                new PlayerInformPayload(readout(type, value), id, LIFETIME), player);
    }

    public static Component readout(PollutionType type, float value) {
        float shown = ((int) (value * 100F)) / 100F;
        String key =
                switch (type) {
                    case SOOT -> "pollution.soot";
                    case POISON -> "pollution.poison";
                    case HEAVYMETAL -> "pollution.heavymetal";
                    default -> throw new IllegalArgumentException("No detector line for " + type);
                };
        return Component.translatable(key)
                .append(Component.translatable("desc.item.pollutionDetector.value", shown))
                .withStyle(ChatFormatting.YELLOW);
    }
}
