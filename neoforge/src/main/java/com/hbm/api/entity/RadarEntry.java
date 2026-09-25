// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class RadarEntry {

    public final String radarName;
    public final int blipLevel;
    public final int posX;
    public final int posY;
    public final int posZ;
    public final int entityID;

    public boolean redstone;

    public RadarEntry() {
        this("", 0, 0, 0, 0, 0, false);
    }

    public RadarEntry(String name, int level, int x, int y, int z, int entityID, boolean redstone) {
        this.radarName = name;
        this.blipLevel = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.entityID = entityID;
        this.redstone = redstone;
    }

    public RadarEntry(IRadarDetectableNT detectable, Entity entity, boolean redstone) {
        this(
                detectable.getRadarName(),
                detectable.getBlipLevel(),
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ()),
                entity.getId(),
                redstone);
    }

    public RadarEntry(IRadarDetectable detectable, Entity entity) {
        this(
                detectable.getTargetType().name,
                detectable.getTargetType().ordinal(),
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ()),
                entity.getId(),
                entity.getDeltaMovement().y < 0);
    }

    public RadarEntry(Player player) {
        this(
                player.getName().getString(),
                IRadarDetectableNT.PLAYER,
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ()),
                player.getId(),
                true);
    }

    public RadarEntry(ByteBuf buf) {
        this.radarName = ByteBufCodecs.STRING_UTF8.decode(buf);
        this.blipLevel = buf.readShort();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.entityID = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.STRING_UTF8.encode(buf, this.radarName);
        buf.writeShort(this.blipLevel);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeInt(this.entityID);
    }

    public final boolean sameWire(RadarEntry other) {
        return getClass() == RadarEntry.class
                && other.getClass() == RadarEntry.class
                && blipLevel == other.blipLevel
                && posX == other.posX
                && posY == other.posY
                && posZ == other.posZ
                && entityID == other.entityID
                && radarName.equals(other.radarName);
    }
}
