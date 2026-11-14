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

package com.hbm.util;

import com.hbm.lib.internal.GenerationTracking;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class SectionGeneration {
    private static final GenerationTracking TRACKING = new GenerationTracking(advanceHandle());
    private static final MethodHandle CHANGED = TRACKING.invoker();

    private SectionGeneration() {}

    public static void changed(LevelChunkSection section) {
        try {
            CHANGED.invokeExact(section);
        } catch (Throwable failure) {
            __asm__ {
                aload failure;
                athrow;
            }
        }
    }

    public static long generation(LevelChunkSection section) {
        return __asm__(long) {
            aload section;
            getfield "net/minecraft/world/level/chunk/LevelChunkSection" "hbm$generation" "J";
        };
    }

    public static void acquire() {
        TRACKING.acquire();
    }

    public static void release() {
        TRACKING.release();
    }

    private static MethodHandle advanceHandle() {
        try {
            return MethodHandles.lookup()
                    .findStatic(
                            SectionGeneration.class,
                            "advance",
                            MethodType.methodType(void.class, LevelChunkSection.class));
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void advance(LevelChunkSection section) {
        __asm__ {
            aload section;
            dup;
            getfield "net/minecraft/world/level/chunk/LevelChunkSection" "hbm$generation" "J";
            lconst_1;
            ladd;
            putfield "net/minecraft/world/level/chunk/LevelChunkSection" "hbm$generation" "J";
        }
    }
}
