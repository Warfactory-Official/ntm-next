// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.VersatileConfig;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuStorageDrum;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.MaterialShapeRoster;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemWasteLong;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityStorageDrum extends BlockEntityMachineBase
        implements FluidFlushSender, IGUIProvider, IFluidCopiable, SyncUnitSchema {

    public static final int SLOT_COUNT = 24;

    public static final int TANK_CAPACITY = 16_000;

    private static final double RADIATE_RANGE = 32D;
    private static final int CENSUS_PERIOD = 20;

    private static final float ACTIVATION_DECAY = 0.9965402628F;

    private static final int[] ALL_SLOTS = new int[SLOT_COUNT];

    static {
        for (int i = 0; i < SLOT_COUNT; i++) ALL_SLOTS[i] = i;
    }

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityStorageDrum(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASTE_STORAGE_DRUM.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.WASTEFLUID, TANK_CAPACITY);
        tanks[1] = new FluidTankNTM(NTMFluids.WASTEGAS, TANK_CAPACITY);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.storageDrum");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void tickServer() {
        double rad = 0D;
        int liquid = 0;
        int gas = 0;
        boolean census = TickPhase.every(this, CENSUS_PERIOD);
        int longChance = VersatileConfig.getLongDecayChance();
        int shortChance = VersatileConfig.getShortDecayChance();

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;

            if (census)
                rad += HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);

            if (ContaminationUtil.neutronActivateItem(stack, 0F, ACTIVATION_DECAY)) setChanged();

            Item item = stack.getItem();

            ItemWasteLong.WasteClass longClass = ModItems.NUCLEAR_WASTE_LONG.typeOf(item);
            if (longClass != null && roll(longChance)) {
                liquid += longClass.liquid;
                gas += longClass.gas;
                inventory.set(i, ModItems.NUCLEAR_WASTE_LONG_DEPLETED.stack(longClass));
            }

            longClass = ModItems.NUCLEAR_WASTE_LONG_TINY.typeOf(item);
            if (longClass != null && roll(longChance / 10)) {
                liquid += longClass.liquid / 10;
                gas += longClass.gas / 10;
                inventory.set(i, ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.stack(longClass));
            }

            ItemWasteShort.WasteClass shortClass = ModItems.NUCLEAR_WASTE_SHORT.typeOf(item);
            if (shortClass != null && roll(shortChance)) {
                ItemWasteShort.WasteClass yield = shortYield(shortClass);
                liquid += yield.liquid;
                gas += yield.gas;
                inventory.set(i, ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.stack(shortClass));
            }

            shortClass = ModItems.NUCLEAR_WASTE_SHORT_TINY.typeOf(item);
            if (shortClass != null && roll(shortChance / 10)) {
                ItemWasteShort.WasteClass yield = shortYield(shortClass);
                liquid += yield.liquid / 10;
                gas += yield.gas / 10;
                inventory.set(i, ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.stack(shortClass));
            }

            if (item == Decay.GOLD_198_INGOT && roll(shortChance / 20)) {
                inventory.set(i, new ItemStack(ModItems.NUGGET_MERCURY.get()));
            }
            if (item == Decay.GOLD_198_NUGGET && roll(shortChance / 100)) {
                inventory.set(i, new ItemStack(ModItems.NUGGET_MERCURY_TINY.get()));
            }

            if (item == Decay.LEAD_209_INGOT && roll(shortChance / 10)) {
                inventory.set(i, new ItemStack(Decay.BISMUTH_INGOT));
            }
            if (item == Decay.LEAD_209_NUGGET && roll(shortChance / 50)) {
                inventory.set(i, new ItemStack(Decay.BISMUTH_NUGGET));
            }

            if (item == ModItems.POWDER_SR90.get() && roll(shortChance / 10)) {
                inventory.set(i, new ItemStack(Decay.ZIRCONIUM_POWDER));
            }
            if (item == ModItems.NUGGET_SR90.get() && roll(shortChance / 50)) {
                inventory.set(i, new ItemStack(Decay.ZIRCONIUM_NUGGET));
            }
        }

        fill(tanks[0], liquid);
        fill(tanks[1], gas);

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);

        if (rad > 0D) {
            ContaminationUtil.radiate(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    RADIATE_RANGE,
                    rad,
                    ContaminationUtil.ContaminationType.CREATIVE);
        }
    }

    private boolean roll(int chance) {
        return chance > 0 && level.getRandom().nextInt(chance) == 0;
    }

    private static ItemWasteShort.WasteClass shortYield(ItemWasteShort.WasteClass type) {
        return ItemWasteShort.WasteClass.VALUES[
                type.ordinal() % ItemWasteLong.WasteClass.VALUES.length];
    }

    private static void fill(FluidTankNTM tank, int amount) {
        if (amount <= 0) return;
        tank.setFill(Math.min(tank.getFill() + amount, tank.getMaxFill()));
    }

    private static final class Decay {

        static final Item GOLD_198_INGOT = require(MaterialShapes.INGOT, Mats.MAT_AU198);
        static final Item GOLD_198_NUGGET = require(MaterialShapes.NUGGET, Mats.MAT_AU198);
        static final Item LEAD_209_INGOT = require(MaterialShapes.INGOT, Mats.MAT_PB209);
        static final Item LEAD_209_NUGGET = require(MaterialShapes.NUGGET, Mats.MAT_PB209);
        static final Item BISMUTH_INGOT = require(MaterialShapes.INGOT, Mats.MAT_BISMUTH);
        static final Item BISMUTH_NUGGET = require(MaterialShapes.NUGGET, Mats.MAT_BISMUTH);
        static final Item ZIRCONIUM_POWDER = require(MaterialShapes.DUST, Mats.MAT_ZIRCONIUM);
        static final Item ZIRCONIUM_NUGGET = require(MaterialShapes.NUGGET, Mats.MAT_ZIRCONIUM);

        private Decay() {}

        private static Item require(MaterialShapes shape, NTMMaterial mat) {
            Item item = MaterialShapeRoster.find(shape, mat);
            if (item == null) {
                throw new IllegalStateException(
                        "no " + shape.name() + " registered for " + mat.tagPath);
            }
            return item;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        if (ContaminationUtil.isContaminated(stack)) return true;
        Item item = stack.getItem();
        return ModItems.NUCLEAR_WASTE_LONG.typeOf(item) != null
                || ModItems.NUCLEAR_WASTE_LONG_TINY.typeOf(item) != null
                || ModItems.NUCLEAR_WASTE_SHORT.typeOf(item) != null
                || ModItems.NUCLEAR_WASTE_SHORT_TINY.typeOf(item) != null
                || item == Decay.GOLD_198_INGOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {

        if (ContaminationUtil.isContaminated(stack)) return false;
        Item item = stack.getItem();
        return ModItems.NUCLEAR_WASTE_LONG.typeOf(item) == null
                && ModItems.NUCLEAR_WASTE_LONG_TINY.typeOf(item) == null
                && ModItems.NUCLEAR_WASTE_SHORT.typeOf(item) == null
                && ModItems.NUCLEAR_WASTE_SHORT_TINY.typeOf(item) == null
                && item != Decay.GOLD_198_INGOT
                && item != Decay.GOLD_198_NUGGET
                && item != Decay.LEAD_209_INGOT
                && item != Decay.LEAD_209_NUGGET
                && item != ModItems.POWDER_SR90.get()
                && item != ModItems.NUGGET_SR90.get();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && tank.getPressure() == pressure) return tank.getFill();
        }
        return 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && tank.getPressure() == pressure) {
                tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                return;
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuStorageDrum(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("liquid").ifPresent(tanks[0]::deserialize);
        input.child("gas").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("liquid"));
        tanks[1].serialize(output.child("gas"));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
