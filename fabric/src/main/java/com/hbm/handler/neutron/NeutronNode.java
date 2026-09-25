// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.neutron;

import com.hbm.handler.neutron.NeutronStream.NeutronType;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public abstract class NeutronNode {

    private final Map<Key<?>, Object> data = new HashMap<>();
    protected NeutronType type;
    protected BlockPos pos;
    protected BlockEntity tile;

    public NeutronNode(BlockEntity tile, NeutronType type) {
        this.type = type;
        this.tile = tile;
        this.pos = tile.getBlockPos();
    }

    public <T> void set(Key<T> key, T value) {
        data.put(key, value);
    }

    public <T> @Nullable T get(Key<T> key) {
        return key.type().cast(data.get(key));
    }

    public record Key<T>(String name, Class<T> type) {}
}
