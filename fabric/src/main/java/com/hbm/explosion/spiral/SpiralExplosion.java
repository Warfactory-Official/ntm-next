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

package com.hbm.explosion.spiral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class SpiralExplosion {
    private static final int MAX_RADIUS = 23170;

    private static final String[] KEYS = {
        "posX", "posY", "posZ", "lastposX", "lastposZ", "radius", "radius2", "n", "nlimit"
    };

    private final Level level;
    private final @Nullable ServerLevel server;
    private final ExplosionColumnProcessor action;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int originX;
    private int originY;
    private int originZ;
    private int radius;
    private int step;
    private int nextX;
    private int nextZ;

    public SpiralExplosion(
            Level level, int x, int y, int z, int radius, ExplosionColumnProcessor action) {
        checkRadius(radius);
        this.level = level;
        this.server = level instanceof ServerLevel serverLevel ? serverLevel : null;
        this.action = action;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.radius = radius;
    }

    private static void checkRadius(int radius) {
        if (radius < 0 || radius > MAX_RADIUS) {
            throw new IllegalArgumentException(
                    "Sweep radius " + radius + " outside 0.." + MAX_RADIUS);
        }
    }

    public Level getLevel() {
        return level;
    }

    public ExplosionColumnProcessor getProcessor() {
        return action;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public int getRadius() {
        return radius;
    }

    public int getProcessedColumns() {
        return step;
    }

    public int getNextX() {
        return nextX;
    }

    public int getNextZ() {
        return nextZ;
    }

    public BlockPos.MutableBlockPos getCursor() {
        return cursor;
    }

    public boolean advanceColumn() {
        if (server != null) {
            switch (action.admit(this, server, nextX, nextZ)) {
                case HOLD -> {
                    return false;
                }
                case END -> {
                    return true;
                }
                case PROCEED -> action.column(this, server, nextX, nextZ);
            }
        }
        place(++step);
        boolean complete = step + 1L > 4L * radius * radius;
        if (complete && server != null) action.completed(this);
        return complete;
    }

    private void place(int k) {
        int ring = (int) ((Math.sqrt(k) + 1D) / 2D);
        int inner = 2 * ring - 1;
        int along = k - inner * inner;
        int leg = along / (2 * ring);
        int offset = along - 2 * ring * leg - ring + 1;
        switch (leg) {
            case 0 -> {
                nextX = ring;
                nextZ = offset;
            }
            case 1 -> {
                nextX = -offset;
                nextZ = ring;
            }
            case 2 -> {
                nextX = -ring;
                nextZ = -offset;
            }
            default -> {
                nextX = offset;
                nextZ = -ring;
            }
        }
    }

    public void write(ValueOutput output, String prefix) {
        output.putInt(prefix + "posX", originX);
        output.putInt(prefix + "posY", originY);
        output.putInt(prefix + "posZ", originZ);
        output.putInt(prefix + "lastposX", nextX);
        output.putInt(prefix + "lastposZ", nextZ);
        output.putInt(prefix + "radius", radius);
        output.putInt(prefix + "radius2", radius * radius);
        output.putInt(prefix + "n", step + 1);
        output.putInt(prefix + "nlimit", 4 * radius * radius);
    }

    public void read(ValueInput input, String prefix) {
        int present = 0;
        for (var key : KEYS) {
            if (input.getInt(prefix + key).isPresent()) present++;
        }
        if (present == 0) return;
        if (present != KEYS.length) throw malformed(prefix, "missing keys");
        int savedRadius = input.getIntOr(prefix + "radius", 0);
        int saved = input.getIntOr(prefix + "n", 0);
        if (savedRadius < 0 || savedRadius > MAX_RADIUS)
            throw malformed(prefix, "radius " + savedRadius);
        if (saved < 1) throw malformed(prefix, "n " + saved);
        if (input.getIntOr(prefix + "radius2", 0) != savedRadius * savedRadius
                || input.getIntOr(prefix + "nlimit", 0) != 4 * savedRadius * savedRadius) {
            throw malformed(prefix, "limits disagree with radius " + savedRadius);
        }
        originX = input.getIntOr(prefix + "posX", 0);
        originY = input.getIntOr(prefix + "posY", 0);
        originZ = input.getIntOr(prefix + "posZ", 0);
        nextX = input.getIntOr(prefix + "lastposX", 0);
        nextZ = input.getIntOr(prefix + "lastposZ", 0);
        radius = savedRadius;
        step = saved - 1;
    }

    private static IllegalStateException malformed(String prefix, String detail) {
        return new IllegalStateException(
                "Malformed saved column sweep '" + prefix + "': " + detail);
    }
}
