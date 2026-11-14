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

package com.hbm.mixin.compat.bulk;

import com.hbm.world.bulk.BulkHistory;
import com.hbm.world.bulk.BulkHistoryCarrier;
import nl.requios.effortlessbuilding.utilities.UndoManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = UndoManager.UndoEntry.class, remap = false)
public abstract class MixinUndoEntry implements BulkHistoryCarrier {
    @Unique private @Nullable BulkHistory hbm$history;

    @Override
    public @Nullable BulkHistory hbm$bulkHistory() {
        return hbm$history;
    }

    @Override
    public void hbm$bulkHistory(BulkHistory history) {
        hbm$history = history;
    }
}
