// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.registration.FabricRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class HbmParticles {

    public static RegistryHandle<SimpleParticleType> RAD_FOG;
    public static RegistryHandle<SimpleParticleType> RBMK_MUSH;
    public static RegistryHandle<SimpleParticleType> DIGAMMA_SMOKE;
    public static RegistryHandle<SimpleParticleType> LAUNCH_SMOKE;
    public static RegistryHandle<SimpleParticleType> SCHRAB_FOG;
    public static RegistryHandle<SimpleParticleType> GAS_FLAME;
    public static RegistryHandle<SimpleParticleType> HADRON;
    public static RegistryHandle<ParticleType<AshRevealParticleOptions>> ASH_REVEAL;
    public static RegistryHandle<ParticleType<CoolingTowerParticleOptions>> MIST_TOWER;
    public static RegistryHandle<ParticleType<SplashParticleOptions>> SPLASH;
    public static RegistryHandle<ParticleType<FlameParticleOptions>> FLAME;
    public static RegistryHandle<SimpleParticleType> BLACK_POWDER_SPARK;
    public static RegistryHandle<SimpleParticleType> VOLCANO_SMOKE;

    private HbmParticles() {}

    public static void register() {
        RAD_FOG =
                FabricRegistrar.registerParticleType(
                        "rad_fog", () -> new SimpleParticleType(false) {});
        RBMK_MUSH =
                FabricRegistrar.registerParticleType(
                        "rbmk_mush", () -> new SimpleParticleType(true) {});
        DIGAMMA_SMOKE =
                FabricRegistrar.registerParticleType(
                        "digamma_smoke", () -> new SimpleParticleType(true) {});
        LAUNCH_SMOKE =
                FabricRegistrar.registerParticleType(
                        "launch_smoke", () -> new SimpleParticleType(true) {});
        SCHRAB_FOG =
                FabricRegistrar.registerParticleType(
                        "schrab_fog", () -> new SimpleParticleType(false) {});
        GAS_FLAME =
                FabricRegistrar.registerParticleType(
                        "gas_flame", () -> new SimpleParticleType(true) {});
        HADRON =
                FabricRegistrar.registerParticleType(
                        "hadron", () -> new SimpleParticleType(true) {});
        ASH_REVEAL =
                FabricRegistrar.registerParticleType(
                        "ash_reveal",
                        () ->
                                new ParticleType<AshRevealParticleOptions>(true) {
                                    @Override
                                    public MapCodec<AshRevealParticleOptions> codec() {
                                        return AshRevealParticleOptions.codec(this);
                                    }

                                    @Override
                                    public StreamCodec<
                                                    ? super RegistryFriendlyByteBuf,
                                                    AshRevealParticleOptions>
                                            streamCodec() {
                                        return AshRevealParticleOptions.streamCodec(this);
                                    }
                                });
        MIST_TOWER =
                FabricRegistrar.registerParticleType(
                        "mist_tower",
                        () ->
                                new ParticleType<CoolingTowerParticleOptions>(true) {
                                    @Override
                                    public MapCodec<CoolingTowerParticleOptions> codec() {
                                        return CoolingTowerParticleOptions.codec(this);
                                    }

                                    @Override
                                    public StreamCodec<
                                                    ? super RegistryFriendlyByteBuf,
                                                    CoolingTowerParticleOptions>
                                            streamCodec() {
                                        return CoolingTowerParticleOptions.streamCodec(this);
                                    }
                                });
        SPLASH =
                FabricRegistrar.registerParticleType(
                        "particle_splash",
                        () ->
                                new ParticleType<SplashParticleOptions>(true) {
                                    @Override
                                    public MapCodec<SplashParticleOptions> codec() {
                                        return SplashParticleOptions.codec(this);
                                    }

                                    @Override
                                    public StreamCodec<
                                                    ? super RegistryFriendlyByteBuf,
                                                    SplashParticleOptions>
                                            streamCodec() {
                                        return SplashParticleOptions.streamCodec(this);
                                    }
                                });
        FLAME =
                FabricRegistrar.registerParticleType(
                        "flame",
                        () ->
                                new ParticleType<FlameParticleOptions>(true) {
                                    @Override
                                    public MapCodec<FlameParticleOptions> codec() {
                                        return FlameParticleOptions.codec(this);
                                    }

                                    @Override
                                    public StreamCodec<
                                                    ? super RegistryFriendlyByteBuf,
                                                    FlameParticleOptions>
                                            streamCodec() {
                                        return FlameParticleOptions.streamCodec(this);
                                    }
                                });
        BLACK_POWDER_SPARK =
                FabricRegistrar.registerParticleType(
                        "black_powder_spark", () -> new SimpleParticleType(true) {});
        VOLCANO_SMOKE =
                FabricRegistrar.registerParticleType(
                        "volcano_smoke", () -> new SimpleParticleType(true) {});
    }
}
