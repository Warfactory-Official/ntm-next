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

package com.hbm.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class UncurableEffectInstance extends MobEffectInstance {

    public UncurableEffectInstance(Holder<MobEffect> effect, int duration) {
        super(effect, duration);
    }

    public UncurableEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        super(effect, duration, amplifier);
    }

    public UncurableEffectInstance(
            Holder<MobEffect> effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean visible) {
        super(effect, duration, amplifier, ambient, visible);
    }
}
