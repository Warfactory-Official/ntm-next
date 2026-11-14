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

package com.hbm.client.render;

import com.hbm.config.GunVisualConfig;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public final class DoorAnimations {
    private final Supplier<BusAnimation> opening, closing;
    private volatile @Nullable Built built;

    private DoorAnimations(Supplier<BusAnimation> opening, Supplier<BusAnimation> closing) {
        this.opening = opening;
        this.closing = closing;
    }

    public static DoorAnimations of(
            Supplier<BusAnimation> opening, Supplier<BusAnimation> closing) {
        return new DoorAnimations(opening, closing);
    }

    public static DoorAnimations linear(int ticks) {
        return of(
                () ->
                        new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new AnimationSequence()
                                                .jump(0, 0, 0)
                                                .to(0, 1, 0, ticks * 50)),
                () ->
                        new BusAnimation()
                                .addBus(
                                        "DOOR",
                                        new AnimationSequence()
                                                .jump(0, 1, 0)
                                                .to(0, 0, 0, ticks * 50)));
    }

    public BusAnimation clip(boolean opening) {
        double speed = GunVisualConfig.animationSpeed;
        var current = built;
        if (current == null || current.speed != speed) {
            built = current = new Built(speed, this.opening.get(), closing.get());
        }
        return opening ? current.opening : current.closing;
    }

    private record Built(double speed, BusAnimation opening, BusAnimation closing) {}
}
