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

package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.hbm.util.NtmMath;
import net.minecraft.world.level.Level;

import static com.hbm.client.render.LPW2AnimationData.*;

public final class LPW2Clock {
    public double axialPhase;
    public double rockingPhase;
    public double pistonPhase;
    public double rotorPhase;
    public double serverPhase;
    public double turbine;
    public float scroll;

    public static float getWorldTime(Level level, float partialTick) {
        return GameTime.millis(level) / 50 % 1_000_000 + partialTick;
    }

    public static float getTerminalTime(double worldTime) {
        double phase = worldTime / TERMINAL_DIVISOR;
        return (float) ((softPeak(phase) + phase * HALF) % 1);
    }

    public void prepare(double mechanicalTime, double worldTime) {
        double rockingTime = mechanicalTime / ROCK_DIVISOR;
        axialPhase = (mechanicalTime / AXIAL_DIVISOR) % AXIAL_PERIOD;
        rockingPhase = rockingTime % ROCK_PERIOD;
        pistonPhase = rockingTime % PISTON_PERIOD;
        rotorPhase = rockingTime % ROTOR_PERIOD;
        serverPhase = (mechanicalTime / SERVER_DIVISOR) % ROCK_PERIOD;
        turbine = (mechanicalTime % TURBINE_PERIOD) / TURBINE_PERIOD;
        scroll = getTerminalTime(worldTime);
    }

    public static double softPeak(double phase) {
        return NtmMath.flatSine(phase);
    }

    public static double harmonic(double phase) {
        return Math.sin(phase)
                + Math.sin(HARMONIC_SECOND * phase)
                + Math.sin(HARMONIC_FOURTH * phase);
    }

    public static double rocking(double phase, double offset) {
        return (Math.sin(phase + offset) + Math.sin(ROCK_FREQUENCY * phase)) / ROCK_NORMALIZATION;
    }
}
