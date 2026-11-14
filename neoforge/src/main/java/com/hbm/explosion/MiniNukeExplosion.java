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

package com.hbm.explosion;

import com.hbm.api.explosion.NukeDetonations;
import com.hbm.data.ExplosionData;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record MiniNukeExplosion(
        boolean miniNuke,
        boolean safe,
        float blastRadius,
        float killRadius,
        float radiation,
        boolean cloud,
        int shrapnel,
        int resolution) {

    public static final MiniNukeExplosion SAFE =
            new MiniNukeExplosion(true, true, 0F, 45F, 2F, true, 25, 64);
    public static final MiniNukeExplosion MEDIUM =
            new MiniNukeExplosion(true, false, 20F, 55F, 3F, true, 25, 64);

    private static final float VOLUME = 15F;

    public MiniNukeExplosion {
        if (blastRadius < 0F || killRadius < 0F || shrapnel < 0 || resolution < 2) {
            throw new IllegalArgumentException(
                    "Malformed mini-nuke "
                            + blastRadius
                            + "/"
                            + killRadius
                            + ", "
                            + shrapnel
                            + " shrapnel, resolution "
                            + resolution);
        }
    }

    public static MiniNukeExplosion high() {
        return new MiniNukeExplosion(
                false, false, ExplosionData.FATMAN_RADIUS.get(), 0F, 1F, true, 0, 64);
    }

    public void detonate(Level level, double x, double y, double z) {
        var server = (ServerLevel) level;
        var at = new Vec3(x, y, z);
        if (cloud) MiniNukeAftermath.rollCloud(server, at, 0.5D);
        MiniNukeAftermath.soundWithoutVibration(server, at, VOLUME, 1F);
        boolean crater = miniNuke && !safe;
        if (!crater) server.gameEvent(null, GameEvent.EXPLODE, at);
        if (shrapnel > 0) BlastDebris.shrapnel(server, x, y, z, shrapnel);
        if (crater) {
            new ComposableExplosion(server, x, y, z, blastRadius)
                    .blockSelector(new AttenuatedRayBlockSelector(resolution))
                    .blockProcessor(CellDestruction.noDrops().transform(new BlockMutatorFire()))
                    .explode();
        }
        if (killRadius > 0F) NuclearShockwave.sweep(server, x, y, z, killRadius);
        if (!miniNuke) {
            server.addFreshEntity(
                    NukeExplosionMK5Entity.statFac(server, (int) blastRadius, x, y, z));
            return;
        }
        NukeDetonations.detonated(server, x, z, blastRadius);
        MiniNukeAftermath.forEachFalloutCell(
                at,
                (pos, ring) ->
                        RadiationSystemNT.incrementRad(
                                server, pos, 50 / (ring + 1) * (radiation / 3F)));
    }
}
