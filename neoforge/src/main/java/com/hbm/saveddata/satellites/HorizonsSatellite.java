/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.TomEntity;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public class HorizonsSatellite extends Satellite {

    public static final MapCodec<HorizonsSatellite> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.BOOL
                                                    .optionalFieldOf("used", false)
                                                    .forGetter(sat -> sat.spent))
                                    .apply(i, HorizonsSatellite::new));
    private static final Component SPENT = Component.translatable("satellite.spent");

    public boolean spent;

    public HorizonsSatellite() {
        this(false);
    }

    private HorizonsSatellite(boolean spent) {
        this.spent = spent;
    }

    @Override
    public SatelliteType kind() {
        return SatelliteType.HORIZONS;
    }

    @Override
    public void enterOrbit(ServerLevel level, double x, double y, double z) {
        super.enterOrbit(level, x, y, z);
        var payload = new ItemStack(kind().item());
        for (var player : level.getPlayers(EntitySelector.NO_SPECTATORS))
            HbmCriteria.orbit(player, payload);
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        switch (tokens[0]) {
            case "fire" -> fire(level, targetX(), targetZ());
            case "settarget" -> reply(spent ? "FALSE" : "TRUE");
            default -> {}
        }
    }

    @Override
    public void designate(ServerLevel level, Player player, int x, int y, int z) {
        aim(x, z);
        changed();
        fire(level, x, z);
    }

    @Override
    public List<Component> info(ServerLevel level) {
        return List.of(kind().stationName(), spent ? SPENT : READY);
    }

    protected void fire(ServerLevel level, int x, int z) {
        if (spent) return;
        spent = true;
        changed();
        ChunkUtil.loadTickingForEntity(level, new ChunkPos(x >> 4, z >> 4));
        var tom = new TomEntity(ModEntities.TOM_THE_MOONSTONE.get(), level);
        tom.setPos(x + 0.5D, 600D, z + 0.5D);
        level.addFreshEntity(tom);
        var payload = new ItemStack(kind().item());
        for (var player : level.getPlayers(EntitySelector.NO_SPECTATORS))
            HbmCriteria.satelliteAction(player, payload);
        level.getServer()
                .getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable("chat.satelliteHorizons.horizonsHasBeenActivated")
                                .withStyle(ChatFormatting.RED),
                        false);
    }
}
