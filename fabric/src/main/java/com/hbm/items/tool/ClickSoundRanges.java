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

package com.hbm.items.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;
import net.minecraft.util.RandomSource;

public final class ClickSoundRanges {

    private final List<Band> bands = new ArrayList<>();

    public ClickSoundRanges band(int click, DoublePredicate contains) {
        bands.add(new Band(click, contains));
        return this;
    }

    public int pick(double rate, RandomSource random) {
        int ballots = 0;
        for (int i = 0; i < bands.size(); i++) if (bands.get(i).contains.test(rate)) ballots++;
        if (ballots == 0) return 0;

        int drawn = random.nextInt(ballots);
        for (int i = 0; ; i++) {
            Band band = bands.get(i);
            if (band.contains.test(rate) && drawn-- == 0) return band.click;
        }
    }

    private record Band(int click, DoublePredicate contains) {}
}
