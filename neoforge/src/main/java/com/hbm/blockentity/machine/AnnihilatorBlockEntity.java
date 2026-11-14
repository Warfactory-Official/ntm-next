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
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.AnnihilatorMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.EffectAnnihilatorFlameLayout;
import com.hbm.saveddata.AnnihilatorSavedData;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Utf8String;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class AnnihilatorBlockEntity extends MachineBlockEntity
        implements MenuProvider, FluidTankHandler, FluidTankAccess, SyncUnitSchema {

    public static final int SLOT_TRASH = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_PAYOUT_START = 2;
    public static final int SLOT_PAYOUT_END = 7;
    public static final int SLOT_MONITOR = 8;
    public static final int SLOT_PAYOUT_REQUEST = 9;
    public static final int SLOT_PAYOUT_RESULT = 10;
    public static final int SLOT_COUNT = 11;
    public static final int TANK_CAPACITY = 2_500_000;

    private static final int[] AUTOMATED_SLOTS = {0, 2, 3, 4, 5, 6, 7};

    public final FluidTankNTM tank = new FluidTankNTM(TANK_CAPACITY);
    private final FluidTankNTM[] tanks = {tank};
    private ItemStack monitorSelection = ItemStack.EMPTY;
    private @Nullable Object monitorKey;

    public @Nullable String pool;

    {
        hbm$syncSet$AnnihilatorBlockEntity$pool("Recycling");
    }

    public BigInteger monitorAmount;

    {
        hbm$syncSet$AnnihilatorBlockEntity$monitorAmount(BigInteger.ZERO);
    }

    public AnnihilatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANNIHILATOR.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        unpackLootTable(null);
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();

        var selectedPool = pool;
        if (selectedPool != null && !selectedPool.isEmpty()) {
            var server = (ServerLevel) level;
            var ledger = AnnihilatorSavedData.get(server);
            boolean destroyed = false;

            var trash = getItem(SLOT_TRASH);
            if (!trash.isEmpty()) {
                releaseRadiation(server, trash);
                offerNormalReward(ledger.pushToPool(selectedPool, trash, false));
                setItem(SLOT_TRASH, ItemStack.EMPTY);
                destroyed = true;
            }

            if (tank.getFill() > 0) {
                FluidReleaseType.BURN.dispatch(
                        server, worldPosition, tank.getFluid(), tank, tank.getFill() * 2);
                offerNormalReward(
                        ledger.pushToPool(selectedPool, tank.getFluid(), tank.getFill(), false));
                tank.setFill(0);
                setChanged();
                destroyed = true;
            }

            if (destroyed) emitExhaust(server);
            refreshMonitor(ledger, selectedPool);
            processRequest(server, ledger, selectedPool);
        }
        networkPackNT(25);
    }

    private void offerNormalReward(@Nullable ItemStack reward) {
        if (reward == null) return;
        int firstEmpty = -1;
        for (int slot = SLOT_PAYOUT_START; slot <= SLOT_PAYOUT_END; slot++) {
            var destination = inventory.get(slot);
            if (destination.isEmpty()) {
                if (firstEmpty < 0) firstEmpty = slot;
            } else if (fullyMergeable(destination, reward)) {
                destination.grow(reward.getCount());
                inventory.set(slot, destination);
                setChanged();
                return;
            }
        }
        if (firstEmpty >= 0) {
            inventory.set(firstEmpty, reward);
            setChanged();
        }
    }

    private static boolean fullyMergeable(ItemStack destination, ItemStack reward) {
        return ItemStack.isSameItemSameComponents(destination, reward)
                && destination.getCount() + reward.getCount() <= destination.getMaxStackSize();
    }

    private void refreshMonitor(AnnihilatorSavedData ledger, String selectedPool) {
        var selection = getItem(SLOT_MONITOR);
        if (selection.isEmpty()) return;

        if (!ItemStack.isSameItemSameComponents(selection, monitorSelection)) {
            monitorSelection = selection.copyWithCount(1);
            if (selection.getItem() instanceof FluidIdentifierItem) {
                monitorKey =
                        selection
                                .getOrDefault(
                                        ModDataComponents.FLUID_IDENTIFIER.get(),
                                        FluidIdentifierData.EMPTY)
                                .primary();
            } else {
                monitorKey = AnnihilatorRecipes.StackKey.of(monitorSelection);
            }
        }

        BigInteger amount = BigInteger.ZERO;
        var worldPool = ledger.pools.get(selectedPool);
        if (worldPool != null) amount = worldPool.items.getOrDefault(monitorKey, BigInteger.ZERO);
        if (!monitorAmount.equals(amount)) hbm$syncSet$AnnihilatorBlockEntity$monitorAmount(amount);
    }

    private void processRequest(
            ServerLevel server, AnnihilatorSavedData ledger, String selectedPool) {
        var request = getItem(SLOT_PAYOUT_REQUEST);
        if (request.isEmpty()) return;

        var singleItem = request.copyWithCount(1);
        releaseRadiation(server, singleItem);
        var reward = ledger.pushToPool(selectedPool, singleItem, true);
        request.shrink(1);
        inventory.set(SLOT_PAYOUT_REQUEST, request);
        setChanged();
        if (reward == null) return;

        var destination = inventory.get(SLOT_PAYOUT_RESULT);
        if (destination.isEmpty()) {
            inventory.set(SLOT_PAYOUT_RESULT, reward);
            setChanged();
        } else if (fullyMergeable(destination, reward)) {
            destination.grow(reward.getCount());
            inventory.set(SLOT_PAYOUT_RESULT, destination);
            setChanged();
        }
    }

    private void releaseRadiation(ServerLevel server, ItemStack stack) {
        double hazard = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        if (hazard > 0D) {
            var facing = MultiblockCoreBlock.coreFacing(getBlockState());
            var position =
                    new BlockPos(
                            worldPosition.getX() - facing.getStepX() * 3,
                            worldPosition.getY() + 9,
                            worldPosition.getZ() - facing.getStepZ() * 3);
            RadiationSystemNT.incrementRad(server, position, Math.min(hazard * 5D, 1000D));
        }
    }

    private void emitExhaust(ServerLevel server) {
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        double x = worldPosition.getX() + 0.5D - facing.getStepX() * 3;
        double y = worldPosition.getY() + 8.75D;
        double z = worldPosition.getZ() + 0.5D - facing.getStepZ() * 3;
        PacketWire.sendToAllAround(
                EffectAnnihilatorFlameLayout.encode(x, y, z),
                new TargetPoint(server, x, y, z, 150D));
        if (TickPhase.every(this, 3)) {
            float pitch = 0.5F + server.getRandom().nextFloat() * 0.25F;
            WorldSounds.machine(
                    server,
                    null,
                    x,
                    y,
                    z,
                    ModSounds.FLAMETHROWER_SHOOT.get(),
                    SoundSource.BLOCKS,
                    1F,
                    pitch);
        }
    }

    public void setPool(String name) {
        if (!name.isEmpty() && !name.equals(pool)) {
            hbm$syncSet$AnnihilatorBlockEntity$pool(name);
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_TRASH, SLOT_MONITOR, SLOT_PAYOUT_REQUEST -> true;
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_TRASH;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_PAYOUT_START && slot <= SLOT_PAYOUT_END;
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
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    public AnnihilatorMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AnnihilatorMenu(containerId, playerInventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.annihilator");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
        hbm$syncSet$AnnihilatorBlockEntity$pool(input.getStringOr("pool", pool));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putString("pool", pool == null ? "" : pool);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> Utf8String.write(output, pool == null ? "" : pool, 32767);
            case 1 -> {
                byte[] bytes = monitorAmount.toByteArray();
                output.writeInt(bytes.length);
                output.writeBytes(bytes);
            }
            default -> throw new IllegalArgumentException("Unknown recycler sync unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$AnnihilatorBlockEntity$pool(Utf8String.read(input, 32767));
            case 1 -> {
                int count = input.readInt();
                if (count <= 0 || count > input.readableBytes()) {
                    throw new IllegalArgumentException("Invalid monitor amount byte count");
                }
                byte[] bytes = new byte[count];
                input.readBytes(bytes);
                hbm$syncSet$AnnihilatorBlockEntity$monitorAmount(new BigInteger(bytes));
            }
            default -> throw new IllegalArgumentException("Unknown recycler sync unit " + unit);
        }
    }

    public final void hbm$syncSet$AnnihilatorBlockEntity$pool(String next) {
        if (this.pool != next) {
            boolean changed = !java.util.Objects.equals(this.pool, next);
            this.pool = next;
            if (changed)
                if ((1L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$AnnihilatorBlockEntity$monitorAmount(BigInteger next) {
        if (this.monitorAmount != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.monitorAmount);
            this.monitorAmount = next;
            if (bound) bindSyncValue(next, 3, 2L);
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(AnnihilatorBlockEntity.class, "pool", 3, 1L);
        com.hbm.packet.SyncBindings.field(AnnihilatorBlockEntity.class, "monitorAmount", 3, 2L);
    }
}
