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

package com.hbm.fabric.mixin;

import com.hbm.fabric.platform.CapCacheIndex;
import com.hbm.interfaces.injected.CapCacheHost;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements CapCacheHost {

    @Unique private @Nullable CapCacheIndex hbm$capCaches;

    @Override
    public CapCacheIndex hbm$capCaches() {
        CapCacheIndex index = this.hbm$capCaches;
        if (index == null) this.hbm$capCaches = index = new CapCacheIndex();
        return index;
    }
}
