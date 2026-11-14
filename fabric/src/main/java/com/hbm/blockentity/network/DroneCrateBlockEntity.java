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

package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLane;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.DeliveryDroneEntity;
import com.hbm.inventory.container.DroneCrateMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class DroneCrateBlockEntity extends MachineBlockEntity
        implements DroneContainer,
                FluidTankHandler,
                FluidSettingsHandler,
                MenuProvider,
                SyncUnitSchema {

    public static final int CARGO_COUNT = 18;
    public static final int IDENTIFIER_SLOT = CARGO_COUNT;
    public static final int SLOT_COUNT = CARGO_COUNT + 1;
    public static final int OUTPUT_PERIOD = 20;

    private static final int[] CARGO_INDICES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
    };
    private static final FlushFaces OUTPUT_FACES = FlushFaces.sixFaces();

    public final FluidTankNTM tank = new FluidTankNTM(64_000).storesComponents();
    public @Nullable BlockPos successor;
    public boolean sending;
    public boolean itemCargo;

    {
        hbm$syncSet$DroneCrateBlockEntity$itemCargo(true);
    }

    private final FluidTankNTM[] tanks = {tank};
    private final FluidFlushOutputs output = new FluidFlushOutputs();
    private final AABB arrivalBox;

    public DroneCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_CRATE.get(), pos, state, SLOT_COUNT);
        arrivalBox = new AABB(pos.above());
    }

    @Override
    public void tickServer() {
        if (tank.setType(IDENTIFIER_SLOT, IDENTIFIER_SLOT, inventory)) setChanged();
        output.provide((ServerLevel) level, this);
        var next = successor;
        if (next != null) {
            for (var drone : level.getEntitiesOfClass(DeliveryDroneEntity.class, arrivalBox)) {
                if (drone.getDeltaMovement().length() >= 0.05) continue;
                drone.setTarget(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
                if (itemCargo) {
                    if (sending) loadItems(drone);
                    else receiveItems(drone);
                } else {
                    if (sending) loadFluid(drone);
                    else receiveFluid(drone);
                }
            }
            ParticleCreators.droneLine(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    next.getX() - worldPosition.getX(),
                    next.getY() - worldPosition.getY() - 1,
                    next.getZ() - worldPosition.getZ(),
                    0x00ffff,
                    150);
        }
        networkPackNT(25);
    }

    private void loadItems(DeliveryDroneEntity drone) {
        if (drone.getAppearance() != 0) return;
        boolean moved = false;
        for (int slot = 0; slot < CARGO_COUNT; slot++) {
            var stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            drone.setItem(slot, stack.copy());
            inventory.set(slot, ItemStack.EMPTY);
            moved = true;
        }
        if (moved) {
            drone.setAppearance(1);
            setChanged();
            unpackSound();
        }
    }

    private void receiveItems(DeliveryDroneEntity drone) {
        if (drone.getAppearance() != 1) return;
        boolean moved = false;
        boolean empty = true;
        for (int slot = 0; slot < CARGO_COUNT; slot++) {
            var carried = drone.getItem(slot);
            if (carried.isEmpty()) continue;
            int before = carried.getCount();
            var remainder =
                    InventoryUtil.tryAddItemToInventory(inventory, 0, CARGO_COUNT - 1, carried);
            moved |= remainder.getCount() != before;
            empty &= remainder.isEmpty();
            drone.setItem(slot, remainder);
        }
        if (moved) setChanged();
        if (empty) {
            drone.setAppearance(0);
            unpackSound();
        }
    }

    private void loadFluid(DeliveryDroneEntity drone) {
        if (drone.getAppearance() != 0 || tank.getFill() == 0) return;
        drone.fluid = new FluidStackNTM(tank.getFluid(), tank.getFill(), 0, tank.getComponents());
        tank.setFill(0);
        drone.setAppearance(2);
        setChanged();
        unpackSound();
    }

    private void receiveFluid(DeliveryDroneEntity drone) {
        if (drone.getAppearance() != 2) return;
        var cargo = drone.fluid;
        if (cargo == null || !tank.accepts(cargo.type(), cargo.components())) return;
        int moved =
                tank.receive(
                        cargo.type(),
                        cargo.components(),
                        (int) Math.min(cargo.amount(), Integer.MAX_VALUE));
        long remaining = cargo.amount() - moved;
        if (remaining == 0) {
            drone.fluid = null;
            drone.setAppearance(0);
        } else if (moved != 0 || cargo.pressure() != 0) {
            drone.fluid = new FluidStackNTM(cargo.type(), remaining, 0, cargo.components());
        }
        if (moved != 0) setChanged();
        unpackSound();
    }

    private void unpackSound() {
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.ITEM_UNPACK.get(),
                SoundSource.BLOCKS,
                0.5F,
                0.75F);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return CARGO_INDICES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < CARGO_COUNT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < CARGO_COUNT;
    }

    @Override
    public BlockPos hoverPoint() {
        return worldPosition.above();
    }

    @Override
    public void linkNext(BlockPos next) {
        if (next.equals(successor)) return;
        hbm$syncSet$DroneCrateBlockEntity$successor(next.immutable());
        setChanged();
    }

    public void toggleSending() {
        hbm$syncSet$DroneCrateBlockEntity$sending(!sending);
        setChanged();
    }

    public void toggleCargoType() {
        hbm$syncSet$DroneCrateBlockEntity$itemCargo(!itemCargo);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        return !itemCargo
                        && sending
                        && pressure == tank.getPressure()
                        && tank.accepts(type, components)
                ? tank.getMaxFill() - tank.getFill()
                : 0;
    }

    @Override
    public long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        return !itemCargo
                        && !sending
                        && pressure == tank.getPressure()
                        && tank.provides(type, components)
                ? tank.getFill()
                : 0;
    }

    @Override
    public long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        if (!tank.accepts(type, components)) return amount;
        int moved = tank.receive(type, components, (int) Math.min(amount, Integer.MAX_VALUE));
        if (moved != 0) setChanged();
        return amount - moved;
    }

    @Override
    public void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        int before = tank.getFill();
        tank.setFill(before - amount);
        if (tank.getFill() != before) setChanged();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                new FlushLane(this, tank, OUTPUT_FACES)
                        .every(OUTPUT_PERIOD)
                        .onlyWhen(this::outputsFluid));
    }

    private boolean outputsFluid() {
        return !itemCargo && !sending;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$DroneCrateBlockEntity$successor(
                input.read("next", BlockPos.CODEC).orElse(null));
        hbm$syncSet$DroneCrateBlockEntity$sending(input.getBooleanOr("mode", false));
        hbm$syncSet$DroneCrateBlockEntity$itemCargo(input.getBooleanOr("type", false));
        input.child("t").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (successor != null) output.store("next", BlockPos.CODEC, successor);
        output.putBoolean("mode", sending);
        output.putBoolean("type", itemCargo);
        tank.serialize(output.child("t"));
    }

    @Override
    protected boolean syncName() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 15;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> tank.packetSerialize(output);
            case 1 -> {
                var next = successor;
                output.writeBoolean(next != null);
                if (next != null) {
                    output.writeInt(next.getX());
                    output.writeInt(next.getY());
                    output.writeInt(next.getZ());
                }
            }
            case 2 -> output.writeBoolean(sending);
            case 3 -> output.writeBoolean(itemCargo);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> tank.packetDeserialize(input);
            case 1 ->
                    hbm$syncSet$DroneCrateBlockEntity$successor(
                            input.readBoolean()
                                    ? new BlockPos(
                                            input.readInt(), input.readInt(), input.readInt())
                                    : null);
            case 2 -> hbm$syncSet$DroneCrateBlockEntity$sending(input.readBoolean());
            case 3 -> hbm$syncSet$DroneCrateBlockEntity$itemCargo(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneCrate");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneCrateMenu(containerId, inventory, this);
    }

    public final void hbm$syncSet$DroneCrateBlockEntity$successor(BlockPos next) {
        if (this.successor != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.successor);
            this.successor = next;
            if (bound) bindSyncValue(next, 3, 2);
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$DroneCrateBlockEntity$sending(boolean next) {
        if (this.sending != next) {
            this.sending = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$DroneCrateBlockEntity$itemCargo(boolean next) {
        if (this.itemCargo != next) {
            this.itemCargo = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DroneCrateBlockEntity.class, "tank", 3, 1);
        com.hbm.packet.SyncBindings.field(DroneCrateBlockEntity.class, "successor", 3, 2);
    }
}
