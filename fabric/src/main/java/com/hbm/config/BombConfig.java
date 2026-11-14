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

package com.hbm.config;

public class BombConfig {

    public static long mk5 = 50;

    public static int fDelay = 4;

    public static int blastChunksInFlight = 0;

    public static int explosionAlgorithm = 2;

    public static int maxThreads = -1;

    public static int blastSpeed = 1024;

    public static int limitExplosionLifespan = 0;

    public static void loadFrom(ConfigStore c) {
        limitExplosionLifespan = c.get(ConfigSchema.LIMIT_EXPLOSION_LIFESPAN);
        blastSpeed = c.get(ConfigSchema.BLAST_SPEED);
        mk5 = c.get(ConfigSchema.MK5_BLAST_TIME);
        fDelay = c.get(ConfigSchema.FALLOUT_DELAY);
        blastChunksInFlight = c.get(ConfigSchema.BLAST_CHUNKS_IN_FLIGHT);
        explosionAlgorithm = Math.max(1, c.get(ConfigSchema.EXPLOSION_ALGORITHM));
        maxThreads = c.get(ConfigSchema.BOMB_MAX_THREADS);
    }

    public static int chunksInFlight() {
        int configured = blastChunksInFlight;
        return configured > 0
                ? configured
                : Math.max(64, Runtime.getRuntime().availableProcessors() * 8);
    }
}
