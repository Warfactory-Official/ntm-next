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

package com.hbm.handler.pollution;

import com.mojang.serialization.Codec;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public enum PollutionType {
    SOOT,
    POISON,
    HEAVYMETAL,
    FALLOUT;

    private static final PollutionType[] BY_INDEX = values();
    public static final int COUNT = BY_INDEX.length;
    public static final Codec<PollutionType> CODEC =
            Codec.stringResolver(Enum::name, PollutionType::named);

    private final String traitKey = "pollution.trait." + name().toLowerCase(Locale.ROOT);

    public static PollutionType byIndex(int index) {
        return BY_INDEX[index];
    }

    private static @Nullable PollutionType named(String name) {
        for (var kind : BY_INDEX) if (kind.name().equals(name)) return kind;
        return null;
    }

    public String traitKey() {
        return traitKey;
    }
}
