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

import com.hbm.world.loot.RandomSlotLoot;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTable.Builder.class)
public abstract class MixinBuilder implements RandomSlotLoot {
    @Unique private boolean hbm$randomSlots;

    @Override
    public boolean hbm$randomSlots() {
        return hbm$randomSlots;
    }

    @Override
    public void hbm$randomSlots(boolean enabled) {
        hbm$randomSlots = enabled;
    }

    @Inject(method = "build", at = @At("RETURN"))
    private void hbm$preserveContainerFilling(CallbackInfoReturnable<LootTable> cir) {
        ((com.hbm.world.loot.RandomSlotLoot) (cir.getReturnValue()))
                .hbm$randomSlots(hbm$randomSlots);
    }
}
