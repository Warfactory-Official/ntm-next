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

package com.hbm.api.entity;

import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;

public final class LivingHooks {

    @SuppressWarnings("unchecked")
    private static Consumer<LivingEntity>[] tickListeners = new Consumer[0];

    private LivingHooks() {}

    public static void onTick(Consumer<LivingEntity> listener) {
        tickListeners = Arrays.copyOf(tickListeners, tickListeners.length + 1);
        tickListeners[tickListeners.length - 1] = listener;
    }

    public static void tick(LivingEntity entity) {
        for (Consumer<LivingEntity> listener : tickListeners) listener.accept(entity);
    }
}
