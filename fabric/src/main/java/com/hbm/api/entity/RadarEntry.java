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

package com.hbm.api.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class RadarEntry {

    public final String getRadarName;
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
        this.getRadarName = name;
        this.blipLevel = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.entityID = entityID;
        this.redstone = redstone;
    }

    public RadarEntry(RadarProvider detectable, Entity entity, boolean redstone) {
        this(
                detectable.getRadarName(),
                detectable.getTargetType().id(),
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ()),
                entity.getId(),
                redstone);
    }

    public RadarEntry(Player player) {
        this(
                player.getName().getString(),
                RadarTargetType.PLAYER.id(),
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ()),
                player.getId(),
                true);
    }

    public RadarEntry(ByteBuf buf) {
        this.getRadarName = ByteBufCodecs.STRING_UTF8.decode(buf);
        this.blipLevel = buf.readShort();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.entityID = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.STRING_UTF8.encode(buf, this.getRadarName);
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
                && getRadarName.equals(other.getRadarName);
    }
}
