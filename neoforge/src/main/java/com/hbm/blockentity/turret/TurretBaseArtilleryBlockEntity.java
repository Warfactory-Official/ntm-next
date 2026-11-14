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

package com.hbm.blockentity.turret;

import com.hbm.api.block.RadarCommandHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.PARAM_SEP;

public abstract class TurretBaseArtilleryBlockEntity extends TurretBaseNTBlockEntity
        implements RadarCommandHandler {

    protected final List<Vec3> targetQueue = new ArrayList<>();

    protected TurretBaseArtilleryBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    @Override
    public boolean strikeAt(int x, int y, int z) {
        enqueueTarget(x + 0.5, y, z + 0.5);
        return true;
    }

    @Override
    public boolean strike(Entity target) {
        enqueueTarget(target.getX(), target.getY(), target.getZ());
        return true;
    }

    public int queuedTargets() {
        return targetQueue.size();
    }

    public Vec3 queuedTarget(int index) {
        return targetQueue.get(index);
    }

    public void addQueuedTarget(Vec3 target) {
        targetQueue.add(target);
    }

    public void enqueueTarget(double x, double y, double z) {
        Vec3 pos = getTurretPos();
        Vec3 delta = new Vec3(x - pos.x, y - pos.y, z - pos.z);

        if (delta.length() <= getDetectorRange()) targetQueue.add(new Vec3(x, y, z));
    }

    @Override
    protected void beforeTracking() {
        if (!aimsManually() || !isOn()) targetQueue.clear();
        else if (!targetQueue.isEmpty())
            hbm$syncSet$TurretBaseNTBlockEntity$tPos(targetQueue.getFirst());
    }

    protected void finishFireWindow() {
        if (aimsManually() && !targetQueue.isEmpty()) {
            targetQueue.removeFirst();
            hbm$syncSet$TurretBaseNTBlockEntity$tPos(null);
        }
    }

    protected void abandonEngagement() {
        target = null;
        hbm$syncSet$TurretBaseNTBlockEntity$tPos(null);
        targetQueue.clear();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            FUN + "setactive" + NAME_SEP + "active (0 or 1)",
            FUN + "targetplayers" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetanimals" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetmobs" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetmachines" + NAME_SEP + "enabled (0 or 1)",
            FUN + "addwhitelist" + NAME_SEP + "name",
            FUN + "removewhitelist" + NAME_SEP + "name",
            FUN + "enqueue" + NAME_SEP + "x" + PARAM_SEP + "y" + PARAM_SEP + "z"
        };
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        super.runRadioFunction(name, params);
        if ((FUN + "enqueue").equals(name) && params.length > 2) {
            try {
                int x = Integer.parseInt(params[0]);
                int y = Integer.parseInt(params[1]);
                int z = Integer.parseInt(params[2]);
                strikeAt(x, y, z);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public abstract boolean doLOSCheck();

    @Override
    public boolean entityInLOS(Entity e) {
        if (doLOSCheck()) return super.entityInLOS(e);

        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        Vec3 delta = ent.subtract(pos);
        double length = delta.length();

        if (length < getDetectorGrace() || length > getDetectorRange() * 1.1) return false;

        int height =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING, Mth.floor(e.getX()), Mth.floor(e.getZ()));
        return height < (e.getY() + e.getBbHeight());
    }
}
