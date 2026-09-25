// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        float soot = (float) (rendered - PollutionHandler.sootFogThreshold());
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
