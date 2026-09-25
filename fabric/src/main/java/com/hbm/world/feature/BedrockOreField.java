// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.lib.Library;
import com.hbm.world.WorldgenHash;
import net.minecraft.util.Mth;

public final class BedrockOreField {

    private static final double INPUT_SCALE = 0.01D;
    private static final double OUTPUT_SCALE = 0.05D;
    private static final long RICHNESS =
            WorldgenHash.identifier(Library.id("bedrock_ore/richness"));
    private static final long LIGHT_METAL =
            WorldgenHash.identifier(Library.id("bedrock_ore/light_metal"));
    private static final long HEAVY_METAL =
            WorldgenHash.identifier(Library.id("bedrock_ore/heavy_metal"));
    private static final long RARE_EARTH =
            WorldgenHash.identifier(Library.id("bedrock_ore/rare_earth"));
    private static final long ACTINIDE =
            WorldgenHash.identifier(Library.id("bedrock_ore/actinide"));
    private static final long NON_METAL =
            WorldgenHash.identifier(Library.id("bedrock_ore/non_metal"));
    private static final long CRYSTALLINE =
            WorldgenHash.identifier(Library.id("bedrock_ore/crystalline"));

    private final FractalSimplexNoise richness;
    private final FractalSimplexNoise lightMetal;
    private final FractalSimplexNoise heavyMetal;
    private final FractalSimplexNoise rareEarth;
    private final FractalSimplexNoise actinide;
    private final FractalSimplexNoise nonMetal;
    private final FractalSimplexNoise crystalline;

    public BedrockOreField(long bedrockSeed) {
        richness = noise(bedrockSeed, RICHNESS);
        lightMetal = noise(bedrockSeed, LIGHT_METAL);
        heavyMetal = noise(bedrockSeed, HEAVY_METAL);
        rareEarth = noise(bedrockSeed, RARE_EARTH);
        actinide = noise(bedrockSeed, ACTINIDE);
        nonMetal = noise(bedrockSeed, NON_METAL);
        crystalline = noise(bedrockSeed, CRYSTALLINE);
    }

    public double richness(int x, int z) {
        return richness.get(x * INPUT_SCALE, z * INPUT_SCALE);
    }

    public double oreLevel(double sharedRichness, int x, int z, BedrockOreType type) {
        return switch (type) {
            case LIGHT_METAL -> lightMetal(sharedRichness, x, z);
            case HEAVY_METAL -> heavyMetal(sharedRichness, x, z);
            case RARE_EARTH -> rareEarth(sharedRichness, x, z);
            case ACTINIDE -> actinide(sharedRichness, x, z);
            case NON_METAL -> nonMetal(sharedRichness, x, z);
            case CRYSTALLINE -> crystalline(sharedRichness, x, z);
        };
    }

    public double lightMetal(double sharedRichness, int x, int z) {
        return density(sharedRichness, lightMetal.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double heavyMetal(double sharedRichness, int x, int z) {
        return density(sharedRichness, heavyMetal.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double rareEarth(double sharedRichness, int x, int z) {
        return density(sharedRichness, rareEarth.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double actinide(double sharedRichness, int x, int z) {
        return density(sharedRichness, actinide.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double nonMetal(double sharedRichness, int x, int z) {
        return density(sharedRichness, nonMetal.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double crystalline(double sharedRichness, int x, int z) {
        return density(sharedRichness, crystalline.get(x * INPUT_SCALE, z * INPUT_SCALE));
    }

    public double averageOreLevel(int x, int z) {
        double scaledX = x * INPUT_SCALE;
        double scaledZ = z * INPUT_SCALE;
        double sharedRichness = richness.get(scaledX, scaledZ);
        double total = density(sharedRichness, lightMetal.get(scaledX, scaledZ));
        total += density(sharedRichness, heavyMetal.get(scaledX, scaledZ));
        total += density(sharedRichness, rareEarth.get(scaledX, scaledZ));
        total += density(sharedRichness, actinide.get(scaledX, scaledZ));
        total += density(sharedRichness, nonMetal.get(scaledX, scaledZ));
        total += density(sharedRichness, crystalline.get(scaledX, scaledZ));
        return total / 6D;
    }

    private static double density(double sharedRichness, double material) {
        return Mth.clamp(Math.abs(sharedRichness * material) * OUTPUT_SCALE, 0D, 2D);
    }

    private static FractalSimplexNoise noise(long seed, long domain) {
        return new FractalSimplexNoise(WorldgenHash.domainSeed(seed, domain), 4);
    }
}
