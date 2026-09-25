// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.api.block.IRadarCommandReceiver;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityTurretBaseArtillery extends BlockEntityTurretBaseNT
        implements IRadarCommandReceiver {

    protected final List<Vec3> targetQueue = new ArrayList<>();

    protected BlockEntityTurretBaseArtillery(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        enqueueTarget(x + 0.5, y, z + 0.5);
        return true;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
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

    protected void abandonEngagement() {
        target = null;
        tPos = null;
        targetQueue.clear();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_FUNCTION + "setactive" + NAME_SEPARATOR + "active (0 or 1)",
            PREFIX_FUNCTION + "targetplayers" + NAME_SEPARATOR + "enabled (0 or 1)",
            PREFIX_FUNCTION + "targetanimals" + NAME_SEPARATOR + "enabled (0 or 1)",
            PREFIX_FUNCTION + "targetmobs" + NAME_SEPARATOR + "enabled (0 or 1)",
            PREFIX_FUNCTION + "targetmachines" + NAME_SEPARATOR + "enabled (0 or 1)",
            PREFIX_FUNCTION + "addwhitelist" + NAME_SEPARATOR + "name",
            PREFIX_FUNCTION + "removewhitelist" + NAME_SEPARATOR + "name",
            PREFIX_FUNCTION
                    + "enqueue"
                    + NAME_SEPARATOR
                    + "x"
                    + PARAM_SEPARATOR
                    + "y"
                    + PARAM_SEPARATOR
                    + "z"
        };
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        super.runRORFunction(name, params);
        if ((PREFIX_FUNCTION + "enqueue").equals(name) && params.length > 2) {
            try {
                int x = Integer.parseInt(params[0]);
                int y = Integer.parseInt(params[1]);
                int z = Integer.parseInt(params[2]);
                sendCommandPosition(x, y, z);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
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
