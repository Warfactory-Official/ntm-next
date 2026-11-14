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

package com.hbm.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum TriggerResult {
    NONE(false, ""),
    DETONATED(true, "bomb.detonated"),
    ACTUATED(true, "bomb.triggered"),
    LAUNCHED(true, "bomb.launched"),
    MISSING_COMPONENT(false, "bomb.missingComponent"),
    INCOMPATIBLE(false, "bomb.incompatible"),
    NO_DEVICE(false, "bomb.nobomb"),
    TNT_DISABLED(false, "block.minecraft.tnt.disabled");

    private final boolean success;
    private final String key;

    TriggerResult(boolean success, String key) {
        this.success = success;
        this.key = key;
    }

    public boolean success() {
        return success;
    }

    public MutableComponent message() {
        return Component.translatable(key);
    }
}
