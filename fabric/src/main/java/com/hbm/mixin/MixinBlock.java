// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.injected.BlockEntityLifecycle;
import com.hbm.interfaces.injected.MachineCapsHolder;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Block.class)
public abstract class MixinBlock implements MachineCapsHolder, BlockEntityLifecycle {

    @Unique private @Nullable MachineCaps hbm$machineCaps;

    @Override
    public @Nullable MachineCaps hbm$machineCaps() {
        return hbm$machineCaps;
    }

    @Override
    public void hbm$machineCaps(MachineCaps caps) {
        hbm$machineCaps = caps;
    }
}
