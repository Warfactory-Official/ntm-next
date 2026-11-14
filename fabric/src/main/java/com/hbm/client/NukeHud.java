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

package com.hbm.client;

import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.config.HudConfig;
import com.hbm.util.GameTime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class NukeHud {

    private static final long FLASH_DURATION = 5_000L;
    private static final long SHAKE_DURATION = 1_500L;
    private static final long REARM = 1_000L;

    private static long flashStamp = Long.MIN_VALUE / 2;
    private static long shakeStamp = Long.MIN_VALUE / 2;

    private NukeHud() {}

    public static void armFlash() {
        long now = GameTime.millis();
        if (!within(now - flashStamp, REARM)) flashStamp = now;
    }

    public static boolean armShake() {
        long now = GameTime.millis();
        if (within(now - shakeStamp, REARM)) return false;
        shakeStamp = now;
        return true;
    }

    public static void renderFlash(GuiGraphicsExtractor graphics) {
        long elapsed = GameTime.now() - flashStamp;
        if (!within(elapsed, FLASH_DURATION) || !HudConfig.nukeFlash) return;
        float brightness = (FLASH_DURATION - elapsed) / (float) FLASH_DURATION;
        graphics.fill(
                HbmRenderPipelines.GUI_ADDITIVE,
                0,
                0,
                graphics.guiWidth(),
                graphics.guiHeight(),
                ARGB.colorFromFloat(brightness, 1F, 1F, 1F));
    }

    public static boolean pushShake(GuiGraphicsExtractor graphics) {
        long now = GameTime.now();
        long elapsed = now - shakeStamp;
        if (!within(elapsed, SHAKE_DURATION) || !HudConfig.nukeShake) return false;
        double mult = (SHAKE_DURATION - elapsed) / (double) SHAKE_DURATION * 2D;
        double horizontal = Mth.clamp(Math.sin(now * 0.02D), -0.7D, 0.7D) * 15D;
        double vertical = Mth.clamp(Math.sin(now * 0.01D + 2D), -0.7D, 0.7D) * 3D;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (horizontal * mult), (float) (vertical * mult));
        return true;
    }

    public static void popShake(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static void shaken(GuiGraphicsExtractor graphics, Runnable layer) {
        boolean pushed = pushShake(graphics);
        layer.run();
        if (pushed) popShake(graphics);
    }

    private static boolean within(long elapsed, long duration) {
        return elapsed >= 0L && elapsed < duration;
    }
}
