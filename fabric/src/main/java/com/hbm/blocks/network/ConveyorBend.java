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

package com.hbm.blocks.network;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ConveyorBend implements StringRepresentable {
    STRAIGHT,
    LEFT,
    RIGHT;

    public static final ConveyorBend[] VALUES = values();

    private final String id = name().toLowerCase(Locale.ROOT);

    public ConveyorBend next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public ConveyorBend mirrored() {
        return switch (this) {
            case STRAIGHT -> STRAIGHT;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
