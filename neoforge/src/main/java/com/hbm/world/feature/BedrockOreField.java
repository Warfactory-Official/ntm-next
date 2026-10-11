// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.lib.Library;
import com.hbm.world.WorldgenHash;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;

public final class BedrockOreField {

    private static final double INPUT_SCALE = 0.01D;
    private static final double OUTPUT_SCALE = 0.05D;
    private static final long RICHNESS =
            WorldgenHash.identifier(Library.id("bedrock_ore/richness"));

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
