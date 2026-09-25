// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineSILEX;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SILEXRecipe;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.WeightedRandomObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntitySILEX extends BlockEntityMachineBase
        implements IFluidHandlerMK2, MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_QUEUE_START = 5;
    public static final int SLOT_QUEUE_END = 10;
    public static final int SLOT_COUNT = 11;

    public static final int maxFill = 16000;
    private static final int[] ACCESSIBLE_SLOTS = {0, 5, 6, 7, 8, 9, 10};

    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT, 5, 6, 7, 8, 9, 10};
    public final int processTime = 100;

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.PEROXIDE, maxFill);

    @SyncField(units = 1L << 2)
    public EnumWavelengths mode = EnumWavelengths.NULL;

    @SyncField(units = 1L << 4)
    public @Nullable ItemStack current;

    @SyncField(units = 1L << 5)
    public @Nullable Fluid currentFluid;

    @SyncField(units = 1L << 0)
    public int currentFill;

    @SyncField(units = 1L << 1)
    public int progress;

    public int recipeIndex;
    private int loadDelay;

    public BlockEntitySILEX(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILEX.get(), pos, state, SLOT_COUNT);
    }

    public void setMode(EnumWavelengths mode) {
        this.mode = mode;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSILEX");
    }

    @Override
    public void tickServer() {
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tank.loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();
        loadFluid();

        if (!process()) {
            this.progress = 0;
        }

        dequeue();

        if (currentFill <= 0) {
            current = null;
            currentFluid = null;
        }

        networkPackNT(50);

        this.mode = EnumWavelengths.NULL;
    }

    private void loadFluid() {
        Fluid type = tank.getTankType();
        if (SILEXRecipes.INSTANCE.getOutput(type) != null) {
            if (currentFill == 0) {
                current = null;
                currentFluid = type;
            }
            if (currentFluid == type) {
                int toFill = Math.min(50, Math.min(maxFill - currentFill, tank.getFill()));
                if (toFill > 0) {
                    currentFill += toFill;
                    tank.setFill(tank.getFill() - toFill);
                    setChanged();
                }
            }
        }

        loadDelay++;
        if (loadDelay > 20) loadDelay = 0;

        if (loadDelay != 0) return;

        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty() || type != NTMFluids.PEROXIDE || currentFluid != null) return;
        if (current != null && !ItemStack.isSameItemSameComponents(current, in.copyWithCount(1)))
            return;

        SILEXRecipe recipe = SILEXRecipes.INSTANCE.getOutput(in);
        if (recipe == null) return;

        int load = recipe.fluidProduced;
        if (load <= maxFill - currentFill && load <= tank.getFill()) {
            currentFill += load;
            current = in.copyWithCount(1);
            tank.setFill(tank.getFill() - load);
            in.shrink(1);
            if (in.isEmpty()) inventory.set(SLOT_INPUT, ItemStack.EMPTY);
            setChanged();
        }
    }

    private boolean process() {
        if (currentFill <= 0) return false;

        SILEXRecipe recipe =
                currentFluid != null
                        ? SILEXRecipes.INSTANCE.getOutput(currentFluid)
                        : current != null ? SILEXRecipes.INSTANCE.getOutput(current) : null;
        if (recipe == null) return false;

        if (recipe.laserStrength.ordinal() > this.mode.ordinal()) return false;
        if (currentFill < recipe.fluidConsumed) return false;
        if (!inventory.get(SLOT_OUTPUT).isEmpty()) return false;

        int progressSpeed =
                (int) Math.pow(2, this.mode.ordinal() - recipe.laserStrength.ordinal() + 1) / 2;
        progress += progressSpeed;

        if (progress >= processTime) {
            currentFill -= recipe.fluidConsumed;

            int[] cursor = {recipeIndex};
            ItemStack out = WeightedRandomObject.pickDeterministic(recipe.outputs(), cursor);
            recipeIndex = cursor[0];
            if (out != null) inventory.set(SLOT_OUTPUT, out);

            progress = 0;
            setChanged();
        }

        return true;
    }

    private void dequeue() {
        ItemStack output = inventory.get(SLOT_OUTPUT);
        if (output.isEmpty()) return;

        for (int i = SLOT_QUEUE_START; i <= SLOT_QUEUE_END; i++) {
            ItemStack slot = inventory.get(i);
            if (!slot.isEmpty()
                    && slot.getCount() < slot.getMaxStackSize()
                    && ItemStack.isSameItemSameComponents(output, slot)) {
                slot.grow(1);
                output.shrink(1);
                if (output.isEmpty()) inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }

        for (int i = SLOT_QUEUE_START; i <= SLOT_QUEUE_END; i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, output.copy());
                inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
    }

    public int getProgressScaled(int i) {
        return progress * i / processTime;
    }

    public int getFluidScaled(int i) {
        return tank.getMaxFill() <= 0 ? 0 : tank.getFill() * i / tank.getMaxFill();
    }

    public int getFillScaled(int i) {
        return currentFill * i / maxFill;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!tank.accepts(type) || pressure != tank.getPressure()) return 0L;
        return (long) maxFill - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!tank.accepts(type) || pressure != tank.getPressure()) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && SILEXRecipes.INSTANCE.getOutput(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_QUEUE_START;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("void")) {
            currentFill = 0;
            current = null;
            currentFluid = null;
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineSILEX(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
        currentFill = input.getIntOr("fill", 0);
        recipeIndex = input.getIntOr("recipeIndex", 0);
        mode = EnumWavelengths.valueOf(input.getStringOr("mode", "NULL"));
        current = input.read("current", ItemStack.CODEC).orElse(null);
        currentFluid =
                input.read("currentFluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putInt("fill", currentFill);
        output.putInt("recipeIndex", recipeIndex);
        output.putString("mode", mode.name());
        if (current != null) output.store("current", ItemStack.CODEC, current);
        if (currentFluid != null)
            output.store("currentFluid", BuiltInRegistries.FLUID.byNameCodec(), currentFluid);
    }

    private void writeMode(ByteBuf output) {
        output.writeInt(mode.ordinal());
    }

    private void readMode(ByteBuf input) {
        mode = EnumWavelengths.values()[input.readInt()];
    }

    private void writeCurrent(ByteBuf output) {
        output.writeBoolean(current != null);
        if (current != null) {
            output.writeInt(BuiltInRegistries.ITEM.getId(current.getItem()));
        }
    }

    private void readCurrent(ByteBuf input) {
        if (input.readBoolean()) {
            current = new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt()));
        } else {
            current = null;
        }
    }

    private void writeCurrentFluid(ByteBuf output) {
        output.writeBoolean(currentFluid != null);
        if (currentFluid != null) output.writeInt(BuiltInRegistries.FLUID.getId(currentFluid));
    }

    private void readCurrentFluid(ByteBuf input) {
        currentFluid = input.readBoolean() ? BuiltInRegistries.FLUID.byId(input.readInt()) : null;
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.currentFill);
            case 1 -> output.writeInt(this.progress);
            case 2 -> writeMode(output);
            case 3 -> this.tank.packetSerialize(output);
            case 4 -> writeCurrent(output);
            case 5 -> writeCurrentFluid(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.currentFill = input.readInt();
            case 1 -> this.progress = input.readInt();
            case 2 -> readMode(input);
            case 3 -> this.tank.packetDeserialize(input);
            case 4 -> readCurrent(input);
            case 5 -> readCurrentFluid(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
