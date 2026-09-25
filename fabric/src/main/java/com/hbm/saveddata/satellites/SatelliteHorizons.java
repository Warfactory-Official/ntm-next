// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public class SatelliteHorizons extends Satellite {

    public static final MapCodec<SatelliteHorizons> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.BOOL
                                                    .optionalFieldOf("used", false)
                                                    .forGetter(sat -> sat.used))
                                    .apply(i, SatelliteHorizons::new));

    public boolean used;

    public SatelliteHorizons() {
        this(false);
    }

    private SatelliteHorizons(boolean used) {
        this.used = used;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.HORIZONS;
    }

    @Override
    public List<Component> getInfo(ServerLevel level) {
        return List.of(
                Component.translatable(type().stationNameKey()),
                Component.translatable(used ? "satellite.spent" : "satellite.ready"));
    }

    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        super.onOrbit(level, x, y, z);
        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, chip);
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        setTarget(x, z);
        SatelliteSavedData.get(level).setDirty();
        theHorizons(level, x, z);
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        if (command[0].equals("fire")) {
            theHorizons(level, targetX, targetZ);
        } else if (command[0].equals("settarget")) {
            tx = Boolean.toString(!used).toUpperCase(Locale.US);
        }
    }

    public void theHorizons(ServerLevel level, int x, int z) {
        if (used) return;

        used = true;
        SatelliteSavedData.get(level).setDirty();

        EntityTom tom = new EntityTom(ModEntities.TOM_THE_MOONSTONE.get(), level);
        tom.setPos(x + 0.5D, 600, z + 0.5D);
        ChunkUtil.loadTickingForEntity(level, new ChunkPos(x >> 4, z >> 4));
        level.addFreshEntity(tom);

        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer p : level.players()) HbmCriteria.satelliteAction(p, chip);

        level.getServer()
                .getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable("chat.satelliteHorizons.horizonsHasBeenActivated")
                                .withStyle(ChatFormatting.RED),
                        false);
    }
}
