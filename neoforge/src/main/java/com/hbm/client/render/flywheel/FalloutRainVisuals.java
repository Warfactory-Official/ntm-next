// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FalloutRainColumns;
import com.hbm.client.render.RenderFallout;
import com.hbm.entity.effect.EntityFalloutRain;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FalloutRainVisuals {
    private static volatile FalloutRainVisual[] active = new FalloutRainVisual[0];

    private FalloutRainVisuals() {}

    static synchronized void add(FalloutRainVisual visual) {
        FalloutRainVisual[] current = active;
        FalloutRainVisual[] next = Arrays.copyOf(current, current.length + 1);
        next[current.length] = visual;
        active = next;
    }

    static synchronized void remove(FalloutRainVisual visual) {
        FalloutRainVisual[] current = active;
        for (int i = 0; i < current.length; i++) {
            if (current[i] != visual) continue;
            FalloutRainVisual[] next = new FalloutRainVisual[current.length - 1];
            System.arraycopy(current, 0, next, 0, i);
            System.arraycopy(current, i + 1, next, i, next.length - i);
            active = next;
            return;
        }
    }

    public static boolean ready(EntityFalloutRain entity) {
        for (FalloutRainVisual visual : active) {
            if (visual.entity() == entity && visual.hasFrame()) return true;
        }
        return false;
    }

    public static void capture(Camera camera, DeltaTracker deltaTracker, ClientLevel level) {
        Vec3 cameraPos = camera.position();
        for (FalloutRainVisual visual : active) {
            EntityFalloutRain entity = visual.entity();
            if (entity.isRemoved()
                    || entity.level() != level
                    || !RenderFallout.withinRenderDistance(
                            entity, cameraPos.x, cameraPos.y, cameraPos.z)) {
                visual.publishFrame(null);
                continue;
            }
            float partialTick =
                    deltaTracker.getGameTimeDeltaPartialTick(
                            !level.tickRateManager().isEntityFrozen(entity));
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            double dx = cameraPos.x - x, dy = cameraPos.y - y, dz = cameraPos.z - z;
            if (dx * dx + dy * dy + dz * dz > (double) entity.getScale() * entity.getScale()) {
                visual.publishFrame(null);
                continue;
            }
            visual.publishFrame(
                    FalloutRainColumns.capture(
                            level, x, y, z, cameraPos, entity.tickCount + partialTick));
        }
    }
}
