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

package com.hbm.hazard;

import com.hbm.hazard.type.HazardKind;
import java.util.ArrayList;
import java.util.List;

public class HazardData {
    public static final int DEFAULT_PRIORITY = 1000;

    public int mutexBits = 0;
    public int priority = DEFAULT_PRIORITY;

    public List<HazardEntry> entries = new ArrayList<>();

    public HazardData addEntry(HazardKind hazard) {
        return this.addEntry(hazard, 1D);
    }

    public HazardData addEntry(HazardKind hazard, double level) {
        this.entries.add(new HazardEntry(hazard, level));
        return this;
    }

    public HazardData addEntry(HazardEntry entry) {
        this.entries.add(entry);
        return this;
    }

    public int getMutex() {
        return mutexBits;
    }

    public HazardData setMutex(int mutex) {
        this.mutexBits = mutex;
        return this;
    }

    public HazardData setPriority(int priority) {
        this.priority = priority;
        return this;
    }
}
