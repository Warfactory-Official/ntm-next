/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FalloutRainColumns;
import com.hbm.client.render.FalloutRenderer;
import com.hbm.entity.effect.FalloutRainEntity;
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

    public static boolean ready(FalloutRainEntity entity) {
        for (FalloutRainVisual visual : active) {
            if (visual.entity() == entity && visual.hasFrame()) return true;
        }
        return false;
    }

    public static void capture(Camera camera, DeltaTracker deltaTracker, ClientLevel level) {
        Vec3 cameraPos = camera.position();
        for (FalloutRainVisual visual : active) {
            FalloutRainEntity entity = visual.entity();
            if (entity.isRemoved()
                    || entity.level() != level
                    || !FalloutRenderer.withinRenderDistance(
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
