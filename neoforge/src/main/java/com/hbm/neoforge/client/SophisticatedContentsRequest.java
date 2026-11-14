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

import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ContainedStorageRequestLayout;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;

final class SophisticatedContentsRequest {
    private SophisticatedContentsRequest() {}

    static void send(ItemStack root, UUID id, boolean backpack) {
        ItemStackReference.find(Minecraft.getInstance().player, root)
                .ifPresent(
                        reference ->
                                PacketWire.sendToServer(
                                        ContainedStorageRequestLayout.encode(
                                                reference.containerId(),
                                                reference.slot(),
                                                id,
                                                backpack)));
    }
}
