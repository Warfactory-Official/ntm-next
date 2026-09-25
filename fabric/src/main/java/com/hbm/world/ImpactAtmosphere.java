// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.data.WorldData;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.platform.Services;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timelines;

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
                (value, tick) -> {
                    float clear = 1 - dust(level);
                    return ARGB.scaleRGB(ARGB.multiplyAlpha(value, clear), clear);
                });
        builder.addTimeBasedLayer(
                EnvironmentAttributes.CLOUD_COLOR,
                (value, tick) -> ARGB.scaleRGB(value, 1 - dust(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.SKY_COLOR,
                (value, tick) -> skyColor(value, dust(level), fire(level)));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.FOG_COLOR,
                (value, tick) -> fogColor(value, dust(level), fire(level)));
    }

    public static int skyColor(int sky, float dust, float fire) {
        float brightness = fire + (1 - dust);
        if (fire > 0) {
            return ARGB.scaleRGB(
                    sky,
                    1.3F * brightness,
                    Math.max(1 - dust * 1.4F, 0) * brightness,
                    Math.max(1 - dust * 4, 0) * brightness);
        }
        return ARGB.scaleRGB(
                sky, brightness, (1 - dust * 0.5F) * brightness, (1 - dust) * brightness);
    }

    public static int fogColor(int fog, float dust, float fire) {
        float brightness = fire > 0 ? Math.max(1 - dust * 2, 0) : 1 - dust;
        return ARGB.scaleRGB(
                fog, brightness, (1 - dust * 0.5F) * brightness, (1 - dust) * brightness);
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
