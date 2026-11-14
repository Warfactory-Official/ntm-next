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

package com.hbm.inventory.container;

import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityMenu<BE extends BlockEntity> extends NtmContainerMenu
        implements UpgradeTooltipMenu {

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

    @Override
    public void appendUpgradeTooltip(MachineUpgradeItem upgrade, List<Component> tooltip) {
        if (blockEntity instanceof UpgradeInfoProvider provider) {
            UpgradeInfo.append(blockEntity.getBlockState().getBlock(), provider, upgrade, tooltip);
        }
    }
}
