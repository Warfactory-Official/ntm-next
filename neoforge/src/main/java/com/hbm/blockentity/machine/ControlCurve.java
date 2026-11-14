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

package com.hbm.blockentity.machine;

public enum ControlCurve {
    LINEAR("desc.gui.rbmkControlAuto.linear", "desc.gui.rbmkControlAuto.selectLinear") {
        @Override
        public double level(
                double heat,
                double heatLower,
                double heatUpper,
                double levelLower,
                double levelUpper) {
            return (heat - heatLower) * ((levelUpper - levelLower) / (heatUpper - heatLower))
                    + levelLower;
        }
    },
    RISING("desc.gui.rbmkControlAuto.quadratic", "desc.gui.rbmkControlAuto.selectQuadratic") {
        @Override
        public double level(
                double heat,
                double heatLower,
                double heatUpper,
                double levelLower,
                double levelUpper) {
            double t = (heat - heatLower) / (heatUpper - heatLower);
            return t * t * (levelUpper - levelLower) + levelLower;
        }
    },
    FALLING(
            "desc.gui.rbmkControlAuto.inverseQuadratic",
            "desc.gui.rbmkControlAuto.selectInverseQuadratic") {
        @Override
        public double level(
                double heat,
                double heatLower,
                double heatUpper,
                double levelLower,
                double levelUpper) {
            double t = (heat - heatUpper) / (heatLower - heatUpper);
            return t * t * (levelLower - levelUpper) + levelUpper;
        }
    };

    public static final ControlCurve[] VALUES = values();

    private final String nameKey;
    private final String selectKey;

    ControlCurve(String nameKey, String selectKey) {
        this.nameKey = nameKey;
        this.selectKey = selectKey;
    }

    public String nameKey() {
        return nameKey;
    }

    public String selectKey() {
        return selectKey;
    }

    public abstract double level(
            double heat, double heatLower, double heatUpper, double levelLower, double levelUpper);

    public double target(
            double heat, double heatLower, double heatUpper, double levelLower, double levelUpper) {
        double level;
        if (heat < Math.min(heatLower, heatUpper)) {
            level = levelLower;
        } else if (heat > Math.max(heatLower, heatUpper)) {
            level = levelUpper;
        } else {
            level = level(heat, heatLower, heatUpper, levelLower, levelUpper);
        }
        double fraction = level * 0.01;
        return fraction < 0D ? 0D : fraction > 1D ? 1D : fraction;
    }
}
