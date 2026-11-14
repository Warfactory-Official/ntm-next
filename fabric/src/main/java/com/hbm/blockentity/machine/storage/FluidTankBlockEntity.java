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

import com.google.common.base.Suppliers;
import com.hbm.api.fluidmk2.ComponentFluidProvider;
import com.hbm.api.fluidmk2.ComponentFluidReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.FluidTankBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.interfaces.Extinguishable;
import com.hbm.interfaces.OverpressureHandler;
import com.hbm.interfaces.RepairHandler;
import com.hbm.inventory.container.FluidTankMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FluidTankBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                FluidFlushSender,
                ComponentFluidProvider,
                ComponentFluidReceiver,
                FluidSettingsHandler,
                PersistentDrop,
                OverpressureHandler,
                RepairHandler,
                Extinguishable,
                FluidTankHazard.Host,
                SyncUnitSchema,
                RadioValueProvider,
                RadioControlHandler,
                MenuProvider {

    public static final int CAPACITY = 256_000;
    public static final int SLOT_IDENTIFIER = 0;
    public static final int SLOT_IDENTIFIER_OUT = 1;
    public static final int SLOT_LOAD = 2;
    public static final int SLOT_LOAD_OUT = 3;
    public static final int SLOT_UNLOAD = 4;
    public static final int SLOT_UNLOAD_OUT = 5;
    public static final int SLOT_COUNT = 6;

    private static final long RATE_FLOOR = 500L;
    private static final String FUN_SET_MODE = RadioCommandParser.FUN + "setmode";
    private static final String[] ROR = {
        RadioCommandParser.VAL + "type",
        RadioCommandParser.VAL + "fill",
        RadioCommandParser.VAL + "fillpercent",
        FUN_SET_MODE + "!mode (0-3)",
        FUN_SET_MODE + "!mode:fallback (0-3)"
    };
    private static final String[] PERSISTENT_KEYS = {"tank", "mode", "onFire"};
    private static final FluidTankNTM[] NONE = {};
    private static final Supplier<List<CountIngredient>> REPAIR_COST =
            Suppliers.memoize(
                    () -> List.of(CountIngredient.of(MaterialTagRegistry.STEEL.plate(), 6)));

    private TransferMode mode;

    {
        hbm$syncSet$FluidTankBlockEntity$mode(TransferMode.RECEIVE);
    }

    public final FluidTankNTM tank;
    private final FluidTankNTM[] tanks;
    public final FluidTankHazard hazard;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public @Nullable Explosion lastHitBy;
    private @Nullable Fluid bufferFluid;
    private int comparator;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, SLOT_COUNT);
        tank =
                new FluidTankNTM(CAPACITY) {
                    @Override
                    protected void declaredFluidChanged(@Nullable Fluid previous) {
                        FluidTankBlockEntity.this.declaredFluidChanged();
                    }
                }.storesComponents();
        tanks = new FluidTankNTM[] {tank};
        hazard = new FluidTankHazard(this, tank);
    }

    @Override
    public void tickServer() {
        ServerLevel server = (ServerLevel) level;
        if (!isDamaged()) {
            if (mode == TransferMode.SEND) flush.provide(server, this);
            boolean moved = tank.loadTank(SLOT_LOAD, SLOT_LOAD_OUT, inventory);
            moved |= tank.setType(SLOT_IDENTIFIER, SLOT_IDENTIFIER_OUT, inventory);
            if (moved || TickPhase.every(this, 20)) setChanged();
        }
        int now = StorageComparatorSignal.rising(tank.getFill(), tank.getMaxFill());
        if (now != comparator) {
            comparator = now;
            setChanged();
            ((MultiblockCoreBlock) getBlockState().getBlock())
                    .updateComparatorOutput(server, worldPosition);
        }
        hazard.tickServer();
        if (isRemoved()) return;
        if (tank.unloadTank(SLOT_UNLOAD, SLOT_UNLOAD_OUT, inventory)) setChanged();
        networkPackNT(150);
    }

    @Override
    public int getComparatorPower() {
        return StorageComparatorSignal.rising(tank.getFill(), tank.getMaxFill());
    }

    @Override
    public boolean isDamaged() {
        return getBlockState().getValue(FluidTankBlock.DAMAGED);
    }

    @Override
    public void setDamaged(boolean damaged) {
        BlockState state = getBlockState();
        if (state.getValue(FluidTankBlock.DAMAGED) == damaged) return;
        level.setBlock(
                worldPosition,
                state.setValue(FluidTankBlock.DAMAGED, damaged),
                Block.UPDATE_CLIENTS);
        refreshBufferNodes();
    }

    public void explode() {
        hazard.explode();
    }

    @Override
    public void explode(Level level, BlockPos pos) {
        if (!isDamaged()) explode();
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return REPAIR_COST.get();
    }

    @Override
    public void repair(Player player) {
        setDamaged(false);
        setChanged();
    }

    @Override
    public void tryExtinguish(Level level, BlockPos pos, Agent agent) {
        hazard.tryExtinguish(level, agent);
    }

    public TransferMode getMode() {
        return mode;
    }

    public void setMode(TransferMode next) {
        if (next == mode) return;
        hbm$syncSet$FluidTankBlockEntity$mode(next);
        setChanged();
        refreshBufferNodes();
    }

    public void cycleMode() {
        setMode(mode.next());
    }

    public boolean connectsFluid(Fluid fluid) {
        return tank.joins(fluid);
    }

    private @Nullable Fluid wantedBufferFluid() {
        Fluid fluid = tank.getDeclaredFluid();
        return mode == TransferMode.BUFFER && !isDamaged() && fluid != null && fluid != Fluids.EMPTY
                ? fluid
                : null;
    }

    private void declaredFluidChanged() {
        if (!(level instanceof ServerLevel) || isRemoved()) return;
        flush.invalidate();
        refreshBufferNodes();
    }

    private void refreshBufferNodes() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        Fluid wanted = wantedBufferFluid();
        if (wanted == bufferFluid) return;
        if (bufferFluid != null) FluidBufferCells.leave(server, bufferCells());
        if (wanted != null) FluidBufferCells.join(server, worldPosition, bufferCells(), wanted);
        bufferFluid = wanted;
    }

    private Long2IntOpenHashMap bufferCells() {
        return FluidBufferCells.activePlane(worldPosition, getBlockState());
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        Fluid wanted = wantedBufferFluid();
        if (wanted != null) FluidBufferCells.join(server, worldPosition, bufferCells(), wanted);
        else FluidBufferCells.leave(server, bufferCells());
        bufferFluid = wanted;
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        if (bufferFluid != null) FluidBufferCells.leave(server, bufferCells());
        bufferFluid = null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) onGraphDetach(server);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        if (isDamaged()
                || !mode.receives()
                || tank.getPressure() != pressure
                || !tank.accepts(type, components)) {
            return 0L;
        }
        return tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        long demand = getDemand(type, components, pressure);
        if (demand <= 0L) return amount;
        int added =
                tank.receive(
                        type,
                        components,
                        (int) Math.min(Math.min(amount, demand), Integer.MAX_VALUE));
        setChanged();
        return amount - added;
    }

    @Override
    public long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        if (isDamaged()
                || !mode.sends()
                || tank.getPressure() != pressure
                || !tank.provides(type, components)) {
            return 0L;
        }
        return tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        long available = getFluidAvailable(type, components, pressure);
        if (available <= 0L) return;
        tank.setFill(tank.getFill() - (int) Math.min(amount, available));
        setChanged();
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return !isDamaged() && mode.sends() ? Math.max(RATE_FLOOR, tank.getFill() / 100L) : 0L;
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return !isDamaged() && mode.receives()
                ? Math.max(RATE_FLOOR, (tank.getMaxFill() - tank.getFill()) / 100L)
                : 0L;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return !isDamaged() && mode.sends() ? tanks : NONE;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tank, FlushFaces.activePlane());
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tank);
    }

    @Override
    public void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor) {
        DataComponentPatch patch = tank.getComponents();
        if (getSendingTanks().length > 0
                && !patch.isEmpty()
                && tank.getFill() > 0
                && tank.getFluid() == type) {
            visitor.accept(patch);
        }
    }

    @Override
    public int providingPressureMask(Fluid type, DataComponentPatch components) {
        return tank.provides(type, components) ? 1 << tank.getPressure() : 0;
    }

    @Override
    public int receivingPressureMask(Fluid type, DataComponentPatch components) {
        return tank.accepts(type, components) ? 1 << tank.getPressure() : 0;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return mode == TransferMode.BUFFER ? ConnectionPriority.LOW : ConnectionPriority.NORMAL;
    }

    @Override
    public boolean bufferedEndpoint() {
        return !isDamaged() && mode == TransferMode.BUFFER;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_IDENTIFIER -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_LOAD, SLOT_UNLOAD -> FluidTankNTM.isFluidContainer(stack);
            default -> false;
        };
    }

    @Override
    public @Nullable FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public String[] settingsFluidIds() {
        Fluid fluid = tank.getDeclaredFluid();
        return new String[] {
            BuiltInRegistries.FLUID.getKey(fluid == null ? Fluids.EMPTY : fluid).toString()
        };
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case RadioCommandParser.VAL + "type" -> NTMFluids.legacyName(tank.getTankType());
            case RadioCommandParser.VAL + "fill" -> Integer.toString(tank.getFill());
            case RadioCommandParser.VAL + "fillpercent" ->
                    Long.toString(100L * tank.getFill() / tank.getMaxFill());
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (FUN_SET_MODE.equals(key)) setMode(TransferMode.chosen(mode, params));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fluidtank");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidTankMenu(containerId, inventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        boolean damaged = isDamaged();
        if (tank.getFill() <= 0 && !damaged) return;
        Fluid fluid = tank.getFluid();
        FluidStackNTM content =
                new FluidStackNTM(
                        fluid == null ? NTMFluids.NONE : fluid,
                        tank.getFill(),
                        tank.getPressure(),
                        tank.getComponents());
        components.set(
                ModDataComponents.FLUID_TANK_CONTENTS.get(),
                new FluidTankContents(
                        content, tank.getMaxFill(), mode.code(), damaged, hazard.isOnFire()));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        FluidTankContents carried = components.get(ModDataComponents.FLUID_TANK_CONTENTS.get());
        if (carried == null) return;
        FluidStackNTM content = carried.tank();
        tank.changeTankSize(carried.capacity());
        tank.setTankTypeByIdentifier(content.type());
        tank.withPressure(content.pressure());
        if (content.type() != NTMFluids.NONE) {
            tank.receive(
                    content.type(),
                    content.components(),
                    (int) Math.min(content.amount(), Integer.MAX_VALUE));
        }
        setMode(TransferMode.of(carried.mode()));
        hazard.setOnFire(carried.onFire());
        setDamaged(carried.damaged());
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FluidTankBlockEntity$mode(
                TransferMode.of(input.getIntOr("mode", TransferMode.RECEIVE.code())));
        input.child("tank").ifPresent(tank::deserialize);
        hazard.load(input);
        flush.invalidate();
        refreshBufferNodes();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.code());
        tank.serialize(output.child("tank"));
        hazard.save(output);
    }

    @Override
    public long syncUnitMask() {
        return 0b11L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(mode.code());
            case 1 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException("fluid tank sync unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$FluidTankBlockEntity$mode(TransferMode.of(input.readInt()));
            case 1 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException("fluid tank sync unit " + unit);
        }
    }

    public final void hbm$syncSet$FluidTankBlockEntity$mode(TransferMode next) {
        if (this.mode != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.mode);
            this.mode = next;
            if (bound) bindSyncValue(next, 3, 1L);
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FluidTankBlockEntity.class, "mode", 3, 1L);
        com.hbm.packet.SyncBindings.field(FluidTankBlockEntity.class, "tank", 3, 1L << 1);
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
