// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.tileentity.network.RTTYSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SatelliteRelay extends Satellite {

    public static final MapCodec<SatelliteRelay> CODEC = MapCodec.unit(SatelliteRelay::new);

    public SatelliteRelay() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.RELAY;
    }

    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        super.onOrbit(level, x, y, z);
        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, chip);
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length <= 3 || !command[0].equals("relay")) return;

        Identifier id = command[1].contains(":") ? Identifier.tryParse(command[1]) : null;
        if (id == null) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel target = level.getServer().getLevel(key);
        if (target == null) return;

        StringBuilder message = new StringBuilder();
        for (int i = 3; i < command.length; i++) {
            if (i > 3) message.append(' ');
            message.append(command[i]);
        }
        RTTYSystem.broadcast(target, command[2], message.toString());
    }
}
