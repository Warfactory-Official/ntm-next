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

package com.hbm.items.tool;

import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.util.TooltipStyle;
import com.hbm.world.NtmWorldgenFields;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ColtanCompassItem extends Item {
    private static final int ID_COMPASS = 2;
    private static final double SPREAD = 1500;

    public ColtanCompassItem(Properties props) {
        super(props);
    }

    public static BlockPos deposit(ServerLevel level) {
        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        return new BlockPos(fields.coltanX(SPREAD), level.getSeaLevel(), fields.coltanZ(SPREAD));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) {
            tracker =
                    new LodestoneTracker(
                            Optional.of(new GlobalPos(level.dimension(), deposit(level))), false);
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
        if (!(owner instanceof ServerPlayer player)) return;

        BlockPos target = tracker.target().orElseThrow().pos();
        double dx = owner.getX() - target.getX();
        double dz = owner.getZ() - target.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        PacketWire.sendTo(
                PlayerInformLayout.encode(
                        Component.translatable("desc.item.coltanTool.distance", distance),
                        ID_COMPASS,
                        ClientEventConstants.PLAYER_INFORM_DEFAULT_MILLIS),
                player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (Component line :
                TooltipStyle.lines(Component.translatableWithFallback("desc.item.coltanTool", "")))
            adder.accept(line);
    }
}
