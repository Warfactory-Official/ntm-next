// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.ChunkUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public final class SatelliteDeathRay extends Satellite {

    public static final long CHARGE_TICKS = 5L * 60L * 20L;
    public static final MapCodec<SatelliteDeathRay> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastShot", 0L)
                                                    .forGetter(sat -> sat.lastShot))
                                    .apply(i, SatelliteDeathRay::new));

    public long lastShot;

    public SatelliteDeathRay() {
        this(0L);
    }

    private SatelliteDeathRay(long lastShot) {
        this.lastShot = lastShot;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.DEATH_RAY;
    }

    public boolean canFire(ServerLevel level) {
        return lastShot + CHARGE_TICKS < level.getGameTime();
    }

    @Override
    public List<Component> getInfo(ServerLevel level) {
        long cooldown = lastShot + CHARGE_TICKS - level.getGameTime();
        return List.of(
                Component.translatable(type().stationNameKey()),
                canFire(level)
                        ? Component.translatable("satellite.ready")
                        : Component.translatable(
                                "satellite.cooldown",
                                Component.translatable("unit.seconds", cooldown / 20)));
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "fire" -> strike(level, targetX, targetZ);
            case "canfire" -> tx = Boolean.toString(canFire(level)).toUpperCase(Locale.US);
            default -> {}
        }
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        setTarget(x, z);

        SatelliteSavedData.get(level).setDirty();
        strike(level, x, z);
    }

    public void strike(ServerLevel level, int x, int z) {
        if (!canFire(level)) return;
        lastShot = level.getGameTime();
        SatelliteSavedData.get(level).setDirty();

        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        ChunkUtil.loadTickingForEntity(level, chunk);
        int y = WorldgenHeight.lightBlocking(level.getChunk(chunk.x(), chunk.z()), x, z);
        EntityDeathBlast blast = new EntityDeathBlast(ModEntities.LASER_BLAST.get(), level);
        blast.setPos(x, y, z);
        level.addFreshEntity(blast);
    }
}
