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

package com.hbm.world.feature;

import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.util.RegistryUtil;
import com.hbm.world.WorldgenHash;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;

public final class BedrockOreField {

    private static final double INPUT_SCALE = 0.01D;
    private static final double OUTPUT_SCALE = 0.05D;
    private static final long RICHNESS =
            WorldgenHash.identifier(RegistryUtil.id("bedrock_ore/richness"));

    private final long bedrockSeed;
    private final FractalSimplexNoise richness;
    private final ConcurrentHashMap<BedrockOreKind, FractalSimplexNoise> channels =
            new ConcurrentHashMap<>();

    public BedrockOreField(long bedrockSeed) {
        this.bedrockSeed = bedrockSeed;
        richness = noise(bedrockSeed, RICHNESS);
    }

    public double richness(int x, int z) {
        return richness.get(x * INPUT_SCALE, z * INPUT_SCALE);
    }

    public double oreLevel(double sharedRichness, int x, int z, BedrockOreKind kind) {
        return density(sharedRichness, channel(kind).get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double averageOreLevel(BedrockOreSet set, int x, int z) {
        double scaledX = x * INPUT_SCALE;
        double scaledZ = z * INPUT_SCALE;
        double sharedRichness = richness.get(scaledX, scaledZ);
        double total = 0D;
        for (BedrockOreKind kind : set.kinds()) {
            total += density(sharedRichness, channel(kind).get(scaledX, scaledZ));
        }
        return total / set.kinds().size();
    }

    private FractalSimplexNoise channel(BedrockOreKind kind) {
        return channels.computeIfAbsent(
                kind, k -> noise(bedrockSeed, WorldgenHash.identifier(k.channel())));
    }

    private static double density(double sharedRichness, double material) {
        return Mth.clamp(Math.abs(sharedRichness * material) * OUTPUT_SCALE, 0D, 2D);
    }

    private static FractalSimplexNoise noise(long seed, long domain) {
        return new FractalSimplexNoise(WorldgenHash.domainSeed(seed, domain), 4);
    }
}
