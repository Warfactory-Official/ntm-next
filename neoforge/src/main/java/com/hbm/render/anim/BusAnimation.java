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

package com.hbm.render.anim;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class BusAnimation {

    private final HashMap<String, AnimationSequence> animationBuses = new HashMap<>();
    private int totalTime;

    public BusAnimation addBus(String name, AnimationSequence bus) {
        animationBuses.put(name, bus);
        totalTime = Math.max(totalTime, bus.getDuration());
        return this;
    }

    public AnimationSequence getBus(String name) {
        return animationBuses.get(name);
    }

    public void forEachBus(BiConsumer<String, AnimationSequence> action) {
        animationBuses.forEach(action);
    }

    public int getDuration() {
        return totalTime;
    }
}
