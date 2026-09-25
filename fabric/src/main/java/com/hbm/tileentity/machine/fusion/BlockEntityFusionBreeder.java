// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.MenuFusionBreeder;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FluidBreederRecipe;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.OutgasserRecipe;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFusionBreeder extends BlockEntityMachineBase
        implements FluidTankEndpoint,
                IFusionPowerReceiver,
                MenuProvider,
                IPortHost,
                SyncUnitSchema {
    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final double CAPACITY = 10_000D;
    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};

    private static final ItemPort OUTPUT_PORT = ItemPort.extractOnly(SLOT_OUTPUT);

    @SyncField(units = 1L << 2)
    public final FluidTankNTM[] tanks;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final FusionItemAccessCache itemPorts = new FusionItemAccessCache(OUTPUT_PORT);
    public double neutronEnergy;

    @SyncField(units = 1L << 0)
    public double neutronEnergySync;

    @SyncField(units = 1L << 1)
    public double progress;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityFusionBreeder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BREEDER.get(), pos, state, SLOT_COUNT);
        tanks = new FluidTankNTM[] {new FluidTankNTM(16_000), new FluidTankNTM(16_000)};
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2),
                        dir));
    }

    @Override
    public void tickServer() {
        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();

        if (!canProcessSolid() && !canProcessLiquid()) progress = 0;

        neutronEnergySync = neutronEnergy;

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
        neutronEnergy = 0;
    }

    public boolean canProcessSolid() {

        if (inventory.get(SLOT_INPUT).is(ModItems.METEORITE_SWORD_IRRADIATED.get())
                && inventory.get(SLOT_OUTPUT).isEmpty()) {
            return true;
        }

        OutgasserRecipe recipe = recipeFor(inventory.get(SLOT_INPUT));
        if (recipe == null) return false;

        FluidStackNTM fluid = recipe.fluidOutput();
        if (fluid != null) {
            if (tanks[1].getTankType() != fluid.type() && tanks[1].getFill() > 0) return false;
            tanks[1].setTankType(fluid.type());
            if (tanks[1].getFill() + fluid.amount() > tanks[1].getMaxFill()) return false;
        }

        ItemStack out = recipe.solidOutput();
        ItemStack held = inventory.get(SLOT_OUTPUT);
        if (held.isEmpty() || out.isEmpty()) return true;

        return ItemStack.isSameItemSameComponents(held, out)
                && held.getCount() + out.getCount() <= held.getMaxStackSize();
    }

    public boolean canProcessLiquid() {
        FluidBreederRecipe recipe = FluidBreederRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        if (recipe == null) return false;
        if (tanks[0].getFill() < recipe.cost()) return false;

        FluidStackNTM fluid = recipe.output();
        if (tanks[1].getTankType() != fluid.type() && tanks[1].getFill() > 0) return false;
        tanks[1].setTankType(fluid.type());
        return tanks[1].getFill() + fluid.amount() <= tanks[1].getMaxFill();
    }

    private void processSolid() {

        if (inventory.get(SLOT_INPUT).is(ModItems.METEORITE_SWORD_IRRADIATED.get())) {
            removeItem(SLOT_INPUT, 1);
            inventory.set(SLOT_OUTPUT, new ItemStack(ModItems.METEORITE_SWORD_FUSED.get()));
            progress = 0;
            return;
        }

        OutgasserRecipe recipe = recipeFor(inventory.get(SLOT_INPUT));
        removeItem(SLOT_INPUT, 1);
        progress = 0;

        FluidStackNTM fluid = recipe.fluidOutput();
        if (fluid != null) tanks[1].setFill(tanks[1].getFill() + (int) fluid.amount());

        ItemStack out = recipe.solidOutput();
        if (out.isEmpty()) return;

        ItemStack held = inventory.get(SLOT_OUTPUT);
        if (held.isEmpty()) {
            inventory.set(SLOT_OUTPUT, out.copy());
        } else {
            held.grow(out.getCount());
        }
    }

    private void processLiquid() {
        FluidBreederRecipe recipe = FluidBreederRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        tanks[0].setFill(tanks[0].getFill() - recipe.cost());
        tanks[1].setFill(tanks[1].getFill() + (int) recipe.output().amount());
    }

    public void doProgress() {
        if (canProcessSolid()) {
            progress += neutronEnergy;
            if (progress > CAPACITY) {
                processSolid();
                progress = 0;
                setChanged();
            }
        } else if (canProcessLiquid()) {
            progress += neutronEnergy;
            if (progress > CAPACITY) {
                processLiquid();
                progress = 0;
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    private @Nullable OutgasserRecipe recipeFor(ItemStack stack) {
        return level == null ? null : OutgasserRecipes.INSTANCE.getRecipeForFusion(stack, level);
    }

    @Override
    public boolean receivesFusionPower() {
        return false;
    }

    @Override
    public void receiveFusionPower(
            long fusionPower, double neutronPower, float r, float g, float b) {
        neutronEnergy = neutronPower;
        doProgress();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof FluidIdentifierItem;
        if (slot == SLOT_INPUT) return recipeFor(stack) != null;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getDoubleOr("progress", progress);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionBreeder");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFusionBreeder(containerId, playerInventory, this);
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
    }

    private void readNeutronEnergy(ByteBuf input) {
        neutronEnergy = input.readDouble();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(this.neutronEnergySync);
            case 1 -> output.writeDouble(this.progress);
            case 2 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readNeutronEnergy(input);
            case 1 -> this.progress = input.readDouble();
            case 2 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
