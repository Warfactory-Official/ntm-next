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

package com.hbm.logistics.courier;

import com.hbm.module.ModulePatternMatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record CourierItemRequest(ItemStack filter, String mode) {
    public static final Codec<CourierItemRequest> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ItemStack.CODEC
                                                    .fieldOf("filter")
                                                    .forGetter(CourierItemRequest::filter),
                                            Codec.STRING
                                                    .fieldOf("mode")
                                                    .forGetter(CourierItemRequest::mode))
                                    .apply(instance, CourierItemRequest::new));

    public boolean accepts(ItemStack candidate) {
        return ModulePatternMatcher.matches(mode, filter, candidate);
    }
}
