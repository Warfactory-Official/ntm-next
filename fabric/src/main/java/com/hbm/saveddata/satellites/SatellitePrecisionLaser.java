// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityOrbitalLaser;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public final class SatellitePrecisionLaser extends Satellite {

    public static final int MAX_TARGET_RANGE = 1_000;
    public static final int CHARGE_TICKS = 5 * 20;
    public static final MapCodec<SatellitePrecisionLaser> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastShot", 0L)
                                                    .forGetter(s -> s.lastShot),
                                            Codec.INT
                                                    .optionalFieldOf("targetedEntity", -1)
                                                    .forGetter(s -> s.targetedEntity))
                                    .apply(i, SatellitePrecisionLaser::new));

    public long lastShot;
    public int targetedEntity = -1;

    public SatellitePrecisionLaser() {}

    private SatellitePrecisionLaser(long lastShot, int targetedEntity) {
        this.lastShot = lastShot;
        this.targetedEntity = targetedEntity;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.PRECISION_LASER;
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
            case "fire" -> {
                if (targetedEntity != -1) {
                    Entity entity = level.getEntity(targetedEntity);
                    targetedEntity = -1;
                    if (entity == null || entity.isRemoved()) return;
                    int x = (int) Math.floor(entity.getX());
                    int z = (int) Math.floor(entity.getZ());
                    double dx = x - targetX;
                    double dz = z - targetZ;
                    if (dx * dx + dz * dz <= (double) MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
                        double offX = level.getRandom().nextDouble() * 0.05D - 0.025D;
                        double offY = level.getRandom().nextDouble() * 0.05D - 0.025D;
                        double offZ = level.getRandom().nextDouble() * 0.05D - 0.025D;
                        strike(
                                level,
                                entity.getX() + offX,
                                entity.getY() + offY,
                                entity.getZ() + offZ);
                        return;
                    }
                }
                strike(level, targetX, targetZ);
            }
            case "canfire" -> tx = Boolean.toString(canFire(level)).toUpperCase(Locale.US);
            case "setentitytarget" -> {
                if (command.length == 2)
                    targetedEntity =
                            IRORInteractive.parseInt(
                                    command[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            default -> {}
        }
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        setTarget(x, z);
        SatelliteSavedData.get(level).setDirty();
        strike(level, targetX, targetZ);
    }

    public void strike(ServerLevel level, int x, int z) {
        if (!canFire(level)) return;
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        ChunkUtil.loadTickingForEntity(level, chunk);
        int y = WorldgenHeight.lightBlocking(level.getChunk(chunk.x(), chunk.z()), x, z);
        strike(level, x + 0.5D, y, z + 0.5D);
    }

    public void strike(ServerLevel level, double x, double y, double z) {
        if (!canFire(level)) return;
        lastShot = level.getGameTime();
        SatelliteSavedData.get(level).setDirty();
        EntityOrbitalLaser blast = new EntityOrbitalLaser(ModEntities.ORBITAL_LASER.get(), level);
        blast.setPos(x, y, z);
        blast.explode();
        level.addFreshEntity(blast);
    }
}
