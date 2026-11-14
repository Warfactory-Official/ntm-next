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

package com.hbm.platform;

import appeng.api.ids.AEComponents;
import com.hbm.integration.ae2.AE2BlockItemLayouts;
import com.hbm.integration.ae2.AE2ItemContents;
import com.hbm.inventory.ItemContents;
import com.hbm.inventory.SlotSource;
import com.hbm.neoforge.inventory.AE2MenuInventories;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

public class InventoryPlatform {
    private static final boolean AE = Platform.isModLoaded("ae2");

    public static void registerContents() {
        if (AE) {
            AE2ItemContents.register();
            AE2BlockItemLayouts.register();
            ItemContents.independentComponent(AEComponents.STORED_ENERGY);
        }
    }

    public static @Nullable SlotSource menuInventory(AbstractContainerMenu menu) {
        return AE ? AE2MenuInventories.inventory(menu) : null;
    }
}
