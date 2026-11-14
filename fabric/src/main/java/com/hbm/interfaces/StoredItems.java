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

import com.hbm.inventory.InventoryPublications.Notifications;
import com.hbm.inventory.ItemContents.Editor;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface StoredItems {
    void visitStoredItems(Visitor visitor);

    interface Visitor extends Editor, Notifications {
        boolean visit(ItemStack stack);

        boolean visit(Container container);

        boolean visit(BlockEntity blockEntity);

        boolean visit(StoredItemSlots slots);

        boolean visitOwned(Container container);

        void afterChanges(Runnable notification);
    }
}
