// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuICFPress;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityICFPress extends BlockEntityMachineBase
        implements IFluidHandlerMK2, MenuProvider, IFluidCopiable, SyncUnitSchema {

    public static final int SLOT_EMPTY_PELLET = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_MUON_IN = 2;
    public static final int SLOT_MUON_OUT = 3;
    public static final int SLOT_SOLID_A = 4;
    public static final int SLOT_SOLID_B = 5;
    public static final int SLOT_FLUID_ID_A = 6;
    public static final int SLOT_FLUID_ID_B = 7;
    public static final int SLOT_COUNT = 8;

    public static final int maxMuon = 16;

    private static final int FLUID_PER_PELLET = 1000;
    private static final int[] TOP_BOTTOM = {0, 1, 2, 3, 4};
    private static final int[] SIDES = {0, 1, 2, 3, 5};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final boolean[] usedFluid = new boolean[2];

    @SyncField(units = 1L << 0)
    public int muon;

    public BlockEntityICFPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_PRESS.get(), pos, state, SLOT_COUNT);
        this.tanks[0] = new FluidTankNTM(NTMFluids.DEUTERIUM, 16_000);
        this.tanks[1] = new FluidTankNTM(NTMFluids.TRITIUM, 16_000);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineICFPress");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuICFPress(id, inv, this);
    }

    @Override
    public void tickServer() {

        tanks[0].setType(SLOT_FLUID_ID_A, SLOT_FLUID_ID_A, inventory);
        tanks[1].setType(SLOT_FLUID_ID_B, SLOT_FLUID_ID_B, inventory);

        if (muon <= 0 && inventory.get(SLOT_MUON_IN).is(ModItems.PARTICLE_MUON.get())) {

            ItemStackTemplate remainder = inventory.get(SLOT_MUON_IN).getCraftingRemainder();
            ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
            boolean canStore = false;

            if (container.isEmpty()) {
                canStore = true;
            } else if (inventory.get(SLOT_MUON_OUT).isEmpty()) {
                inventory.set(SLOT_MUON_OUT, container.copy());
                canStore = true;
            } else if (ItemStack.isSameItemSameComponents(inventory.get(SLOT_MUON_OUT), container)
                    && inventory.get(SLOT_MUON_OUT).getCount()
                            < inventory.get(SLOT_MUON_OUT).getMaxStackSize()) {
                inventory.get(SLOT_MUON_OUT).grow(1);
                canStore = true;
            }

            if (canStore) {
                this.muon = maxMuon;
                removeItem(SLOT_MUON_IN, 1);
                setChanged();
            }
        }

        press();
        this.networkPackNT(15);
    }

    public void press() {
        if (!inventory.get(SLOT_EMPTY_PELLET).is(ModItems.ICF_PELLET_EMPTY.get())) return;
        if (!inventory.get(SLOT_OUTPUT).isEmpty()) return;

        ItemICFPellet.init();

        EnumICFFuel fuel1 = getFuel(tanks[0], inventory.get(SLOT_SOLID_A), 0);
        EnumICFFuel fuel2 = getFuel(tanks[1], inventory.get(SLOT_SOLID_B), 1);

        if (fuel1 == null || fuel2 == null || fuel1 == fuel2) return;

        inventory.set(
                SLOT_OUTPUT,
                ItemICFPellet.setup(new ItemStack(ModItems.ICF_PELLET), fuel1, fuel2, muon > 0));

        if (muon > 0) muon--;

        removeItem(SLOT_EMPTY_PELLET, 1);
        if (usedFluid[0]) tanks[0].setFill(tanks[0].getFill() - FLUID_PER_PELLET);
        else removeItem(SLOT_SOLID_A, 1);
        if (usedFluid[1]) tanks[1].setFill(tanks[1].getFill() - FLUID_PER_PELLET);
        else removeItem(SLOT_SOLID_B, 1);

        setChanged();
    }

    public @Nullable EnumICFFuel getFuel(FluidTankNTM tank, ItemStack slot, int index) {
        usedFluid[index] = false;
        Fluid type = tank.getTankType();
        if (tank.getFill() >= FLUID_PER_PELLET
                && type != null
                && ItemICFPellet.fluidMap.containsKey(type)) {
            usedFluid[index] = true;
            return ItemICFPellet.fluidMap.get(type);
        }
        if (slot.isEmpty()) return null;
        List<MaterialStack> mats = Mats.getMaterialsFromItem(slot);

        if (mats == null || mats.size() != 1) return null;
        MaterialStack mat = mats.getFirst();
        if (mat.amount != MaterialShapes.INGOT.q(1)) return null;
        return ItemICFPellet.materialMap.get(mat.material);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.is(ModItems.ICF_PELLET_EMPTY.get())) return slot == SLOT_EMPTY_PELLET;
        if (stack.is(ModItems.PARTICLE_MUON.get())) return slot == SLOT_MUON_IN;
        return slot == SLOT_SOLID_A || slot == SLOT_SOLID_B;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN || side == Direction.UP ? TOP_BOTTOM : SIDES;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || slot == SLOT_MUON_OUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tanks[0].deserialize(input.childOrEmpty("t0"));
        tanks[1].deserialize(input.childOrEmpty("t1"));
        this.muon = input.getByteOr("muon", (byte) 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        output.putByte("muon", (byte) muon);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        FluidTankNTM tank = tankFor(type, pressure);
        return tank == null ? 0L : (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        FluidTankNTM tank = tankFor(type, pressure);
        if (tank == null) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    private @Nullable FluidTankNTM tankFor(Fluid type, int pressure) {
        for (FluidTankNTM tank : tanks) {
            if (tank.accepts(type) && tank.getPressure() == pressure) return tank;
        }
        return null;
    }

    private void writeMuon(ByteBuf output) {
        output.writeByte(muon);
    }

    private void readMuon(ByteBuf input) {
        muon = input.readByte();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeMuon(output);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readMuon(input);
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
