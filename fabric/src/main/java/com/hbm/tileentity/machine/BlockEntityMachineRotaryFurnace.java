// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.handler.FuelHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachineRotaryFurnace;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.RotaryFurnaceRecipe;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineRotaryFurnace extends BlockEntityMachinePolluting
        implements FluidTankEndpoint, MenuProvider, IPortHost, IFluidCopiable, SyncUnitSchema {

    public static final int SLOT_COUNT = 5;
    public static final int SLOT_FUEL = 4;
    public static final int MAX_OUTPUT = MaterialShapes.BLOCK.q(16);

    public static final ModuleBurnTime BURN_MODULE =
            new ModuleBurnTime()
                    .setCokeTimeMod(1.25)
                    .setRocketTimeMod(1.5)
                    .setSolidTimeMod(1.5)
                    .setBalefireTimeMod(1.5)
                    .setSolidHeatMod(1.5)
                    .setRocketHeatMod(3)
                    .setBalefireHeatMod(10);
    private static final int[] SLOTS_RED = {0};
    private static final int[] SLOTS_YELLOW = {1};
    private static final int[] SLOTS_GREEN = {2};
    private static final int[] SLOTS_FUEL = {SLOT_FUEL};

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];

    public final List<BlockEntityCrucible.PourStream> streams = new ArrayList<>();

    @SyncField(units = 1L << 1)
    public boolean isProgressing;

    @SyncField(units = 1L << 2)
    public float progress;

    @SyncField(units = 1L << 3)
    public int burnTime;

    public double burnHeat = 1D;

    @SyncField(units = 1L << 4)
    public int maxBurnTime;

    public int steamUsed = 0;

    @SyncField(units = 1L << 5)
    public @Nullable MaterialStack output;

    public int anim;
    public int lastAnim;

    @SyncField(units = 1L << 6)
    private int pourColor = -1;

    @SyncField(units = 1L << 6)
    private float pourLen;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public BlockEntityMachineRotaryFurnace(BlockPos pos, BlockState state) {

        super(ModBlockEntities.ROTARY_FURNACE.get(), pos, state, SLOT_COUNT, 50);
        tanks[0] = new FluidTankNTM(NTMFluids.NONE, 16_000);
        tanks[1] = new FluidTankNTM(NTMFluids.STEAM, 12_000);
        tanks[2] = new FluidTankNTM(NTMFluids.SPENTSTEAM, 120);
        receiving = new FluidTankNTM[] {tanks[0], tanks[1]};
        sending = new FluidTankNTM[] {tanks[2], smoke};
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[2], FlushFaces.activePlane((cell, side) -> side.getAxis().isHorizontal()));
        out.add(smoke, FlushFaces.activePlane((cell, side) -> side == Direction.UP));
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRotaryFurnace");
    }

    public Direction coreFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockMultiblockCore.FACING)
                ? state.getValue(BlockMultiblockCore.FACING)
                : Direction.NORTH;
    }

    @Override
    public void tickServer() {
        this.pourColor = -1;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        updateTypeFromIdentifier();

        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();

        flush.provide((ServerLevel) level, this);

        if (this.output != null) {
            int prev = this.output.amount;
            CrucibleUtil.ImpactPos impact = new CrucibleUtil.ImpactPos();
            MaterialStack leftover =
                    CrucibleUtil.pourSingleStack(
                            level,
                            x + 0.5D + rot.getStepX() * 2.875D,
                            y + 1.25D,
                            z + 0.5D + rot.getStepZ() * 2.875D,
                            6,
                            true,
                            this.output,
                            MaterialShapes.INGOT.q(1),
                            impact);
            this.output = leftover;

            if (prev != this.output.amount) {
                this.pourColor = leftover.material.moltenColor;
                this.pourLen = Math.max(1F, y + 1 - (float) (Math.ceil(impact.y) - 1.125));
                markSyncEvent();
            }

            if (output.amount <= 0) this.output = null;
        }

        RotaryFurnaceRecipe recipe =
                RotaryFurnaceRecipes.getRecipe(
                        inventory.get(0), inventory.get(1), inventory.get(2));
        this.isProgressing = false;

        if (recipe != null) {

            ItemStack fuel = inventory.get(4);
            if (this.burnTime <= 0 && !fuel.isEmpty() && FuelHandler.getBurnTime(level, fuel) > 0) {
                this.burnHeat = BURN_MODULE.getHeatMod(fuel);
                this.maxBurnTime = this.burnTime = BURN_MODULE.getBurnTime(level, fuel) / 2;
                removeItem(4, 1);
                setChanged();
            }

            float processSpeed = Math.max((float) burnHeat, 1);
            float steamUseMult = (float) (10 * Math.log10(processSpeed) + 1);

            if (this.canProcess(recipe, steamUseMult)) {
                this.progress += processSpeed / recipe.duration;

                tanks[1].setFill((int) (tanks[1].getFill() - recipe.steam * steamUseMult));
                steamUsed += (int) (recipe.steam * steamUseMult);
                this.isProgressing = true;

                if (this.progress >= 1F) {
                    this.progress -= 1F;
                    this.consumeItems(recipe);

                    if (this.output == null) {
                        this.output = recipe.output.copy();
                    } else {
                        this.output.amount += recipe.output.amount;
                    }
                    setChanged();
                }

                if (this.burnTime > 0) {
                    this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 10F);
                    this.burnTime--;
                }

            } else {
                this.progress = 0;
            }

            if (this.steamUsed >= 100) {
                int steamReturn = this.steamUsed / 100;
                int canReturn = tanks[2].getMaxFill() - tanks[2].getFill();
                int doesReturn = Math.min(steamReturn, canReturn);
                this.steamUsed -= doesReturn * 100;
                tanks[2].setFill(tanks[2].getFill() + doesReturn);
            }

        } else {
            this.progress = 0;
        }

        this.networkPackNT(50);
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        for (Iterator<BlockEntityCrucible.PourStream> it = streams.iterator(); it.hasNext(); ) {
            if (now - it.next().birth() >= 20) it.remove();
        }

        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();

        if (this.burnTime > 0) {
            RandomSource rand = level.getRandom();
            level.addParticle(
                    ParticleTypes.FLAME,
                    worldPosition.getX()
                            + 0.5
                            + dir.getStepX() * 0.5
                            + rot.getStepX()
                            + rand.nextGaussian() * 0.25,
                    worldPosition.getY() + 0.375,
                    worldPosition.getZ()
                            + 0.5
                            + dir.getStepZ() * 0.5
                            + rot.getStepZ()
                            + rand.nextGaussian() * 0.25,
                    0,
                    0,
                    0);
        }

        this.lastAnim = this.anim;
        if (this.isProgressing) {
            this.anim += (int) Math.max(BURN_MODULE.getHeatMod(inventory.get(4)), 1);
        }
    }

    private void updateTypeFromIdentifier() {
        ItemStack id = inventory.get(3);
        if (!(id.getItem() instanceof FluidIdentifierItem)) return;
        FluidIdentifierData data =
                id.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid target = data.primary();
        if (target == null || target == Fluids.EMPTY) return;
        if (tanks[0].getDeclaredFluid() != target || !tanks[0].isStrict()) {
            tanks[0].setTankTypeByIdentifier(target);
            setChanged();
        }
    }

    public boolean canProcess(RotaryFurnaceRecipe recipe, float steamUseMult) {
        if (this.burnTime <= 0) return false;

        FluidStackNTM fluid = recipe.fluid();
        if (fluid != null) {
            if (tanks[0].getTankType() != fluid.type()) return false;
            if (tanks[0].getFill() < fluid.amount()) return false;
        }

        if (tanks[1].getFill() < recipe.steam * steamUseMult) return false;
        if (tanks[2].getMaxFill() - tanks[2].getFill() < recipe.steam * steamUseMult / 100)
            return false;
        if (this.steamUsed > 100) return false;

        if (this.output != null) {
            if (this.output.material != recipe.output.material) return false;
            return this.output.amount + recipe.output.amount <= MAX_OUTPUT;
        }

        return true;
    }

    public void consumeItems(RotaryFurnaceRecipe recipe) {
        for (CountIngredient ingredient : recipe.inputItem) {
            for (int i = 0; i < 3; i++) {
                if (ingredient.test(inventory.get(i))) {
                    removeItem(i, ingredient.count());
                    break;
                }
            }
        }

        FluidStackNTM fluid = recipe.fluid();
        if (fluid != null) {
            tanks[0].setFill(tanks[0].getFill() - (int) fluid.amount());
        }
    }

    @Override
    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank =
                type == PollutionType.SOOT
                        ? smoke
                        : type == PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
        int fluidAmount = (int) Math.ceil(amount * 100);
        int overflow = fluidAmount - tank.fill(tank.getTankType(), fluidAmount, true);
        if (overflow > 0) {
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 3 || slot == SLOT_FUEL;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        Direction facing = coreFacing();
        Direction rot = facing.getCounterClockWise();
        BlockPos back = worldPosition.relative(facing.getOpposite());

        int[] slots = null;
        if (side == null || side == facing.getOpposite()) {
            if (cell.equals(back.relative(rot, 2))) slots = SLOTS_RED;
            else if (cell.equals(back.relative(rot))) slots = SLOTS_YELLOW;
            else if (cell.equals(back)) slots = SLOTS_GREEN;
        }
        if (slots == null
                && (side == null || side == facing)
                && cell.equals(worldPosition.relative(facing).relative(rot))) {
            slots = SLOTS_FUEL;
        }
        return slots == null ? ItemPort.none() : new ItemPort(slots, this::canPlaceItem, null);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tanks[0], tanks[1], tanks[2], smoke};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
        tanks[2].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
        tanks[2].packetDeserialize(input);
    }

    private void writeOutput(ByteBuf outputBuffer) {
        outputBuffer.writeBoolean(output != null);
        if (output != null) {
            outputBuffer.writeInt(output.material.id);
            outputBuffer.writeInt(output.amount);
        }
    }

    private void readOutput(ByteBuf input) {
        output =
                input.readBoolean()
                        ? new MaterialStack(Mats.matById.get(input.readInt()), input.readInt())
                        : null;
    }

    private void writePour(ByteBuf output) {
        output.writeInt(pourColor);
        output.writeFloat(pourLen);
    }

    private void readPour(ByteBuf input) {
        pourColor = input.readInt();
        pourLen = input.readFloat();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        input.child("t2").ifPresent(tanks[2]::deserialize);
        progress = input.getFloatOr("prog", progress);
        input.getInt("burn").ifPresent(v -> burnTime = v);
        burnHeat = input.getDoubleOr("heat", burnHeat);
        input.getInt("maxBurn").ifPresent(v -> maxBurnTime = v);
        input.getInt("steamUsed").ifPresent(v -> steamUsed = v);
        input.getInt("outType")
                .ifPresent(
                        type ->
                                this.output =
                                        new MaterialStack(
                                                Mats.matById.get(type),
                                                input.getIntOr("outAmount", 0)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        tanks[2].serialize(output.child("t2"));
        output.putFloat("prog", progress);
        output.putInt("burn", burnTime);
        output.putDouble("heat", burnHeat);
        output.putInt("maxBurn", maxBurnTime);
        output.putInt("steamUsed", steamUsed);
        if (this.output != null) {
            output.putInt("outType", this.output.material.id);
            output.putInt("outAmount", this.output.amount);
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRotaryFurnace(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            case 1 -> output.writeBoolean(this.isProgressing);
            case 2 -> output.writeFloat(this.progress);
            case 3 -> output.writeInt(this.burnTime);
            case 4 -> output.writeInt(this.maxBurnTime);
            case 5 -> writeOutput(output);
            case 6 -> writePour(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            case 1 -> this.isProgressing = input.readBoolean();
            case 2 -> this.progress = input.readFloat();
            case 3 -> this.burnTime = input.readInt();
            case 4 -> this.maxBurnTime = input.readInt();
            case 5 -> readOutput(input);
            case 6 -> readPour(input);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public long syncEventUnits() {
        return 1L << 6;
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 1L << 6) != 0 && level != null && level.isClientSide() && pourColor != -1) {
            streams.add(
                    new BlockEntityCrucible.PourStream(
                            pourColor, false, pourLen, level.getGameTime()));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}
}
