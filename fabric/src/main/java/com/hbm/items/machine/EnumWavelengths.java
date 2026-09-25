// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.ChatFormatting;

public enum EnumWavelengths {
    NULL("la creatura", "6 dollar", 0x010101, 0x010101, ChatFormatting.WHITE),

    IR("wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED),
    VISIBLE(
            "wavelengths.name.visible",
            "wavelengths.waveRange.visible",
            0,
            0,
            ChatFormatting.GREEN),
    UV("wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA),
    GAMMA(
            "wavelengths.name.gamma",
            "wavelengths.waveRange.gamma",
            0x150560,
            0xEF00FF,
            ChatFormatting.LIGHT_PURPLE),
    DRX(
            "wavelengths.name.drx",
            "wavelengths.waveRange.drx",
            0xFF0000,
            0xFF0000,
            ChatFormatting.DARK_RED);

    public final String name;
    public final String wavelengthRange;
    public final int renderedBeamColor;
    public final int guiColor;
    public final ChatFormatting textColor;

    EnumWavelengths(
            String name,
            String wavelengthRange,
            int renderedBeamColor,
            int guiColor,
            ChatFormatting textColor) {
        this.name = name;
        this.wavelengthRange = wavelengthRange;
        this.renderedBeamColor = renderedBeamColor;
        this.guiColor = guiColor;
        this.textColor = textColor;
    }
}
