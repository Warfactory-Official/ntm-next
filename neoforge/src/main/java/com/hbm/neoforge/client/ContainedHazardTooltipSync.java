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

package com.hbm.neoforge.client;

import com.hbm.neoforge.hazard.ContainedStorage;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

final class ContainedHazardTooltipSync {
    private static final ContainedStorage walk =
            ContainedStorage.STORAGE || ContainedStorage.BACKPACKS
                    ? new ContainedStorage(ContainedHazardTooltipSync::requestContents)
                    : null;
    private static ItemStack root = ItemStack.EMPTY;

    private ContainedHazardTooltipSync() {}

    static void request(ItemStack stack) {
        if (walk == null || stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !ContainedStorage.hasContents(stack)) return;
        assert minecraft.isSameThread();
        root = stack;
        walk.walk(stack);
        root = ItemStack.EMPTY;
    }

    static void reset() {
        HazardTooltipRequests.reset();
    }

    private static boolean requestContents(UUID id, boolean backpack) {
        if (HazardTooltipRequests.reserve(id, backpack))
            SophisticatedContentsRequest.send(root, id, backpack);
        return false;
    }
}
