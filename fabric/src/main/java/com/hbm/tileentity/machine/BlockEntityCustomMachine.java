// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockCMPort;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.blocks.machine.CustomMachinePorts;
import com.hbm.blocks.machine.ReactorResearch;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineCustom;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.recipes.CustomMachineRecipe;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityCustomMachine extends BlockEntityMachinePolluting
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                IGUIProvider,
                PersistentDrop,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID_START = 1;
    public static final int SLOT_ITEM_IN_START = 4;
    public static final int SLOT_TEMPLATE_START = 10;
    public static final int SLOT_ITEM_OUT_START = 16;
    public static final int SLOT_COUNT = 22;
    public static final int MAX_FLUID_IN = CustomMachineDefinition.MAX_FLUID_SLOTS;
    public static final int MAX_ITEM_IN = CustomMachineDefinition.MAX_ITEM_SLOTS;
    public static final int MAX_ITEM_OUT = CustomMachineDefinition.MAX_ITEM_SLOTS;

    private static final int DEFAULT_POLLUTION_BUFFER = 100;

    private static final int STRUCTURE_CHECK_INTERVAL = 300;

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    private static final String[] PERSISTENT_KEYS = {"machineType"};

    @SyncField(units = 1L)
    public @Nullable ResourceKey<CustomMachineDefinition> machineType;

    public @Nullable CustomMachineDefinition definition;

    @SyncField(units = 1L << 1)
    public long power;

    @SyncField(units = 1L << 3)
    public int flux;

    @SyncField(units = 1L << 4)
    public int heat;

    @SyncField(units = 1L << 2)
    public int progress;

    @SyncField(units = 1L << 6)
    public int maxProgress = 1;

    @SyncField(units = 1L << 5)
    public boolean structureOK;

    @SyncField(units = 1L)
    public FluidTankNTM[] inputTanks = NO_TANKS;

    @SyncField(units = 1L)
    public FluidTankNTM[] outputTanks = NO_TANKS;

    @SyncField(units = 1L)
    public ModulePatternMatcher matcher = new ModulePatternMatcher(0);

    private HolderLookup.@Nullable Provider syncLookup;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private int structureCheckDelay;
    private @Nullable CustomMachineRecipe cachedRecipe;

    private List<BlockPos> portCells = List.of();
    private List<BlockPos> fluxCells = List.of();
    private List<BlockPos> heatCells = List.of();

    public BlockEntityCustomMachine(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CUSTOM_MACHINE.get(),
                pos,
                state,
                SLOT_COUNT,
                DEFAULT_POLLUTION_BUFFER);
    }

    public static BlockPos cellPos(
            BlockPos core, Direction facing, CustomMachineDefinition.Cell cell) {
        int x = cell.x();
        int z = cell.z();
        return switch (facing) {
            case SOUTH -> core.offset(-x, cell.y(), -z);
            case WEST -> core.offset(z, cell.y(), -x);
            case EAST -> core.offset(-z, cell.y(), x);
            default -> core.offset(x, cell.y(), z);
        };
    }

    private static ItemStack sample(CustomMachineRecipe recipe, int index) {
        for (var weighted : recipe.outputItems()[index].unwrap()) {
            if (!weighted.value().isEmpty()) return weighted.value();
        }
        return ItemStack.EMPTY;
    }

    public void setMachineType(@Nullable ResourceKey<CustomMachineDefinition> type) {
        this.machineType = type;
        init();
        if (level != null) setChanged();
    }

    public void init() {
        if (level != null) init(level.registryAccess());
    }

    private void init(HolderLookup.Provider registries) {
        CustomMachineDefinition found =
                machineType == null
                        ? null
                        : registries
                                .lookupOrThrow(CustomMachineDefinition.REGISTRY)
                                .get(machineType)
                                .map(Holder::value)
                                .orElse(null);
        if (found == null) return;
        this.definition = found;
        inputTanks = new FluidTankNTM[found.fluidInCount()];
        for (int i = 0; i < inputTanks.length; i++)
            inputTanks[i] = new FluidTankNTM(found.fluidInCap());
        outputTanks = new FluidTankNTM[found.fluidOutCount()];
        for (int i = 0; i < outputTanks.length; i++)
            outputTanks[i] = new FluidTankNTM(found.fluidOutCap());
        matcher = new ModulePatternMatcher(found.itemInCount());
        smoke.changeTankSize(found.maxPollutionCap());
        smokeLeaded.changeTankSize(found.maxPollutionCap());
        smokePoison.changeTankSize(found.maxPollutionCap());
        flush.invalidate();
    }

    @Override
    public void tickServer() {
        if (definition == null) {

            init();
            if (definition == null) {
                level.destroyBlock(worldPosition, false);
                return;
            }
        }

        power +=
                ItemEnergyTransfer.extract(
                        this, SLOT_BATTERY, definition.maxPower() - power, false);

        for (int i = 0; i < Math.min(inputTanks.length, MAX_FLUID_IN); i++) {
            inputTanks[i].setType(SLOT_FLUID_ID_START + i, SLOT_FLUID_ID_START + i, inventory);
        }

        structureCheckDelay--;
        if (structureCheckDelay <= 0) checkStructure();

        if (TickPhase.every(this, 20)) {
            readFlux();
            pullHeat();
        }

        flush.provide((ServerLevel) level, this);

        if (!structureOK) {
            progress = 0;
        } else if (definition.generatorMode()) {
            tickGenerator();
        } else {
            tickConsumer();
        }
        networkPackNT(50);
    }

    private void tickGenerator() {
        if (cachedRecipe == null) {
            CustomMachineRecipe recipe = getMatchingRecipe();
            if (recipe != null && hasRequiredQuantities(recipe) && hasSpace(recipe)) {
                cachedRecipe = recipe;
                useUpInput(recipe);
            }
        }
        if (cachedRecipe == null) return;

        maxProgress = (int) Math.max(cachedRecipe.duration / definition.recipeSpeedMult(), 1);
        long powerReq = (long) Math.max(cachedRecipe.power * definition.recipeConsumptionMult(), 1);

        progress++;
        power = Math.min(power + powerReq, definition.maxPower());
        heat -= cachedRecipe.heat;
        if (TickPhase.every(this, 20)) {
            pollution(cachedRecipe);
            radiation(cachedRecipe);
        }
        if (progress >= maxProgress) {
            progress = 0;
            processRecipe(cachedRecipe);
            cachedRecipe = null;
        }
    }

    private void tickConsumer() {
        CustomMachineRecipe recipe = getMatchingRecipe();
        if (recipe == null) {
            progress = 0;
            return;
        }

        maxProgress = (int) Math.max(recipe.duration / definition.recipeSpeedMult(), 1);
        long powerReq = (long) Math.max(recipe.power * definition.recipeConsumptionMult(), 1);

        if (power < powerReq || !hasRequiredQuantities(recipe) || !hasSpace(recipe)) return;

        progress++;
        power -= powerReq;
        heat -= recipe.heat;
        if (TickPhase.every(this, 20)) {
            pollution(recipe);
            radiation(recipe);
        }
        if (progress >= maxProgress) {
            progress = 0;
            useUpInput(recipe);
            processRecipe(recipe);
        }
    }

    private void readFlux() {
        for (BlockPos cell : fluxCells) {
            for (Direction dir : Direction.VALUES) {
                BlockPos probe = cell.relative(dir);
                if (!(level.getBlockState(probe).getBlock() instanceof ReactorResearch)) continue;
                BlockPos core = MultiblockSurface.coreOfAny(level, probe);
                if (core == null) continue;
                if (level.getBlockEntity(core) instanceof BlockEntityReactorResearch reactor) {
                    flux = reactor.totalFlux;
                }
            }
        }
    }

    private void pullHeat() {
        if (definition.maxHeat() <= 0) return;
        for (BlockPos cell : heatCells) {
            for (Direction dir : Direction.VALUES) {
                BlockPos probe = cell.relative(dir);
                IHeatSource source = NtmContracts.HEAT_SOURCE.at(level, probe);
                if (source == null) continue;
                int diff = source.getHeatStored(level, probe) - heat;
                if (diff <= 0) continue;
                source.useUpHeat(level, probe, diff);
                heat = Math.min(heat + diff, definition.maxHeat());
            }
        }
    }

    public @Nullable CustomMachineRecipe getMatchingRecipe() {
        outer:
        for (CustomMachineRecipe recipe : CustomMachineRecipes.byKey(definition.recipeKey())) {

            if (recipe.inputFluid.length > inputTanks.length
                    || recipe.outputFluid.length > outputTanks.length
                    || recipe.inputItem.length > definition.itemInCount()
                    || recipe.outputItems().length > definition.itemOutCount()) continue;
            for (int i = 0; i < recipe.inputFluid.length; i++) {
                FluidTankNTM tank = inputTanks[i];
                FluidStackNTM want = recipe.inputFluid[i];
                if (tank.getTankType() != want.type() || tank.getPressure() != want.pressure())
                    continue outer;
            }
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (!recipe.inputItem[i].test(inventory.get(SLOT_ITEM_IN_START + i)))
                    continue outer;
            }
            return recipe;
        }
        return null;
    }

    public boolean hasRequiredQuantities(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluid.length; i++) {
            if (inputTanks[i].getFill() < recipe.inputFluid[i].amount()) return false;
        }
        for (int i = 0; i < recipe.inputItem.length; i++) {
            ItemStack stack = inventory.get(SLOT_ITEM_IN_START + i);
            if (!stack.isEmpty() && stack.getCount() < recipe.inputItem[i].count()) return false;
        }
        if (definition.fluxMode() && flux < recipe.flux) return false;
        return definition.maxHeat() <= 0 || recipe.heat <= 0 || heat >= recipe.heat;
    }

    public boolean hasSpace(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluid.length; i++) {
            if (!recipe.outputFluid[i].fitsInto(outputTanks[i])) return false;
        }
        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack held = inventory.get(SLOT_ITEM_OUT_START + i);
            if (held.isEmpty()) continue;
            ItemStack out = sample(recipe, i);
            if (!ItemStack.isSameItemSameComponents(held, out)) return false;
            if (held.getCount() + out.getCount() > held.getMaxStackSize()) return false;
        }
        return true;
    }

    public void useUpInput(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluid.length; i++)
            recipe.inputFluid[i].drainFrom(inputTanks[i]);
        for (int i = 0; i < recipe.inputItem.length; i++) {
            removeItem(SLOT_ITEM_IN_START + i, recipe.inputItem[i].count());
        }
    }

    public void processRecipe(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluid.length; i++)
            recipe.outputFluid[i].fillInto(outputTanks[i]);

        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack rolled =
                    recipe.outputItems()[i].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
            if (rolled.isEmpty()) continue;
            int slot = SLOT_ITEM_OUT_START + i;
            ItemStack held = inventory.get(slot);
            if (held.isEmpty()) {
                setItem(slot, rolled.copy());
            } else {
                held.grow(rolled.getCount());
                setChanged();
            }
        }
    }

    public void pollution(CustomMachineRecipe recipe) {
        PollutionType type = recipe.pollutionType;
        if (type == null || recipe.pollutionAmount == 0F) return;
        if (recipe.pollutionAmount > 0) {
            pollute(type, recipe.pollutionAmount);
        } else if (PollutionHandler.getPollution(level, worldPosition, type)
                >= -recipe.pollutionAmount) {
            PollutionHandler.decrementPollution(
                    level, worldPosition, type, -recipe.pollutionAmount);
        }
    }

    public void radiation(CustomMachineRecipe recipe) {
        if (recipe.radiationAmount == 0F) return;
        ServerLevel server = (ServerLevel) level;
        if (recipe.radiationAmount > 0) {
            RadiationSystemNT.incrementRad(server, worldPosition, recipe.radiationAmount);
        } else {

            double now = RadiationSystemNT.getRadForCoord(server, worldPosition);
            RadiationSystemNT.setRadForCoord(server, worldPosition, now + recipe.radiationAmount);
        }
    }

    public boolean checkStructure() {
        structureCheckDelay = STRUCTURE_CHECK_INTERVAL;
        List<BlockPos> priorPorts = portCells;
        structureOK = false;
        portCells = List.of();
        fluxCells = List.of();
        heatCells = List.of();
        if (definition == null) {
            releasePorts(priorPorts);
            return false;
        }

        Direction facing = getBlockState().getValue(BlockMachineHorizontal.FACING);
        List<BlockPos> ports = new ArrayList<>();
        List<BlockPos> flux = new ArrayList<>();
        List<BlockPos> heats = new ArrayList<>();

        for (CustomMachineDefinition.Cell cell : definition.cells()) {
            BlockPos at = cellPos(worldPosition, facing, cell);
            BlockState state = level.getBlockState(at);
            if (!cell.blocks().contains(state.getBlock().builtInRegistryHolder())) {
                releasePorts(priorPorts);
                return false;
            }
            if (state.is(ModBlocks.CM_FLUX.get())) flux.add(at);
            if (state.is(ModBlocks.CM_HEAT.get())) heats.add(at);
            if (state.getBlock() instanceof BlockCMPort) ports.add(at);
        }

        portCells = List.copyOf(ports);
        fluxCells = List.copyOf(flux);
        heatCells = List.copyOf(heats);
        structureOK = true;

        if (!priorPorts.equals(portCells)) {
            releasePorts(priorPorts);
            flush.invalidate();
        }
        CustomMachinePorts.index((ServerLevel) level, worldPosition, portCells);
        return true;
    }

    private void releasePorts(List<BlockPos> cells) {
        if (cells.isEmpty() || !(level instanceof ServerLevel server)) return;
        CustomMachinePorts.unindex(server, worldPosition, cells);
        flush.invalidate();
    }

    public boolean claimsPort(BlockPos pos) {
        return structureOK && portCells.contains(pos);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) {
            out.add(
                    tank,
                    (level, pos, visitor) -> {
                        for (Direction dir : Direction.VALUES)
                            visitor.contact(pos.relative(dir), dir.getOpposite());
                        for (BlockPos cell : portCells) {
                            for (Direction dir : Direction.VALUES)
                                visitor.contact(cell.relative(dir), dir.getOpposite());
                        }
                    });
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server)
            CustomMachinePorts.unindex(server, worldPosition, portCells);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server)
            CustomMachinePorts.withdraw(server, worldPosition, portCells);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        structureCheckDelay = 0;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return definition != null ? definition.maxPower() : 1;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (definition != null && definition.generatorMode()) return power;
        return IEnergyHandlerMK2.super.transferPower(power, simulate);
    }

    @Override
    public long getReceiverSpeed() {
        return definition != null && !definition.generatorMode() ? getMaxPower() : 0;
    }

    @Override
    public long getProviderSpeed() {
        return definition != null && definition.generatorMode() ? getMaxPower() : 0;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputTanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        FluidTankNTM[] smokeTanks = getSmokeTanks();
        FluidTankNTM[] all = new FluidTankNTM[outputTanks.length + smokeTanks.length];
        System.arraycopy(outputTanks, 0, all, 0, outputTanks.length);
        System.arraycopy(smokeTanks, 0, all, outputTanks.length, smokeTanks.length);
        return all;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (definition == null) return new int[0];
        int inputs = Math.min(definition.itemInCount(), MAX_ITEM_IN);
        int[] slots = new int[inputs + MAX_ITEM_OUT];
        for (int i = 0; i < inputs; i++) slots[i] = SLOT_ITEM_IN_START + i;
        for (int i = 0; i < MAX_ITEM_OUT; i++) slots[inputs + i] = SLOT_ITEM_OUT_START + i;
        return slots;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_ITEM_OUT_START && slot < SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < SLOT_ITEM_IN_START || slot >= SLOT_TEMPLATE_START) return false;
        ItemStack filter = inventory.get(slot + MAX_ITEM_IN);
        return filter.isEmpty()
                || matcher.isValidForFilter(filter, slot - SLOT_ITEM_IN_START, stack);
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot < SLOT_TEMPLATE_START || slot >= SLOT_ITEM_OUT_START;
    }

    private void writeDefinitionState(ByteBuf output) {
        new FriendlyByteBuf(output)
                .writeUtf(machineType == null ? "" : machineType.identifier().toString());
        for (FluidTankNTM tank : inputTanks) tank.packetSerialize(output);
        for (FluidTankNTM tank : outputTanks) tank.packetSerialize(output);
        matcher.serialize(output);
    }

    private void readDefinitionState(ByteBuf input) {
        HolderLookup.Provider registries = level != null ? level.registryAccess() : syncLookup;
        readType(new FriendlyByteBuf(input).readUtf(), registries);
        for (FluidTankNTM tank : inputTanks) tank.packetDeserialize(input);
        for (FluidTankNTM tank : outputTanks) tank.packetDeserialize(input);
        matcher.deserialize(input);
    }

    private void readType(String id, HolderLookup.Provider registries) {
        ResourceKey<CustomMachineDefinition> read =
                id.isEmpty()
                        ? null
                        : ResourceKey.create(
                                CustomMachineDefinition.REGISTRY, Identifier.parse(id));
        if (definition != null && read == machineType) return;
        machineType = read;
        init(registries);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeDefinitionState(output);
            case 1 -> output.writeLong(power);
            case 2 -> output.writeInt(progress);
            case 3 -> output.writeInt(flux);
            case 4 -> output.writeInt(heat);
            case 5 -> output.writeBoolean(structureOK);
            case 6 -> output.writeInt(maxProgress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readDefinitionState(input);
            case 1 -> power = input.readLong();
            case 2 -> progress = input.readInt();
            case 3 -> flux = input.readInt();
            case 4 -> heat = input.readInt();
            case 5 -> structureOK = input.readBoolean();
            case 6 -> maxProgress = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void loadWithComponents(ValueInput input) {
        syncLookup = input.lookup();
        try {
            super.loadWithComponents(input);
        } finally {
            syncLookup = null;
        }
    }

    @Override
    public void loadCustomOnly(ValueInput input) {
        syncLookup = input.lookup();
        try {
            super.loadCustomOnly(input);
        } finally {
            syncLookup = null;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        readType(input.getStringOr("machineType", ""), input.lookup());
        super.loadAdditional(input);
        if (definition == null) return;
        for (int i = 0; i < inputTanks.length; i++)
            input.child("i" + i).ifPresent(inputTanks[i]::deserialize);
        for (int i = 0; i < outputTanks.length; i++)
            input.child("o" + i).ifPresent(outputTanks[i]::deserialize);
        matcher.load(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("heat").ifPresent(v -> heat = v);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getString("cachedRecipe")
                .ifPresent(
                        name -> {
                            for (CustomMachineRecipe recipe :
                                    CustomMachineRecipes.byKey(definition.recipeKey())) {
                                if (recipe.getInternalName().equals(name)) cachedRecipe = recipe;
                            }
                        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (machineType != null)
            output.putString("machineType", machineType.identifier().toString());
        if (definition == null) return;
        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].serialize(output.child("i" + i));
        for (int i = 0; i < outputTanks.length; i++)
            outputTanks[i].serialize(output.child("o" + i));
        matcher.save(output);
        output.putLong("power", power);
        output.putInt("heat", heat);
        output.putInt("progress", progress);

        if (cachedRecipe != null) output.putString("cachedRecipe", cachedRecipe.getInternalName());
    }

    @Override
    protected Component getDefaultName() {
        return definition != null ? definition.name() : super.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCustom(containerId, playerInventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (machineType != null)
            components.set(ModDataComponents.CUSTOM_MACHINE_TYPE.get(), machineType);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        setMachineType(components.get(ModDataComponents.CUSTOM_MACHINE_TYPE.get()));
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }
}
