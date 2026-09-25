// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class HudConfig {

    public static volatile boolean nukeFlash = true;
    public static volatile boolean nukeShake = true;
    public static volatile int geigerOffsetHorizontal = 0;
    public static volatile int geigerOffsetVertical = 0;

    public static volatile int infoPosition = 0;
    public static volatile int infoOffsetHorizontal = 0;
    public static volatile int infoOffsetVertical = 0;
    public static volatile int toolIndicatorX = 0;
    public static volatile int toolIndicatorY = 0;
    public static volatile boolean tooltipOreDict = true;
    public static volatile boolean tooltipCustomNuke = true;
    public static volatile boolean doddRbmkDiagnostic = true;

    private HudConfig() {}

    public static void loadFrom(ConfigStore c) {
        nukeFlash = c.get(ConfigSchema.NUKE_HUD_FLASH);
        nukeShake = c.get(ConfigSchema.NUKE_HUD_SHAKE);
        geigerOffsetHorizontal = c.get(ConfigSchema.GEIGER_OFFSET_HORIZONTAL);
        geigerOffsetVertical = c.get(ConfigSchema.GEIGER_OFFSET_VERTICAL);
        infoPosition = c.get(ConfigSchema.INFO_POSITION);
        infoOffsetHorizontal = c.get(ConfigSchema.INFO_OFFSET_HORIZONTAL);
        infoOffsetVertical = c.get(ConfigSchema.INFO_OFFSET_VERTICAL);
        toolIndicatorX = c.get(ConfigSchema.TOOL_HUD_INDICATOR_X);
        toolIndicatorY = c.get(ConfigSchema.TOOL_HUD_INDICATOR_Y);
        tooltipOreDict = c.get(ConfigSchema.TOOLTIP_ORE_DICT);
        tooltipCustomNuke = c.get(ConfigSchema.TOOLTIP_CUSTOM_NUKE);
        doddRbmkDiagnostic = c.get(ConfigSchema.DODD_RBMK_DIAGNOSTIC);
    }
}
