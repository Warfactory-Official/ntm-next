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

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.WorldSounds;
import com.hbm.util.NtmMath;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class RefuelerBlockEntity extends MachineBlockEntity
        implements FluidTankAccess, FluidTankHandler, SyncUnitSchema {

    private static final EquipmentSlot[] EQUIPMENT = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    public static Consumer<RefuelerBlockEntity> CLIENT_PARTICLE = refueler -> {};

    private boolean operating;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.KEROSENE, 100);
    private final FluidTankNTM[] receiving = {tank};
    private int operatingTime;

    public double fillLevel;
    public double prevFillLevel;

    public RefuelerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFUELER.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$RefuelerBlockEntity$operating(false);
        AABB bounds =
                new AABB(
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        worldPosition.getX() + 1,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 1);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            for (EquipmentSlot slot : EQUIPMENT) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;

                if (fill(stack)) hbm$syncSet$RefuelerBlockEntity$operating(true);
                if (!ArmorModHandler.isArmor(stack) || !ArmorModHandler.hasMods(stack)) continue;

                var mods = ArmorModHandler.mods(stack);
                for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                    if (!(mods.item(socket) instanceof FluidContainerItem container)
                            || !container.machineFillable()) continue;
                    ItemStack mod = mods.copy(socket);
                    if (!fill(mod)) continue;
                    ArmorModHandler.applyMod(stack, mod);
                    hbm$syncSet$RefuelerBlockEntity$operating(true);
                }
            }
        }

        if (operating) {
            if (operatingTime % 20 == 0) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.2F,
                        0.5F);
            }
            operatingTime++;
            setChanged();
        } else {
            operatingTime = 0;
        }

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        if (operating) CLIENT_PARTICLE.accept(this);

        prevFillLevel = fillLevel;
        double target = (double) tank.getFill() / tank.getMaxFill();
        fillLevel =
                NtmMath.lerp(fillLevel, target, target > fillLevel || !operating ? 0.1F : 0.01F);
    }

    private boolean fill(ItemStack stack) {
        if (!(stack.getItem() instanceof FluidContainerItem container)
                || !container.machineFillable()) {
            return false;
        }
        Fluid fluid = tank.getFluid();
        if (fluid == null) return false;
        int accepted = container.fill(stack, fluid, tank.getFill(), tank.getPressure());
        if (accepted <= 0) return false;
        tank.setFill(tank.getFill() - accepted);
        return true;
    }

    public Direction receivingFace() {
        return getBlockState().getValue(HorizontalMachineBlock.FACING).getOpposite();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public int tankCount() {
        return receiving.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return receiving[index];
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(operating);
            case 1 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RefuelerBlockEntity$operating(input.readBoolean());
            case 1 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$RefuelerBlockEntity$operating(boolean next) {
        if (this.operating != next) {
            this.operating = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RefuelerBlockEntity.class, "tank", 3, 1L << 1);
    }
}
