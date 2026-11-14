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

package com.hbm.config;

public final class HudConfig {

    public static volatile boolean nukeFlash = true;
    public static volatile boolean nukeShake = true;
    public static volatile boolean modeBadges = true;
    public static volatile int geigerOffsetHorizontal = 0;
    public static volatile int geigerOffsetVertical = 0;
    public static volatile int infoPosition = 0;
    public static volatile int infoOffsetHorizontal = 0;
    public static volatile int infoOffsetVertical = 0;
    public static volatile int toolIndicatorX = 0;
    public static volatile int toolIndicatorY = 0;
    public static volatile boolean tooltipCustomNuke = true;
    public static volatile boolean doddRbmkDiagnostic = true;

    private HudConfig() {}

    public static void loadFrom(ConfigStore c) {
        nukeFlash = c.get(ConfigSchema.NUKE_HUD_FLASH);
        nukeShake = c.get(ConfigSchema.NUKE_HUD_SHAKE);
        modeBadges = c.get(ConfigSchema.MODE_BADGES);
        geigerOffsetHorizontal = c.get(ConfigSchema.GEIGER_OFFSET_HORIZONTAL);
        geigerOffsetVertical = c.get(ConfigSchema.GEIGER_OFFSET_VERTICAL);
        infoPosition = c.get(ConfigSchema.INFO_POSITION);
        infoOffsetHorizontal = c.get(ConfigSchema.INFO_OFFSET_HORIZONTAL);
        infoOffsetVertical = c.get(ConfigSchema.INFO_OFFSET_VERTICAL);
        toolIndicatorX = c.get(ConfigSchema.TOOL_HUD_INDICATOR_X);
        toolIndicatorY = c.get(ConfigSchema.TOOL_HUD_INDICATOR_Y);
        tooltipCustomNuke = c.get(ConfigSchema.TOOLTIP_CUSTOM_NUKE);
        doddRbmkDiagnostic = c.get(ConfigSchema.DODD_RBMK_DIAGNOSTIC);
    }
}
