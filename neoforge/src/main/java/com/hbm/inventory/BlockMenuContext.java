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

package com.hbm.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

public record BlockMenuContext<D>(BlockPos pos, D data) {

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockMenuContext<Unit>> UNIT =
            codec(StreamCodec.unit(Unit.INSTANCE));

    public static <D> StreamCodec<RegistryFriendlyByteBuf, BlockMenuContext<D>> codec(
            StreamCodec<? super RegistryFriendlyByteBuf, D> data) {
        return StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                BlockMenuContext::pos,
                data,
                BlockMenuContext::data,
                BlockMenuContext::new);
    }
}
