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

package com.hbm.mixin;

import com.hbm.blocks.ClimbBox;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.injected.BlockEntityLifecycle;
import com.hbm.interfaces.injected.ClimbBoxBlockAccess;
import com.hbm.interfaces.injected.MachineCapsHolder;
import com.hbm.world.bulk.BulkRegistry;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class MixinBlock
        implements MachineCapsHolder, BlockEntityLifecycle, ClimbBoxBlockAccess {

    @Unique private static final byte BULK_MANAGED = 1;
    @Unique private static final byte CLIMB_BOX = 2;

    @Unique private @Nullable MachineCaps hbm$machineCaps;

    @Unique private byte hbm$flags;

    @Override
    public boolean hbm$bulkManaged() {
        return (hbm$flags & BULK_MANAGED) != 0;
    }

    @Override
    public void hbm$bulkManaged(boolean managed) {
        if (managed) hbm$flags |= BULK_MANAGED;
        else hbm$flags &= ~BULK_MANAGED;
    }

    @Unique private int hbm$bulkKind;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$bindBulk(CallbackInfo _) {
        hbm$bulkKind = BulkRegistry.bind(getClass());
        hbm$flags =
                (byte)
                        ((hbm$bulkKind != 0 ? BULK_MANAGED : 0)
                                | ((Object) this instanceof ClimbBox ? CLIMB_BOX : 0));
    }

    @Override
    public boolean hbm$mayHaveClimbBox() {
        return (hbm$flags & CLIMB_BOX) != 0;
    }

    @Override
    public int hbm$bulkKind() {
        return hbm$bulkKind;
    }

    @Override
    public @Nullable MachineCaps hbm$machineCaps() {
        return hbm$machineCaps;
    }

    @Override
    public void hbm$machineCaps(MachineCaps caps) {
        hbm$machineCaps = caps;
    }
}
