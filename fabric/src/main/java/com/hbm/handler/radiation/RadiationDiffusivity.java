// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record RadiationDiffusivity(HolderSet<Block> blocks, float value) {

    public static final ResourceKey<Registry<RadiationDiffusivity>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("rad_diffusivity"));

    public static final float NEUTRAL = 1.0F;

    public static final float MIN = 0.01F, MAX = 1.50F;

    public static final Codec<RadiationDiffusivity> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            RegistryCodecs.homogeneousList(Registries.BLOCK)
                                                    .fieldOf("blocks")
                                                    .forGetter(RadiationDiffusivity::blocks),
                                            Codec.floatRange(MIN, MAX)
                                                    .fieldOf("value")
                                                    .forGetter(RadiationDiffusivity::value))
                                    .apply(instance, RadiationDiffusivity::new));

    public RadiationDiffusivity(List<Block> blocks, float value) {
        this(HolderSet.direct(blocks.stream().map(Block::builtInRegistryHolder).toList()), value);
    }

    private static final Reference2FloatOpenHashMap<BlockState> STATES =
            new Reference2FloatOpenHashMap<>(512);
    private static boolean resolved;

    static volatile int stateVersion;

    static {
        STATES.defaultReturnValue(NEUTRAL);
    }

    static void resolveFor(ServerLevel level) {
        if (resolved) return;
        resolved = true;
        level.registryAccess()
                .lookupOrThrow(REGISTRY)
                .listElements()
                .forEach(
                        holder -> {
                            RadiationDiffusivity entry = holder.value();

                            if (entry.value() == NEUTRAL) return;
                            for (Holder<Block> member : entry.blocks()) {
                                Block block = member.value();
                                for (BlockState state :
                                        block.getStateDefinition().getPossibleStates()) {
                                    if (!STATES.containsKey(state)
                                            || entry.value() < STATES.getFloat(state)) {
                                        STATES.put(state, entry.value());
                                    }
                                }
                            }
                        });
        stateVersion++;
    }

    static void clear() {
        STATES.clear();
        resolved = false;
        stateVersion++;
    }

    static float of(BlockState state) {
        return STATES.getFloat(state);
    }

    static boolean isTrivial() {
        return STATES.isEmpty();
    }

    static boolean transportEnabled(ServerLevel level) {
        resolveFor(level);
        return RadiationSettings.forLevel(level).diffusivityTransport().orElse(!isTrivial());
    }

    static float clamp(float value) {
        if (!Float.isFinite(value)) return MIN;
        return Math.min(Math.max(value, MIN), MAX);
    }

    static double edge(double lenA, double diffA, double lenB, double diffB) {
        if (!(diffA > 0.0D) || !(diffB > 0.0D)) return 0.0D;
        double denom = (lenA / diffA) + (lenB / diffB);
        double lenSum = lenA + lenB;
        if (!(denom > 0.0D)
                || !(lenSum > 0.0D)
                || !Double.isFinite(denom)
                || !Double.isFinite(lenSum)) {
            return 0.0D;
        }
        return lenSum / denom;
    }

    static double decay(double k) {
        if (!(k > 0.0D) || !Double.isFinite(k)) return 1.0D;
        if (k >= 700.0D) return 0.0D;
        return Math.exp(-k);
    }
}
