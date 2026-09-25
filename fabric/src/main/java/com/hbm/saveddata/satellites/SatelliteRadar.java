// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class SatelliteRadar extends Satellite {

    public static final MapCodec<SatelliteRadar> CODEC = MapCodec.unit(SatelliteRadar::new);
    public static final int MAX_SCAN_RANGE = 1_000;

    public List<Entity> cachedRadarResults = new ArrayList<>();
    public List<Entity> filteredRadarResults = new ArrayList<>();

    public SatelliteRadar() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.RADAR;
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "survey" -> {
                cachedRadarResults.clear();
                for (Entity entity : BlockEntityMachineRadar.MATCHING) {
                    if (entity.level() != level) continue;
                    int x = (int) Math.floor(entity.getX());
                    int z = (int) Math.floor(entity.getZ());
                    double dx = x - targetX;
                    double dz = z - targetZ;
                    if (dx * dx + dz * dz <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                        cachedRadarResults.add(entity);
                    }
                }
                filteredRadarResults = new ArrayList<>(cachedRadarResults);
            }
            case "filter" -> {
                if (command.length != 2) return;
                filteredRadarResults.clear();
                String filter = command[1].toLowerCase(Locale.US);
                for (Entity entity : cachedRadarResults) {
                    if (entity.isRemoved()) continue;
                    String name = entity.getClass().getSimpleName().toLowerCase(Locale.US);
                    if (name.contains(filter)) filteredRadarResults.add(entity);
                }
            }
            case "count" -> tx = Integer.toString(filteredRadarResults.size());
            case "gettargetid" -> {
                if (command.length != 2) return;
                Entity target = getTargetFromIndex(command[1]);
                tx = target == null ? "" : Integer.toString(target.getId());
            }
            case "getposition" -> {
                if (command.length != 2) return;
                Entity target = getTargetFromIndex(command[1]);
                tx =
                        target == null
                                ? ""
                                : (int) Math.floor(target.getX())
                                        + ";"
                                        + (int) Math.floor(target.getY())
                                        + ";"
                                        + (int) Math.floor(target.getZ());
            }
            case "getname" -> {
                if (command.length != 2) return;
                Entity target = getTargetFromIndex(command[1]);
                tx = target == null ? "" : target.getClass().getSimpleName().toLowerCase(Locale.US);
            }
            default -> {}
        }
    }

    public @Nullable Entity getTargetFromIndex(String indexText) {
        if (filteredRadarResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(indexText, 1, filteredRadarResults.size()) - 1;
        Entity target = filteredRadarResults.get(index);
        return target.isRemoved() ? null : target;
    }
}
