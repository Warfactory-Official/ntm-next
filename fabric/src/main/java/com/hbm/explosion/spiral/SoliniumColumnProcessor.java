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

import com.hbm.explosion.SoliniumBlockConversion;
import net.minecraft.server.level.ServerLevel;

public final class SoliniumColumnProcessor implements ExplosionColumnProcessor {

    public static final SoliniumColumnProcessor INSTANCE = new SoliniumColumnProcessor();

    private SoliniumColumnProcessor() {}

    @Override
    public void column(SpiralExplosion sweep, ServerLevel level, int x, int z) {
        int half = FleijaColumnProcessor.halfHeight(sweep.getRadius(), x, z);
        if (half < 0) return;
        int cx = sweep.getOriginX() + x;
        int cz = sweep.getOriginZ() + z;
        int bottom = Math.max(sweep.getOriginY() - half + 1, level.getMinY());
        int top = Math.min(sweep.getOriginY() + half, level.getMaxY());
        if (top < bottom) return;
        var chunk = level.getChunk(cx >> 4, cz >> 4);
        var cursor = sweep.getCursor();
        for (int y = top; y >= bottom; y--) {
            cursor.set(cx, y, cz);
            SoliniumBlockConversion.apply(level, cursor, chunk.getBlockState(cursor));
        }
    }
}
