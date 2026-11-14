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

package com.hbm.neoforge.hazard;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;

public final class SophisticatedContentsRequests {
    private SophisticatedContentsRequests() {}

    public static void serve(
            ServerPlayer player, int rootContainer, int rootSlot, UUID target, boolean backpack) {
        if (!(backpack ? ContainedStorage.BACKPACKS : ContainedStorage.STORAGE)) return;
        new ContainedStorage(
                        (id, kind) -> {
                            if (kind != backpack || !id.equals(target)) return false;
                            if (backpack) SophisticatedBackpackContents.send(player, id);
                            else SophisticatedStorageContents.send(player, id);
                            return true;
                        })
                .walk(new ItemStackReference(rootContainer, rootSlot, "", "").resolve(player));
    }
}
