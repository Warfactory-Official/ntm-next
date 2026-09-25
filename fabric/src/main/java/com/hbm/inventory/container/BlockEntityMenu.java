// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityMenu<BE extends BlockEntity> extends NtmContainerMenu {

    private final BE blockEntity;

    protected BlockEntityMenu(MenuType<?> menuType, int containerId, BE blockEntity) {
        this(menuType, containerId, blockEntity, blockEntity instanceof Container c ? c : null);
    }

    protected BlockEntityMenu(
            MenuType<?> menuType, int containerId, BE blockEntity, @Nullable Container container) {
        super(menuType, containerId, container);
        this.blockEntity = blockEntity;
    }

    public final BE blockEntity() {
        return blockEntity;
    }
}
