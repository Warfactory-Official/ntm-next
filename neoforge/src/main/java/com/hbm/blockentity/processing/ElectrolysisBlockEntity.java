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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MaterialSettingsHandler;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.BlockMenuType;
import com.hbm.inventory.container.ElectrolyserFluidMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipe;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipe;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncInventory;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ElectrolysisBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                PlainFluidReceiver,
                PlainFluidProvider,
                FluidFlushSender,
                FluidSettingsHandler,
                MenuProvider,
                UpgradeInfoProvider,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 21;
    public static final long MAX_POWER = 20_000_000L;
    public static final int MAX_MATERIAL = MaterialShape.BLOCK.amount(16);
    private static final int STEP_POWER = 10_000;
    private static final long TANK_UNITS = 0x78L;
    private static final long LOOP_UNITS = 6L;
    private static final long POUR_UNITS = 3L << 10;
    private static final int[] ACCESSIBLE = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);

    static {
        SyncInventory.slots(
                ElectrolysisBlockEntity.class, new int[] {1, 2, 14}, LOOP_UNITS, false, true);
    }

    public long power;
    public int progressFluid;
    public int usageFluid;

    {
        hbm$syncSet$ElectrolysisBlockEntity$usageFluid(STEP_POWER);
    }

    public int processFluidTime;

    {
        hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(100);
    }

    public int progressOre;
    public int usageOre;

    {
        hbm$syncSet$ElectrolysisBlockEntity$usageOre(STEP_POWER);
    }

    public int processOreTime;

    {
        hbm$syncSet$ElectrolysisBlockEntity$processOreTime(600);
    }

    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.WATER, 16_000),
        new FluidTankNTM(NTMFluids.HYDROGEN, 16_000),
        new FluidTankNTM(NTMFluids.OXYGEN, 16_000),
        new FluidTankNTM(NTMFluids.NITRIC_ACID, 16_000)
    };
    public @Nullable MaterialStack leftStack;
    public @Nullable MaterialStack rightStack;
    public int lastSelectedGUI;
    private int leftPourColor;

    {
        hbm$syncSet$ElectrolysisBlockEntity$leftPourColor(-1);
    }

    private float leftPourLen;
    private int rightPourColor;

    {
        hbm$syncSet$ElectrolysisBlockEntity$rightPourColor(-1);
    }

    private float rightPourLen;

    public final List<PourStream> streams = new ArrayList<>();
    private final FluidTankNTM[] sending = {tanks[1], tanks[2]};
    private final List<MaterialStack> leftPour = Arrays.asList(new MaterialStack[1]);
    private final List<MaterialStack> rightPour = Arrays.asList(new MaterialStack[1]);
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public ElectrolysisBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYSER.get(), pos, state, SLOT_COUNT);
        for (var tank : tanks) tank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        int previousFluidProgress = progressFluid;
        int previousOreProgress = progressOre;
        int previousFluidTime = processFluidTime;
        int previousOreTime = processOreTime;
        hbm$syncSet$ElectrolysisBlockEntity$power(
                (long) (power + (ItemEnergyTransfer.extract(this, 0, MAX_POWER - power, false))));
        if (tanks[0].setType(3, 4, inventory)
                | tanks[0].loadTank(5, 6, inventory)
                | tanks[1].unloadTank(7, 8, inventory)
                | tanks[2].unloadTank(9, 10, inventory)) setChanged();
        upgrades.scan(this, 1, 2);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int efficiency = upgrades.getLevel(UpgradeType.POWER);
        int usage = STEP_POWER - STEP_POWER * efficiency / 4 + STEP_POWER * speed;
        hbm$syncSet$ElectrolysisBlockEntity$usageOre(usage);
        hbm$syncSet$ElectrolysisBlockEntity$usageFluid(usage);

        var fluidRecipe = ElectrolyserFluidRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        var metalRecipe = ElectrolyserMetalRecipes.INSTANCE.getRecipe(getItem(14));
        hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(
                duration(fluidRecipe, 100, speed, efficiency));
        hbm$syncSet$ElectrolysisBlockEntity$processOreTime(
                duration(metalRecipe, 600, speed, efficiency));
        int cycles = getCycleCount();
        for (int cycle = 0; cycle < cycles; cycle++) {
            if (power >= usageFluid && fluidRecipe != null && admitsFluid(fluidRecipe)) {
                hbm$syncSet$ElectrolysisBlockEntity$progressFluid((int) (progressFluid + 1));
                hbm$syncSet$ElectrolysisBlockEntity$power((long) (power - (usageFluid)));
                if (progressFluid >= processFluidTime) {
                    settleFluid(fluidRecipe);
                    hbm$syncSet$ElectrolysisBlockEntity$progressFluid(0);
                    fluidRecipe =
                            ElectrolyserFluidRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
                    hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(
                            duration(fluidRecipe, 100, speed, efficiency));
                }
            }
            if (power >= usageOre && metalRecipe != null && admitsMetal(metalRecipe)) {
                hbm$syncSet$ElectrolysisBlockEntity$progressOre((int) (progressOre + 1));
                hbm$syncSet$ElectrolysisBlockEntity$power((long) (power - (usageOre)));
                if (progressOre >= processOreTime) {
                    settleMetal(metalRecipe);
                    hbm$syncSet$ElectrolysisBlockEntity$progressOre(0);
                    metalRecipe = ElectrolyserMetalRecipes.INSTANCE.getRecipe(getItem(14));
                    hbm$syncSet$ElectrolysisBlockEntity$processOreTime(
                            duration(metalRecipe, 600, speed, efficiency));
                }
            }
        }

        hbm$syncSet$ElectrolysisBlockEntity$rightPourColor(-1);
        hbm$syncSet$ElectrolysisBlockEntity$leftPourColor(-1);
        int rate = MaterialShape.NUGGET.amount(3) * Math.max(cycles * speed, 1);
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        if (leftStack != null) {
            leftPour.set(0, leftStack);
            var result = pour(leftPour, facing.getOpposite(), rate);
            if (result != null) {
                hbm$syncSet$ElectrolysisBlockEntity$leftPourColor(result.material().moltenColor());
                hbm$syncSet$ElectrolysisBlockEntity$leftPourLen(streamLength(result));
                if (leftStack.amount == 0) hbm$syncSet$ElectrolysisBlockEntity$leftStack(null);
            }
        }
        if (rightStack != null) {
            rightPour.set(0, rightStack);
            var result = pour(rightPour, facing, rate);
            if (result != null) {
                hbm$syncSet$ElectrolysisBlockEntity$rightPourColor(result.material().moltenColor());
                hbm$syncSet$ElectrolysisBlockEntity$rightPourLen(streamLength(result));
                if (rightStack.amount == 0) hbm$syncSet$ElectrolysisBlockEntity$rightStack(null);
            }
        }
        if (leftPourColor != -1 || rightPourColor != -1) {
            markSyncEvent();
            setChanged();
        }
        flush.provide((ServerLevel) level, this);
        if (power != previousPower
                || progressFluid != previousFluidProgress
                || progressOre != previousOreProgress
                || processFluidTime != previousFluidTime
                || processOreTime != previousOreTime) setChanged();
        networkPackNT(50);
    }

    private boolean admitsFluid(ElectrolyserFluidRecipe recipe) {
        conformOutput(tanks[1], recipe, 0);
        conformOutput(tanks[2], recipe, 1);
        return recipe.inputFluid[0].matches(tanks[0])
                && recipe.outputFluidsFit(sending)
                && byproductsFit(recipe, 11);
    }

    private void conformOutput(FluidTankNTM tank, GenericRecipe recipe, int output) {
        var previousType = tank.getDeclaredFluid();
        int previousPressure = tank.getPressure();
        int previousFill = tank.getFill();
        if (output < recipe.outputFluid.length) tank.conform(recipe.outputFluid[output]);
        else tank.resetTank();
        if (previousType != tank.getDeclaredFluid()
                || previousPressure != tank.getPressure()
                || previousFill != tank.getFill()) setChanged();
    }

    private boolean admitsMetal(ElectrolyserMetalRecipe recipe) {
        return !getItem(14).isEmpty()
                && tanks[3].getFill() >= 100
                && materialFits(leftStack, recipe.output1)
                && materialFits(rightStack, recipe.output2)
                && byproductsFit(recipe, 15);
    }

    private static boolean materialFits(
            @Nullable MaterialStack stock, @Nullable MaterialStack product) {
        if (stock == null) return product == null || product.amount <= MAX_MATERIAL;
        return product != null
                && stock.material == product.material
                && (long) stock.amount + product.amount <= MAX_MATERIAL;
    }

    private boolean byproductsFit(GenericRecipe recipe, int firstSlot) {
        var outputs = recipe.outputItems();
        if (outputs == null) return true;
        for (int i = 0; i < outputs.length; i++) {
            if (!Outputs.fits(getItem(firstSlot + i), outputs[i])) return false;
        }
        return true;
    }

    private void settleFluid(ElectrolyserFluidRecipe recipe) {
        recipe.inputFluid[0].drainFrom(tanks[0]);
        recipe.fillOutputFluids(sending);
        placeByproducts(recipe, 11);
        setChanged();
    }

    private void settleMetal(ElectrolyserMetalRecipe recipe) {
        if (leftStack == null) hbm$syncSet$ElectrolysisBlockEntity$leftStack(recipe.output1.copy());
        else
            leftStack.hbm$syncSet$MaterialStack$amount(
                    (int) (leftStack.amount + (recipe.output1.amount)));
        if (recipe.output2 != null) {
            if (rightStack == null)
                hbm$syncSet$ElectrolysisBlockEntity$rightStack(recipe.output2.copy());
            else
                rightStack.hbm$syncSet$MaterialStack$amount(
                        (int) (rightStack.amount + (recipe.output2.amount)));
        }
        placeByproducts(recipe, 15);
        tanks[3].setFill(tanks[3].getFill() - 100);
        removeItem(14, 1);
        setChanged();
    }

    private void placeByproducts(GenericRecipe recipe, int firstSlot) {
        var outputs = recipe.outputItems();
        if (outputs == null) return;
        for (int i = 0; i < outputs.length; i++) {
            var product = outputs[i].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
            if (product.isEmpty()) continue;
            var stock = getItem(firstSlot + i);
            if (stock.isEmpty()) setItem(firstSlot + i, product.copy());
            else stock.grow(product.getCount());
        }
    }

    private CrucibleUtil.@Nullable Pour pour(
            List<MaterialStack> stock, Direction direction, int rate) {
        return CrucibleUtil.pourFirst(
                level,
                worldPosition.getX() + .5D + direction.getStepX() * 5.875D,
                worldPosition.getY() + 2D,
                worldPosition.getZ() + .5D + direction.getStepZ() * 5.875D,
                6D,
                stock,
                rate);
    }

    private float streamLength(CrucibleUtil.Pour result) {
        return Math.max(
                1F, worldPosition.getY() - (float) (Math.ceil(result.impact().y) - .875D) + 2F);
    }

    private static int duration(
            @Nullable GenericRecipe recipe, int fallback, int speed, int efficiency) {
        int adjustedSpeed = speed - Math.min(efficiency, 1);
        float multiplier = 1F - .25F * adjustedSpeed;
        return recipe == null
                ? fallback
                : (int) Math.ceil(recipe.duration * Math.max(multiplier, .2D));
    }

    public int getCycleCount() {
        return Math.min(1 + 2 * upgrades.getLevel(UpgradeType.OVERDRIVE), 7);
    }

    public int getDurationFluid() {
        return processFluidTime;
    }

    public int getDurationMetal() {
        return processOreTime;
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        for (int index = streams.size() - 1; index >= 0; index--) {
            if (now - streams.get(index).birth() >= 20) streams.remove(index);
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void setPower(long amount) {
        hbm$syncSet$ElectrolysisBlockEntity$power(Math.clamp(amount, 0L, MAX_POWER));
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type) || tanks[3].joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        return room(tanks[0], type) + room(tanks[3], type);
    }

    private static long room(FluidTankNTM tank, Fluid type) {
        return tank.accepts(type) ? tank.getMaxFill() - tank.getFill() : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        if (tanks[0].accepts(type))
            remaining -= tanks[0].receive(type, (int) Math.min(remaining, Integer.MAX_VALUE));
        if (tanks[3].accepts(type))
            remaining -= tanks[3].receive(type, (int) Math.min(remaining, Integer.MAX_VALUE));
        if (remaining != amount) setChanged();
        return remaining;
    }

    @Override
    public int receivingPressureMask(Fluid type) {
        return canReceiveFluid(type) ? 1 : 0;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        long amount = 0;
        for (var tank : sending) {
            if (tank.getPressure() == pressure && tank.provides(type)) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        long remaining = amount;
        for (var tank : sending) {
            if (tank.getPressure() == pressure && tank.provides(type)) {
                remaining -= tank.drain((int) Math.min(remaining, Integer.MAX_VALUE), true);
            }
        }
        if (remaining != amount) setChanged();
    }

    @Override
    public int providingPressureMask(Fluid type) {
        int mask = 0;
        for (var tank : sending) {
            if (tank.provides(type)) mask |= 1 << tank.getPressure();
        }
        return mask;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[0];
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (var tank : tanks) visitor.accept(tank);
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        var faces = FlushFaces.activePlane();
        for (var tank : sending) lanes.add(tank, faces, 20);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 14 && ElectrolyserMetalRecipes.INSTANCE.getRecipe(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 11 && slot < SLOT_COUNT && slot != 14;
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        return MaterialSettingsHandler.writeMaterials(
                FluidSettingsHandler.super.copySettings(level, pos), materialIds());
    }

    private int[] materialIds() {
        if (leftStack == null)
            return rightStack == null ? new int[0] : new int[] {rightStack.material.id()};
        return rightStack == null
                ? new int[] {leftStack.material.id()}
                : new int[] {leftStack.material.id(), rightStack.material.id()};
    }

    @Override
    public String[] getSettingsOptions(Level level, BlockPos pos) {
        var fluids = FluidSettingsHandler.super.getSettingsOptions(level, pos);
        var materials = MaterialSettingsHandler.materialKeys(materialIds());
        var options = Arrays.copyOf(fluids, fluids.length + materials.length);
        System.arraycopy(materials, 0, options, fluids.length, materials.length);
        return options;
    }

    public BlockMenuType<ElectrolysisBlockEntity, ? extends AbstractContainerMenu> selectedMenu() {
        return switch (lastSelectedGUI) {
            case 0 -> ModMenus.MACHINE_ELECTROLYSER_FLUID;
            case 1 -> ModMenus.MACHINE_ELECTROLYSER_METAL;
            default ->
                    throw new IllegalArgumentException(
                            "Unknown electrolysis menu " + lastSelectedGUI);
        };
    }

    public void openFluidMenu(Player player) {
        openMenu(player, 0);
    }

    public void openMetalMenu(Player player) {
        openMenu(player, 1);
    }

    private void openMenu(Player player, int menu) {
        if (!player.isSpectator() && lastSelectedGUI != menu) {
            hbm$syncSet$ElectrolysisBlockEntity$lastSelectedGUI(menu);
            setChanged();
        }
        if (menu == 0) ModMenus.MACHINE_ELECTROLYSER_FLUID.open(player, this);
        else ModMenus.MACHINE_ELECTROLYSER_METAL.open(player, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineElectrolyser");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ElectrolyserFluidMenu(id, inventory, this);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        switch (type) {
            case SPEED -> UpgradeInfo.speedAndPower(tooltip, -level * 25, level * 100);
            case POWER -> UpgradeInfo.powerAndDelay(tooltip, -level * 25, level == 0 ? 0 : 25);
            case OVERDRIVE -> UpgradeInfo.percent(tooltip, "upgrade.speed", level * 200, true);
            default -> throw new IllegalArgumentException(type.name());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progressFluid", progressFluid);
        output.putInt("progressOre", progressOre);
        output.putInt("processFluidTime", processFluidTime);
        output.putInt("processOreTime", processOreTime);
        output.putInt("lastSelectedGUI", lastSelectedGUI);
        saveMaterial(output, "left", leftStack);
        saveMaterial(output, "right", rightStack);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
    }

    private static void saveMaterial(
            ValueOutput output, String side, @Nullable MaterialStack stock) {
        if (stock == null) return;
        output.putInt(side + "Type", stock.material.id());
        output.putInt(side + "Amount", stock.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ElectrolysisBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$ElectrolysisBlockEntity$progressFluid(
                input.getIntOr("progressFluid", progressFluid));
        hbm$syncSet$ElectrolysisBlockEntity$progressOre(input.getIntOr("progressOre", progressOre));
        hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(
                input.getIntOr("processFluidTime", processFluidTime));
        hbm$syncSet$ElectrolysisBlockEntity$processOreTime(
                input.getIntOr("processOreTime", processOreTime));
        hbm$syncSet$ElectrolysisBlockEntity$lastSelectedGUI(
                input.getIntOr("lastSelectedGUI", lastSelectedGUI));
        hbm$syncSet$ElectrolysisBlockEntity$leftStack(loadMaterial(input, "left"));
        hbm$syncSet$ElectrolysisBlockEntity$rightStack(loadMaterial(input, "right"));
        for (int i = 0; i < tanks.length; i++) {
            var tank = tanks[i];
            input.child("t" + i).ifPresent(tank::deserialize);
        }
    }

    private static @Nullable MaterialStack loadMaterial(ValueInput input, String side) {
        return input.getInt(side + "Type")
                .map(id -> new MaterialStack(material(id), input.getIntOr(side + "Amount", 0)))
                .orElse(null);
    }

    private static Material material(int id) {
        var material = MaterialRegistry.byId(id);
        if (material == null)
            throw new IllegalArgumentException("Unknown electrolysis material " + id);
        return material;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 4095L;
    }

    @Override
    public long syncEventUnits() {
        return POUR_UNITS;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> {
                output.writeInt(progressFluid);
                output.writeInt(usageFluid);
                output.writeInt(processFluidTime);
            }
            case 2 -> {
                output.writeInt(progressOre);
                output.writeInt(usageOre);
                output.writeInt(processOreTime);
            }
            case 3, 4, 5, 6 -> tanks[unit - 3].packetSerialize(output);
            case 7 -> writeMaterial(output, leftStack);
            case 8 -> writeMaterial(output, rightStack);
            case 9 -> output.writeInt(lastSelectedGUI);
            case 10 -> {
                output.writeInt(leftPourColor);
                output.writeFloat(leftPourLen);
            }
            case 11 -> {
                output.writeInt(rightPourColor);
                output.writeFloat(rightPourLen);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    private static void writeMaterial(ByteBuf output, @Nullable MaterialStack stock) {
        output.writeBoolean(stock != null);
        if (stock == null) return;
        output.writeInt(stock.material.id());
        output.writeInt(stock.amount);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ElectrolysisBlockEntity$power(input.readLong());
            case 1 -> {
                hbm$syncSet$ElectrolysisBlockEntity$progressFluid(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$usageFluid(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(input.readInt());
            }
            case 2 -> {
                hbm$syncSet$ElectrolysisBlockEntity$progressOre(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$usageOre(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$processOreTime(input.readInt());
            }
            case 3, 4, 5, 6 -> tanks[unit - 3].packetDeserialize(input);
            case 7 -> hbm$syncSet$ElectrolysisBlockEntity$leftStack(readMaterial(input));
            case 8 -> hbm$syncSet$ElectrolysisBlockEntity$rightStack(readMaterial(input));
            case 9 -> hbm$syncSet$ElectrolysisBlockEntity$lastSelectedGUI(input.readInt());
            case 10 -> {
                hbm$syncSet$ElectrolysisBlockEntity$leftPourColor(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$leftPourLen(input.readFloat());
            }
            case 11 -> {
                hbm$syncSet$ElectrolysisBlockEntity$rightPourColor(input.readInt());
                hbm$syncSet$ElectrolysisBlockEntity$rightPourLen(input.readFloat());
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    private static @Nullable MaterialStack readMaterial(ByteBuf input) {
        return input.readBoolean()
                ? new MaterialStack(material(input.readInt()), input.readInt())
                : null;
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & (1L << 10)) != 0 && leftPourColor != -1)
            streams.add(new PourStream(leftPourColor, true, leftPourLen, level.getGameTime()));
        if ((units & (1L << 11)) != 0 && rightPourColor != -1)
            streams.add(new PourStream(rightPourColor, false, rightPourLen, level.getGameTime()));
    }

    @Override
    public void afterInitialSyncUnits() {}

    @Override
    public void bindSyncValue(Object value, int mask, long units) {
        if (value == tanks) {
            for (int i = 0; i < tanks.length; i++)
                super.bindSyncValue(tanks[i], mask, 1L << (i + 3));
        } else super.bindSyncValue(value, mask, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int mask, long units) {
        if (value != tanks) {
            super.syncArrayChanged(value, index, mask, units);
        } else if (index < 0) {
            bindSyncValue(value, mask, TANK_UNITS);
            syncUnitsChanged(mask, TANK_UNITS);
        } else {
            long unit = 1L << (index + 3);
            if (syncBound()) super.bindSyncValue(tanks[index], mask, unit);
            syncUnitsChanged(mask, unit);
        }
    }

    public record PourStream(int color, boolean left, float len, long birth) {}

    public final void hbm$syncSet$ElectrolysisBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$progressFluid(int next) {
        if (this.progressFluid != next) {
            this.progressFluid = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$usageFluid(int next) {
        if (this.usageFluid != next) {
            this.usageFluid = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$processFluidTime(int next) {
        if (this.processFluidTime != next) {
            this.processFluidTime = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$progressOre(int next) {
        if (this.progressOre != next) {
            this.progressOre = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$usageOre(int next) {
        if (this.usageOre != next) {
            this.usageOre = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$processOreTime(int next) {
        if (this.processOreTime != next) {
            this.processOreTime = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$ElectrolysisBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, TANK_UNITS);
            }
        }
        return next;
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$leftStack(MaterialStack next) {
        if (this.leftStack != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.leftStack);
            this.leftStack = next;
            if (bound) bindSyncValue(next, 3, 1L << 7);
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$rightStack(MaterialStack next) {
        if (this.rightStack != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.rightStack);
            this.rightStack = next;
            if (bound) bindSyncValue(next, 3, 1L << 8);
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$lastSelectedGUI(int next) {
        if (this.lastSelectedGUI != next) {
            this.lastSelectedGUI = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$leftPourColor(int next) {
        if (this.leftPourColor != next) {
            this.leftPourColor = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$leftPourLen(float next) {
        if (Float.floatToRawIntBits(this.leftPourLen) != Float.floatToRawIntBits(next)) {
            this.leftPourLen = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$rightPourColor(int next) {
        if (this.rightPourColor != next) {
            this.rightPourColor = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    public final void hbm$syncSet$ElectrolysisBlockEntity$rightPourLen(float next) {
        if (Float.floatToRawIntBits(this.rightPourLen) != Float.floatToRawIntBits(next)) {
            this.rightPourLen = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ElectrolysisBlockEntity.class, "tanks", 3, TANK_UNITS);
        com.hbm.packet.SyncBindings.field(ElectrolysisBlockEntity.class, "leftStack", 3, 1L << 7);
        com.hbm.packet.SyncBindings.field(ElectrolysisBlockEntity.class, "rightStack", 3, 1L << 8);
        com.hbm.packet.SyncBindings.field(ElectrolysisBlockEntity.class, "upgrades", 3, LOOP_UNITS);
    }
}
