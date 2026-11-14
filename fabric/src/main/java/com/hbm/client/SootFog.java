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

package com.hbm.client;

import com.hbm.data.RadiationData;
import com.hbm.handler.pollution.PollutionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import org.joml.Vector4f;

public final class SootFog {

    private static final float STEP = 0.05F;
    private static final float SOOT_COLOR = 0.15F;

    private static float synced;
    private static float rendered;

    private SootFog() {}

    public static void receive(float soot) {
        synced = soot;
    }

    public static void clientTick() {
        if (Minecraft.getInstance().level == null) {
            synced = 0F;
            rendered = 0F;
        } else if (Math.abs(rendered - synced) < STEP) {
            rendered = synced;
        } else {
            rendered += rendered < synced ? STEP : -STEP;
        }
    }

    public static void apply(FogData fog, boolean atmospheric, int renderDistanceChunks) {
        float soot = (float) (rendered - PollutionHandler.fogThreshold());
        if (soot <= 0F || !RadiationData.ENABLE_SOOT_FOG.get()) return;
        float divisor = RadiationData.SOOT_FOG_DIVISOR.get().floatValue();

        if (atmospheric) {
            fog.environmentalStart = 0F;
            fog.environmentalEnd = renderDistanceChunks * 16 / (1 + soot * 5F / divisor);
        }

        float interp = Math.min(soot / divisor, 1F);
        Vector4f color = fog.color;
        color.set(
                color.x * (1 - interp) + SOOT_COLOR * interp,
                color.y * (1 - interp) + SOOT_COLOR * interp,
                color.z * (1 - interp) + SOOT_COLOR * interp,
                color.w);
    }
}
