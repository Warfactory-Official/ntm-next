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

package com.hbm.blockentity.machine.storage;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

public class CrateSupplyBlockEntity extends MachineBlockEntity implements PersistentDrop {

    private static final String[] PERSISTENT_KEYS = {"inventory"};

    private static final int SLOTS = 64;

    public CrateSupplyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPPLY_CRATE.get(), pos, state, SLOTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public void readPersistent(DataComponentGetter components) {
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
