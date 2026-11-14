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

package com.hbm.blockentity.door;

import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

public record DoorSoundTimeline(
        Supplier<RegistryHandle<SoundEvent>> scrape,
        int openingScrape,
        int closingScrape,
        Supplier<RegistryHandle<SoundEvent>> thud,
        int thudFirst,
        int thudLast,
        int thudStep) {

    public DoorSoundTimeline {
        if (thudStep <= 0 || thudFirst > thudLast) {
            throw new IllegalArgumentException(
                    "door thuds " + thudFirst + ".." + thudLast + " every " + thudStep);
        }
    }

    public @Nullable RegistryHandle<SoundEvent> at(boolean opening, int ticks) {
        if (ticks == (opening ? openingScrape : closingScrape)) return scrape.get();
        return ticks >= thudFirst && ticks <= thudLast && (ticks - thudFirst) % thudStep == 0
                ? thud.get()
                : null;
    }

    void checkWithin(int ticks) {
        for (int at : new int[] {openingScrape, closingScrape, thudFirst, thudLast}) {
            if (at < 0 || at > ticks) {
                throw new IllegalArgumentException(
                        "door cue at " + at + " lies outside its 0.." + ticks + " ticks");
            }
        }
    }
}
