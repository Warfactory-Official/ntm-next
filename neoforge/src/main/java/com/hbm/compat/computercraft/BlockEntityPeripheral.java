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

package com.hbm.compat.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityPeripheral<BE extends BlockEntity> implements IPeripheral {

    protected final BE target;
    private final String type;

    protected BlockEntityPeripheral(BE target, String type) {
        this.target = target;
        this.type = type;
    }

    protected final BE machine() {
        assert target.getLevel() != null
                        && target.getLevel().getServer() != null
                        && target.getLevel().getServer().isSameThread()
                : type + " touched its block entity off-thread";
        return target;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final Object getTarget() {
        return target;
    }

    @Override
    public final boolean equals(@Nullable IPeripheral other) {
        return other instanceof BlockEntityPeripheral<?> peripheral
                && peripheral.target == target
                && peripheral.getClass() == getClass();
    }
}
