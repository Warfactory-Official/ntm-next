// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.interfaces.injected.IServerLevelExtension;
import com.hbm.lib.Library;
import com.hbm.world.feature.BedrockOreField;
import com.hbm.world.feature.FractalSimplexNoise;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;

public final class NtmWorldgenFields {

    private static final long BEDROCK = WorldgenHash.identifier(Library.id("bedrock_ore"));
    private static final long COLTAN = WorldgenHash.identifier(Library.id("coltan_deposit"));
    private static final long AUSTRALIUM = WorldgenHash.identifier(Library.id("australium_window"));

    private final long worldDimensionSeed;
    private final long bedrockSeed;
    private final ConcurrentHashMap<Identifier, NoiseEntry> fractals = new ConcurrentHashMap<>();
    private volatile BedrockOreField bedrock;
    private final double coltanX;
    private final double coltanZ;
    private final double australiumRadiusUnit;
    private final double australiumDirectionX;
    private final double australiumDirectionZ;

    public NtmWorldgenFields(long worldSeed, Identifier dimension) {
        this.worldDimensionSeed = WorldgenHash.worldDimensionSeed(worldSeed, dimension);
        this.bedrockSeed = domainSeed(BEDROCK);

        long coltanSeed = domainSeed(COLTAN);
        double radius =
                Math.sqrt(
                        -2.0D
                                * Math.log(
                                        Math.max(
                                                0x1.0p-53D,
                                                WorldgenHash.unitDouble(
                                                        WorldgenHash.position(
                                                                coltanSeed, 0, 0, 0, 0)))));
        double angle =
                Math.TAU * WorldgenHash.unitDouble(WorldgenHash.position(coltanSeed, 0, 0, 0, 1));
        this.coltanX = radius * Math.cos(angle);
        this.coltanZ = radius * Math.sin(angle);

        long australiumSeed = domainSeed(AUSTRALIUM);
        this.australiumRadiusUnit =
                WorldgenHash.unitDouble(WorldgenHash.position(australiumSeed, 0, 0, 0, 0));
        double australiumAngle =
                Math.TAU
                        * WorldgenHash.unitDouble(
                                WorldgenHash.position(australiumSeed, 0, 0, 0, 1));
        this.australiumDirectionX = Math.cos(australiumAngle);
        this.australiumDirectionZ = Math.sin(australiumAngle);
    }

    public static NtmWorldgenFields get(ServerLevelAccessor level) {
        return ((IServerLevelExtension) level.getLevel()).hbm$worldgenFields();
    }

    public long domainSeed(long domainSalt) {
        return WorldgenHash.domainSeed(this.worldDimensionSeed, domainSalt);
    }

    public RandomSource random(long domainSalt, BlockPos pos) {
        return RandomSource.create(
                WorldgenHash.position(
                        domainSeed(domainSalt), pos.getX(), pos.getY(), pos.getZ(), 0));
    }

    public BedrockOreField bedrock() {
        BedrockOreField field = this.bedrock;
        if (field == null) field = installBedrock();
        return field;
    }

    public FractalSimplexNoise fractal(Identifier domain, int octaveCount) {
        NoiseEntry entry = this.fractals.get(domain);
        if (entry == null) entry = install(domain, octaveCount);
        if (entry.octaveCount != octaveCount) {
            throw new IllegalArgumentException(
                    "worldgen noise domain "
                            + domain
                            + " already uses "
                            + entry.octaveCount
                            + " octaves, not "
                            + octaveCount);
        }
        return entry.noise;
    }

    public int coltanX(double spread) {
        return (int) (this.coltanX * spread);
    }

    public int coltanZ(double spread) {
        return (int) (this.coltanZ * spread);
    }

    public long australiumCenter(int minimumRadius, int maximumRadius) {
        double minimumSquared = (double) minimumRadius * minimumRadius;
        double maximumSquared = (double) maximumRadius * maximumRadius;
        double radius =
                Math.sqrt(
                        minimumSquared
                                + this.australiumRadiusUnit * (maximumSquared - minimumSquared));
        int x = (int) Math.round(radius * this.australiumDirectionX);
        int z = (int) Math.round(radius * this.australiumDirectionZ);
        return (long) x << 32 | Integer.toUnsignedLong(z);
    }

    private synchronized NoiseEntry install(Identifier domain, int octaveCount) {
        NoiseEntry entry = this.fractals.get(domain);
        if (entry == null) {
            entry =
                    new NoiseEntry(
                            octaveCount,
                            new FractalSimplexNoise(
                                    domainSeed(WorldgenHash.identifier(domain)), octaveCount));
            this.fractals.put(domain, entry);
        }
        return entry;
    }

    private synchronized BedrockOreField installBedrock() {
        BedrockOreField field = this.bedrock;
        if (field == null) this.bedrock = field = new BedrockOreField(this.bedrockSeed);
        return field;
    }

    private record NoiseEntry(int octaveCount, FractalSimplexNoise noise) {}
}
