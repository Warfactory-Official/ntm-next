// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        explosionAlgorithm = c.get(ConfigSchema.EXPLOSION_ALGORITHM);
        maxThreads = c.get(ConfigSchema.BOMB_MAX_THREADS);
    }

    public static int chunksInFlight() {
        int configured = blastChunksInFlight;
        return configured > 0
                ? configured
                : Math.max(64, Runtime.getRuntime().availableProcessors() * 8);
    }
}
