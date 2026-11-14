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

package com.hbm.explosion.assembly;

public record ChargeYield(
        float conventional,
        float nuclear,
        float thermonuclear,
        float antimatter,
        float fallout,
        float schrabidium,
        float antiMass) {

    public float adjustedNuclear() {
        if (nuclear == 0F) return 0F;
        return Math.min(nuclear + conventional / 2F, 200F);
    }

    public float adjustedThermonuclear() {
        if (thermonuclear == 0F) return 0F;
        return Math.min((thermonuclear + nuclear / 2F) + conventional / 4F, 350F);
    }

    public float adjustedAntimatter() {
        if (antimatter == 0F) return 0F;
        return Math.min(
                ((antimatter + thermonuclear / 2F) + nuclear / 4F) + conventional / 8F, 350F);
    }

    public float adjustedSchrabidium() {
        if (schrabidium == 0F) return 0F;
        return Math.min(
                (((schrabidium + antimatter / 2F) + thermonuclear / 4F) + nuclear / 8F)
                        + conventional / 16F,
                250F);
    }
}
