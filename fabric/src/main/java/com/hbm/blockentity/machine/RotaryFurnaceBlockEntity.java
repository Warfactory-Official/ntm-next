/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.capability.port.ItemPortCache;
import com.hbm.handler.FuelHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.RotaryFurnaceMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.recipes.RotaryFurnaceRecipe;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
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

public class RotaryFurnaceBlockEntity extends PollutingMachineBlockEntity
        implements FluidTankAccess,
                FluidTankHandler,
                MenuProvider,
                CapabilityPortProvider,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 5;
    public static final int SLOT_FUEL = 4;
    public static final int MAX_OUTPUT = MaterialShape.BLOCK.amount(16);

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
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    public final List<CrucibleBlockEntity.PourStream> streams = new ArrayList<>();
    public boolean isProgressing;
    public float progress;
    public int burnTime;
    public double burnHeat = 1D;
    public int maxBurnTime;
    public int steamUsed = 0;
    public @Nullable MaterialStack output;
    public int anim;
    public int lastAnim;
    private int pourColor;

    {
        hbm$syncSet$RotaryFurnaceBlockEntity$pourColor(-1);
    }

    private float pourLen;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public RotaryFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTARY_FURNACE.get(), pos, state, SLOT_COUNT, 50);
        hbm$syncArraySet$RotaryFurnaceBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.NONE, 16_000));
        hbm$syncArraySet$RotaryFurnaceBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.STEAM, 12_000));
        hbm$syncArraySet$RotaryFurnaceBlockEntity$tanks(
                tanks, 2, new FluidTankNTM(NTMFluids.SPENTSTEAM, 120));
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
        return state.hasProperty(MultiblockCoreBlock.FACING)
                ? state.getValue(MultiblockCoreBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    public void tickServer() {
        float prevProgress = this.progress;
        this.hbm$syncSet$RotaryFurnaceBlockEntity$pourColor(-1);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        updateTypeFromIdentifier();

        Direction dir = coreFacing();
        Direction rot = dir.getCounterClockWise();

        flush.provide((ServerLevel) level, this);

        if (this.output != null) {
            CrucibleUtil.Pour pour =
                    CrucibleUtil.pourStack(
                            level,
                            x + 0.5D + rot.getStepX() * 2.875D,
                            y + 1.25D,
                            z + 0.5D + rot.getStepZ() * 2.875D,
                            6,
                            this.output);

            if (pour != null && pour.amount() != 0) {
                this.hbm$syncSet$RotaryFurnaceBlockEntity$pourColor(pour.material().moltenColor());
                this.hbm$syncSet$RotaryFurnaceBlockEntity$pourLen(
                        Math.max(1F, y + 1 - (float) (Math.ceil(pour.impact().y) - 1.125)));
                markSyncEvent();
                markChanged();
            }

            if (output.amount <= 0) this.hbm$syncSet$RotaryFurnaceBlockEntity$output(null);
        }

        RotaryFurnaceRecipe recipe =
                RotaryFurnaceRecipes.getRecipe(
                        inventory.get(0), inventory.get(1), inventory.get(2));
        this.hbm$syncSet$RotaryFurnaceBlockEntity$isProgressing(false);

        if (recipe != null) {

            ItemStack fuel = inventory.get(4);
            if (this.burnTime <= 0 && !fuel.isEmpty() && FuelHandler.getBurnTime(level, fuel) > 0) {
                this.burnHeat = BURN_MODULE.getHeatMod(fuel);
                this.hbm$syncSet$RotaryFurnaceBlockEntity$burnTime(
                        BURN_MODULE.getBurnTime(level, fuel) / 2);
                this.hbm$syncSet$RotaryFurnaceBlockEntity$maxBurnTime(this.burnTime);
                removeItem(4, 1);
                setChanged();
            }

            float processSpeed = Math.max((float) burnHeat, 1);
            float steamUseMult = (float) (10 * Math.log10(processSpeed) + 1);

            if (this.canProcess(recipe, steamUseMult)) {
                this.hbm$syncSet$RotaryFurnaceBlockEntity$progress(
                        (float) (this.progress + (processSpeed / recipe.duration)));

                tanks[1].setFill((int) (tanks[1].getFill() - recipe.steam * steamUseMult));
                steamUsed += (int) (recipe.steam * steamUseMult);
                this.hbm$syncSet$RotaryFurnaceBlockEntity$isProgressing(true);

                if (this.progress >= 1F) {
                    this.hbm$syncSet$RotaryFurnaceBlockEntity$progress(
                            (float) (this.progress - (1F)));
                    this.consumeItems(recipe);

                    if (this.output == null) {
                        this.hbm$syncSet$RotaryFurnaceBlockEntity$output(recipe.output.copy());
                    } else {
                        this.output.hbm$syncSet$MaterialStack$amount(
                                (int) (this.output.amount + (recipe.output.amount)));
                    }
                    setChanged();
                }

                if (this.burnTime > 0) {
                    this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 10F);
                    this.hbm$syncSet$RotaryFurnaceBlockEntity$burnTime((int) (this.burnTime - 1));
                }

            } else {
                this.hbm$syncSet$RotaryFurnaceBlockEntity$progress(0);
            }

            if (this.steamUsed >= 100) {
                int steamReturn = this.steamUsed / 100;
                int canReturn = tanks[2].getMaxFill() - tanks[2].getFill();
                int doesReturn = Math.min(steamReturn, canReturn);
                this.steamUsed -= doesReturn * 100;
                tanks[2].setFill(tanks[2].getFill() + doesReturn);
                if (doesReturn > 0) markChanged();
            }

        } else {
            this.hbm$syncSet$RotaryFurnaceBlockEntity$progress(0);
        }
        if (this.progress != prevProgress) markChanged();

        this.networkPackNT(50);
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        for (var it = streams.iterator(); it.hasNext(); ) {
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
            if (this.output.amount + recipe.output.amount > MAX_OUTPUT) return false;
        }

        return breatheAir(1);
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
        if (overflow < fluidAmount) markChanged();
        if (overflow > 0) {
            PollutionHandler.add(level, worldPosition, type, overflow / 100F);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 3 || slot == SLOT_FUEL;
    }

    private final ItemPortCache portCache = new ItemPortCache(this::canPlaceItem, null);

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
        return portCache.forSlots(slots);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int tankCount() {
        return 4;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> tanks[0];
            case 1 -> tanks[1];
            case 2 -> tanks[2];
            case 3 -> smoke;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public FluidTankNTM pasteTank() {
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
            outputBuffer.writeInt(output.material.id());
            outputBuffer.writeInt(output.amount);
        }
    }

    private void readOutput(ByteBuf input) {
        hbm$syncSet$RotaryFurnaceBlockEntity$output(
                input.readBoolean()
                        ? new MaterialStack(MaterialRegistry.byId(input.readInt()), input.readInt())
                        : null);
    }

    private void writePour(ByteBuf output) {
        output.writeInt(pourColor);
        output.writeFloat(pourLen);
    }

    private void readPour(ByteBuf input) {
        hbm$syncSet$RotaryFurnaceBlockEntity$pourColor(input.readInt());
        hbm$syncSet$RotaryFurnaceBlockEntity$pourLen(input.readFloat());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        input.child("t2").ifPresent(tanks[2]::deserialize);
        hbm$syncSet$RotaryFurnaceBlockEntity$progress(input.getFloatOr("prog", progress));
        input.getInt("burn").ifPresent(v -> hbm$syncSet$RotaryFurnaceBlockEntity$burnTime(v));
        burnHeat = input.getDoubleOr("heat", burnHeat);
        input.getInt("maxBurn").ifPresent(v -> hbm$syncSet$RotaryFurnaceBlockEntity$maxBurnTime(v));
        input.getInt("steamUsed").ifPresent(v -> steamUsed = v);
        input.getInt("outType")
                .ifPresent(
                        type ->
                                this.hbm$syncSet$RotaryFurnaceBlockEntity$output(
                                        new MaterialStack(
                                                MaterialRegistry.byId(type),
                                                input.getIntOr("outAmount", 0))));
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
            output.putInt("outType", this.output.material.id());
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
        return new RotaryFurnaceMenu(containerId, playerInventory, this);
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
            case 1 -> this.hbm$syncSet$RotaryFurnaceBlockEntity$isProgressing(input.readBoolean());
            case 2 -> this.hbm$syncSet$RotaryFurnaceBlockEntity$progress(input.readFloat());
            case 3 -> this.hbm$syncSet$RotaryFurnaceBlockEntity$burnTime(input.readInt());
            case 4 -> this.hbm$syncSet$RotaryFurnaceBlockEntity$maxBurnTime(input.readInt());
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
                    new CrucibleBlockEntity.PourStream(
                            pourColor, false, pourLen, level.getGameTime()));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}

    public final FluidTankNTM hbm$syncArraySet$RotaryFurnaceBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 0);
            }
        }
        return next;
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$progress(float next) {
        if (Float.floatToRawIntBits(this.progress) != Float.floatToRawIntBits(next)) {
            this.progress = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$maxBurnTime(int next) {
        if (this.maxBurnTime != next) {
            this.maxBurnTime = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$output(MaterialStack next) {
        if (this.output != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.output);
            this.output = next;
            if (bound) bindSyncValue(next, 3, 1L << 5);
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$pourColor(int next) {
        if (this.pourColor != next) {
            this.pourColor = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$RotaryFurnaceBlockEntity$pourLen(float next) {
        if (Float.floatToRawIntBits(this.pourLen) != Float.floatToRawIntBits(next)) {
            this.pourLen = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RotaryFurnaceBlockEntity.class, "tanks", 3, 1L << 0);
        com.hbm.packet.SyncBindings.field(RotaryFurnaceBlockEntity.class, "output", 3, 1L << 5);
    }
}
