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

package com.hbm.items.machine;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum DriveType implements StringRepresentable {
    FLASH_EMPTY,
    DISK_EMPTY,
    FLASH_BROKEN,
    DISK_BROKEN,
    FLASH_FLIGHTSIM,
    FLASH_PARTICLESIM,
    DISK_FLIGHTDATA,
    DISK_FLIGHTDATA_PROCESSED,
    DISK_ORBITDATA,
    DISK_ORBITDATA_PROCESSED,
    KLAUS;

    public static final Codec<DriveType> CODEC = StringRepresentable.fromEnum(DriveType::values);

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
