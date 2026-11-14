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

package com.hbm.mixin.storage;

import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.TrackedStorage;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import java.util.function.Consumer;
import net.minecraft.world.inventory.CraftingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingTerminalBlockEntity.class, remap = false)
public abstract class MixinCraftingTerminalBlockEntity implements StoredItems, TrackedStorage {
    @Shadow
    public abstract CraftingContainer getCraftingInv();

    @Override
    public void inventorySources(Consumer<Object> sources) {
        sources.accept(getCraftingInv());
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visitOwned(getCraftingInv());
    }
}
