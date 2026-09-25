// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.fabric.platform.CapCacheIndex;
import com.hbm.interfaces.injected.CapCacheHost;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerLevelCapCacheMixin implements CapCacheHost {

    @Unique private @Nullable CapCacheIndex hbm$capCaches;

    @Override
    public CapCacheIndex hbm$capCaches() {
        CapCacheIndex index = this.hbm$capCaches;
        if (index == null) this.hbm$capCaches = index = new CapCacheIndex();
        return index;
    }
}
