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

package com.hbm.blockentity.ordnance.launch;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.LaunchTableMenu;
import com.hbm.items.weapon.CustomMissilePartItem.PartSize;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class LaunchTableBlockEntity extends MissileAssemblyLauncherBlockEntity {

    public static final int SOLID_CAPACITY = 100_000;
    public static final int TANK_CAPACITY = 100_000;

    public PartSize selectedSize;

    {
        hbm$syncSet$LaunchTableBlockEntity$selectedSize(PartSize.SIZE_10);
    }

    public boolean assemblyFits;
    public int lastTowerHeight = 10;

    public LaunchTableBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_LAUNCH_TABLE.get(),
                pos,
                state,
                SOLID_CAPACITY,
                TANK_CAPACITY,
                4,
                0.65F,
                false,
                4);
    }

    @Override
    protected PartSize acceptedSize() {
        return selectedSize;
    }

    @Override
    protected void updateAssemblyPresentation() {
        hbm$syncSet$LaunchTableBlockEntity$assemblyFits(acceptsAssembly());
    }

    public void selectSize(PartSize size) {
        if (selectedSize == size) return;
        hbm$syncSet$LaunchTableBlockEntity$selectedSize(size);
        setChanged();
    }

    public void selectSizeOrdinal(int ordinal) {
        var sizes = PartSize.values();
        if (ordinal < 0 || ordinal >= sizes.size()) return;
        selectSize(sizes.get(ordinal));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchTable");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LaunchTableMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$LaunchTableBlockEntity$selectedSize(
                PartSize.byName(input.getStringOr("padSize", ""), PartSize.SIZE_10));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("padSize", selectedSize.name());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(storedEnergy);
            case 1 -> output.writeInt(solidPropellant);
            case 2 -> output.writeByte(selectedSize.ordinal());
            case 3 -> output.writeBoolean(assemblyFits);
            case 4 -> propellantTank.packetSerialize(output);
            case 5 -> oxidizerTank.packetSerialize(output);
            case 6 -> writeAssembly(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(input.readLong());
            case 1 ->
                    hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(input.readInt());
            case 2 ->
                    hbm$syncSet$LaunchTableBlockEntity$selectedSize(
                            PartSize.values().get(input.readByte()));
            case 3 -> hbm$syncSet$LaunchTableBlockEntity$assemblyFits(input.readBoolean());
            case 4 -> propellantTank.packetDeserialize(input);
            case 5 -> oxidizerTank.packetDeserialize(input);
            case 6 -> readAssembly(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$LaunchTableBlockEntity$selectedSize(PartSize next) {
        if (this.selectedSize != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.selectedSize);
            this.selectedSize = next;
            if (bound) bindSyncValue(next, 3, 1L << 2);
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$LaunchTableBlockEntity$assemblyFits(boolean next) {
        if (this.assemblyFits != next) {
            this.assemblyFits = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(LaunchTableBlockEntity.class, "selectedSize", 3, 1L << 2);
        com.hbm.packet.SyncInventory.slots(
                LaunchTableBlockEntity.class,
                new int[] {MissileAssemblyLauncherBlockEntity.MISSILE_SLOT},
                1L << 6,
                false,
                true);
    }
}
