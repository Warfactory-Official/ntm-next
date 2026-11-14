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

package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.redstoneoverradio.RadioCommandException;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.handler.radio.RadioSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SatelliteRelay extends Satellite {

    public static final MapCodec<SatelliteRelay> CODEC = MapCodec.unit(SatelliteRelay::new);

    public SatelliteRelay() {}

    @Override
    public SatelliteType kind() {
        return SatelliteType.RELAY;
    }

    @Override
    public void enterOrbit(ServerLevel level, double x, double y, double z) {
        super.enterOrbit(level, x, y, z);
        ItemStack chip = new ItemStack(kind().item());
        for (ServerPlayer player : level.getPlayers(EntitySelector.NO_SPECTATORS))
            HbmCriteria.orbit(player, chip);
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] command) {
        if (command.length <= 3 || !command[0].equals("relay")) return;
        Identifier id = command[1].contains(":") ? Identifier.tryParse(command[1]) : null;
        if (id == null) throw new RadioCommandException(RadioCommandParser.BAD_PARAMETER);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel target = level.getServer().getLevel(key);
        if (target == null) return;

        StringBuilder message = new StringBuilder();
        for (int i = 3; i < command.length; i++) {
            if (i > 3) message.append(' ');
            message.append(command[i]);
        }
        RadioSystem.send(target, command[2], message.toString());
    }
}
