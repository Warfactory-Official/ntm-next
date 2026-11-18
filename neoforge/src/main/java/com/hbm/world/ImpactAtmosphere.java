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

package com.hbm.world;

import com.hbm.data.WorldData;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timelines;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class ImpactAtmosphere {

    private ImpactAtmosphere() {}

    public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        if (level.dimension() != Level.OVERWORLD || !WorldData.ENABLE_IMPACT_ATMOSPHERE.get())
            return;

        builder.addTimeBasedLayer(
                EnvironmentAttributes.SKY_LIGHT_LEVEL,
                (value, tick) -> value - (value - Timelines.NIGHT_SKY_LIGHT_LEVEL) * dust(level));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.SKY_LIGHT_FACTOR, (value, tick) -> value * (1 - dust(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.STAR_BRIGHTNESS, (value, tick) -> value * (1 - dust(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.SUNRISE_SUNSET_COLOR,
                (value, tick) -> sunriseColor(value, dust(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.CLOUD_COLOR, (value, tick) -> cloudColor(value, dust(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.SKY_COLOR,
                (value, tick) -> skyColor(value, dust(level), fire(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.FOG_COLOR,
                (value, tick) -> fogColor(value, dust(level), fire(level)));
    }

    public static Vector4fc sunriseColor(Vector4fc sunrise, float dust) {
        if (dust == 0) return sunrise;
        float clear = 1 - dust;
        return new Vector4f(
                sunrise.x() * clear, sunrise.y() * clear, sunrise.z() * clear, sunrise.w() * clear);
    }

    public static Vector4fc cloudColor(Vector4fc cloud, float dust) {
        if (dust == 0) return cloud;
        float clear = 1 - dust;
        return new Vector4f(cloud.x() * clear, cloud.y() * clear, cloud.z() * clear, cloud.w());
    }

    public static Vector3fc skyColor(Vector3fc sky, float dust, float fire) {
        if (dust == 0 && fire == 0) return sky;
        float brightness = fire + (1 - dust);
        float red;
        float green;
        float blue;
        if (fire > 0) {
            red = sky.x() * 1.3F;
            green = sky.y() * Math.max(1 - dust * 1.4F, 0);
            blue = sky.z() * Math.max(1 - dust * 4, 0);
        } else {
            red = sky.x();
            green = sky.y() * (1 - dust * 0.5F);
            blue = sky.z() * (1 - dust);
        }
        return new Vector3f(red * brightness, green * brightness, blue * brightness);
    }

    public static Vector3fc fogColor(Vector3fc fog, float dust, float fire) {
        if (dust == 0) return fog;
        float brightness = fire > 0 ? Math.max(1 - dust * 2, 0) : 1 - dust;
        float green = fog.y() * (1 - dust * 0.5F);
        float blue = fog.z() * (1 - dust);
        return new Vector3f(fog.x() * brightness, green * brightness, blue * brightness);
    }

    private static float dust(Level level) {
        if (level instanceof ServerLevel server) {
            TomSaveData data = TomSaveData.published(server);
            return data == null ? 0 : data.dust;
        }
        return ImpactWorldHandler.getDustForClient(level);
    }

    private static float fire(Level level) {
        if (level instanceof ServerLevel server) {
            TomSaveData data = TomSaveData.published(server);
            return data == null ? 0 : data.fire;
        }
        return ImpactWorldHandler.getFireForClient(level);
    }
}
