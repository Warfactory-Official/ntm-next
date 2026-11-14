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
import com.hbm.inventory.container.CompactLauncherMenu;
import com.hbm.items.weapon.CustomMissilePartItem.FuelType;
import com.hbm.items.weapon.CustomMissilePartItem.PartSize;
import com.hbm.registration.Registrar;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class CompactLauncherBlockEntity extends MissileAssemblyLauncherBlockEntity {

    public static final int SOLID_CAPACITY = 25_000;
    public static final int TANK_CAPACITY = 25_000;
    public static final PartSize SUPPORTED_SIZE = PartSize.SIZE_10;

    private static final List<FuelAssociation> ADDITIONAL_FUELS = new ArrayList<>();

    public CompactLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.SMALL_LAUNCHER.get(),
                pos,
                state,
                SOLID_CAPACITY,
                TANK_CAPACITY,
                1,
                0.5F,
                true,
                2);
    }

    public static void registerFuelAssociation(FuelAssociation association) {
        Registrar.assertOpen("compact launcher fuel");
        ADDITIONAL_FUELS.add(association);
    }

    public record FuelAssociation(
            FuelType type, Supplier<Fluid> propellant, @Nullable Supplier<Fluid> oxidizer) {}

    @Override
    protected PartSize acceptedSize() {
        return SUPPORTED_SIZE;
    }

    @Override
    protected @Nullable FuelAssociation registeredFuel(FuelType type) {
        for (int index = 0; index < ADDITIONAL_FUELS.size(); index++) {
            FuelAssociation association = ADDITIONAL_FUELS.get(index);
            if (association.type() == type) return association;
        }
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.compactLauncher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CompactLauncherMenu(containerId, inventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(storedEnergy);
            case 1 -> output.writeInt(solidPropellant);
            case 2 -> propellantTank.packetSerialize(output);
            case 3 -> oxidizerTank.packetSerialize(output);
            case 4 -> writeAssembly(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(input.readLong());
            case 1 ->
                    hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(input.readInt());
            case 2 -> propellantTank.packetDeserialize(input);
            case 3 -> oxidizerTank.packetDeserialize(input);
            case 4 -> readAssembly(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                CompactLauncherBlockEntity.class,
                new int[] {MissileAssemblyLauncherBlockEntity.MISSILE_SLOT},
                1L << 4,
                false,
                true);
    }
}
