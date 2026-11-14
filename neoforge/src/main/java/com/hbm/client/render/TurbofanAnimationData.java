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

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ChainInstance;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class TurbofanAnimationData {
    public static final HFRWavefrontObject MESH = ResourceManager.turbofan;
    public static final Identifier MAIN_TEXTURE = ResourceManager.turbofan_tex;
    public static final Identifier COLD_REAR_TEXTURE = ResourceManager.turbofan_back_tex;
    public static final Identifier HOT_REAR_TEXTURE = ResourceManager.turbofan_afterburner_tex;

    public static final String CASING_GROUP = "Body";
    public static final String ROTOR_GROUP = "Blades";
    public static final String REAR_GROUP = "Afterburner";

    public static final float CENTER_X = 0.5F;
    public static final float CENTER_Y = 0.0F;
    public static final float CENTER_Z = 0.5F;
    public static final float PIVOT_X = 0.0F;
    public static final float PIVOT_Y = 1.5F;
    public static final float PIVOT_Z = 0.0F;
    public static final int NORTH_REFERENCE_YAW = 90;
    public static final float DEGREES_TO_RADIANS = Mth.DEG_TO_RAD;
    public static final float ROTOR_RADIANS_PER_DEGREE = -Mth.DEG_TO_RAD;

    public static final ChainInstance.Chain[] ROTOR_CHAINS = {
        new ChainInstance.Chain()
                .translate(PIVOT_X, PIVOT_Y, PIVOT_Z)
                .rotateZ(0, ROTOR_RADIANS_PER_DEGREE)
                .translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z)
                .end()
    };

    private TurbofanAnimationData() {}

    public static float interpolate(float previous, float current, float partialTick) {
        return previous + (current - previous) * partialTick;
    }
}
