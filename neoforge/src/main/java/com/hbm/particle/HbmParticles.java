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

package com.hbm.particle;

import com.hbm.registration.Registrar;
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
        RAD_FOG = Registrar.registerParticleType("rad_fog", () -> new SimpleParticleType(false) {});
        RBMK_MUSH =
                Registrar.registerParticleType("rbmk_mush", () -> new SimpleParticleType(true) {});
        DIGAMMA_SMOKE =
                Registrar.registerParticleType(
                        "digamma_smoke", () -> new SimpleParticleType(true) {});
        LAUNCH_SMOKE =
                Registrar.registerParticleType(
                        "launch_smoke", () -> new SimpleParticleType(true) {});
        SCHRAB_FOG =
                Registrar.registerParticleType(
                        "schrab_fog", () -> new SimpleParticleType(false) {});
        GAS_FLAME =
                Registrar.registerParticleType("gas_flame", () -> new SimpleParticleType(true) {});
        HADRON = Registrar.registerParticleType("hadron", () -> new SimpleParticleType(true) {});
        ASH_REVEAL =
                Registrar.registerParticleType(
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
                Registrar.registerParticleType(
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
                Registrar.registerParticleType(
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
                Registrar.registerParticleType(
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
                Registrar.registerParticleType(
                        "black_powder_spark", () -> new SimpleParticleType(true) {});
        VOLCANO_SMOKE =
                Registrar.registerParticleType(
                        "volcano_smoke", () -> new SimpleParticleType(true) {});
    }
}
