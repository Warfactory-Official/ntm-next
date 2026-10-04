// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public interface ItemInteriorAdapter {
    boolean handles(Class<?> itemClass);

    @Nullable UUID reference(ItemInstance stack);

    ItemInteriorBackend backend(MinecraftServer server, UUID id, ItemStackTemplate prototype);
}
