// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.inventory.container.MenuPWR;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRFuelStage;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachinePWRController extends BlockEntityMachineBase
        implements AudioLoop,
                IControlReceiver,
                MenuProvider,
                FluidFlushSender,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final long coreHeatCapacityBase = 10_000_000;
    public static final long hullHeatCapacityBase = 10_000_000;
    public static final int RAY_REPORT_PERIOD = 100;
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "rods",
                PREFIX_VALUE + "coreheat",
                PREFIX_VALUE + "hullheat",
                PREFIX_VALUE + "coldbuf",
                PREFIX_VALUE + "hotbuf",
                PREFIX_VALUE + "flux",
                PREFIX_VALUE + "depletion",
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "jettison",
            };
    private static final int[] ACCESS = {0, 1};
    public static Consumer<BlockEntityMachinePWRController> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 12)
    public final FluidTankNTM[] tanks;

    protected final List<BlockPos> ports = new ArrayList<>();
    protected final List<BlockPos> rods = new ArrayList<>();

    @SyncField(units = 1L << 2)
    public long coreHeat;

    @SyncField(units = 1L << 11)
    public long coreHeatCapacity = coreHeatCapacityBase;

    @SyncField(units = 1L << 3)
    public long hullHeat;

    @SyncField(units = 1L << 4)
    public double flux;

    @SyncField(units = 1L << 9)
    public double rodLevel = 100;

    @SyncField(units = 1L << 10)
    public double rodTarget = 100;

    @SyncField(units = 1L << 7)
    public int typeLoaded = -1;

    @SyncField(units = 1L << 8)
    public int amountLoaded;

    @SyncField(units = 1L << 6)
    public double progress;

    @SyncField(units = 1L << 5)
    public double processTime;

    @SyncField(units = 1L << 1)
    public int rodCount;

    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int heatsinkCount;
    public int channelCount;
    public int sourceCount;
    public int unloadDelay = 0;

    @SyncField(units = 1L << 0)
    public boolean assembled;

    private @Nullable BoundingBox members;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachinePWRController(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER.get(), pos, state, 3);
        this.tanks = new FluidTankNTM[2];
        this.tanks[0] = new FluidTankNTM(NTMFluids.COOLANT, 128_000);
        this.tanks[1] = new FluidTankNTM(NTMFluids.COOLANT_HOT, 128_000);
        this.sending = new FluidTankNTM[] {tanks[1]};
    }

    public void setup(Map<BlockPos, BlockState> partMap, Map<BlockPos, BlockState> rodMap) {
        members = BoundingBox.encapsulatingPositions(partMap.keySet()).orElseThrow();
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, BlockState> entry : partMap.entrySet()) {
            var block = entry.getValue().getBlock();
            if (block == ModBlocks.PWR_FUEL.get()) rodCount++;
            if (block == ModBlocks.PWR_HEATEX.get()) heatexCount++;
            if (block == ModBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == ModBlocks.PWR_HEATSINK.get()) heatsinkCount++;
            if (block == ModBlocks.PWR_NEUTRON_SOURCE.get()) sourceCount++;
            if (block == ModBlocks.PWR_PORT.get()) ports.add(entry.getKey());
        }

        for (BlockPos fuelPos : rodMap.keySet()) {
            rods.add(fuelPos);

            for (Direction dir : Direction.VALUES) {
                boolean controlled = false;
                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.relative(dir, i);
                    BlockState stateAtPos = partMap.get(checkPos);
                    var atPos = stateAtPos != null ? stateAtPos.getBlock() : null;

                    if (atPos == null || atPos == ModBlocks.PWR_CASING.get()) break;
                    if (atPos == ModBlocks.PWR_CONTROL.get()) controlled = true;
                    if (atPos == ModBlocks.PWR_FUEL.get()) {
                        if (controlled) connectionsControlledDouble++;
                        else connectionsDouble++;
                        break;
                    }
                    if (atPos == ModBlocks.PWR_REFLECTOR.get()) {
                        if (controlled) connectionsControlledDouble += 2;
                        else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        flush.invalidate();
        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);
        this.coreHeatCapacity =
                coreHeatCapacityBase + this.heatsinkCount * (coreHeatCapacityBase / 20);
    }

    @Override
    public void tickServer() {
        setupTanks();

        if (unloadDelay > 0) unloadDelay--;

        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        var cs = level.getChunkSource();
        if (!cs.hasChunk(chunkX, chunkZ)
                || !cs.hasChunk(chunkX + 2, chunkZ + 2)
                || !cs.hasChunk(chunkX + 2, chunkZ - 2)
                || !cs.hasChunk(chunkX - 2, chunkZ + 2)
                || !cs.hasChunk(chunkX - 2, chunkZ - 2)) {
            this.unloadDelay = 60;
        }

        if (!this.assembled) {
            networkPackNT(150);
            return;
        }

        flush.provide((ServerLevel) level, this);

        if (this.unloadDelay <= 0) {
            ItemStack rodStack = inventory.get(0);
            ItemStack rodHotStack = inventory.get(1);

            if ((typeLoaded == -1 || amountLoaded <= 0)
                    && rodStack.getItem() instanceof ItemPWRFuel cold) {
                typeLoaded = cold.fuel.ordinal();
                amountLoaded++;
                rodStack.shrink(1);
                markChanged();
            } else if (rodStack.getItem() instanceof ItemPWRFuel cold
                    && cold.fuel.ordinal() == typeLoaded
                    && amountLoaded < rodCount) {
                amountLoaded++;
                rodStack.shrink(1);
                markChanged();
            }

            double diff = this.rodLevel - this.rodTarget;
            if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
            if (this.rodTarget > this.rodLevel) this.rodLevel++;
            if (this.rodTarget < this.rodLevel) this.rodLevel--;

            FT_PWRModerator moderator =
                    NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_PWRModerator.class);
            double multiplier = moderator != null ? moderator.getMultiplier() : 1D;

            int newFlux = this.sourceCount * 20;

            if (typeLoaded != -1 && amountLoaded > 0) {
                EnumPWRFuel fuel =
                        EnumPWRFuel.VALUES[
                                Mth.positiveModulo(typeLoaded, EnumPWRFuel.VALUES.length)];
                double usedRods = getTotalProcessMultiplier();
                double fluxPerRod = this.flux / this.rodCount;
                double outputPerRod = fuel.function.effonix(fluxPerRod);
                double totalOutput = outputPerRod * amountLoaded * usedRods;
                double totalHeatOutput = totalOutput * fuel.heatEmission;

                if (tanks[0].getFill() > 0) totalHeatOutput *= multiplier;

                this.coreHeat += totalHeatOutput;
                newFlux += totalOutput;

                this.processTime = fuel.yield;
                this.progress += totalOutput;

                if (this.progress >= this.processTime) {
                    this.progress -= this.processTime;

                    ItemPWRFuelStage hotItem =
                            ModItems.PWR_FUEL_HOT.get(EnumPWRFuel.VALUES[typeLoaded]);
                    if (rodHotStack.isEmpty()) {
                        inventory.set(1, new ItemStack(hotItem));
                    } else if (rodHotStack.getItem() == hotItem
                            && rodHotStack.getCount() < rodHotStack.getMaxStackSize()) {
                        rodHotStack.grow(1);
                    }
                    this.amountLoaded--;
                    markChanged();
                }
                if (level.getGameTime() % RAY_REPORT_PERIOD == 0L) {
                    SatelliteRayEvents.report(
                            (ServerLevel) level,
                            worldPosition,
                            SatelliteRayEvents.NEUTRON_EMISSION,
                            200);
                }
            }

            if (this.amountLoaded <= 0) this.typeLoaded = -1;
            if (amountLoaded > rodCount) amountLoaded = rodCount;

            double coreCoolingApproachNum =
                    getXOverE((double) this.heatexCount * 5 / (double) getRodCountForCoolant(), 2)
                            / 2D;
            long averageCoreHeat = (this.coreHeat + this.hullHeat) / 2;
            this.coreHeat -= (coreHeat - averageCoreHeat) * coreCoolingApproachNum;
            this.hullHeat -= (hullHeat - averageCoreHeat) * coreCoolingApproachNum;

            updateCoolant();

            this.coreHeat *= 0.999D;
            this.hullHeat *= 0.999D;

            this.flux = newFlux;

            if (tanks[0].getFill() > 0) this.flux *= multiplier;

            if (this.coreHeat > this.coreHeatCapacity) {
                meltDown();
                return;
            }
        } else {
            this.hullHeat = 0;
            this.coreHeat = 0;
        }

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.REACTOR_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                10F,
                1.0F,
                20);
    }

    private void setupTanks() {
        if (amountLoaded <= 0) tanks[0].setType(2, 2, inventory);

        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) {
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
            return;
        }
        tanks[1].setTankType(trait.getFirstStep().typeProduced());
    }

    private void updateCoolant() {
        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = (double) this.channelCount / (double) getRodCountForCoolant() * 0.1D;
        if (coolingEff > 1D) coolingEff = 1D;

        int heatToUse =
                (int)
                        Math.min(
                                Math.min(
                                        this.hullHeat,
                                        (long)
                                                (this.hullHeat
                                                        * coolingEff
                                                        * trait.getEfficiency(HeatingType.PWR))),
                                2_000_000_000);
        HeatingStep step = trait.getFirstStep();
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        int heatCycles = heatToUse / step.heatReq;
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        this.hullHeat -= (long) step.heatReq * cycles;
        this.tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        this.tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
    }

    protected int getRodCountForCoolant() {
        return this.rodCount + (int) Math.ceil(this.heatsinkCount / 4D);
    }

    public double getTotalProcessMultiplier() {
        double totalConnections =
                this.connections + this.connectionsControlled * (1D - (this.rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D))
                + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1 - Math.pow(Math.E, -x / d);
    }

    protected void meltDown() {

        level.destroyBlock(worldPosition, false);

        double x = 0, y = 0, z = 0;
        for (BlockPos pos : this.rods) {

            level.setBlock(pos, ModBlocks.CORIUM.get().defaultBlockState(), 3);
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        level.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && stack.getItem() instanceof ItemPWRFuel;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemPWRFuel;
        if (slot == 2) return stack.getItem() instanceof FluidIdentifierItem;
        return false;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                tanks[1],
                (server, pos, contacts) -> {
                    for (BlockPos port : ports) {
                        for (Direction dir : Direction.VALUES)
                            contacts.contact(port.relative(dir), dir.getOpposite());
                    }
                });
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (pressure != tanks[1].getPressure() || !tanks[1].provides(type)) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || pressure != tanks[1].getPressure()) return;
        tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        markChanged();
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) markChanged();
        return amount - accepted;
    }

    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int control = data.getIntOr("control", -1);
        if (control >= 0) {
            this.rodTarget = Math.clamp(control, 0, 100);
            markChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuPWR(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) (100 - this.rodLevel);
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + this.coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + this.hullHeat;
        if ((PREFIX_VALUE + "coldbuf").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "hotbuf").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) this.flux;
        if ((PREFIX_VALUE + "depletion").equals(name))
            return "" + (int) (this.progress * 100 / this.processTime);
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            this.rodTarget = 100 - IRORInteractive.parseInt(params[0], 0, 100);
            markChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            markChanged();
            return null;
        }
        return null;
    }

    public void disassemble() {
        if (!assembled) return;
        assembled = false;
        markChanged();
        restoreMembers();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        assembled = false;
        restoreMembers();
    }

    private void restoreMembers() {
        if (members == null || !(level instanceof ServerLevel server)) return;
        BoundingBox bounds = members;
        members = null;
        for (BlockPos pos : AssembledMembers.members(server, worldPosition, bounds)) {
            BlockState shell = server.getBlockState(pos);
            if (shell.getBlock() instanceof BlockPWR) {
                server.setBlock(pos, shell.getValue(BlockPWR.PART).original(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        this.assembled = input.getBooleanOr("assembled", false);
        this.members = input.read("members", BoundingBox.CODEC).orElse(null);
        this.coreHeat = input.getLongOr("coreHeat", 0);
        this.hullHeat = input.getLongOr("hullHeat", 0);
        this.flux = input.getDoubleOr("flux", 0);
        this.rodLevel = input.getDoubleOr("rodLevel", 100);
        this.rodTarget = input.getDoubleOr("rodTarget", 100);
        this.typeLoaded = input.getIntOr("typeLoaded", -1);
        this.amountLoaded = input.getIntOr("amountLoaded", 0);
        this.progress = input.getDoubleOr("progress", 0);
        this.processTime = input.getDoubleOr("processTime", 0);
        this.coreHeatCapacity =
                Math.max(
                        input.getLongOr("coreHeatCapacity", coreHeatCapacityBase),
                        coreHeatCapacityBase);
        this.rodCount = input.getIntOr("rodCount", 0);
        this.connections = input.getIntOr("connections", 0);
        this.connectionsControlled = input.getIntOr("connectionsControlled", 0);
        this.heatexCount = input.getIntOr("heatexCount", 0);
        this.channelCount = input.getIntOr("channelCount", 0);
        this.sourceCount = input.getIntOr("sourceCount", 0);
        this.heatsinkCount = input.getIntOr("heatsinkCount", 0);

        ports.clear();
        input.read("ports", BlockPos.CODEC.listOf()).ifPresent(ports::addAll);
        rods.clear();
        input.read("rods", BlockPos.CODEC.listOf()).ifPresent(rods::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        output.putBoolean("assembled", assembled);
        if (members != null) output.store("members", BoundingBox.CODEC, members);
        output.putLong("coreHeat", coreHeat);
        output.putLong("hullHeat", hullHeat);
        output.putDouble("flux", flux);
        output.putDouble("rodLevel", rodLevel);
        output.putDouble("rodTarget", rodTarget);
        output.putInt("typeLoaded", typeLoaded);
        output.putInt("amountLoaded", amountLoaded);
        output.putDouble("progress", progress);
        output.putDouble("processTime", processTime);
        output.putLong("coreHeatCapacity", coreHeatCapacity);
        output.putInt("rodCount", rodCount);
        output.putInt("connections", connections);
        output.putInt("connectionsControlled", connectionsControlled);
        output.putInt("heatexCount", heatexCount);
        output.putInt("channelCount", channelCount);
        output.putInt("sourceCount", sourceCount);
        output.putInt("heatsinkCount", heatsinkCount);

        output.store("ports", BlockPos.CODEC.listOf(), ports);
        output.store("rods", BlockPos.CODEC.listOf(), rods);
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.assembled);
            case 1 -> output.writeInt(this.rodCount);
            case 2 -> output.writeLong(this.coreHeat);
            case 3 -> output.writeLong(this.hullHeat);
            case 4 -> output.writeDouble(this.flux);
            case 5 -> output.writeDouble(this.processTime);
            case 6 -> output.writeDouble(this.progress);
            case 7 -> output.writeInt(this.typeLoaded);
            case 8 -> output.writeInt(this.amountLoaded);
            case 9 -> output.writeDouble(this.rodLevel);
            case 10 -> output.writeDouble(this.rodTarget);
            case 11 -> output.writeLong(this.coreHeatCapacity);
            case 12 -> writeTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.assembled = input.readBoolean();
            case 1 -> this.rodCount = input.readInt();
            case 2 -> this.coreHeat = input.readLong();
            case 3 -> this.hullHeat = input.readLong();
            case 4 -> this.flux = input.readDouble();
            case 5 -> this.processTime = input.readDouble();
            case 6 -> this.progress = input.readDouble();
            case 7 -> this.typeLoaded = input.readInt();
            case 8 -> this.amountLoaded = input.readInt();
            case 9 -> this.rodLevel = input.readDouble();
            case 10 -> this.rodTarget = input.readDouble();
            case 11 -> this.coreHeatCapacity = input.readLong();
            case 12 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
