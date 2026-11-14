/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.storage;

import com.hbm.api.fluidmk2.ComponentFluidProvider;
import com.hbm.api.fluidmk2.ComponentFluidReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.storage.FluidBarrelBlock;
import com.hbm.inventory.container.BarrelMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CorrosiveTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.TomSaveData;
import com.hbm.sound.WorldSounds;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FluidBarrelBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                FluidFlushSender,
                ComponentFluidProvider,
                ComponentFluidReceiver,
                FluidSettingsHandler,
                PersistentDrop,
                SyncUnitSchema,
                RadioValueProvider,
                RadioControlHandler,
                MenuProvider {

    public static final int SLOT_IDENTIFIER = 0;
    public static final int SLOT_IDENTIFIER_OUT = 1;
    public static final int SLOT_LOAD = 2;
    public static final int SLOT_LOAD_OUT = 3;
    public static final int SLOT_UNLOAD = 4;
    public static final int SLOT_UNLOAD_OUT = 5;
    public static final int SLOT_COUNT = 6;

    private static final int[] FACE_SLOTS = {
        SLOT_LOAD, SLOT_LOAD_OUT, SLOT_UNLOAD, SLOT_UNLOAD_OUT
    };
    private static final Direction[] ARM_SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final BooleanProperty[] ARMS = {
        FluidBarrelBlock.NORTH, FluidBarrelBlock.EAST, FluidBarrelBlock.SOUTH, FluidBarrelBlock.WEST
    };
    private static final String FUN_SET_MODE = RadioCommandParser.FUN + "setmode";
    private static final String[] ROR = {
        RadioCommandParser.VAL + "type",
        RadioCommandParser.VAL + "fill",
        RadioCommandParser.VAL + "fillpercent",
        FUN_SET_MODE + "!mode (0-3)",
        FUN_SET_MODE + "!mode:fallback (0-3)"
    };
    private static final String[] PERSISTENT_KEYS = {"tank", "mode"};
    private static final FluidTankNTM[] NONE = {};

    private TransferMode mode;

    {
        hbm$syncSet$FluidBarrelBlockEntity$mode(TransferMode.RECEIVE);
    }

    public final FluidTankNTM tank;
    private final FluidTankNTM[] tanks;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private @Nullable Fluid bufferFluid;

    public FluidBarrelBlockEntity(BlockPos pos, BlockState state) {
        this(
                ModBlockEntities.FLUID_BARREL.get(),
                pos,
                state,
                ((FluidBarrelBlock) state.getBlock()).capacity);
    }

    protected FluidBarrelBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, SLOT_COUNT);
        tank =
                new FluidTankNTM(capacity) {
                    @Override
                    protected void declaredFluidChanged(@Nullable Fluid previous) {
                        FluidBarrelBlockEntity.this.declaredFluidChanged();
                    }
                }.storesComponents();
        tanks = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        boolean moved = tank.setType(SLOT_IDENTIFIER, SLOT_IDENTIFIER_OUT, inventory);
        moved |= tank.loadTank(SLOT_LOAD, SLOT_LOAD_OUT, inventory);
        moved |= tank.unloadTank(SLOT_UNLOAD, SLOT_UNLOAD_OUT, inventory);
        if (moved) setChanged();
        if (mode == TransferMode.SEND && !leaning()) flush.provide((ServerLevel) level, this);
        if (tank.getFill() > 0) checkHazards();
        if (isRemoved()) return;
        networkPackNT(50);
    }

    protected boolean leaning() {
        return false;
    }

    protected void checkHazards() {
        Fluid type = tank.getTankType();
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        BlockState state = getBlockState();
        if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Amat.class)
                && !state.is(ModBlocks.BARREL_ANTIMATTER.get())) {
            level.destroyBlock(worldPosition, false);
            level.explode(null, x, y, z, 5F, true, Level.ExplosionInteraction.BLOCK);
            return;
        }
        if (state.is(ModBlocks.BARREL_PLASTIC.get())
                && (NTMFluidProperties.hasTrait(type, CorrosiveTrait.class) || hot(type))) {
            level.destroyBlock(worldPosition, false);
            WorldSounds.play(
                    level, null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
            return;
        }
        if (type == NTMFluids.WATER && level.getBrightness(LightLayer.SKY, worldPosition) > 7) {
            TomSaveData impact = TomSaveData.published((ServerLevel) level);
            if (impact != null && impact.fire > 1E-5F)
                level.explode(null, x, y, z, 5F, true, Level.ExplosionInteraction.BLOCK);
        }
    }

    private static boolean hot(Fluid type) {
        NTMFluidProperty property = NTMFluidProperties.get(type);
        return property != null && property.temperature() >= 100;
    }

    public TransferMode getMode() {
        return mode;
    }

    public void setMode(TransferMode next) {
        if (next == mode) return;
        hbm$syncSet$FluidBarrelBlockEntity$mode(next);
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
        return mode == TransferMode.BUFFER && fluid != null && fluid != Fluids.EMPTY ? fluid : null;
    }

    private void declaredFluidChanged() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        flush.invalidate();
        refreshBufferNodes();
        refreshArms(server, Block.UPDATE_CLIENTS);
        server.updateNeighborsAt(worldPosition, getBlockState().getBlock(), null);
    }

    private void refreshBufferNodes() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        Fluid wanted = wantedBufferFluid();
        if (wanted == bufferFluid) return;
        if (bufferFluid != null) leaveFluidGraph(server);
        if (wanted != null) joinFluidGraph(server, wanted);
        bufferFluid = wanted;
    }

    protected void joinFluidGraph(ServerLevel server, Fluid fluid) {
        FluidPipeGraph.get(server, fluid)
                .addMachineNode(worldPosition.asLong(), new PipeData(fluid), 0x3F, true);
    }

    protected void leaveFluidGraph(ServerLevel server) {
        FluidBufferCells.release(server, worldPosition.asLong());
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshArms(server, Block.UPDATE_CLIENTS);
        Fluid wanted = wantedBufferFluid();
        if (wanted != null) joinFluidGraph(server, wanted);
        else leaveFluidGraph(server);
        bufferFluid = wanted;
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        if (bufferFluid != null) leaveFluidGraph(server);
        bufferFluid = null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) onGraphDetach(server);
        super.preRemoveSideEffects(pos, state);
    }

    protected boolean hasArms() {
        return true;
    }

    public void refreshConnectors() {
        if (level instanceof ServerLevel server && !isRemoved())
            refreshArms(server, Block.UPDATE_CLIENTS);
    }

    private void refreshArms(ServerLevel server, int flags) {
        if (!hasArms()) return;
        BlockState state = getBlockState();
        BlockState armed = state;
        Fluid fluid = tank.getDeclaredFluid();
        for (int i = 0; i < ARM_SIDES.length; i++) {
            Direction side = ARM_SIDES[i];
            if (ChunkUtil.chunkIfLoaded(server, worldPosition.relative(side)) == null) continue;
            armed =
                    armed.setValue(
                            ARMS[i],
                            fluid != null && fluid != Fluids.EMPTY && arm(server, side, fluid));
        }
        if (armed != state) server.setBlock(worldPosition, armed, flags);
    }

    private boolean arm(LevelReader level, Direction side, Fluid fluid) {
        return PhysicalGraph.fluidContact(
                level, worldPosition.relative(side), side.getOpposite(), fluid);
    }

    protected long receiveRate() {
        return mode.receives() ? Long.MAX_VALUE : 0L;
    }

    protected long provideRate() {
        return mode == TransferMode.BUFFER || mode == TransferMode.SEND && !leaning()
                ? Long.MAX_VALUE
                : 0L;
    }

    @Override
    public long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        if (leaning()
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
        if (tank.getPressure() != pressure || !tank.accepts(type, components)) return amount;
        int before = tank.getFill();
        int added = tank.receive(type, components, (int) Math.min(amount, Integer.MAX_VALUE));
        if (added > 0) stockMoved(before);
        return amount - added;
    }

    @Override
    public long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        return tank.getPressure() == pressure && tank.provides(type, components)
                ? tank.getFill()
                : 0L;
    }

    @Override
    public void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        if (tank.getPressure() != pressure || !tank.provides(type, components)) return;
        int before = tank.getFill();
        int taken = (int) Math.min(amount, before);
        if (taken <= 0) return;
        tank.setFill(before - taken);
        stockMoved(before);
    }

    private void stockMoved(int before) {
        int after = tank.getFill();
        int capacity = tank.getMaxFill();
        if (before == 0
                || after == 0
                || StorageComparatorSignal.rising(before, capacity)
                        != StorageComparatorSignal.rising(after, capacity)) {
            setChanged();
        } else {
            markChanged();
        }
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return provideRate();
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return receiveRate();
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return mode.sends() ? tanks : NONE;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tank, FlushFaces.own());
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
        if (mode.sends() && !patch.isEmpty() && tank.getFill() > 0 && tank.getFluid() == type)
            visitor.accept(patch);
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
        return mode == TransferMode.BUFFER;
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
    public int getComparatorPower() {
        return StorageComparatorSignal.rising(tank.getFill(), tank.getMaxFill());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_IDENTIFIER -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_LOAD -> fits(tank.containerContent(stack));
            case SLOT_UNLOAD -> fits(tank.containerRoom(stack));
            default -> false;
        };
    }

    private boolean fits(long amount) {
        return amount > 0L && amount <= tank.getMaxFill();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_LOAD_OUT || slot == SLOT_UNLOAD_OUT;
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
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        Fluid fluid = FluidSettingsHandler.readFluid(settings, option);
        if (fluid == null) return;
        Fluid before = tank.getDeclaredFluid();
        boolean strict = tank.isStrict();
        tank.setTankTypeByIdentifier(fluid);
        if (tank.getDeclaredFluid() != before || tank.isStrict() != strict) setChanged();
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
        return Component.translatable("container.hbm.barrel");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BarrelMenu(containerId, inventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        Fluid fluid = tank.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY || tank.getFill() <= 0) return;
        components.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(fluid, tank.getFill(), tank.getPressure(), tank.getComponents()));
        components.set(ModDataComponents.TRANSFER_MODE.get(), mode);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        FluidStackNTM content = components.get(ModDataComponents.FLUID_CONTENT.get());
        if (content != null) {
            tank.setTankTypeByIdentifier(content.type());
            tank.receive(
                    content.type(),
                    content.components(),
                    (int) Math.min(content.amount(), Integer.MAX_VALUE));
        }
        TransferMode carried = components.get(ModDataComponents.TRANSFER_MODE.get());
        if (carried != null) setMode(carried);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FluidBarrelBlockEntity$mode(
                TransferMode.of(input.getIntOr("mode", TransferMode.RECEIVE.code())));
        input.child("tank").ifPresent(tank::deserialize);
        flush.invalidate();
        refreshBufferNodes();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.code());
        tank.serialize(output.child("tank"));
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
            default -> throw new IllegalArgumentException("barrel sync unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$FluidBarrelBlockEntity$mode(TransferMode.of(input.readInt()));
            case 1 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException("barrel sync unit " + unit);
        }
    }

    public final void hbm$syncSet$FluidBarrelBlockEntity$mode(TransferMode next) {
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
        com.hbm.packet.SyncBindings.field(FluidBarrelBlockEntity.class, "mode", 3, 1L);
        com.hbm.packet.SyncBindings.field(FluidBarrelBlockEntity.class, "tank", 3, 1L << 1);
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
