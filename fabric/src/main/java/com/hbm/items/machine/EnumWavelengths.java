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

package com.hbm.items.machine;

import com.hbm.registration.OpenRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;

public final class EnumWavelengths {

    public static final OpenRegistry<EnumWavelengths> REGISTRY =
            new OpenRegistry<>("wavelength", EnumWavelengths::name);

    public static final EnumWavelengths NULL =
            REGISTRY.add(
                    new EnumWavelengths(
                            "NULL",
                            "la creatura",
                            "6 dollar",
                            0x010101,
                            0x010101,
                            ChatFormatting.WHITE));

    public static final EnumWavelengths IR =
            REGISTRY.add(
                    new EnumWavelengths(
                            "IR",
                            "wavelengths.name.ir",
                            "wavelengths.waveRange.ir",
                            0xBB1010,
                            0xCC4040,
                            ChatFormatting.RED));
    public static final EnumWavelengths VISIBLE =
            REGISTRY.add(
                    new EnumWavelengths(
                            "VISIBLE",
                            "wavelengths.name.visible",
                            "wavelengths.waveRange.visible",
                            0,
                            0,
                            ChatFormatting.GREEN));
    public static final EnumWavelengths UV =
            REGISTRY.add(
                    new EnumWavelengths(
                            "UV",
                            "wavelengths.name.uv",
                            "wavelengths.waveRange.uv",
                            0x0A1FC4,
                            0x00EFFF,
                            ChatFormatting.AQUA));
    public static final EnumWavelengths GAMMA =
            REGISTRY.add(
                    new EnumWavelengths(
                            "GAMMA",
                            "wavelengths.name.gamma",
                            "wavelengths.waveRange.gamma",
                            0x150560,
                            0xEF00FF,
                            ChatFormatting.LIGHT_PURPLE));
    public static final EnumWavelengths DRX =
            REGISTRY.add(
                    new EnumWavelengths(
                            "DRX",
                            "wavelengths.name.drx",
                            "wavelengths.waveRange.drx",
                            0xFF0000,
                            0xFF0000,
                            ChatFormatting.DARK_RED));

    private static final EnumWavelengths[] LEGACY = {NULL, IR, VISIBLE, UV, GAMMA, DRX};

    private final String id;
    public final String name;
    public final String wavelengthRange;
    public final int renderedBeamColor;
    public final int guiColor;
    public final ChatFormatting textColor;

    public EnumWavelengths(
            String id,
            String name,
            String wavelengthRange,
            int renderedBeamColor,
            int guiColor,
            ChatFormatting textColor) {
        this.id = id;
        this.name = name;
        this.wavelengthRange = wavelengthRange;
        this.renderedBeamColor = renderedBeamColor;
        this.guiColor = guiColor;
        this.textColor = textColor;
    }

    public static List<EnumWavelengths> values() {
        return REGISTRY.values();
    }

    public static EnumWavelengths byName(String name) {
        return REGISTRY.byName(name, NULL);
    }

    public static EnumWavelengths legacy(int ordinal) {
        return LEGACY[ordinal];
    }

    public String name() {
        return id;
    }

    public int ordinal() {
        return REGISTRY.index(this);
    }

    @Override
    public String toString() {
        return id;
    }
}
