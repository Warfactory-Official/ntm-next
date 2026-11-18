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

package com.hbm.util.datafix;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

public final class DataFixClock {
    public static final String GAME_TIME = "__hbm_game_time";
    public static final String CONTEXT_GAME_TIME = "hbm_game_time";

    private DataFixClock() {}

    public static CompoundTag withGameTime(CompoundTag tag, long gameTime) {
        if (tag.getIntOr(HbmDataFixers.VERSION_KEY, 0) >= HbmDataFixers.CURRENT) return tag;
        var input = tag.copy();
        input.putLong(GAME_TIME, gameTime);
        return input;
    }

    public static CompoundTag savedContext(Path levelDat) {
        try {
            var context = new CompoundTag();
            context.putLong(
                    CONTEXT_GAME_TIME,
                    NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap())
                            .getCompoundOrEmpty("Data")
                            .getLong("Time")
                            .orElseThrow());
            return context;
        } catch (IOException e) {
            throw new UncheckedIOException("reading the saved datafix clock", e);
        }
    }
}
